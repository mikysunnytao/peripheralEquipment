package com.fengtao.device.peripheralequipment.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.fengtao.device.peripheralequipment.APP;
import com.fengtao.device.peripheralequipment.R;
import com.fengtao.device.peripheralequipment.service.BleBindService;


/**
 * BLE客户端(主机/中心设备/Central)
 */
public class BleClientActivity extends Activity {

    private EditText mWriteET;
    private TextView mTips;
    public static final String TAG = BleClientActivity.class.getSimpleName();
    private boolean isBind;
    private BleBindService bindService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleclient);
        mWriteET = findViewById(R.id.et_write);
        mTips = findViewById(R.id.tv_tips);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            APP.toast("本机没有找到蓝牙硬件或驱动！", 0);
            finish();
            return;
        } else {
            if (!adapter.isEnabled()) {
                //直接开启蓝牙
                adapter.enable();
                //跳转到设置界面
            }
        }

        // 检查是否支持BLE蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            APP.toast("本机不支持低功耗蓝牙！", 0);
            finish();
            return;
        }

        // Android 6.0动态请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE
                    , Manifest.permission.READ_EXTERNAL_STORAGE
                    , Manifest.permission.ACCESS_COARSE_LOCATION};
            for (String str : permissions) {
                if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, 111);
                    break;
                }
            }
        }else {
            bindBleService();
        }

    }

    private void bindBleService(){
        Intent service = new Intent(this, BleBindService.class);
        bindService(service, connection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBind = true;
            Log.i(TAG,"service connected");
            BleBindService.ValueChangeBinder binder = (BleBindService.ValueChangeBinder) service;
            bindService = binder.getService();
            bindService.scanBle();

            bindService.setMsgChangeListener(new BleBindService.OnMsgChangeListener() {
                @Override
                public void onChange(String msg) {
                    logTv(msg);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"service disconnected");
            isBind = false;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (isBind) {
            bindService.closeConn();
            unbindService(connection);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 111) {
            if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                bindBleService();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        closeConn();
    }


    // 扫描BLE
//    public void reScan(View view) {
//        scanBle();
//    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 读取数据成功会回调->onCharacteristicChanged()
//    public void read(View view) {
//        BluetoothGattService service = getGattService(UUID_SERVICE);
//        if (service != null) {
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY);//通过UUID获取可读的Characteristic
//            mBluetoothGatt.readCharacteristic(characteristic);
//        }
//    }

    // 注意：连续频繁读写数据容易失败，读写操作间隔最好200ms以上，或等待上次回调完成后再进行下次读写操作！
    // 写入数据成功会回调->onCharacteristicWrite()
//    public void write(View view) {
//        BluetoothGattService service = getGattService(UUID_SERVICE);
//        if (service != null) {
//            String text = mWriteET.getText().toString();
//
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);//通过UUID获取可写的Characteristic
//            characteristic.setValue(text); //单次最多20个字节
//            mBluetoothGatt.writeCharacteristic(characteristic);
//        }
//    }


    public void clearConsole(View view) {
        mTips.setText("");

    }


    // 输出日志
    private void logTv(final String msg) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                APP.toast(msg, 0);
                mTips.setText(msg);
            }
        });
    }
}