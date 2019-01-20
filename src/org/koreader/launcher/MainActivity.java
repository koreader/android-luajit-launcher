package org.koreader.launcher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import org.koreader.launcher.ClipboardHelper;
import org.koreader.launcher.PowerHelper;
import org.koreader.launcher.ScreenHelper;


public class MainActivity extends android.app.NativeActivity {

    private final static String LOGGER_NAME = "luajit-launcher";

    static {
        System.loadLibrary("luajit");
    }

    private FramelessProgressDialog dialog;
    private PowerHelper power;
    private ScreenHelper screen;
    private ClipboardHelper clipboard;

    public MainActivity() {
        super();
        Log.i(LOGGER_NAME, "Creating luajit launcher main activity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        power = new PowerHelper(MainActivity.this, LOGGER_NAME);
        screen = new ScreenHelper(MainActivity.this, LOGGER_NAME);
        clipboard = new ClipboardHelper(MainActivity.this, LOGGER_NAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(LOGGER_NAME, "App resumed");
        screen.setWakelock(true);

        // set fullscreen immediately on general principle
        screen.setFullscreenLayout();

        // set fullscreen delayed because presumably some devices don't work right otherwise
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                screen.setFullscreenLayout();
            }
        }, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(LOGGER_NAME, "App paused");
        screen.setWakelock(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(LOGGER_NAME, "App stopped");
    }

    @Override
    protected void onDestroy() {
        power = null;
        screen = null;
        clipboard = null;
        Log.i(LOGGER_NAME, "App destroyed from java");
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /** These functions are exposed to lua in assets/android.lua
     *  If you add a new function here remember to write the companion
     *  lua function in that file */


    /** Clipboard */

    public String getClipboardText() {
        return clipboard.getClipboardText();
    }

    public int hasClipboardTextIntResultWrapper() {
        return clipboard.hasClipboardText();
    }

    public void setClipboardText(final String text) {
        clipboard.setClipboardText(text);
    }

    /** Device */

    public String getProduct() {
        return android.os.Build.PRODUCT;
    }

    public String getVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /** Dialogs */

    public void showProgress(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = FramelessProgressDialog.show(MainActivity.this,
                    title, message, true, false);
            }
        });
    }

    public void dismissProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });
    }

    /** Power */

    public int getBatteryLevel() {
        return power.batteryPercent();
    }

    public int isCharging() {
        return power.batteryCharging();
    }

    public int isWifiEnabled() {
        return power.wifiEnabled();
    }

    public void setWakeLock(final boolean enabled) {
        power.setWakelockState(enabled);
    }

    public void setWifiEnabled(final boolean enabled) {
        power.setWifi(enabled);
    }

    /** Screen */

    public int getScreenBrightness() {
        return screen.getScreenBrightness();
    }

    public int getScreenHeight() {
        return screen.getScreenHeight();
    }

    public int getScreenWidth() {
        return screen.getScreenWidth();
    }

    public int isFullscreen() {
        return screen.isFullscreen();
    }

    public void setFullscreen(final boolean enabled) {
        screen.setFullscreen(enabled);
    }

    public void setScreenBrightness(final int brightness) {
        screen.setScreenBrightness(brightness);
    }
}
