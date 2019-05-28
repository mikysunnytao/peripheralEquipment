package com.fengtao.device.central.broadcast;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fengtao.device.central.util.Constant;

public class BluetoothReceiver extends BroadcastReceiver {
    private String TAG = Constant.getTag(BluetoothReceiver.class);
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent!=null) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "onReceive---------蓝牙正在打开中");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        context.sendBroadcast(new Intent());
                        Log.e(TAG, "onReceive---------蓝牙已经打开");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e(TAG, "onReceive---------蓝牙正在关闭中");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "onReceive---------蓝牙已经关闭");
                        break;

                }
            }
        }
    }
}
