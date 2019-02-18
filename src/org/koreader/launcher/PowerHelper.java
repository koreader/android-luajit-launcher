package org.koreader.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;


public class PowerHelper {
    private static String TAG;
    private static Context context;
    private static Intent intent;
    private static IntentFilter filter;
    private static PowerManager.WakeLock wakelock;
    private static boolean isWakeLockAllowed = false;

    public PowerHelper(Context context, String logger_name) {
        this.context = context;
        this.TAG = logger_name;
        this.filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
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
        /** release wakelock first, if present and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock != null) wakelockRelease();
        /** update wakelock settings */
        isWakeLockAllowed = enabled;
        /** acquire wakelock if we don't have one and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock == null) wakelockAcquire();
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

    @SuppressWarnings("deprecation")
    private static void wakelockAcquire() {
        if (isWakeLockAllowed) {
            wakelockRelease();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "ko-wakelock");
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
