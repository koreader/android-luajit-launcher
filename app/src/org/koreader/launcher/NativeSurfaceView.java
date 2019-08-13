package org.koreader.launcher;

import java.util.Locale;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.koreader.launcher.device.EPDController;
import org.koreader.launcher.device.EPDFactory;




/* Provides a surface embedded inside of a view hierarchy.
 * Draw is delegated to native code using ANativeWindow API.
 *
 * Includes some routines to update e-ink screens. */

public final class NativeSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final EPDController epd;
    private final String tag;

    public NativeSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
        this.epd = EPDFactory.getEPDController();
        this.tag = this.getClass().getSimpleName();
        Logger.d(tag, "Starting");
    }

    // update the entire screen (rockchip)
    public void einkUpdate(int mode) {
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
            Logger.e(String.format(Locale.US,"%s: %d", mode_name, mode));
            return;
        }

        Logger.v(tag, String.format(Locale.US,
            "requesting epd update, type: %s", mode_name));
        epd.setEpdMode(this, 0, 0, 0, 0, 0, 0, mode_name);
    }

    // update a region or the entire screen (freescale)
    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        Logger.v(tag, String.format(Locale.US,
            "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
            mode, delay, x, y, width, height));
        epd.setEpdMode(this, mode, delay, x, y, width, height, null);
    }

    /* log surface callbacks */
    public void surfaceCreated(SurfaceHolder holder) {
        Logger.v(tag, "surface created");
        // override ondraw method in surfaceview.
        setWillNotDraw(false);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.v(tag, String.format(Locale.US,
            "surface changed {\n  width:  %d\n  height: %d\n}", width, height));
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Logger.v(tag, "surface destroyed");
    }
}
