package org.koreader.launcher;

import java.util.List;
import java.util.Locale;

import android.app.NativeActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.koreader.launcher.device.DeviceInfo;
import org.koreader.launcher.helper.*;

/* BaseActivity.java
 *
 * Convenience wrapper on top of NativeActivity that implements JNILuaInterface
 *
 * This class is intended to be subclassed. It requests read&write permission at launch
 * but it doesn't care about return values. Please implement OnRequestPermissionsResultCallback
 * on classes that extend from this class.
 *
 */

abstract class BaseActivity extends NativeActivity implements JNILuaInterface {
    private final static String TAG = "BaseActivity";

    // device info
    private final static String PRODUCT = DeviceInfo.PRODUCT;
    private final static String RUNTIME_VERSION = android.os.Build.VERSION.RELEASE;
    private final static boolean HAS_EINK_SUPPORT = DeviceInfo.EINK_SUPPORT;
    private final static boolean HAS_FULL_EINK_SUPPORT = DeviceInfo.EINK_FULL_SUPPORT;
    private final static boolean NEEDS_WAKELOCK_ENABLED = DeviceInfo.BUG_WAKELOCKS;

    // permissions
    final static int REQUEST_WRITE_STORAGE = 1;

    // windows insets
    private int top_inset_height;

    // helpers
    private FramelessProgressDialog dialog;
    private ClipboardHelper clipboard;
    private NetworkHelper network;
    PowerHelper power;
    ScreenHelper screen;

    /*---------------------------------------------------------------
     *                        activity callbacks                    *
     *--------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        requestReadAndWritePermission();
        clipboard = new ClipboardHelper(this);
        network = new NetworkHelper(this);
        power = new PowerHelper(this);
        screen = new ScreenHelper(this);
    }

    @Override
    public void onAttachedToWindow() {
        Logger.d(TAG, "onAttachedToWindow()");
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.view.DisplayCutout cutout;
            cutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
            if (cutout != null) {
                int cutout_pixels = cutout.getSafeInsetTop();
                if (top_inset_height != cutout_pixels) {
                    Logger.v(TAG, String.format(Locale.US,
                        "top %dpx are not available, reason: window inset", cutout_pixels));
                    top_inset_height = cutout_pixels;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        Logger.d(TAG, "onDestroy()");
        clipboard = null;
        network = null;
        power = null;
        screen = null;
        super.onDestroy();
    }

    /*---------------------------------------------------------------
     *             implement methods used by lua/JNI                *
     *--------------------------------------------------------------*/

    /* build */
    public String getFlavor() {
        return getResources().getString(R.string.app_flavor);
    }

    public String getName() {
        return getResources().getString(R.string.app_name);
    }

    public int isDebuggable() {
        return BuildConfig.DEBUG ? 1 : 0;
    }

    /* clipboard */
    public String getClipboardText() {
        return clipboard.getClipboardText();
    }

    public int hasClipboardTextIntResultWrapper() {
        return clipboard.hasClipboardText();
    }

    public void setClipboardText(final String text) {
        clipboard.setClipboardText(text);
    }

    /* device */
    public String getEinkPlatform() {
        if (DeviceInfo.EINK_FREESCALE) {
            return "freescale";
        } else if (DeviceInfo.EINK_ROCKCHIP){
            return "rockchip";
        } else {
            return "none";
        }
    }

    public String getProduct() {
        return PRODUCT;
    }

    public String getVersion() {
        return RUNTIME_VERSION;
    }

    public int isEink() {
        return HAS_EINK_SUPPORT ? 1 : 0;
    }

    public int isEinkFull() {
        return HAS_FULL_EINK_SUPPORT ? 1 : 0;
    }

    public int needsWakelocks() {
        return NEEDS_WAKELOCK_ENABLED ? 1 : 0;
    }

