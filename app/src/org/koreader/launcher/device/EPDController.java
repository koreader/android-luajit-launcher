/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device;

public interface EPDController {
    void setEpdMode(android.view.View targetView,
                    int mode, long delay,
                    int x, int y, int width, int height, String epdMode);
}
