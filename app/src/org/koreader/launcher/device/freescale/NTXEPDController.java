/* generic EPD Controller for Android,
 *
 * interface for newer ntx devices.
 * based on https://github.com/koreader/koreader/issues/3517
 *
 * Thanks to @char11
 *
 *	public static final int EINK_DITHER_COLOR_Y1 = 268435456;
 *	public static final int EINK_DITHER_COLOR_Y4 = 0;
 *	public static final int EINK_DITHER_MODE_DITHER = 256;
 *	public static final int EINK_DITHER_MODE_NODITHER = 0;
 *	public static final int EINK_MONOCHROME_MODE_MONOCHROME = 2048;
 *	public static final int EINK_MONOCHROME_MODE_NOMONOCHROME = 0;
 *	public static final int EINK_UPDATE_MODE_FULL = 32;
 *	public static final int EINK_UPDATE_MODE_PARTIAL = 0;
 *	public static final int EINK_WAIT_MODE_NOWAIT = 0;
 *	public static final int EINK_WAIT_MODE_WAIT = 64;
 *	public static final int EINK_WAVEFORM_MODE_A2 = 5;
 *	public static final int EINK_WAVEFORM_MODE_AUTO = 4;
 *	public static final int EINK_WAVEFORM_MODE_DU = 1;
 *	public static final int EINK_WAVEFORM_MODE_GC16 = 2;
 *	public static final int EINK_WAVEFORM_MODE_GC4 = 3;
 *	public static final int EINK_WAVEFORM_MODE_GL16 = 6;
 *	public static final int EINK_WAVEFORM_MODE_GLD16 = 8;
 *	public static final int EINK_WAVEFORM_MODE_GLR16 = 7;
 *	public static final int EINK_WAVEFORM_MODE_INIT = 0;
 */

package org.koreader.launcher.device.freescale;

import java.util.Locale;
import android.util.Log;


/* This abstract class is copied and used on einkTest.
   Suppress some warnings to avoid distractions */

@SuppressWarnings ({
    "JavaReflectionMemberAccess",
    "UnusedReturnValue",
    "WeakerAccess",
    "unused" })

public abstract class NTXEPDController {
    private static final String TAG = "epd";

    public static boolean requestEpdMode(android.view.View view,
                                         int mode, long delay,
                                         int x, int y, int width, int height) {
        try {
            Class.forName("android.view.View").getMethod("postInvalidateDelayed",
                    Long.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE
                ).invoke(view, delay, x, y, width, height, mode
            );

            Log.i(TAG, String.format(Locale.US,
                "requested eink refresh, type: %d x:%d y:%d w:%d h:%d",
                mode, x, y, width, height));
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }
}
