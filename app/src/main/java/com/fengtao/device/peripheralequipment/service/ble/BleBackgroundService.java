package com.fengtao.device.peripheralequipment.service.ble;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.fengtao.device.peripheralequipment.APP;
import com.fengtao.device.peripheralequipment.activity.BleClientActivity;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * 后台服务进程
 */
public class BleBackgroundService extends Service {


    public static final int FLAG = 0x110;

    private static final String TAG = BleClientActivity.class.getSimpleName();
    public static final UUID UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000000"); //自定义UUID
    public static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000");
    public static final UUID UUID_DESC_NOTITY = UUID.fromString("11100000-0000-0000-0000-000000000000");
    public static final UUID UUID_CHAR_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000");

    public static final String CLOSE_TAG = "com.hryt.service.stopself";
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    public boolean isScanning;
    private String  readStr;//从服务端读取的数据
    private String writeBackStr;//写会服务端的数据

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CLOSE_TAG);
        registerReceiver(receiver,filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this).build();
        startForeground(FLAG, notification);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanBle();
        return START_NOT_STICKY;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CLOSE_TAG)){
                stopSelf();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 与服务端连接的Callback
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice dev = gatt.getDevice();
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", dev.getName(), dev.getAddress(), status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                gatt.discoverServices(); //启动服务发现

            } else {
                isConnected = false;
                scanBle();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, String.format("onServicesDiscovered:%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            if (status == BluetoothGatt.GATT_SUCCESS) { //BLE服务发现成功
                // 遍历获取BLE服务Services/Characteristics/Descriptors的全部UUID
                for (BluetoothGattService service : gatt.getServices()) {
                    StringBuilder allUUIDs = new StringBuilder("UUIDs={\nS=" + service.getUuid().toString());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        allUUIDs.append(",\nC=").append(characteristic.getUuid());
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors())
                            allUUIDs.append(",\nD=").append(descriptor.getUuid());
                    }
                    allUUIDs.append("}");
                    Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());

//                    logTv("发现服务" + allUUIDs);
                }
                read();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Integer readVal = Integer.valueOf(valueStr);
            readStr = String.valueOf(readVal);
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, readStr, status));

//            logTv("读取Characteristic[" + uuid + "]:\n" + readStr);
            readStr = String.valueOf(++readVal);
            writeBackStr = readStr;
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    write();
                }
            };
            timer.schedule(task,1000);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Integer writeVal  = Integer.valueOf(writeBackStr);
            writeVal++;
            writeBackStr = String.valueOf(writeVal);
            Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, writeVal, status));

//            logTv("写入Characteristic[" + uuid + "]:\n" + valueStr);
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    read();
                }
            };
            timer.schedule(task,1000);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicChanged:%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr));

//            logTv("通知Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));

//            logTv("读取Descriptor[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));

//            logTv("写入Descriptor[" + uuid + "]:\n" + valueStr);
        }
    };


    private final ScanCallback mScanCallback = new ScanCallback() {// 扫描Callback
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
//            BleDevAdapter.BleDev dev = new BleDevAdapter.BleDev(result.getDevice(), result);
            if (result.getDevice().getName() != null) {
                if (result.getDevice().getName().contains("mi") || result.getDevice().getName().contains("OPPO")) {
                    mBluetoothGatt = result.getDevice().connectGatt(BleBackgroundService.this, false, mBluetoothGattCallback);
                    bluetoothLeScanner.stopScan(mScanCallback);
                }
            }
        }
    };

    public void scanBle() {
        isScanning = true;
//        BluetoothAdapter bluetoothAdapter = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE).getDefaultAdapter();
        // Android5.0新增的扫描API，扫描返回的结果更友好，比如BLE广播数据以前是byte[] scanRecord，而新API帮我们解析成ScanRecord类
        bluetoothLeScanner.startScan(mScanCallback);
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                bluetoothLeScanner.stopScan(mScanCallback); //停止扫描
//                isScanning = false;
//            }
//        }, 3000);
    }

    // BLE中心设备连接外围设备的数量有限(大概2~7个)，在建立新连接之前必须释放旧连接资源，否则容易出现连接错误133
    public void closeConn() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }

    public void read() {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY);//通过UUID获取可读的Characteristic
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }
    public void write() {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);//通过UUID获取可写的Characteristic
            characteristic.setValue(writeBackStr); //单次最多20个字节
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    // 设置通知Characteristic变化会回调->onCharacteristicChanged()
    public void setNotify() {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            // 设置Characteristic通知
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY);//通过UUID获取可通知的Characteristic
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);

            // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESC_NOTITY);
            // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);//和通知类似,但服务端不主动发数据,只指示客户端读取数据
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }


    // 获取Gatt服务
    private BluetoothGattService getGattService(UUID uuid) {
        if (!isConnected) {
            APP.toast("没有连接", 0);
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(uuid);
        if (service == null)
            APP.toast("没有找到服务UUID=" + uuid, 0);
        return service;
    }
}
