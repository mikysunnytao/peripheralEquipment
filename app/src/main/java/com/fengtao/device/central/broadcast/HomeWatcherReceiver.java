package com.fengtao.device.central.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fengtao.device.central.service.ble.BleBackgroundService;

/**
 * 监听Home键事件，通过广播获取Home键事件
 */
public class HomeWatcherReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "HomeReceiver";
    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
    private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "onReceive: action: " + action);
            if (action != null) {
                if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                    // android.intent.action.CLOSE_SYSTEM_DIALOGS
                    String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                    Log.i(LOG_TAG, "reason: " + reason);

                    if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                        // 短按Home键
                        Log.i(LOG_TAG, "homekey");
                        context.startForegroundService(new Intent(context, BleBackgroundService.class));

                    } else if (SYSTEM_DIALOG_REASON_LOCK.equals(reason)) {
                        // 锁屏
                        Log.i(LOG_TAG, "lock");
                        context.startForegroundService(new Intent(context, BleBackgroundService.class));
                    }
                }
            }

        }
    }

}