    /* dialogs */
    public void showProgress(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = FramelessProgressDialog.show(BaseActivity.this, title);
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

    /* intents */
    public int openLink(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        return (startActivityIfSafe(intent)) ? 0 : 1;
    }

    public void dictLookup(String text, String pkg, String action) {
        Intent intent = new Intent(IntentUtils.getByAction(text, pkg, action));
        if (!startActivityIfSafe(intent)) {
            Logger.e(TAG, "dictionary lookup: can't find a package able to resolve action " + action);
        }
    }

    /* network */
    public int download(final String url, final String name) {
        return network.download(url, name);
    }

    public String getNetworkInfo() {
        return network.info();
    }

    public int isWifiEnabled() {
        return network.isWifi();
    }

    public void setWifiEnabled(final boolean enabled) {
        network.setWifi(enabled);
    }

    /* package manager */
    public int isPackageEnabled(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            boolean enabled = pm.getApplicationInfo(pkg, 0).enabled;
            return (enabled) ? 1 : 0;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    /* power */
    public int isCharging() {
        return power.batteryCharging();
    }

    public int getBatteryLevel() {
        return power.batteryPercent();
    }

    public void setWakeLock(final boolean enabled) {
        power.setWakelockState(enabled);
    }

    /* screen */
    public void einkUpdate(int mode) {
        Logger.w(TAG,
            "einkUpdate(mode) not implemented in this class!");
    }

    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        Logger.w(TAG,
            "einkUpdate(mode, delay, x, y, width, height) not implemented in this class!");
    }

    public int getScreenBrightness() {
        return screen.getScreenBrightness();
    }

    public int getScreenOffTimeout() {
        return screen.app_timeout;
    }

    public int getScreenAvailableHeight() {
        return screen.getScreenAvailableHeight();
    }

    public int getScreenHeight() {
        //return screen.getScreenHeight();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (screen.getScreenHeight() - top_inset_height);
        } else {
            return screen.getScreenHeight();
        }
    }

    public int getScreenWidth() {
        return screen.getScreenWidth();
    }

    public int getStatusBarHeight() {
        return screen.getStatusBarHeight();
    }

    public int isFullscreen() {
        // for newer Jelly Bean devices (apis 17 - 18)
        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) ||
            (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            return screen.isFullscreen();
        }
        // for older devices (apis 14 - 15 - 16)
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            return screen.isFullscreenDeprecated();
        }
        // for devices with immersive mode (api 19+)
        else {
            return 1;
        }
    }

    public void setFullscreen(final boolean enabled) {
        // for newer Jelly Bean devices (apis 17 - 18)
        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) ||
            (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            screen.setFullscreen(enabled);
        }
        // for older devices (apis 14 - 15 - 16)
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            screen.setFullscreenDeprecated(enabled);
        }
    }

    public void setScreenBrightness(final int brightness) {
        //screen.setScreenBrightness(brightness);
        if (hasWriteSettingsEnabled()) {
            screen.setScreenBrightness(brightness);
        }
    }

    public void setScreenOffTimeout(final int timeout) {
        //screen.setTimeout(timeout);
        if (timeout == ScreenHelper.TIMEOUT_WAKELOCK) {
            power.setWakelockState(true);
        } else {
            power.setWakelockState(false);
        }

        if (hasWriteSettingsEnabled()) {
            screen.setTimeout(timeout);
        }
    }

    /* widgets */
    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Toast toast = Toast.makeText(BaseActivity.this,
                    message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    /* WRITE_SETTINGS needs to be granted through a permission management screen.
     * See https://developer.android.com/reference/android/Manifest.permission.html#WRITE_SETTINGS */
    private boolean hasWriteSettingsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(BaseActivity.this)) {
                return true;
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                startActivity(intent);
                return false;
            }
        } else {
            // on older apis permissions are granted at install time
            return true;
        }
    }

    /* request WRITE_EXTERNAL_STORAGE permission (READ_EXTERNAL_STORAGE is implicitly granted)
     * see https://developer.android.com/guide/topics/permissions/overview.html#normal-dangerous */
    private void requestReadAndWritePermission() {
        String permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
        boolean is_granted = (ContextCompat.checkSelfPermission(BaseActivity.this,
            permission) == PackageManager.PERMISSION_GRANTED);

        if (!is_granted) {
            Logger.i(TAG, String.format(Locale.US,
                "Requesting %s permission", permission));
            ActivityCompat.requestPermissions(this, new String[]{ permission },
                REQUEST_WRITE_STORAGE);
        }
    }

    /* start activity if we find a package able to handle a given intent */
    private boolean startActivityIfSafe(Intent intent) {

        if (intent == null) {
            return false;
        }

        String intentStr = IntentUtils.intentToString(intent);

        try {
            PackageManager pm = getPackageManager();
            List<ResolveInfo> act = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

            if (act.size() > 0) {
                Logger.d(TAG,"starting activity with intent: " + intentStr);
                startActivity(intent);
                return true;
            } else {
                Logger.w(TAG,"unable to find a package for " + intentStr);
            }
            return false;
        } catch (Exception e) {
            Logger.e(TAG,
                "Error while looking for a package to open " + intentStr +
                "\nException: " + e.toString());
            return false;
        }
    }
}
