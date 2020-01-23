package org.koreader.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

/* start the service on boot or after apk install/update ¿?, unused */

public class BootReceiver extends android.content.BroadcastReceiver {
    public void onReceive(Context context, Intent i) {
        StringBuilder sb = new StringBuilder("onBootReceive() -> starting ");
        Intent intent = new Intent(context, Service.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sb.append("foreground service");
            context.startForegroundService(intent);
        } else {
            sb.append("service");
            context.startService(intent);
        }
        Logger.v(sb.toString());
    }
}
