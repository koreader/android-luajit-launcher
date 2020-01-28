package org.koreader.service;

import java.util.Locale;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.View;

/* fullscreen overlay, works on any api if built with targetApi 17-19
 * it won't overlay ui bars but works fine in immersive/fullscreen mode. */

class Overlay {
    // simulate dim
    private final View dimView;
    private float mDimAlpha = 0.0f;
    private int mDimColor = Color.BLACK;

    // simulate warmth
    private final View warmthView;
    private float mWarmthAlpha = 0.2f;
    private int mWarmthColor = Color.TRANSPARENT;

    private final ColorHelper colorHelper;
    private final WindowManager windowManager;

    Overlay(Context context){
        this.colorHelper = new ColorHelper();
        this.dimView = new View(context.getApplicationContext());
        this.warmthView = new View(context.getApplicationContext());
        this.windowManager = (WindowManager)
            context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        windowManager.addView(dimView, getOverlayLayout());
        windowManager.addView(warmthView, getOverlayLayout());

        resume();
    }

    void resume() {
        setDimAlpha(mDimAlpha);
        setDimBackground(mDimColor);
        setWarmthBackground(mWarmthColor);
        setWarmthAlpha(mWarmthAlpha);
    }

    void pause() {
        setDimAlpha(ColorHelper.NULL_ALPHA);
        setDimBackground(ColorHelper.NULL_COLOR);
        setWarmthBackground(ColorHelper.NULL_COLOR);
        setWarmthAlpha(ColorHelper.NULL_ALPHA);
    }

    void destroyOverlay() {
        windowManager.removeView(dimView);
        windowManager.removeView(warmthView);
    }

    void setDimLevel(final int level) {
        if (dimView != null) {
            float alpha = colorHelper.getAlpha(level);
            Boolean ok = setDimAlpha(alpha);
            if (ok) mDimAlpha = alpha;
        } else {
            Logger.e("setDimLevel: no view");
        }
    }

    void setWarmthLevel(final int level) {
        if (warmthView != null) {
            int argb = colorHelper.getARGB(level);
            Boolean ok = setWarmthBackground(argb);
            if (ok) mWarmthColor = argb;
        } else {
            Logger.e("setWarmthLevel: no view");
        }
    }

    void setWarmthAlpha(final float alpha) {
        if (warmthView != null) {
            Boolean ok = warmthAlpha(alpha);
            if (ok) mWarmthAlpha = alpha;
        } else {
            Logger.e("setColorAlpha: no view");
        }
    }

    void setDimColor(final int color) {
        if (dimView != null) {
            Boolean ok = setDimBackground(color);
            if (ok) mDimColor = color;
        } else {
            Logger.e("setDimColor: no view");
        }
    }

    String status() {
        return String.format(Locale.US,
            "Dim view: %f alpha, %d color\nWarmth view: %f alpha, %d color\n",
            mDimAlpha, mDimColor, mWarmthAlpha, mWarmthColor);
    }


    private Boolean setDimAlpha(float alpha) {
        if (dimView != null) {
            LayoutParams params = getOverlayLayout();
            params.alpha = alpha;
            windowManager.updateViewLayout(dimView, params);
            return true;
        } else {
            Logger.e("Calling setDimAlpha without a view, ignoring");
            return false;
        }
    }

    private Boolean setDimBackground(int color) {
        if (dimView != null) {
            dimView.setBackgroundColor(color);
            return true;
        } else {
            Logger.e("Calling setDimBackground without a view, ignoring");
            return false;
        }
    }

    private Boolean warmthAlpha(float alpha) {
        if (warmthView != null) {
            LayoutParams params = getOverlayLayout();
            params.alpha = alpha;
            windowManager.updateViewLayout(warmthView, params);
            return true;
        } else {
            Logger.e("Calling warmthAlpha without a view, ignoring");
            return false;
        }
    }

    private Boolean setWarmthBackground(int color) {
        if (warmthView != null) {
            warmthView.setBackgroundColor(color);
            return true;
        } else {
            Logger.e("Calling setWarmthBackground without a view, ignoring");
            return false;
        }
    }

    private LayoutParams getOverlayLayout() {
        LayoutParams params = new WindowManager.LayoutParams();
        params.type = android.os.Build.VERSION.SDK_INT >= 19 ?
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

        params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            | WindowManager.LayoutParams.FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.START | Gravity.TOP;
        return params;
    }
}
