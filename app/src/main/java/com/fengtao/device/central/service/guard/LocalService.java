package com.fengtao.device.central.service.guard;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class LocalService extends Service {
    public static final String TAG = LocalService.class.getSimpleName();

    private MyBinder myBinder;
    @Override
    public IBinder onBind(Intent intent) {
        myBinder = new MyBinder();
        return myBinder;
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IMyAidlInterface aidlInterface = IMyAidlInterface.Stub.asInterface(service);
            try {
                Log.i(TAG,"connect with "+aidlInterface.getServiceName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(LocalService.this,"链接断开，重新启动 RemoteService",Toast.LENGTH_LONG).show();
            startService(new Intent(LocalService.this,RemoteService.class));
            bindService(new Intent(LocalService.this,RemoteService.class),connection, Context.BIND_IMPORTANT);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"LocalService 启动",Toast.LENGTH_LONG).show();
        startService(new Intent(LocalService.this,RemoteService.class));
        bindService(new Intent(this,RemoteService.class),connection, Context.BIND_IMPORTANT);
        return START_STICKY;
    }

    private class MyBinder extends IMyAidlInterface.Stub{

        @Override
        public String getServiceName() throws RemoteException {
            return LocalService.class.getName();
        }
    }
}
