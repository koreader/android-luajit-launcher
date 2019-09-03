package org.koreader.launcher.helper;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import org.koreader.launcher.Logger;


/* Screen helper.

   Some methods are intended to be executed on the UIThread.
   You'll need to pass an Activity as a parameter of your methods. */

public class ScreenHelper extends BaseHelper {
    public final static int BRIGHTNESS_MIN = 0;
    public final static int BRIGHTNESS_MAX = 255;
    public final static int TIMEOUT_WAKELOCK = -1;
    public final static int TIMEOUT_SYSTEM = 0;

    // Application brightness, from BRIGHTNESS_MIN to BRIGHTNESS_MAX
    public int app_brightness;

    // Application timeout. It can be TIMEOUT_WAKELOCK, TIMEOUT_SYSTEM
    // or a number greater than 0, representing a custom timeout in milliseconds.
    public int app_timeout;

    private boolean has_custom_brightness;

    // keep track of system brightness
    private int sys_brightness;

    // keep track of system timeout, to restore it when the application looses focus.
    private int sys_timeout;

    // fullscreen state, only used on API levels 16-18
    private boolean is_fullscreen = true;


    public ScreenHelper(Context context) {
        super(context);
        this.sys_brightness = readSettingScreenBrightness();
        this.sys_timeout = readSettingScreenOffTimeout();
    }

    /* Screen size */
    public int getScreenWidth(Activity activity) {
        return getScreenSize(activity).x;
    }

    public int getScreenHeight(Activity activity) {
        return getScreenSize(activity).y;
    }

    public int getScreenAvailableHeight(Activity activity) {
        return getScreenSizeWithConstraints(activity).y;
    }

    // DEPRECATED: returns 0 on API16+
    public int getStatusBarHeight(Activity activity) {
        final Activity a = activity;
        Rect rectangle = new Rect();
        final Window window = a.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        return rectangle.top;
    }

    /* Screen brightness */
    public int getScreenBrightness() {
        if (has_custom_brightness) {
            return ((app_brightness >= BRIGHTNESS_MIN)
                && (app_brightness <= BRIGHTNESS_MAX)) ?
                    app_brightness : sys_brightness;
        } else {
            return sys_brightness;
        }
    }

    public void setScreenBrightness(Activity activity, final int brightness) {
        boolean custom = true;
        if (brightness < 0) {
            Logger.d(getTag(), "using system brightness");
            sys_brightness = readSettingScreenBrightness();
            custom = false;
        } else {
            Logger.d(getTag(), "using custom brightness: " + brightness);
        }
        final boolean custom_value = custom;
        final float level = brightness * 1.0f / BRIGHTNESS_MAX;
        final Activity a = activity;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    WindowManager.LayoutParams params = a.getWindow().getAttributes();
                    params.screenBrightness = level;
                    a.getWindow().setAttributes(params);
                    app_brightness = brightness;
                    has_custom_brightness = custom_value;
                } catch (Exception e) {
                    Logger.w(getTag(), e.toString());
                }
            }
        });
    }

    /* Screen timeout */

    /**
     * set the new timeout state
     *
     * known timeout states are TIMEOUT_SYSTEM, TIMEOUT_WAKELOCK
     * and values greater than 0 (milliseconds of the new timeout).
     *
     * @param new_timeout - new timeout state:
     */

    public void setTimeout(final int new_timeout) {
        // update app_timeout first
        app_timeout = new_timeout;
        // custom timeout in milliseconds
        if (app_timeout > TIMEOUT_SYSTEM) {
            Logger.v(getTag(), String.format(Locale.US,
                "set timeout for app: %d seconds",
                app_timeout / 1000));
            writeSettingScreenOffTimeout(app_timeout);
        // default timeout (by using system settings with or without wakelocks)
        } else if ((app_timeout == TIMEOUT_SYSTEM) || (app_timeout == TIMEOUT_WAKELOCK)) {
            Logger.v(getTag(), String.format(Locale.US,
                "set timeout for app: (state: %d), restoring defaults: %d seconds",
                app_timeout, sys_timeout / 1000));
            writeSettingScreenOffTimeout(sys_timeout);
        }
    }

    /**
     * set timeout based on activity state
     *
     * @param resumed - is the activity resumed and focused?
     */

    public void setTimeout(final boolean resumed) {
        try {
            if (resumed) {
                // back from paused: update android screen off timeout first
                sys_timeout = readSettingScreenOffTimeout();

                // apply a custom timeout for the application
                if ((sys_timeout != app_timeout) && (app_timeout > TIMEOUT_SYSTEM)) {
                    Logger.v(getTag(), String.format(Locale.US,
                        "restoring app timeout: %d -> %d seconds",
                        sys_timeout / 1000, app_timeout / 1000));

                    writeSettingScreenOffTimeout(app_timeout);
                }
            } else {
                // app paused: restore system timeout.
                if ((sys_timeout != app_timeout) && (app_timeout > TIMEOUT_SYSTEM)) {
                    Logger.v(getTag(), String.format(Locale.US,
                        "restoring system timeout: %d -> %d seconds",
                        app_timeout / 1000, sys_timeout / 1000));

                    writeSettingScreenOffTimeout(sys_timeout);
                }
            }
        } catch (Exception e) {
            Logger.w(getTag(), e.toString());
        }
    }

    /* Screen layout */
    public int isFullscreen() {
        return (is_fullscreen) ? 1 : 0;
    }

    public int isFullscreenDeprecated(Activity activity) {
        return ((activity.getWindow().getAttributes().flags &
            WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) ? 1 : 0;
    }

    public void setFullscreen(final boolean fullscreen) {
        is_fullscreen = fullscreen;
    }

    public void setFullscreenDeprecated(Activity activity, final boolean fullscreen) {
        final CountDownLatch cd = new CountDownLatch(1);
        final Activity a = activity;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Window window = a.getWindow();
                    if (fullscreen) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    }
                } catch (Exception e) {
                    Logger.w(getTag(), e.toString());
                }
                cd.countDown();
            }
        });
        try {
            cd.await();
        } catch (InterruptedException ex) {
            Logger.e(getTag(), ex.toString());
        }
    }

    private Point getScreenSize(Activity activity) {
        Point size = new Point();
        Display display = activity.getWindowManager().getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            size.set(metrics.widthPixels, metrics.heightPixels);
        } else {
            display.getSize(size);
        }
        return size;
    }

    private Point getScreenSizeWithConstraints(Activity activity) {
        Point size = new Point();
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        size.set(metrics.widthPixels, metrics.heightPixels);
        return size;
    }

    private class Box<T> {
        T value;
    }

    private int readSettingScreenBrightness() {
        try {
            return Settings.System.getInt(getApplicationContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
            Logger.w(getTag(), e.toString());
            return 0;
        }
    }

    private int readSettingScreenOffTimeout() {
        try {
            return Settings.System.getInt(getApplicationContext().getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Exception e) {
            Logger.w(getTag(), e.toString());
            return 0;
        }
    }

    private void writeSettingScreenOffTimeout(final int timeout) {
        if (timeout <= 0) return;

        try {
            Settings.System.putInt(getApplicationContext().getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, timeout);
        } catch (Exception e) {
            Logger.w(getTag(), e.toString());
        }
    }
}
