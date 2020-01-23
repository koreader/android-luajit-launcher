package org.koreader.service;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/* This class implements a service, intended to be used as an (opt-in) extension
   It is exported to apps that declare a permission, see manifest/service.xml

   NOTE1: Build with a low api level (19) to avoid boilerplate code for runtime permissions.
   NOTE2: Keep this module in java, to avoid bloating the apk with kotlin runtime.
*/

public class Service extends android.app.Service {
    private Api binder;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.i("onCreate");
        binder = new Api(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        Logger.i("onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.i("onBind -> client: " + Binder.getCallingPid());
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        Logger.i("onUnbind");
        binder.destroy();
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i("starting service");
        return super.onStartCommand(intent, flags, startId);
    }
}
