package com.fengtao.device.central.service.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.Service;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.fengtao.device.central.R;

import java.util.List;

public class BleService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new Notification.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground)).build();
        startForeground(110, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        //获取返回的错误码
        int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1);
        //获取到的扫描的蓝牙设备的回调类型
        int callback = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1);
        if (errorCode == -1) {
            List<ScanResult> results = (List<ScanResult>) intent.getSerializableExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
            if (results != null) {
                for (ScanResult result : results) {
                    String address = result.getDevice().getAddress();
                    Log.i("address","device address"+address);
                }
            }
        }

        return START_NOT_STICKY;
    }
}
