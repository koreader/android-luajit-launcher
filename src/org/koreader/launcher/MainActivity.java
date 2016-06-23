package org.koreader.launcher;

import android.app.NativeActivity;
import android.provider.Settings;
import android.view.WindowManager;
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
        if(SDK_INT >= 11 && SDK_INT < 14) {
            getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        } else if (SDK_INT >= 14) {
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
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = (float) brightness / 25;
                    getWindow().setAttributes(lp);
                } catch (Exception e) {
                    Log.v(TAG, e.toString());
                }
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
                if(dialog!= null && dialog.isShowing()){
                    dialog.dismiss();
                }
            }
        });
    }
}
