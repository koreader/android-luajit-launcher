package org.koreader.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

public class PowerHelper {
    private static String TAG;
    private static Context context;
    private static IntentFilter filter;
    private static Intent intent;

    private int percent;
    private int charging;

    public PowerHelper(Context context, String logger_name) {
        this.context = context;
        this.TAG = logger_name;
        this.filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Log.v(logger_name, "power helper class started");
    }

    public int batteryPercent() {
        return getBatteryState(true);
    }

    public int batteryCharging() {
        return getBatteryState(false);
    }

    public static void setWakelock(final boolean enable) {
        if (enable) {
            wakelockAcquire();
        } else {
            wakelockRelease();
        }
    }

    public static void setWakelockState(final boolean enabled) {
        // release current wakelock if allowed & acquired
        if (isWakeLockAllowed && wakelock != null) {
            setWakelock(false);
        }

        isWakeLockAllowed = enabled;

        // acquire wakelock if allowed
        if (isWakeLockAllowed && wakelock == null) {
            setWakelock(true);
        }
    }

    public void setWifi(final boolean enabled) {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWifiManager().setWifiEnabled(enabled);
            }
        });
    }

    public int wifiEnabled() {
        return getWifiManager().isWifiEnabled() ? 1 : 0;
    }

    private WifiManager getWifiManager() {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    private int getBatteryState(boolean isPercent) {
        intent = context.registerReceiver(null, filter);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int percent = (level * 100) / scale;

        if (isPercent) return percent;

        if (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB)
            return (percent != 100) ? 1 : 0;
        else
            return 0;
    }

    private static void wakelockAcquire() {
        if (isWakeLockAllowed) {
            wakelockRelease();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ko-wakelock");
            Log.v(TAG, "Acquiring wakelock");
            wakelock.acquire();
        }
    }

    private static void wakelockRelease() {
        if (isWakeLockAllowed && wakelock != null) {
            Log.v(TAG, "Releasing wakelock");
            wakelock.release();
            wakelock = null;
        }
    }

}
