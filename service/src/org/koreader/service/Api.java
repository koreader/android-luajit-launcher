package org.koreader.service;

import android.content.Context;
import android.os.Handler;

import org.koreader.IService;


/* Implementation of IPC calls

This is not thread-safe and cannot be called from multiple processes at the same time.
If you plan to use it with two or more apps concurrently, please improve or switch to a
Messenger instead. When in doubt look at:

https://developer.android.com/guide/components/bound-services

*/

class Api extends IService.Stub {

    private final Overlay overlay;
    private final Handler handler;

    Api(Context context) {
        overlay = new Overlay(context);
        handler = new Handler();
    }

    void destroy() {
        overlay.destroyOverlay();
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String status() {
        return overlay.status();
    }

    @Override
    public void resume() { overlay.resume(); }

    @Override
    public void pause() { overlay.pause(); }

    @Override
    public void setDim(final int level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                overlay.setDimLevel(level);
            }
        });
    }

    @Override
    public void setDimColor(final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                overlay.setDimColor(color);
            }
        });
    }

    @Override
    public void setWarmth(final int level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                overlay.setWarmthLevel(level);
            }
        });
    }

    @Override
    public void setWarmthAlpha(final float alpha) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                overlay.setWarmthAlpha(alpha);
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        Logger.d("running in Ui Thread: " + runnable.toString());
        handler.post(runnable);
    }
}
