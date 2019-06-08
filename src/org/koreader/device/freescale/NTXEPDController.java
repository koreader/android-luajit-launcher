/**
 * generic EPD Controller for Android devices,
 *
 * interface for newer freescale(ntx) soc.
 */

package org.koreader.device.freescale;

import android.util.Log;


public abstract class NTXEPDController {
    public static boolean requestEpdMode(android.view.View view, int mode, long delay, int x, int y, int width, int height) {
        try {
            Class.forName("android.view.View").getMethod("postInvalidateDelayed", new Class[] {
                Long.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                Integer.TYPE
            }).invoke(view, new Object[] {
                Long.valueOf(delay),
                Integer.valueOf(x),
                Integer.valueOf(y),
                Integer.valueOf(width),
                Integer.valueOf(height),
                Integer.valueOf(mode)
            });
            Log.i("epd", String.format("requested eink refresh, type: %d x:%d y:%d w:%d h:%d", mode, x, y, width, height));
            return true;
        } catch (Exception e) {
            Log.e("epd", e.toString());
            return false;
        }
    }
}
