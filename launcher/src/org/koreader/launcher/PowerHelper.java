package org.koreader.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;


public class PowerHelper {
    private static final String WAKELOCK_ID = "ko-wakelock";

    private String tag;
    private Context context;
    private IntentFilter filter;
    private PowerManager.WakeLock wakelock;
    private boolean isWakeLockAllowed = false;

    public PowerHelper(Context context) {
        /* use application context */
        this.context = context.getApplicationContext();
        this.filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.tag = context.getResources().getString(R.string.app_name);
    }

    public int batteryPercent() {
        return getBatteryState(true);
    }

    public int batteryCharging() {
        return getBatteryState(false);
    }

    public void setWakelock(final boolean enable) {
        if (enable) {
            wakelockAcquire();
        } else {
            wakelockRelease();
        }
    }

    public void setWakelockState(final boolean enabled) {
        /** release wakelock first, if present and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock != null) wakelockRelease();
        /** update wakelock settings */
        isWakeLockAllowed = enabled;
        /** acquire wakelock if we don't have one and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock == null) wakelockAcquire();
    }

    private int getBatteryState(boolean isPercent) {
        Intent intent = context.registerReceiver(null, filter);
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
    private void wakelockAcquire() {
        if (isWakeLockAllowed) {
            wakelockRelease();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, WAKELOCK_ID);
            Logger.d(tag, "wakelock: acquiring " + WAKELOCK_ID);
            wakelock.acquire();
        }
    }

    private void wakelockRelease() {
        if (isWakeLockAllowed && wakelock != null) {
            Logger.d(tag, "wakelock: releasing " + WAKELOCK_ID);
            wakelock.release();
            wakelock = null;
        }
    }

}
