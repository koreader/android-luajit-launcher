package org.koreader.launcher

import androidx.annotation.WorkerThread

/* Declares methods that are exposed to lua via JNI
 * See https://github.com/koreader/android-luajit-launcher/blob/master/assets/android.lua */

@WorkerThread
interface LuaInterface {
    fun canIgnoreBatteryOptimizations(): Boolean
    fun canWriteSystemSettings(): Boolean
    fun dictLookup(text: String?, action: String?, nullablePackage: String?)
    fun download(url: String, name: String): Int
    fun dumpLogs()
    fun einkUpdate(mode: Int)
    fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int)
    fun enableFrontlightSwitch(): Boolean
    fun extractAssets(): Boolean
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
    fun getScreenAvailableWidth(): Int
    fun getScreenBrightness(): Int
    fun getScreenHeight(): Int
    fun getScreenMaxBrightness(): Int
    fun getScreenMinBrightness(): Int
    fun getScreenMaxWarmth(): Int
    fun getScreenMinWarmth(): Int
    fun getScreenOrientation(): Int
    fun getScreenWarmth(): Int
    fun getScreenWidth(): Int
    fun getStatusBarHeight(): Int
    fun getVersion(): String
    fun hasBrokenLifecycle(): Boolean
    fun hasClipboardText(): Boolean
    fun hasNativeRotation(): Boolean
    fun hasOTAUpdates(): Boolean
    fun hasRuntimeChanges(): Boolean
    fun installApk()
    fun isCharging(): Boolean
    fun isChromeOS(): Boolean
    fun isDebuggable(): Boolean
    fun isEink(): Boolean
    fun isEinkFull(): Boolean
    fun isFullscreen(): Boolean
    fun isPackageEnabled(pkg: String): Boolean
    fun isPathInsideSandbox(path: String): Boolean
    fun isActivityResumed(): Boolean
    fun isTv(): Boolean
    fun isWarmthDevice(): Boolean
    fun needsWakelocks(): Boolean
    fun openLink(url: String): Boolean
    fun openWifiSettings()
    fun performHapticFeedback(constant: Int, force: Int)
    fun requestIgnoreBatteryOptimizations(rationale: String, okButton: String, cancelButton: String)
    fun requestWriteSystemSettings(rationale: String, okButton: String, cancelButton: String)
    fun safFilePicker(path: String?): Boolean
    fun sendText(text: String?)
    fun setFullscreen(enabled: Boolean)
    fun setClipboardText(text: String)
    fun setIgnoreInput(enabled: Boolean)
    fun setScreenBrightness(brightness: Int)
    fun setScreenWarmth(warmth: Int)
    fun setScreenOffTimeout(ms: Int)
    fun setScreenOrientation(orientation: Int)
    fun startEPDTestActivity()
    fun showFrontlightDialog(title: String, dim: String, warmth: String, okButton: String, cancelButton: String)
    fun showToast(message: String, longTimeout: Boolean)
    fun untar(filePath: String, outputPath: String): Boolean
}
