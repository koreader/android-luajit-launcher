package org.koreader.launcher.helper;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;

import org.koreader.launcher.Logger;


public class PowerHelper {
    private static final String WAKELOCK_ID = "wakelock:screen_bright";

    private final Context context;
    private final IntentFilter filter;
    private final String tag;

    private PowerManager.WakeLock wakelock;
    private boolean isWakeLockAllowed = false;

    public PowerHelper(Context context) {
        this.context = context.getApplicationContext();
        this.filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.tag = this.getClass().getSimpleName();
        Logger.d(tag, "Starting");
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
        /* release wakelock first, if present and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock != null) wakelockRelease();
        /* update wakelock settings */
        isWakeLockAllowed = enabled;
        /* acquire wakelock if we don't have one and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock == null) wakelockAcquire();
    }

    private int getBatteryState(boolean isPercent) {
        Intent intent = context.registerReceiver(null, filter);

        if (intent != null) {
            final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            final int percent = (level * 100) / scale;

            if (isPercent) {
                return percent;
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                       plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                return (percent != 100) ? 1 : 0;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private void wakelockAcquire() {
        if (isWakeLockAllowed) {
            wakelockRelease();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, WAKELOCK_ID);
            Logger.v(tag, "acquiring " + WAKELOCK_ID);
            // release the wakelock after 30 minutes running in the foreground without inputs.
            // it will be acquired again on the next resume callback.
            wakelock.acquire( 30 * 60 * 1000);
        }
    }

    private void wakelockRelease() {
        if (isWakeLockAllowed && wakelock != null) {
            Logger.v(tag, "releasing " + WAKELOCK_ID);
            wakelock.release();
            wakelock = null;
        }
    }
}
