/* generic EPD Controller for Android devices
 *
 * interface for boyue mimas and maybe others,
 * based on https://github.com/koreader/koreader/issues/4595 */

package org.koreader.launcher.device.rockchip;

import java.util.Locale;
import android.util.Log;


/* This abstract class is copied and used on einkTest.
   Suppress some warnings to avoid distractions */

@SuppressWarnings ({
        "JavaReflectionMemberAccess",
        "UnusedReturnValue",
        "WeakerAccess",
        "unused" })

public abstract class RK33xxEPDController {
    private static final String TAG = "epd";
    public static final int EPD_FULL = 1;
    public static final int EPD_A2 = 2;
    public static final int EPD_PART = 3;
    public static final int EPD_BLACK_WHITE = 6;
    public static final int EPD_FORCE_FULL = 11;
    public static final int EPD_REGAL = 15;
    public static final int EPD_ADAPTATIVE = 17;
    public static final int EPD_FAST = 18;
    public static final int EPD_DITHER = 100;

    public static boolean requestEpdMode(String epdMode) {
        try {
            Class.forName("android.view.View").getMethod("setByEinkUpdateMode",
                Integer.TYPE).invoke(null, getEpdMode(epdMode));
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    private static int getEpdMode(String epdMode) {
        /* only full mode was tested, so return that.
         *  device owners will want to expand this
         *  to return other well known modes
         */
        int mode = EPD_FULL;
        Log.v(TAG, String.format(Locale.US, "Requesting %s: %d", epdMode, mode));
        return mode;
    }
}
