package org.koreader.launcher

/* Declares methods that are exposed to lua via JNI
 * See https://github.com/koreader/android-luajit-launcher/blob/master/assets/android.lua */

internal interface JNILuaInterface {
    fun canWriteSystemSettings(): Int
    fun dictLookup(text: String, action: String)
    fun dictLookup(text: String, pkg: String?, action: String)
    fun download(url: String, name: String): Int
    fun einkUpdate(mode: Int)
    fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int)
    fun extractAssets(): Int
    fun getBatteryLevel(): Int
    fun getClipboardText(): String
    fun getEinkPlatform(): String
    fun getExternalPath(): String
    fun getFlavor(): String
    fun getName(): String
    fun getNetworkInfo(): String
    fun getProduct(): String
    fun getScreenBrightness(): Int
    fun getScreenOffTimeout(): Int
    fun getScreenAvailableHeight(): Int
    fun getScreenHeight(): Int
    fun getScreenWidth(): Int
    fun getStatusBarHeight(): Int
    fun getSystemTimeout(): Int
    fun getVersion(): String
    fun hasClipboardText(): Int
    fun hasExternalStoragePermission(): Int
    fun isCharging(): Int
    fun isDebuggable(): Int
    fun isEink(): Int
    fun isEinkFull(): Int
    fun isFullscreen(): Int
    fun isPackageEnabled(pkg: String): Int
    fun isWifiEnabled(): Int
    fun needsWakelocks(): Int
    fun openLink(url: String): Int
    fun performHapticFeedback(constant: Int)
    fun requestWriteSystemSettings()
    fun setFullscreen(enabled: Boolean)
    fun setClipboardText(text: String)
    fun setHapticOverride(enabled: Boolean)
    fun setScreenBrightness(brightness: Int)
    fun setScreenOffTimeout(timeout: Int)
    fun setWifiEnabled(enabled: Boolean)
    fun showToast(message: String)
    fun showToast(message: String, longTimeout: Boolean)
}
