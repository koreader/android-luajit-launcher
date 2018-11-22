package org.koreader.launcher;

import android.app.NativeActivity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.concurrent.CountDownLatch;

public class MainActivity extends NativeActivity {

    private final static int SDK_INT = android.os.Build.VERSION.SDK_INT;
    private final static String LOGGER_NAME = "luajit-launcher";

    static {
        System.loadLibrary("luajit");
    }

    private FramelessProgressDialog dialog;

    public MainActivity() {
        super();
        Log.v(LOGGER_NAME, "Creating luajit launcher main activity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // set fullscreen immediately on general principle
        setFullscreenLayout();

        // set fullscreen delayed because presumably some devices don't work right otherwise
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setFullscreenLayout();
            }
        }, 500);
    }

    private void setFullscreenLayout() {
        if (SDK_INT >= 11 && SDK_INT < 16) {
            getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        } else if (SDK_INT >= 16 && SDK_INT < 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else if (SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public int getScreenBrightness() {
        final Box<Integer> result = new Box<Integer>();
        final CountDownLatch cd = new CountDownLatch(1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    result.value = new Integer(Settings.System.getInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS));
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                    result.value = new Integer(0);
                }
                cd.countDown();
            }
        });
        try {
            cd.await();
        } catch (InterruptedException ex) {
            return 0;
        }

        if (result.value == null) {
            return 0;
        }

        return result.value.intValue();
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

                    Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, brightness);
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                }
            }
        });
    }

    public int getBatteryLevel() {
        Intent intent = this.registerReceiver(null,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (level * 100) / scale;
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
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });
    }

    public int isFullscreen() {
        return ((getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) ? 1 : 0;
    }


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

    private WifiManager getWifiManager() {
        return (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    }

    public void setKeepScreenOn(final boolean keepOn) {
        final CountDownLatch cd = new CountDownLatch(1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (keepOn) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                }
                cd.countDown();
            }
        });
        try {
            cd.await();
        } catch (InterruptedException ex) {
        }
    }


    public void setFullscreen(final boolean fullscreen) {
        final CountDownLatch cd = new CountDownLatch(1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    if (fullscreen) {
                        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    } else {
                        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    }
                    getWindow().setAttributes(attrs);
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                }
                cd.countDown();
            }
        });
        try {
            cd.await();
        } catch (InterruptedException ex) {
        }
    }

    public String getClipboardText() {
      final Box<String> result = new Box<String>();
      final CountDownLatch cd = new CountDownLatch(1);
      runOnUiThread(new Runnable() {
          @Override
          public void run() {
              try {
                  if (hasClipboardText()) {
                      if (SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                          android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                          result.value = clipboard.getText().toString();
                      } else {
                          ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                          ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                          result.value = item.getText().toString();
                      }
                  }
              } catch (Exception e) {
                  Log.v(LOGGER_NAME, e.toString());
                  result.value = "";
              }
              cd.countDown();
          }
      });
      try {
          cd.await();
      } catch (InterruptedException ex) {
          return "";
      }

      if (result.value == null) {
          return "";
      }

      return result.value;
    }

    public void setClipboardText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(text);
                    } else {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("KOReader_clipboard", text);
                        clipboard.setPrimaryClip(clip);
                    }
                } catch (Exception e) {
                    Log.v(LOGGER_NAME, e.toString());
                }
            }
        });
    }

    public boolean hasClipboardText() {
        if (SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            return clipboard.hasText();
        } else {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            return clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
        }
    }

    public int hasClipboardTextIntResultWrapper() {
      final Box<Integer> result = new Box<Integer>();
      final CountDownLatch cd = new CountDownLatch(1);
      runOnUiThread(new Runnable() {
          @Override
          public void run() {
              try {
                  result.value = hasClipboardText() ? 1 : 0;
              } catch (Exception e) {
                  Log.v(LOGGER_NAME, e.toString());
                  result.value = new Integer(0);
              }
              cd.countDown();
          }
      });
      try {
          cd.await();
      } catch (InterruptedException ex) {
          return 0;
      }

      if (result.value == null) {
          return 0;
      }

      return result.value.intValue();
    }

    public int getStatusBarHeight() {
        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        return statusBarHeight;
    }

    public int getScreenWidth() {
        int width = getSceenSize().x;
        return width;
    }

    public int getScreenHeight() {
        int height = getSceenSize().y;
        return height;
    }

    private Point getSceenSize() {
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();

        try {
            // For JellyBean 4.2 (API 17) and onward
            if (SDK_INT >= 17) {
                DisplayMetrics metrics = new DisplayMetrics();
                display.getRealMetrics(metrics);
                size.set(metrics.widthPixels, metrics.heightPixels);
            // Android 3.0 (API 11) and higher
            } else if (SDK_INT >= 11) {
                display.getSize(size);
            } else {
                size.set(display.getWidth(), display.getHeight());
            }
        } catch (Exception e) {
            Log.v(LOGGER_NAME, e.toString());
        }
        return size;
    }

    private class Box<T> {
        public T value;
    }
}
