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
 * Uses a NativeSurfaceView to hook the window on top of the view hierarchy and
 * overrides einkUpdate methods with working implementations.
 */
public final class MainActivity extends BaseActivity implements
    ActivityCompat.OnRequestPermissionsResultCallback{

    private final static String TAG = "MainActivity";
    private final static int REQUEST_WRITE_STORAGE = 1;

    private NativeSurfaceView view;

    /*---------------------------------------------------------------
     *                        activity callbacks                    *
     *--------------------------------------------------------------*/

    /* Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        getWindow().takeSurface(null);
        view = new NativeSurfaceView(this);
        view.getHolder().addCallback(this);
        setContentView(view);
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

    @Override
    public void einkUpdate(int mode) {
        view.einkUpdate(mode);
    }

    @Override
    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        view.einkUpdate(mode, delay, x, y, width, height);
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

        if (hasWriteSettingsPermission() == 1) {
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
