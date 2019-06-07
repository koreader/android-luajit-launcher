/**
 * generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU
 */

package org.koreader.device.rockchip;


@SuppressWarnings("unused")
public class RK3066EPDController extends RK30xxEPDController implements org.koreader.device.EPDController {
    @Override
    public void setEpdMode(android.view.View targetView, int mode, long delay, int x, int y, int width, int height, String epdMode) {
        requestEpdMode(targetView, epdMode);
    }
}
