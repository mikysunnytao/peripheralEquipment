package com.fengtao.device.central.util;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

public class BluetoothUtils {

    public static void onOpen(Context context){

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            //BluetoothManager是向蓝牙设备通讯的入口
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            //指定需要识别到的蓝牙设备
            List<ScanFilter> scanFilterList = new ArrayList<>();
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setServiceUuid(ParcelUuid.fromString(Constant.UUID_SERVICE.toString()));
            ScanFilter filter = builder.build();
            scanFilterList.add(filter);
            //指定蓝牙的方式，这里设置的ScanSettings.SCAN_MODE_LOW_LATENCY是比较高频率的扫描方式
            ScanSettings.Builder settingBuilder = new ScanSettings.Builder();
            settingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            settingBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
            settingBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            settingBuilder.setLegacy(true);
            ScanSettings settings = settingBuilder.build();

            //指定扫描到蓝牙后是以什么方式通知到app端，这里将以可见服务的形式进行启动
            PendingIntent callbackIntent = PendingIntent.getForegroundService(
                    context,
                    1,
                    new Intent("com.fengtao.device.central.service.BleService").setPackage(context.getPackageName()),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilterList, settings, callbackIntent);
        }
    }


}
