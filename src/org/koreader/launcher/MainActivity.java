package org.koreader.launcher;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import org.koreader.device.DeviceInfo;
import org.koreader.device.EPDController;
import org.koreader.device.EPDFactory;

public class MainActivity extends android.app.NativeActivity {

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
    private ScreenHelper screen;

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
        screen = new ScreenHelper(this, LOGGER_NAME);

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
    }

    /** Called when the activity has become visible. */
    @Override
    protected void onResume() {
        Log.v(LOGGER_NAME, "App resumed");
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
	screen = null;
        super.onDestroy();
    }

    /** Called just before the activity is resumed by an intent */
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

    /** device */
    public String getProduct() {
        return device.PRODUCT;
    }

    public String getVersion() {
        return android.os.Build.VERSION.RELEASE;
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

    /** dialogs */
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

    /** screen */
    public int getScreenBrightness() {
        return screen.getScreenBrightness();
    }

    public int getScreenHeight() {
        return screen.getScreenHeight();
    }

    public int getScreenWidth() {
        return screen.getScreenWidth();
    }

    public int isFullscreen() {
        return screen.isFullscreen();
    }

    public void setFullscreen(final boolean enabled) {
        screen.setFullscreen(enabled);
    }

    public void setScreenBrightness(final int brightness) {
        screen.setScreenBrightness(brightness);
    }

    /** wifi */
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

    public String getNetworkInfo() {
        final WifiInfo wi = getWifiManager().getConnectionInfo();
        final DhcpInfo dhcp = getWifiManager().getDhcpInfo();

        int ip = wi.getIpAddress();
        int gw = dhcp.gateway;
        String ip_address;
        String gw_address;

        if (ip > 0) {
            ip_address = Formatter.formatIpAddress(ip);
        } else {
            ip_address = String.valueOf(ip);
        }

        if (gw > 0) {
            gw_address = Formatter.formatIpAddress(gw);
        } else {
            gw_address = String.valueOf(gw);
        }

        return String.format("%s;%s;%s", wi.getSSID(), ip_address, gw_address);
    }

    // ----------------------------------
    private WifiManager getWifiManager() {
        return (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
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
        } else {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }
}
