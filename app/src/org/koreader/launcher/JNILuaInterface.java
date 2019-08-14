package org.koreader.launcher;

/* Declares methods that are exposed to lua via JNI
 *
 * Note: inspection reports unused methods but they are all used by lua functions.
 * See https://github.com/koreader/android-luajit-launcher/blob/master/assets/android.lua
 */
@SuppressWarnings("unused")
interface JNILuaInterface {

    void setClipboardText(final String text);
    void einkUpdate(int mode);
    void einkUpdate(int mode, long delay, int x, int y, int width, int height);
    void dictLookup(String text, String pkg, String action);
    void dismissProgress();
    void setFullscreen(boolean enabled);
    void setScreenBrightness(int brightness);
    void setScreenOffTimeout(int timeout);
    void setWakeLock(boolean enabled);
    void setWifiEnabled(boolean enabled);
    void showProgress(String title, String message);
    void showToast(String message);
    void showToast(String message, final boolean is_long);

    int download(String url, String name);
    int getBatteryLevel();
    int getScreenOffTimeout();
    int getScreenAvailableHeight();
    int getScreenHeight();
    int getScreenWidth();
    int getStatusBarHeight();
    int hasClipboardTextIntResultWrapper();
    int hasExternalStoragePermission();
    int hasWriteSettingsPermission();
    int importFileFromStorageAccessFramework();
    int isCharging();
    int isDebuggable();
    int isEink();
    int isEinkFull();
    int isFullscreen();
    int isPackageEnabled(String pkg);
    int isWifiEnabled();
    int needsWakelocks();
    int openLink(String url);

    String getClipboardText();
    String getEinkPlatform();
    String getFilePathFromIntent();
    String getFlavor();
    String getLastImportedFilePath();
    String getName();
    String getNetworkInfo();
    String getProduct();
    String getVersion();
}
