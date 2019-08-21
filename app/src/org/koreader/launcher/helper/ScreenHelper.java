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

    // keep track of system timeout, to restore it when the application looses focus.
    private int sys_timeout;

    // keep track of fullscreen state, only useful on non-immersive apis.
    private boolean is_fullscreen = true;

    public final static int TIMEOUT_WAKELOCK = -1;
    public final static int TIMEOUT_SYSTEM = 0;

    /* Application timeout.
     * It can be TIMEOUT_WAKELOCK, TIMEOUT_SYSTEM or a number greater than 0,
     * representing a custom timeout in milliseconds.
     */

    public int app_timeout;

    public ScreenHelper(Context context) {
        super(context);
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
    public void setScreenBrightness(final int brightness) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //this will set the manual mode (set the automatic mode off)
                    Settings.System.putInt(getApplicationContext().getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(getApplicationContext().getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, brightness);
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
