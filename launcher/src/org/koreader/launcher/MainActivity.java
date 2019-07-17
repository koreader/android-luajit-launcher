package org.koreader.launcher;

import android.Manifest;
import android.app.NativeActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.koreader.device.DeviceInfo;

import java.util.List;

@SuppressWarnings("unused")
public class MainActivity extends NativeActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private final static int SDK_INT = Build.VERSION.SDK_INT;
    private final static int REQUEST_WRITE_STORAGE = 1;
    private final static boolean HAS_EINK_SUPPORT = DeviceInfo.EINK_SUPPORT;
    private final static boolean HAS_FULL_EINK_SUPPORT = DeviceInfo.EINK_FULL_SUPPORT;
    private final static boolean NEEDS_WAKELOCK_ENABLED = DeviceInfo.BUG_WAKELOCKS;
    private final static String PRODUCT = DeviceInfo.PRODUCT;

    private Clipboard clipboard;
    private FramelessProgressDialog dialog;
    private IntentUtils intentUtils;
    private NetworkManager network;
    private PowerHelper power;
    private ScreenHelper screen;
    private NativeView view;
    private String tag;

    static {
        System.loadLibrary("luajit");
    }

    // size in pixels of the top notch, if any
    private int notch_height = 0;

    /* Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // (re)initialize helper classes to get rid of old values.
        initHelperClasses();

        // set a tag for logging
        tag = getName();
        Logger.d(tag, "App created");

        // take ownership of the window
        getWindow().takeSurface(null);

        // hook our native window inside the view hierarchy
        view = new NativeView(this);
        view.getHolder().addCallback(this);
        setContentView(view);

        // setup helper classes
        clipboard = new Clipboard(this);
        network = new NetworkManager(this);
        power = new PowerHelper(this);
        screen = new ScreenHelper(this);

        // Listen to visibility changes
        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setFullscreenLayout();
            View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    setFullscreenLayout();
                }
            });
        }

        // runtime permissions: we need read and write on external storage to work!
        boolean is_granted = (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

        if (!is_granted) {
            Logger.i(tag, String.format("Requesting permission: %s", REQUEST_WRITE_STORAGE));
            ActivityCompat.requestPermissions(this,
                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_WRITE_STORAGE);
        }
    }

    /* Called when the activity has become visible. */
    @Override
    protected void onResume() {
        Logger.d(tag, "App resumed");
        power.setWakelock(true);
        super.onResume();
        /** switch to fullscreen for older devices */
        if (SDK_INT < Build.VERSION_CODES.KITKAT) {
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
        Logger.d(tag, "App paused");
        power.setWakelock(false);
        super.onPause();
    }

    /* Called when the activity is no longer visible. */
    @Override
    protected void onStop() {
        Logger.d(tag, "App stopped");
        super.onStop();
    }

    /* Called just before the activity is destroyed. */
    @Override
    protected void onDestroy() {
        Logger.d(tag, "App destroyed");
        super.onDestroy();
    }

    /* Called just before the activity is resumed by an intent */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /* Called when the view is attached to a window */
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Logger.d(tag, "surface attached to a window");
        if (SDK_INT >= Build.VERSION_CODES.P) {
            // handle top "notch" on Android Pie
            android.view.DisplayCutout cutout;
            cutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
            if (cutout != null) {
                int cutout_pixels = cutout.getSafeInsetTop();
                if (notch_height != cutout_pixels) {
                    Logger.d(tag, String.format("found a top notch: %dpx", cutout_pixels));
                    notch_height = cutout_pixels;
                }
            }
        }
    }

    /* Called when the view is detached from its window */
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Logger.d(tag, "surface detached from its window");
    }

    /* Called on permission result */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Logger.d(tag, String.format("Permission granted for request code: %d", requestCode));
        } else {
            String msg = String.format("Permission rejected for request code %d", requestCode);
            switch (requestCode) {
                case REQUEST_WRITE_STORAGE:
                    // we can't work without external storage permissions
                    Logger.e(tag, msg);
                    finish();
                    break;
                default:
                    Logger.w(tag, msg);
                    break;
            }
        }
    }

    /*  These functions are exposed to lua in assets/android.lua
     *  If you add a new function here remember to write the companion
     *  lua function in that file */


    /* build */
    public int isDebuggable() {
        return (BuildConfig.DEBUG) ? 1 : 0;
    }

    public String getFlavor() {
        return getResources().getString(R.string.app_flavor);
    }

    public String getName() {
        return getResources().getString(R.string.app_name);
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
    public String getProduct() {
        return PRODUCT;
    }

    public String getVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    public int isEink() {
        return HAS_EINK_SUPPORT ? 1 : 0;
    }

    public int isEinkFull() {
        return HAS_FULL_EINK_SUPPORT ? 1 : 0;
    }

    public String getEinkPlatform() {
        if (DeviceInfo.EINK_FREESCALE) {
            return "freescale";
        } else if (DeviceInfo.EINK_ROCKCHIP){
            return "rockchip";
        } else {
            return "none";
        }
    }

    public int needsWakelocks() {
        return NEEDS_WAKELOCK_ENABLED ? 1 : 0;
    }

    public void einkUpdate(int mode) {
        view.einkUpdate(mode);
    }

    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        view.einkUpdate(mode, delay, x, y, width, height);
    }

    /* external dictionaries */
    public void dictLookup(String text, String pkg, String action) {
        Intent intent = new Intent(intentUtils.getByAction(text, pkg, action));
        if (!startActivityIfSafe(intent)) {
            Logger.e(tag, "dictionary lookup: can't find a package able to resolve action " + action);
        }
    }

    /* native dialogs and widgets run on UI Thread */
    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
                toast.show();
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

    /* package manager */
    public int isPackageEnabled(String pkg) {
        try {
            // is the package available (installed and enabled) ?
            PackageManager pm = getPackageManager();
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            boolean enabled = pm.getApplicationInfo(pkg, 0).enabled;
            Logger.d(tag, String.format("Package %s is installed. Enabled? -> %s", pkg, Boolean.toString(enabled)));
            return (enabled) ? 1 : 0;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.d(tag, String.format("Package %s is not installed.", pkg));
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
    public int getScreenBrightness() {
        return screen.getScreenBrightness();
    }

    public int getScreenHeight() {
        if (SDK_INT >= Build.VERSION_CODES.P) {
            return (screen.getScreenHeight() - notch_height);
        } else {
            return screen.getScreenHeight();
        }
    }

    public int getScreenAvailableHeight() {
        return screen.getScreenAvailableHeight();
    }

    public int getScreenWidth() {
        return screen.getScreenWidth();
    }

    public int getStatusBarHeight() {
        return screen.getStatusBarHeight();
    }

    public int isFullscreen() {
        // for newer Jelly Bean devices (apis 17 - 18)
        if ((SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) ||
            (SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            return screen.isFullscreen();
        }
        // for older devices (apis 14 - 15 - 16)
        else if (SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            return screen.isFullscreenDeprecated();
        }
        // for devices with immersive mode (api 19+)
        else {
            return 1;
        }
    }

    public void setFullscreen(final boolean enabled) {
        // for newer Jelly Bean devices (apis 17 - 18)
        if ((SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) ||
            (SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            screen.setFullscreen(enabled);
        }
        // for older devices (apis 14 - 15 - 16)
        else if (SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            screen.setFullscreenDeprecated(enabled);
        }
    }

    public void setScreenBrightness(final int brightness) {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(MainActivity.this)) {
                screen.setScreenBrightness(brightness);
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                MainActivity.this.startActivity(intent);
            }
        } else {
            screen.setScreenBrightness(brightness);
        }
    }

    /* wifi */
    public void setWifiEnabled(final boolean enabled) {
        network.setWifi(enabled);
    }

    public int isWifiEnabled() {
        return network.isWifi();
    }

    public String getNetworkInfo() {
        return network.info();
    }

    public int download(final String url, final String name) {
        return network.download(url, name);
    }

    public int openLink(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        return (startActivityIfSafe(intent)) ? 0 : 1;
    }

    // --- end of public exported functions -------------

    private void initHelperClasses() {
        clipboard = null;
        network = null;
        power = null;
        screen = null;
        view = null;
    }

    // https://stackoverflow.com/a/36842135
    private static String intentToString(Intent intent) {
        if (intent == null) return "";
        StringBuilder stringBuilder = new StringBuilder("\naction: ")
            .append(intent.getAction())
            .append("\ndata: ")
            .append(intent.getDataString())
            .append("\nextras: ")
            ;
        for (String key : intent.getExtras().keySet()) {
            stringBuilder.append(key).append("=").append(intent.getExtras().get(key)).append(" ");
        }
        return stringBuilder.toString();
    }

    private void setFullscreenLayout() {
        View decorView = getWindow().getDecorView();
        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else if(SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    private boolean startActivityIfSafe(Intent intent) {
        try {
            PackageManager pm = getPackageManager();
            List<ResolveInfo> act = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

            if (act.size() > 0) {
                Logger.d(tag, "starting activity with intent: " + intentToString(intent));
                startActivity(intent);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
