package com.fengtao.device.peripheralequipment.service.guard;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class RemoteService extends Service {
    private MyBinder mBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IMyAidlInterface iMyAidlInterface = IMyAidlInterface.Stub.asInterface(service);
            try {
                Log.i("RemoteService", "connected with " + iMyAidlInterface.getServiceName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(RemoteService.this,"链接断开，重新启动 LocalService",Toast.LENGTH_LONG).show();
            startService(new Intent(RemoteService.this,LocalService.class));
            bindService(new Intent(RemoteService.this,LocalService.class),connection, Context.BIND_IMPORTANT);
        }
    };

    public RemoteService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"RemoteService 启动",Toast.LENGTH_LONG).show();
        bindService(new Intent(this,LocalService.class),connection,Context.BIND_IMPORTANT);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBinder = new MyBinder();
        return mBinder;
    }

    private class MyBinder extends IMyAidlInterface.Stub{

        @Override
        public String getServiceName() throws RemoteException {
            return RemoteService.class.getName();
        }

    }
}
