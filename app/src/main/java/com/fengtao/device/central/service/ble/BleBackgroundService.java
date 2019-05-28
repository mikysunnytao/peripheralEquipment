package com.fengtao.device.central.service.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.fengtao.device.central.APP;
import com.fengtao.device.central.R;
import com.fengtao.device.central.activity.BleCentralActivity;
import com.fengtao.device.central.broadcast.BluetoothReceiver;
import com.fengtao.device.central.util.DateUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * 后台服务进程
 */
public class BleBackgroundService extends Service {


    public static final int FLAG = 0x110;

    private static final String TAG = BleBackgroundService.class.getSimpleName();
    public static final UUID UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000000"); //自定义UUID
    public static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000");
    public static final UUID UUID_DESC_NOTITY = UUID.fromString("11100000-0000-0000-0000-000000000000");
    public static final UUID UUID_CHAR_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000");
    private UUID [] uuids = new UUID[]{UUID_SERVICE};
    public static final String CLOSE_TAG = "com.hryt.service.stopself";
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    public boolean isScanning;
    private long lastScreentActionTime;
    private String  readStr;//从服务端读取的数据
    private String writeBackStr;//写会服务端的数据

    private NotificationManager notifManager;
    private  PendingIntent callbackIntent;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        BluetoothReceiver bluetoothReceiver = new BluetoothReceiver();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        filter.addAction(CLOSE_TAG);
        notification("蓝牙运行中...");
        callbackIntent = PendingIntent.getForegroundService(
                this,
                1,
                new Intent("com.fengtao.receiver.BleService").setPackage(getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT);

        onOpen();
        registerReceiver(receiver,filter);
        IntentFilter statusFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, statusFilter);

        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceive, intentFilter);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获取返回的错误码
        int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1);//ScanSettings.SCAN_FAILED_*
        //获取到的蓝牙设备的回调类型
        int callbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1);//ScanSettings.CALLBACK_TYPE_*
        if (errorCode == -1) {
            //扫描到蓝牙设备信息
            List<ScanResult> scanResults = (List<ScanResult>) intent.getSerializableExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
            if (scanResults != null) {
                for (ScanResult result : scanResults){
                    mBluetoothGatt = result.getDevice().connectGatt(this,false,mBluetoothGattCallback);
                    Log.i("Wakeup", "onScanResult2: name: " + result.getDevice().getName() +
                            ", address: " + result.getDevice().getAddress() +
                            ", rssi: " + result.getRssi() + ", scanRecord: " + result.getScanRecord());
                }
            }
        } else {
            //此处为扫描失败的错误处理

        }
        return START_STICKY;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CLOSE_TAG)){
                stopSelf();
            }
        }
    };
    private BroadcastReceiver mScreenReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(Intent.ACTION_SCREEN_ON)){
                awakeSysyem();
            }else if(action.equals(Intent.ACTION_SCREEN_OFF)){
                awakeSysyem();
            }else if(action.equals(Intent.ACTION_USER_PRESENT)){

            }
        }
    };

    public void notification(String aMessage) {
        final int NOTIFY_ID = 1003;
        String name = "IBC_SERVICE_CHANNEL";
        String id = "IBC_SERVICE_CHANNEL_1"; // The user-visible name of the channel.
        String description = "IBC_SERVICE_CHANNEL_SHOW"; // The user-visible description of the channel.

        Intent intent;
        PendingIntent pendingIntent;
        android.support.v4.app.NotificationCompat.Builder builder;

        if (notifManager == null) {
            notifManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, importance);
                mChannel.setDescription(description);
                mChannel.enableVibration(false);
                mChannel.enableLights(false);
                //mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                notifManager.createNotificationChannel(mChannel);
            }
            builder = new android.support.v4.app.NotificationCompat.Builder(this);
            intent = new Intent(this, BleCentralActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            builder.setContentTitle(aMessage)  // required
                    .setSmallIcon(R.mipmap.ic_launcher) // required
                    .setContentText(this.getString(R.string.app_name))  // required
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setChannelId(id)
                    .setTicker(aMessage);
            builder.build().sound = null;
            builder.build().vibrate = null;
        } else {
            builder = new NotificationCompat.Builder(this);
            intent = new Intent(this, BleCentralActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            builder.setContentTitle(aMessage)                           // required
                    .setSmallIcon(R.mipmap.ic_launcher) // required
                    .setContentText(this.getString(R.string.app_name))  // required
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(aMessage)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVibrate(new long[]{0L});
        } // else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification notification = builder.build();
        notification.sound = null;
        notification.vibrate = null;
        startForeground(NOTIFY_ID, notification);
        //notifManager.notify(NOTIFY_ID, notification);
    }

    private void awakeSysyem(){
        long current = System.currentTimeMillis();
        if((current - lastScreentActionTime) / 1000 >= DateUtil.ONE_MINUTE * 15){
            onClose();
            onOpen();
        }
        lastScreentActionTime = current;
    }

    public void onOpen(){
        //BluetoothManager是向蓝牙设备通讯的入口
        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        //指定需要识别到的蓝牙设备
        List<ScanFilter> scanFilterList = new ArrayList<>();

        ScanFilter.Builder oppoBuilder = new ScanFilter.Builder();
        oppoBuilder.setDeviceName("OPPO R15");//你要扫描的设备的名称，如果使用lightble这个app来模拟蓝牙可以直接设置name
        ScanFilter oppoFilter = oppoBuilder.build();
        ScanFilter.Builder miBuilder = new ScanFilter.Builder();
        miBuilder.setDeviceName("mi");
        ScanFilter miFilter = miBuilder.build();
//        scanFilterList.add(scanFilter);
        scanFilterList.add(oppoFilter);
        scanFilterList.add(miFilter);

        //指定蓝牙的方式，这里设置的ScanSettings.SCAN_MODE_LOW_LATENCY是比较高频率的扫描方式
        ScanSettings.Builder settingBuilder = new ScanSettings.Builder();
        settingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        settingBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        settingBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        settingBuilder.setLegacy(true);
        ScanSettings settings = settingBuilder.build();


        //启动蓝牙扫描
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilterList,settings,callbackIntent);
        // bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilterList,settings,mScanCallback);
    }

    public void onClose(){
        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        bluetoothAdapter.getBluetoothLeScanner().stopScan(callbackIntent);
        //bluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        startForegroundService(new Intent(this,BleBackgroundService.class));
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
//                scanBle();
                onOpen();
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
//            Timer timer = new Timer();
//            TimerTask task = new TimerTask() {
//                @Override
//                public void run() {
                    read();
//                }
//            };
//            timer.schedule(task,1000);

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
//        bluetoothLeScanner.startScan(mScanCallback);
        bluetoothAdapter.startLeScan(uuids,callback);
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                bluetoothLeScanner.stopScan(mScanCallback); //停止扫描
//                isScanning = false;
//            }
//        }, 3000);
    }

    private BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            mBluetoothGatt = device.connectGatt(BleBackgroundService.this, false, mBluetoothGattCallback);
            bluetoothLeScanner.stopScan(mScanCallback);
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
//            APP.toast("没有连接", 0);
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(uuid);
        if (service == null)
            APP.toast("没有找到服务UUID=" + uuid, 0);
        return service;
    }
}
