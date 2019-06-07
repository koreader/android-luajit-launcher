/**
 * generic EPD Controller for Android devices,
 *
 * Tolinos (based on Freescale imx) with firmware >= 11 (gpu drivers)
 */

package org.koreader.device.freescale;


public class TolinoNewEPDController extends NTXEPDController implements org.koreader.device.EPDController {
    @Override
    public void setEpdMode(android.view.View targetView, int mode, long delay, int x, int y, int width, int height, String epdMode) {
        requestEpdMode(targetView, mode, delay, x, y, width, height);
    }
}
