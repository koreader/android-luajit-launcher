package org.koreader.launcher;

import java.util.Locale;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.koreader.launcher.device.EPDController;
import org.koreader.launcher.device.EPDFactory;
import org.koreader.launcher.helper.ScreenHelper;

/* MainActivity.java
 *
 * Takes care of activity callbacks.
 *
 * Implements runtime permissions:
 *     WRITE_EXTERNAL_STORAGE is requested onCreate() and needed to work.
 *     WRITE_SETTINGS is requested when the user does an action that requires it:
 *         - change screen brightness
 *         - change screen off timeout
 *
 * Overrides einkUpdate methods with working implementations.
 */
public final class MainActivity extends BaseActivity implements
    ActivityCompat.OnRequestPermissionsResultCallback{

    private final static String TAG = "MainActivity";
    private final static int REQUEST_WRITE_STORAGE = 1;

    private final EPDController epd = EPDFactory.getEPDController();

    private NativeSurfaceView view;
    private boolean takes_window_ownership;

    /*---------------------------------------------------------------
     *                        activity callbacks                    *
     *--------------------------------------------------------------*/

    /* Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        /* The NativeActivity framework takes care of the surface/view.
           It seems to work just-fine(TM) in all devices.

           But, apparently, Tolinos (and other ntx boards) need to take control
           of the underlying surface to be able to refresh their e-ink screen.

           This is just a guess based on user feedback, needs to be tested on
           real hardware */

        if ("freescale".equals(getEinkPlatform())) {
            Logger.d(TAG, "onNativeSurfaceViewImpl()");
            getWindow().takeSurface(null);
            view = new NativeSurfaceView(this);
            view.getHolder().addCallback(this);
            setContentView(view);
            takes_window_ownership = true;
        } else {
            Logger.d(TAG, "onNativeWindowImpl()");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setFullscreenLayout();
            View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    setFullscreenLayout();
                }
            });
        }
        requestExternalStoragePermission();
    }

    /* Called when the activity has become visible. */
    @Override
    protected void onResume() {
        Logger.d(TAG, "onResume()");
        setTimeout(true);
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setFullscreenLayout();
                }
            }, 500);
        }
    }

    /* Called when another activity is taking focus. */
    @Override
    protected void onPause() {
        Logger.d(TAG, "onPause()");
        setTimeout(false);
        super.onPause();
    }

    /* Called just before the activity is resumed by an intent */
    @Override
    protected void onNewIntent(Intent intent) {
        Logger.d(TAG, "onNewIntent()");
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /* Called on permission result */
    @Override
    public void onRequestPermissionsResult(int requestCode,
        @NonNull String[] permissions, @NonNull int[] grantResults) {
        Logger.d(TAG, "onRequestPermissionResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ActivityCompat.checkSelfPermission(this,
            permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Logger.v(TAG, String.format(Locale.US,
                "Permission granted for request code: %d", requestCode));
        } else {
            String msg = String.format(Locale.US,
                "Permission rejected for request code %d", requestCode);
            if (requestCode == REQUEST_WRITE_STORAGE) {
                Logger.e(TAG, msg);
            } else {
                Logger.w(TAG, msg);
            }
        }
    }

    /* Called when the activity is going to be destroyed */
    @Override
    public void onDestroy() {
        Logger.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    /*---------------------------------------------------------------
     *             override methods used by lua/JNI                *
     *--------------------------------------------------------------*/

    // update the entire screen (rockchip)

    @Override
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
        Logger.v(TAG, String.format(Locale.US,
            "requesting epd update, type: %s", mode_name));

        if (takes_window_ownership) {
            epd.setEpdMode(view, 0, 0, 0, 0, 0, 0, mode_name);
        } else {
            final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            epd.setEpdMode(rootView, 0, 0, 0, 0, 0, 0, mode_name);
        }
    }

    // update a region or the entire screen (freescale)

    @Override
    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {

        Logger.v(TAG, String.format(Locale.US,
            "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
            mode, delay, x, y, width, height));

        if (takes_window_ownership) {
            epd.setEpdMode(view, mode, delay, x, y, width, height, null);
        } else {
            final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            epd.setEpdMode(rootView, mode, delay, x, y, width, height, null);
        }
    }

    @Override
    public int hasExternalStoragePermission() {
        return (ContextCompat.checkSelfPermission(
            MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED) ? 1 : 0;
    }

    @Override
    public int hasWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(MainActivity.this)) {
                return 1;
            } else {
                return 0;
            }
        } else {
            // on older apis permissions are granted at install time
            return 1;
        }
    }

    @Override
    public void setScreenBrightness(final int brightness) {
        if (hasWriteSettingsPermission() == 1) {
            screen.setScreenBrightness(brightness);
        } else {
            requestWriteSettingsPermission();
        }
    }

    @Override
    public void setScreenOffTimeout(final int timeout) {
        if (timeout == ScreenHelper.TIMEOUT_WAKELOCK) {
            power.setWakelockState(true);
        } else {
            power.setWakelockState(false);
        }

        if ((timeout == ScreenHelper.TIMEOUT_SYSTEM) ||
            (timeout == ScreenHelper.TIMEOUT_WAKELOCK) ||
            (hasWriteSettingsPermission() == 1)) {
            screen.setTimeout(timeout);
        } else {
            requestWriteSettingsPermission();
        }
    }


    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    /* request WRITE_EXTERNAL_STORAGE permission.
     * see https://developer.android.com/guide/topics/permissions/overview.html#normal-dangerous
     */
    private void requestExternalStoragePermission() {
        if (hasExternalStoragePermission() == 0) {
            Logger.i(TAG,"Requesting WRITE_EXTERNAL_STORAGE permission");
            ActivityCompat.requestPermissions(this,
                new String[]{ android.Manifest.permission.WRITE_EXTERNAL_STORAGE },
                REQUEST_WRITE_STORAGE);
        }
    }

    /* request WRITE_SETTINGS permission.
     * It needs to be granted through a management screen.
     * See https://developer.android.com/reference/android/Manifest.permission.html#WRITE_SETTINGS
     */
    @SuppressWarnings("InlinedApi")
    private void requestWriteSettingsPermission() {
        if (hasWriteSettingsPermission() == 0) {
            Logger.i(TAG, "Requesting WRITE_SETTINGS permission");
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            startActivity(intent);
        } else {
            Logger.v(TAG, "write settings permission is already granted");
        }
    }

    /* set a fullscreen layout */
    private void setFullscreenLayout() {
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    /* set screen timeout based on activity state */
    private void setTimeout(final boolean resumed) {
        StringBuilder sb = new StringBuilder("timeout: ");
        if (resumed)
            sb.append("onResume callback -> ");
        else
            sb.append("onPause callback -> ");

        if (screen.app_timeout == ScreenHelper.TIMEOUT_WAKELOCK) {
            sb.append("using wakelocks: ");
            sb.append(resumed);
            Logger.d(TAG, sb.toString());
            power.setWakelock(resumed);
        } else if (screen.app_timeout > ScreenHelper.TIMEOUT_SYSTEM) {
            sb.append("custom settings: ");
            sb.append(resumed);
            Logger.d(TAG, sb.toString());
            screen.setTimeout(resumed);
        }
    }
}
