package org.koreader.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends android.app.NativeActivity implements SurfaceHolder.Callback2,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private final static int SDK_INT = Build.VERSION.SDK_INT;
    private final static int REQUEST_WRITE_STORAGE = 1;

    static {
        System.loadLibrary("luajit");
    }

    private Clipboard clipboard;
    private Device device;
    private FramelessProgressDialog dialog;
    private IntentUtils intentUtils;
    private NetworkManager network;
    private PowerHelper power;
    private ScreenHelper screen;
    private SurfaceView surface;
    private String TAG;

    // size in pixels of the top notch, if any
    private int notch_height = 0;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set a tag for logging
        TAG = getName();
        Logger.d(TAG, "App created");

        // set the native window as an android surface. Useful in *some* eink devices,
        // where the epd driver is hooked in the View class framework.
        getWindow().takeSurface(null);
        surface = new SurfaceView(this);
        SurfaceHolder holder = surface.getHolder();
        holder.addCallback(this);
        setContentView(surface);

        // helper classes
        clipboard = new Clipboard(this);
        device = new Device(this);
        power = new PowerHelper(this);
        screen = new ScreenHelper(this);
        network = new NetworkManager(this);

        /** Listen to visibility changes */
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

        /** runtime permissions: we need read and write on external storage to work! */
        boolean is_granted = (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

        if (!is_granted) {
            Logger.i(TAG, String.format("Requesting permission: %s", REQUEST_WRITE_STORAGE));
            ActivityCompat.requestPermissions(this,
                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_WRITE_STORAGE);
        }
    }

    /** Called when the activity has become visible. */
    @Override
    protected void onResume() {
        Logger.d(TAG, "App resumed");
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

    /** Called when another activity is taking focus. */
    @Override
    protected void onPause() {
        Logger.d(TAG, "App paused");
        power.setWakelock(false);
        super.onPause();
    }

    /** Called when the activity is no longer visible. */
    @Override
    protected void onStop() {
        Logger.d(TAG, "App stopped");
        super.onStop();
    }

    /** Called just before the activity is destroyed. */
    @Override
    protected void onDestroy() {
        Logger.d(TAG, "App destroyed");
        clipboard = null;
        device = null;
        power = null;
        screen = null;
        surface = null;
        network = null;
        super.onDestroy();
    }

    /** Called just before the activity is resumed by an intent */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /** Called when a new surface is created */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logger.d(TAG, "Surface created");
        super.surfaceCreated(holder);
        surface.setWillNotDraw(false);
    }

    /** Called after a surface change */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.d(TAG, String.format(
            "Surface changed {\n  format:	%d\n  width:	%d\n  height:	%d\n}",
            format, width, height));

        super.surfaceChanged(holder,format,width,height);
    }

    /** Called when the surface is destroyed */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Logger.d(TAG, "Surface destroyed");
        super.surfaceDestroyed(holder);
    }

    /** Called when the view is attached to a window */
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Logger.d(TAG, "surface attached to a window");
        if (SDK_INT >= Build.VERSION_CODES.P) {
            // handle top "notch" on Android Pie
            android.view.DisplayCutout cutout;
            cutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
            if (cutout != null) {
                int cutout_pixels = cutout.getSafeInsetTop();
                if (notch_height != cutout_pixels) {
                    Logger.d(TAG, String.format("found a top notch: %dpx", cutout_pixels));
                    notch_height = cutout_pixels;
                }
            }
        }
    }

    /** Called when the view is detached from its window */
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Logger.d(TAG, "surface detached from its window");
    }

    /** Called on permission result */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Logger.d(TAG, String.format("Permission granted for request code: %d", requestCode));
        } else {
            String msg = String.format("Permission rejected for request code %d", requestCode);
            switch (requestCode) {
                case REQUEST_WRITE_STORAGE:
                    // we can't work without external storage permissions
                    Logger.e(TAG, msg);
                    finish();
                    break;
                default:
                    Logger.w(TAG, msg);
                    break;
            }
        }
    }

    /** These functions are exposed to lua in assets/android.lua
     *  If you add a new function here remember to write the companion
     *  lua function in that file */


    /** build */
    @SuppressWarnings("unused")
    public int isDebuggable() {
        return (BuildConfig.DEBUG) ? 1 : 0;
    }

    @SuppressWarnings("unused")
    public String getFlavor() {
        return getResources().getString(R.string.app_flavor);
    }

    @SuppressWarnings("unused")
    public String getName() {
        return getResources().getString(R.string.app_name);
    }

    /** clipboard */
    @SuppressWarnings("unused")
    public String getClipboardText() {
        return clipboard.getClipboardText();
    }

    @SuppressWarnings("unused")
    public int hasClipboardTextIntResultWrapper() {
        return clipboard.hasClipboardText();
    }

    @SuppressWarnings("unused")
    public void setClipboardText(final String text) {
        clipboard.setClipboardText(text);
    }

    /** device */
    @SuppressWarnings("unused")
    public String getProduct() {
        return device.getProduct();
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    @SuppressWarnings("unused")
    public int isEink() {
        return device.isEink();
    }

    @SuppressWarnings("unused")
    public int isEinkFull() {
        return device.isFullEink();
    }

    @SuppressWarnings("unused")
    public String getEinkPlatform() {
        return device.einkPlatform();
    }

    @SuppressWarnings("unused")
    public int needsWakelocks() {
        return device.needsWakelock();
    }

    @SuppressWarnings("unused")
    public void einkUpdate(int mode) {
        device.einkUpdate(surface, mode);
    }

    @SuppressWarnings("unused")
    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        device.einkUpdate(surface, mode, delay, x, y, width, height);
    }

    /** external dictionaries */
    @SuppressWarnings("unused")
    public void dictLookup(String text, String pkg, String action) {
        Intent intent = new Intent(intentUtils.getByAction(text, pkg, action));
        if (!startActivityIfSafe(intent)) {
            Logger.e(TAG, "dictionary lookup: can't find a package able to resolve action " + action);
        }
    }

    /** native dialogs and widgets run on UI Thread */
    @SuppressWarnings("unused")
    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    @SuppressWarnings("unused")
    public void showProgress(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = FramelessProgressDialog.show(MainActivity.this,
                    title, message, true, false);
            }
        });
    }

    @SuppressWarnings("unused")
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

    /** package manager */
    @SuppressWarnings("unused")
    public int isPackageEnabled(String pkg) {
        try {
            // is the package available (installed and enabled) ?
            PackageManager pm = getPackageManager();
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            boolean enabled = pm.getApplicationInfo(pkg, 0).enabled;
            Logger.d(TAG, String.format("Package %s is installed. Enabled? -> %s", pkg, Boolean.toString(enabled)));
            return (enabled) ? 1 : 0;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.d(TAG, String.format("Package %s is not installed.", pkg));
            return 0;
        }
    }

    /** power */
    @SuppressWarnings("unused")
    public int isCharging() {
        return power.batteryCharging();
    }

    @SuppressWarnings("unused")
    public int getBatteryLevel() {
        return power.batteryPercent();
    }

    @SuppressWarnings("unused")
    public void setWakeLock(final boolean enabled) {
        power.setWakelockState(enabled);
    }

    /** screen */
    @SuppressWarnings("unused")
    public int getScreenBrightness() {
        return screen.getScreenBrightness();
    }

    @SuppressWarnings("unused")
    public int getScreenHeight() {
        if (SDK_INT >= Build.VERSION_CODES.P) {
            return (screen.getScreenHeight() - notch_height);
        } else {
            return screen.getScreenHeight();
        }
    }

    @SuppressWarnings("unused")
    public int getScreenAvailableHeight() {
        return screen.getScreenAvailableHeight();
    }

    @SuppressWarnings("unused")
    public int getScreenWidth() {
        return screen.getScreenWidth();
    }

    @SuppressWarnings("unused")
    public int getStatusBarHeight() {
        return screen.getStatusBarHeight();
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
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

    /** wifi */
    @SuppressWarnings("unused")
    public void setWifiEnabled(final boolean enabled) {
        network.setWifi(enabled);
    }

    @SuppressWarnings("unused")
    public int isWifiEnabled() {
        return network.isWifi();
    }

    @SuppressWarnings("unused")
    public String getNetworkInfo() {
        return network.info();
    }

    @SuppressWarnings("unused")
    public int download(final String url, final String name) {
        return network.download(url, name);
    }

    @SuppressWarnings("unused")
    public int openLink(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        return (startActivityIfSafe(intent)) ? 0 : 1;
    }

    // --- end of public exported functions -------------

    // https://stackoverflow.com/a/36842135
    public static String intentToString(Intent intent) {
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
                Logger.d(TAG, "starting activity with intent: " + intentToString(intent));
                startActivity(intent);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
