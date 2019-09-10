package org.koreader.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import android.app.NativeActivity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import org.koreader.launcher.device.DeviceInfo;
import org.koreader.launcher.helper.ClipboardHelper;
import org.koreader.launcher.helper.NetworkHelper;
import org.koreader.launcher.helper.PowerHelper;
import org.koreader.launcher.helper.ScreenHelper;


/* BaseActivity.java
 *
 * Convenience wrapper on top of NativeActivity with a base implementation of the JNILuaInterface.
 * This class doesn't know about views and runtime permissions.
 */

abstract class BaseActivity extends NativeActivity implements JNILuaInterface {
    private final static String TAG = "BaseActivity";

    // device info
    private final static String PRODUCT = DeviceInfo.PRODUCT;
    private final static String RUNTIME_VERSION = android.os.Build.VERSION.RELEASE;
    private final static boolean HAS_EINK_SUPPORT = DeviceInfo.EINK_SUPPORT;
    private final static boolean HAS_FULL_EINK_SUPPORT = DeviceInfo.EINK_FULL_SUPPORT;
    private final static boolean NEEDS_WAKELOCK_ENABLED = DeviceInfo.BUG_WAKELOCKS;

    // power
    private static final int WAKELOCK_MIN_DURATION = 15 * 1000;
    private static final int WAKELOCK_MAX_DURATION = 45 * 60 * 1000;
    private static final String WAKELOCK_ID = "wakelock:screen_bright";
    private int wakelock_duration = WAKELOCK_MAX_DURATION;
    private boolean isWakeLockAllowed = false;
    private PowerManager.WakeLock wakelock;

    // windows insets
    private int top_inset_height;

    // helpers
    private FramelessProgressDialog dialog;
    private IntentFilter powerFilter;
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
        clipboard = new ClipboardHelper(this);
        network = new NetworkHelper(this);
        power = new PowerHelper(this);
        powerFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
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

    /* assets */
    public int extractAssets() {
        String output = getFilesDir().getAbsolutePath();
        try {
            // is there any zip file inside the asset module?
            String zipFile = AssetsUtils.getZipFile(this);
            if (zipFile != null) {
                // zipfile found! it will be extracted or not based on its version name
                Logger.i("Check file in asset module: " + zipFile);
                if (!AssetsUtils.isSameVersion(this, zipFile)) {
                    showProgress("");
                    long startTime = System.nanoTime();
                    Logger.i("Installing new package to " + output);
                    InputStream stream = getAssets().open("module/" + zipFile);
                    AssetsUtils.unzip(stream, output);
                    long endTime = System.nanoTime();
                    long elapsedTime = endTime - startTime;
                    Logger.v("update installed in " + elapsedTime / 1000000000 + " seconds");
                    dismissProgress();
                }
                // extracted without errors.
                return 1;
            } else {
                // check if the app has other, non-zipped, raw assets
                Logger.i("Zip file not found, trying raw assets...");
                return AssetsUtils.copyUncompressedAssets(this) ? 1 : 0;
            }
        } catch (IOException e) {
            Logger.e(TAG, "error extracting assets:\n" + e.toString());
            dismissProgress();
            return 0;
        }
    }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                return "freescale";
            } else {
                return "freescale-legacy";
            }
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

    /* eink updates: to be implemented on subclasses. Here we just log */
    public void einkUpdate(int mode) {
        Logger.w(TAG,
            "einkUpdate(mode) not implemented in this class!");
    }

    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        Logger.w(TAG,
            "einkUpdate(mode, delay, x, y, width, height) not implemented in this class!");
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
            Logger.e(TAG,
                "dictionary lookup: can't find a package able to resolve action " + action);
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
        return getBatteryState(false);
    }

    public int getBatteryLevel() {
        return getBatteryState(true);
    }

    /* screen */
    public int getScreenBrightness() {
        return screen.getScreenBrightness();
    }

    public int getSystemTimeout() {
        return screen.getSystemTimeout();
    }

    public int getScreenOffTimeout() {
        return screen.app_timeout;
    }

    public int getScreenAvailableHeight() {
        return screen.getScreenAvailableHeight(this);
    }

    public int getScreenHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (screen.getScreenHeight(this) - top_inset_height);
        } else {
            return screen.getScreenHeight(this);
        }
    }

    public int getScreenWidth() {
        return screen.getScreenWidth(this);
    }

    public int getStatusBarHeight() {
        return screen.getStatusBarHeight(this);
    }

    public int isFullscreen() {
        // for newer Jelly Bean devices (apis 17 - 18)
        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) ||
            (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            return screen.isFullscreen();
        }
        // for older devices (apis 14 - 15 - 16)
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            return screen.isFullscreenDeprecated(this);
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
            screen.setFullscreenDeprecated(this, enabled);
        }
    }

    public void setScreenBrightness(final int brightness) {
        screen.setScreenBrightness(this, brightness);
    }

    public void setScreenOffTimeout(final int timeout) {
        // update app timeout first
        screen.app_timeout = timeout;
        // toggle wakelocks
        if ((timeout > ScreenHelper.TIMEOUT_SYSTEM) || (timeout == ScreenHelper.TIMEOUT_WAKELOCK)) {
            power.setWakelockDuration(timeout);
            power.setWakelockState(true);
        } else {
            power.setWakelockDuration(0);
            power.setWakelockState(false);
        }
    }

    /* widgets */
    public void showToast(final String message) {
        showToast(message, false);
    }

    public void showToast(final String message, final boolean is_long) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (is_long) {
                    final Toast toast = Toast.makeText(BaseActivity.this,
                        message, Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    final Toast toast = Toast.makeText(BaseActivity.this,
                        message, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }


    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    /* battery */
    private int getBatteryState(boolean isPercent) {
        Intent intent = getApplicationContext().registerReceiver(null, filter);

        if (intent != null) {
            final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            final int percent = (level * 100) / scale;

            if (isPercent) {
                return percent;
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                       plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                return (percent != 100) ? 1 : 0;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }


    /* dialogs */
    private void showProgress(final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = FramelessProgressDialog.show(BaseActivity.this, title);
            }
        });
    }

    private void dismissProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });
    }

    private void setWakelockState(final boolean enabled) {
        /* release wakelock first, if present and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock != null) wakelockRelease();
        /* update wakelock settings */
        isWakeLockAllowed = enabled;
        /* acquire wakelock if we don't have one and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock == null) wakelockAcquire();
    }

    private void wakelockAcquire() {
        if (isWakeLockAllowed) {
            wakelockRelease();
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            // release after a custom time running in the foreground without inputs.
            // it will be acquired again on the next resume callback.
            int time = getWakelockDuration();
            if (time > 0) {
                wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, WAKELOCK_ID);
                Logger.v(getTag(), "acquiring " + WAKELOCK_ID + " during " +
                    String.valueOf(wakelock_duration / (60 * 1000)) + " minutes");
                wakelock.acquire(time);
            }
        }
    }

    private void wakelockRelease() {
        if (isWakeLockAllowed && wakelock != null) {
            Logger.v(getTag(), "releasing " + WAKELOCK_ID);
            wakelock.release();
            wakelock = null;
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
