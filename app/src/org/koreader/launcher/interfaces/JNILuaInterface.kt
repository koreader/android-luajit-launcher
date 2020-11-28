package org.koreader.launcher.interfaces

/* Declares methods that are exposed to lua via JNI
 * See https://github.com/koreader/android-luajit-launcher/blob/master/assets/android.lua */

interface JNILuaInterface {
    fun canIgnoreBatteryOptimizations(): Int
    fun canWriteSystemSettings(): Int
    fun dictLookup(text: String?, action: String?, nullablePackage: String?)
    fun download(url: String, name: String): Int
    fun einkUpdate(mode: Int)
    fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int)
    fun extractAssets(): Int
    fun getBatteryLevel(): Int
    fun getClipboardText(): String
    fun getEinkPlatform(): String
    fun getExternalPath(): String
    fun getExternalSdPath(): String
    fun getFilePathFromIntent(): String?
    fun getFlavor(): String
    fun getLastImportedPath(): String?
    fun getLightDialogState(): Int
    fun getName(): String
    fun getNetworkInfo(): String
    fun getPlatformName(): String
    fun getProduct(): String
    fun getScreenAvailableHeight(): Int
    fun getScreenBrightness(): Int
    fun getScreenHeight(): Int
    fun getScreenMaxBrightness(): Int
    fun getScreenMinBrightness(): Int
    fun getScreenMaxWarmth(): Int
    fun getScreenMinWarmth(): Int
    fun getScreenOffTimeout(): Int
    fun getScreenOrientation(): Int
    fun getScreenWarmth(): Int
    fun getScreenWidth(): Int
    fun getStatusBarHeight(): Int
    fun getSystemTimeout(): Int
    fun getVersion(): String
    fun hasClipboardText(): Int
    fun hasExternalStoragePermission(): Int
    fun hasNativeRotation(): Int
    fun isCharging(): Int
    fun isChromeOS(): Int
    fun isDebuggable(): Int
    fun isEink(): Int
    fun isEinkFull(): Int
    fun isFullscreen(): Int
    fun isPackageEnabled(pkg: String): Int
    fun isPathInsideSandbox(path: String): Int
    fun isTv(): Int
    fun isWarmthDevice(): Int
    fun needsWakelocks(): Int
    fun openLink(url: String): Int
    fun openWifiSettings()
    fun performHapticFeedback(constant: Int, force: Int)
    fun requestIgnoreBatteryOptimizations(rationale: String, okButton: String, cancelButton: String)
    fun requestWriteSystemSettings(rationale: String, okButton: String, cancelButton: String)
    fun safFilePicker(path: String?): Int
    fun sendText(text: String?)
    fun setFullscreen(enabled: Boolean)
    fun setClipboardText(text: String)
    fun setIgnoreInput(enabled: Boolean)
    fun setScreenBrightness(brightness: Int)
    fun setScreenWarmth(warmth: Int)
    fun setScreenOffTimeout(ms: Int)
    fun setScreenOrientation(orientation: Int)
    fun startEPDTestActivity()
    fun showFrontlightDialog(title: String, dim: String, warmth: String, okButton: String, cancelButton: String): Int
    fun showToast(message: String)
    fun showToast(message: String, longTimeout: Boolean)
    fun enableFrontlightSwitch(): Int
}
