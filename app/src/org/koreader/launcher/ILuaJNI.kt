package org.koreader.launcher

/* kotlin functions and properties used by lua/JNI */

interface ILuaJNI {

    // read only properties
    val batteryLevel: Int
    val einkPlatform: String
    val flavor: String
    val isCharging: Int
    val isDebuggable: Int
    val isEink: Int
    val isEinkFull: Int
    val isFullscreen: Int
    val isWifiEnabled: Int
    val name: String
    val networkInfo: String
    val product: String
    val screenAvailableHeight: Int
    val screenHeight: Int
    val screenWidth: Int
    val statusBarHeight: Int
    val version: String

    // mutable properties
    var clipboardText: String
    var screenBrightness: Int
    var screenOffTimeout: Int

    // functions
    fun dictLookup(text: String, pkg: String, action: String)
    fun download(url: String, name: String): Int
    fun einkUpdate(mode: Int)
    fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int)
    fun extractAssets(): Int
    fun hasClipboardTextIntResultWrapper(): Int
    fun hasExternalStoragePermission(): Int
    fun hasWriteSettingsPermission(): Int
    fun needsWakelocks(): Int
    fun isPackageEnabled(pkg: String): Int
    fun openLink(url: String): Int
    fun setFullscreen(enabled: Boolean)
    fun setWakeLock(enabled: Boolean)
    fun setWifiEnabled(enabled: Boolean)
    fun showToast(message: String)
    fun showToast(message: String, is_long: Boolean)
}
