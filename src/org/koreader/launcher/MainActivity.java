package org.koreader.launcher;

import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.concurrent.CountDownLatch;

import org.koreader.device.DeviceInfo;
import org.koreader.device.EPDController;
import org.koreader.device.EPDFactory;

public class MainActivity extends NativeActivity {

    private final static int SDK_INT = Build.VERSION.SDK_INT;
    private final static String LOGGER_NAME = "luajit-launcher";

    static {
        System.loadLibrary("luajit");
    }

    private FramelessProgressDialog dialog;
    private DeviceInfo device;
    private EPDController epd = EPDFactory.getEPDController();
    private Clipboard clipboard;
    private PowerHelper power;

    public MainActivity() {
        super();
        Log.i(LOGGER_NAME, "Creating luajit launcher main activity");
    }

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clipboard = new Clipboard(this, LOGGER_NAME);
        power = new PowerHelper(this, LOGGER_NAME);
    }

    /** Called when the activity has become visible. */
    @Override
    protected void onResume() {
        Log.v(LOGGER_NAME, "App resumed");
        power.setWakelock(true);
        setFullscreenLayout();
        super.onResume();
    }

    /** Called when another activity is taking focus. */
    @Override
    protected void onPause() {
        Log.v(LOGGER_NAME, "App paused");
        power.setWakelock(false);
        super.onPause();
    }

    /** Called when the activity is no longer visible. */
    @Override
    protected void onStop() {
        Log.v(LOGGER_NAME, "App stopped");
        super.onStop();
    }

    /** Called just before the activity is destroyed. */
    @Override
    protected void onDestroy() {
        Log.v(LOGGER_NAME, "App destroyed");
        clipboard = null;
        power = null;
        super.onDestroy();
    }

    /** Called just before the activity is resumed by an Intent */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /** These functions are exposed to lua in assets/android.lua
     *  If you add a new function here remember to write the companion
     *  lua function in that file */

    /** clipboard */
    public String getClipboardText() {
        return clipboard.getClipboardText();
    }

    public int hasClipboardTextIntResultWrapper() {
        return clipboard.hasClipboardText();
    }

    public void setClipboardText(final String text) {
        clipboard.setClipboardText(text);
    }

    /** power */
    public int isCharging() {
        return power.batteryCharging();
    }

    public int getBatteryLevel() {
        return power.batteryPercent();
    }

    public void setWakeLock(final boolean enabled) {
        power.setWakelockState(enabled);
    }

    @SuppressWarnings("deprecation")
    private void setFullscreenLayout() {
        if (SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        } else if (SDK_INT < Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public int getScreenBrightness() {
        final Box<Integer> result = new Box<Integer>();
        final CountDownLatch cd = new CountDownLatch(1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    result.value = new Integer(Settings.System.getInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS));
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                    result.value = new Integer(0);
                }
                cd.countDown();
            }
        });
        try {
            cd.await();
        } catch (InterruptedException ex) {
            return 0;
        }

        if (result.value == null) {
            return 0;
        }

        return result.value.intValue();
    }

    public void setScreenBrightness(final int brightness) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //this will set the manual mode (set the automatic mode off)
                    Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

                    Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, brightness);
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                }
            }
        });
    }

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

    public int isFullscreen() {
        return ((getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) ? 1 : 0;
    }


    public void setWifiEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWifiManager().setWifiEnabled(enabled);
            }
        });
    }

    public int isWifiEnabled() {
        return getWifiManager().isWifiEnabled() ? 1 : 0;
    }

    private WifiManager getWifiManager() {
        return (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    }

    public void setFullscreen(final boolean fullscreen) {
        final CountDownLatch cd = new CountDownLatch(1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Window window = getWindow();
                    if (fullscreen) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    }
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                }
                cd.countDown();
            }
        });
        try {
            cd.await();
        } catch (InterruptedException ex) {
        }
    }

    public int getStatusBarHeight() {
        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        return statusBarHeight;
    }

    public int getScreenWidth() {
        int width = getScreenSize().x;
        return width;
    }

    public int getScreenHeight() {
        int height = getScreenSize().y;
        return height;
    }

    private Point getScreenSize() {
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        try {
            // JellyBean 4.2 (API 17) and higher
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            size.set(metrics.widthPixels, metrics.heightPixels);
        } catch (NoSuchMethodError e) {
            // Honeycomb 3.0 (API 11) adn higher
            display.getSize(size);
        }
        return size;
    }

    public String getProduct() {
        return device.PRODUCT;
    }

    public String getVersion() {
        return Build.VERSION.RELEASE;
    }

    public int isEink() {
        return (device.IS_EINK_SUPPORTED) ? 1 : 0;
    }

    public void einkUpdate(int mode) {
        String mode_name = "invalid mode";
        final View root_view = getWindow().getDecorView().findViewById(android.R.id.content);

        if (mode == device.EPD_FULL) {
            mode_name = "EPD_FULL";
        } else if (mode == device.EPD_PART) {
            mode_name = "EPD_PART";
        } else if (mode == device.EPD_A2) {
            mode_name = "EPD_A2";
        } else if (mode == device.EPD_AUTO) {
            mode_name = "EPD_AUTO";
        } else {
            Log.e(LOGGER_NAME, String.format("%s: %d", mode_name, mode));
            return;
        }
        Log.v(LOGGER_NAME, String.format("requesting eink refresh, type: %s", mode_name));
        epd.setEpdMode(root_view, mode_name);
    }

    private class Box<T> {
        public T value;
    }
}
