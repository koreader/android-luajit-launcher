package org.koreader.launcher;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import org.koreader.device.DeviceInfo;
import org.koreader.device.EPDController;
import org.koreader.device.EPDFactory;

public class MainActivity extends android.app.NativeActivity
    implements SurfaceHolder.Callback2,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private final static int SDK_INT = Build.VERSION.SDK_INT;
    private final static int REQUEST_WRITE_STORAGE = 1;

    static {
        System.loadLibrary("luajit");
    }

    private String TAG;

    private FramelessProgressDialog dialog;
    private Clipboard clipboard;
    private EPDController epd;
    private PowerHelper power;
    private ScreenHelper screen;
    private SurfaceView surface;

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

        // set a epd controller for eink devices
        epd = EPDFactory.getEPDController(TAG);

        // helper classes
        clipboard = new Clipboard(this);
        power = new PowerHelper(this);
        screen = new ScreenHelper(this);

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
        power = null;
	screen = null;
        epd = null;
        surface = null;
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
        return DeviceInfo.PRODUCT;
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    @SuppressWarnings("unused")
    public int isEink() {
        return (DeviceInfo.EINK_SUPPORT) ? 1 : 0;
    }

    @SuppressWarnings("unused")
    public int isEinkFull() {
        return (DeviceInfo.EINK_FULL_SUPPORT) ? 1 : 0;
    }

    @SuppressWarnings("unused")
    public String getEinkPlatform() {
        if (DeviceInfo.EINK_FREESCALE) {
            return "freescale";
        } else if (DeviceInfo.EINK_ROCKCHIP){
            return "rockchip";
        } else {
            return "none";
        }
    }

    @SuppressWarnings("unused")
    public int needsWakelocks() {
        return (DeviceInfo.BUG_WAKELOCKS) ? 1 : 0;
    }

    /** Used on Rockchip devices */
    @SuppressWarnings("unused")
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
            Logger.e(TAG, String.format("%s: %d", mode_name, mode));
            return;
        }
        Logger.v(TAG, String.format("requesting epd update, type: %s", mode_name));
        epd.setEpdMode(surface, 0, 0, 0, 0, 0, 0, mode_name);
    }

    /** Used on Freescale imx devices */
    @SuppressWarnings("unused")
    public void einkUpdate(int mode, long delay, int x, int y, int width, int height) {
        Logger.v(TAG, String.format("requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
            mode, delay, x, y, width, height));
        epd.setEpdMode(surface, mode, delay, x, y, width, height, null);
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
        return screen.getScreenHeight();
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWifiManager().setWifiEnabled(enabled);
            }
        });
    }

    @SuppressWarnings("unused")
    public int isWifiEnabled() {
        return getWifiManager().isWifiEnabled() ? 1 : 0;
    }

    @SuppressWarnings("unused")
    public String getNetworkInfo() {
        final WifiInfo wi = getWifiManager().getConnectionInfo();
        final DhcpInfo dhcp = getWifiManager().getDhcpInfo();
        int ip = wi.getIpAddress();
        int gw = dhcp.gateway;
        String ip_address = formatIp(ip);
        String gw_address = formatIp(gw);
        return String.format("%s;%s;%s", wi.getSSID(), ip_address, gw_address);
    }

    @SuppressWarnings("unused")
    public int download(final String url, final String name) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS) + "/" + name);

        Logger.d(TAG, file.getAbsolutePath());
        if (file.exists()) return 1;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        return 0;
    }

    @SuppressWarnings("unused")
    public int openLink(String url) {
        return openWebPage(url) ? 0 : 1;
    }

    // --- end of public exported functions -------------

    @SuppressWarnings("deprecation")
    private String formatIp(int number) {
        if (number > 0) {
            return Formatter.formatIpAddress(number);
        } else {
            return String.valueOf(number);
        }
    }

    private WifiManager getWifiManager() {
        return (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

    private boolean openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            return true;
        } else {
            // cannot find a package able to open the page
            return false;
        }
    }
}
