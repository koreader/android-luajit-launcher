/**
 * generic EPD Controller for Android devices,
 *
 * New freescale/ntx devices
 * Tested on Tolinos with FW 11+ and Nook Glowlight 3.
 */

package org.koreader.device.freescale;


public class NTXNewEPDController extends NTXEPDController implements org.koreader.device.EPDController {
    @Override
    public void setEpdMode(android.view.View targetView, int mode, long delay, int x, int y, int width, int height, String epdMode) {
        requestEpdMode(targetView, mode, delay, x, y, width, height);
    }
}
