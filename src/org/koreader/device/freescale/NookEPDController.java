/**
 * generic EPD Controller for Android devices,
 *
 * interface for some Nooks (ntx).
 * from https://bitbucket.org/Ryogo/n-toolkit/src/master/app/src/main/java/net/streletsky/ngptoolkit/Logics/ScreenManager.java
 */

package org.koreader.device.freescale;

import android.util.Log;


public abstract class NookEPDController {
    public static boolean requestEpdMode(android.view.View view) {
        try {
            Class.forName("android.view.View").getMethod("invalidate", new Class[] {
                Integer.TYPE
            }).invoke(view, new Object[] {
                Integer.valueOf(536870917)
            });
            Log.i("epd", "requested eink refresh");
            return true;
        } catch (Exception e) {
            Log.e("epd", e.toString());
            return false;
        }
    }
}
