package com.fengtao.device.central.service.ble;

import android.app.PendingIntent;
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
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


import com.fengtao.device.central.APP;
import com.fengtao.device.central.util.DateUtil;

import java.util.Arrays;
import java.util.UUID;

import static com.fengtao.device.central.util.Constant.UUID_CHAR_READ_NOTIFY;
import static com.fengtao.device.central.util.Constant.UUID_CHAR_WRITE;
import static com.fengtao.device.central.util.Constant.UUID_DESC_NOTITY;
import static com.fengtao.device.central.util.Constant.UUID_SERVICE;

public class BleBindService extends Service {

    private static final String TAG = BleBindService.class.getSimpleName();

    private UUID[] uuids = new UUID[]{UUID_SERVICE};
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;
    private BluetoothLeScanner bluetoothLeScanner;
    public boolean isScanning;
    private final IBinder binder = new ValueChangeBinder();
    private String writeBackStr;//写会服务端的数据
    private BluetoothAdapter bluetoothAdapter;
    private OnMsgChangeListener listener;
    private PendingIntent callbackIntent;

    public class ValueChangeBinder extends Binder {

        public BleBindService getService() {
            return BleBindService.this;
        }
    }

    public void setMsgChangeListener(OnMsgChangeListener listener) {
        this.listener = listener;
    }

    public interface OnMsgChangeListener {

        void onChange(String msg);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

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
//                scanBle();
            }
            if (listener != null) {
                listener.onChange(DateUtil.getCurrDateStr() + ":" + String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), dev));
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
                    if (listener != null) {
                        listener.onChange("发现服务" + allUUIDs);
                    }
                }
                read();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Integer readVal = Integer.valueOf(valueStr);
            //从服务端读取的数据
            String readStr = String.valueOf(readVal);
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, readStr, status));
            if (listener != null) {
                listener.onChange(DateUtil.getCurrDateStr() + ":" + "读取Characteristic[" + uuid + "]:\n" + readStr);
            }
            readStr = String.valueOf(++readVal);
            writeBackStr = readStr;
            write();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Integer writeVal = Integer.valueOf(writeBackStr);
            writeVal++;
            writeBackStr = String.valueOf(writeVal);
            Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, writeVal, status));
            if (listener != null) {
                listener.onChange(DateUtil.getCurrDateStr() + ":" + "写入Characteristic[" + uuid + "]:\n" + valueStr);
            }
            read();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicChanged:%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr));
            if (listener != null) {
                listener.onChange(DateUtil.getCurrDateStr() + ":" + "通知Characteristic[" + uuid + "]:\n" + valueStr);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            if (listener != null) {
                listener.onChange(DateUtil.getCurrDateStr() + ":" + "读取Descriptor[" + uuid + "]:\n" + valueStr);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            if (listener != null) {
                listener.onChange(DateUtil.getCurrDateStr() + ":" + "写入Descriptor[" + uuid + "]:\n" + valueStr);
            }
        }
    };


    private final ScanCallback mScanCallback = new ScanCallback() {// 扫描Callback
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
//            if (result.getDevice().getName() != null) {
            //此处连接设备的标志，作为服务端的标志
//                if (result.getDevice().getName().contains("mi") || result.getDevice().getName().contains("OPPO")) {
            mBluetoothGatt = result.getDevice().connectGatt(BleBindService.this, false, mBluetoothGattCallback);
            bluetoothLeScanner.stopScan(mScanCallback);
//                }
//            }
        }
    };

    public void scanBle() {
        isScanning = true;
        // Android5.0新增的扫描API，扫描返回的结果更友好，比如BLE广播数据以前是byte[] scanRecord，而新API帮我们解析成ScanRecord类
        bluetoothLeScanner.startScan(mScanCallback);
//        bluetoothAdapter.startLeScan(uuids, scanCallback);

    }

    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getName() != null) {
                //此处连接设备的标志，作为服务端的标志
                if (device.getName().contains("mi") || device.getName().contains("OPPO")) {
                    mBluetoothGatt = device.connectGatt(BleBindService.this, false, mBluetoothGattCallback);
                    bluetoothLeScanner.stopScan(mScanCallback);
                }
            }
        }
    };

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
