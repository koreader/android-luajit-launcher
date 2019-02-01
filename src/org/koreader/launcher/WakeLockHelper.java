package org.koreader.launcher;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

/* helper class to manage wakelocks.
 *
 * Used to keep CPU, display and touchscreen
 * idle for a long time without sleep. */

@SuppressWarnings("deprecation")
public abstract class WakeLockHelper {
    private static PowerManager.WakeLock wakelock;
    private static final String LOGGER_NAME = "luajit-launcher";

    // disable wakelocks by default.
    private static boolean isWakeLockAllowed = false;

    public static void acquire(Context context) {
        if (isWakeLockAllowed) {
            release();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ko-wakelock");
            Log.v(LOGGER_NAME, "Acquiring wakelock");
            wakelock.acquire();
        }
    }

    public static void release() {
        if (isWakeLockAllowed && wakelock != null) {
            Log.v(LOGGER_NAME, "Releasing wakelock");
            wakelock.release();
            wakelock = null;
        }
    }

    // method to turn on/off wakelock support
    public static void set(boolean enabled) {
        if (isWakeLockAllowed && wakelock != null) release();
        isWakeLockAllowed = enabled;
    }
}
