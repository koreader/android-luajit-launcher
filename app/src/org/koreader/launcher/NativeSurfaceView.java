package org.koreader.launcher;

import java.util.Locale;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/* Provides a surface embedded inside of a view hierarchy.
 * Draw is delegated to native code using ANativeWindow API. */

public final class NativeSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final String tag;

    public NativeSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
        this.tag = this.getClass().getSimpleName();
        Logger.d(tag, "Starting");
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
