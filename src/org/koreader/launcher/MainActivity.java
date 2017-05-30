package org.koreader.launcher;

import android.app.NativeActivity;
import android.provider.Settings;
import android.view.WindowManager;
import android.os.BatteryManager;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

public class MainActivity extends NativeActivity {
    static {
        System.loadLibrary("luajit-launcher");
    }

    private static String TAG = "luajit-launcher";
    private FramelessProgressDialog dialog;

    public MainActivity() {
        super();
        Log.v(TAG, "Creating luajit launcher main activity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void onResume() {
        super.onResume();
        //Hide toolbar
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if(SDK_INT >= 11 && SDK_INT < 16) {
            getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        } else if (SDK_INT >= 16) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    public void setScreenBrightness(final int brightness) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //this will set the manual mode (set the automatic mode off)
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

                    //refreshes the screen
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, brightness);
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = brightness / 255.0f;
                    getWindow().setAttributes(lp);
                } catch (Exception e) {
                    Log.v(TAG, e.toString());
                }
            }
        });
    }

    public int getBatteryLevel() {
        Intent intent  = this.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (level*100)/scale;
    }

    public int isCharging() {
        Intent intent = this.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
            || plugged == BatteryManager.BATTERY_PLUGGED_USB ? 1 : 0;
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
                if(dialog!= null && dialog.isShowing()){
                    dialog.dismiss();
                }
            }
        });
    }

    public boolean isFullscreen() {
    	return (getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }

    public void setFullscreen(final boolean fullscreen) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    if (fullscreen){
                        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    } else {
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    }
                    getWindow().setAttributes(attrs);
                } catch (Exception e) {
                    Log.v(TAG, e.toString());
                }
            }
        });

}
