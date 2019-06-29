package org.koreader.launcher;

import android.content.Context;
import android.view.SurfaceView;

import org.koreader.device.DeviceInfo;
import org.koreader.device.EPDController;
import org.koreader.device.EPDFactory;

public class Device {

    private static final String PRODUCT = DeviceInfo.PRODUCT;
    private static final boolean HAS_EINK_SUPPORT = DeviceInfo.EINK_SUPPORT;
    private static final boolean HAS_FULL_EINK_SUPPORT = DeviceInfo.EINK_FULL_SUPPORT;
    private static final boolean NEEDS_WAKELOCK_ENABLED = DeviceInfo.BUG_WAKELOCKS;

    private String TAG;
    private Context context;
    private EPDController epd;

    public Device(Context context) {
        this.context = context.getApplicationContext();
        this.epd = EPDFactory.getEPDController(TAG);
        this.TAG = this.context.getResources().getString(R.string.app_name);
    }

    /**
     * Used on Rockchip devices
     */
    public void einkUpdate(SurfaceView view, int mode) {
        String mode_name = "invalid mode";

        if (mode == 1) {
            mode_name = "EPD_FULL";
        } else if (mode == 2) {
            mode_name = "EPD_PART";
        } else if (mode == 3) {
            mode_name = "EPD_A2";
        } else if (mode == 4) {
            mode_name = "EPD_AUTO";
        } else {
            Logger.e(TAG, String.format("%s: %d", mode_name, mode));
            return;
        }
        Logger.v(TAG, String.format("requesting epd update, type: %s", mode_name));
        epd.setEpdMode(view, 0, 0, 0, 0, 0, 0, mode_name);
    }

    /**
     * Used on Freescale imx devices
     */
    public void einkUpdate(SurfaceView view, int mode, long delay, int x, int y, int width, int height) {
        Logger.v(TAG, String.format("requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
            mode, delay, x, y, width, height));
        epd.setEpdMode(view, mode, delay, x, y, width, height, null);
    }

    public String einkPlatform() {
        if (DeviceInfo.EINK_FREESCALE) {
            return "freescale";
        } else if (DeviceInfo.EINK_ROCKCHIP){
            return "rockchip";
        } else {
            return "none";
        }
    }

    public String getProduct() {
        return PRODUCT;
    }

    public int isEink() {
        return HAS_EINK_SUPPORT ? 1 : 0;
    }

    public int isFullEink() {
        return HAS_FULL_EINK_SUPPORT ? 1 : 0;
    }

    public int needsWakelock() {
        return NEEDS_WAKELOCK_ENABLED ? 1 : 0;
    }
}
