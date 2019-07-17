package org.koreader.launcher;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.koreader.device.EPDController;
import org.koreader.device.EPDFactory;

/* Provides a dedicated drawing surface embedded inside of a view hierarchy.

   This is intended as a dumb surface that doesn't do any drawing on its own.

   Drawing is delegated to luajit, using ANativeWindow API.
   See https://github.com/koreader/koreader-base/blob/master/ffi/framebuffer_android.lua
   as an example.

   Also provides methods to update the eink screen on some devices. */

public class NativeView extends SurfaceView implements SurfaceHolder.Callback {

    private EPDController epd;
    private String tag;

    public NativeView(Context context) {
        // start and hold a SurfaceView
        super(context);
        getHolder().addCallback(this);

        // use the application name as the log tag
        this.tag = context.getApplicationContext().getResources().getString(R.string.app_name);

        // get the EPD controller for this device.
        this.epd = EPDFactory.getEPDController(tag);
    }

    /* monitor surfaceCreated, surfaceChanged and surfaceDestroyed callbacks */

    public void surfaceCreated(SurfaceHolder holder) {
        Logger.d(tag, "surface created");
        // This view doesn't draw on its own
        // see http://developer.android.com/reference/android/view/View.html#setWillNotDraw%28boolean%29
        setWillNotDraw(false);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.d(tag, String.format("surface changed {\n  width:  %d\n  height: %d\n}", width, height));
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Logger.d(tag, "surface destroyed");
    }

    /* Generic methods to request eink updates for *this* view */

    // used on most rockchip devices
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
            Logger.e(tag, String.format("%s: %d", mode_name, mode));
            return;
        }
        Logger.d(tag, String.format("requesting epd update, type: %s", mode_name));
        epd.setEpdMode(this, 0, 0, 0, 0, 0, 0, mode_name);
    }

    // used on most freescale devices
    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        Logger.d(tag, String.format("requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
            mode, delay, x, y, width, height));
        epd.setEpdMode(this, mode, delay, x, y, width, height, null);
    }
}

