package org.koreader.launcher

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.NativeActivity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.koreader.launcher.interfaces.JNILuaInterface
import org.koreader.launcher.utils.*
import java.util.*

@Keep
class MainActivity : NativeActivity(), JNILuaInterface,
    ActivityCompat.OnRequestPermissionsResultCallback{

    private lateinit var assets: Assets
    private lateinit var clipboard: Clipboard
    private lateinit var device: Device
    private lateinit var event: EventReceiver
    private lateinit var timeout: Timeout

    // Path of last file imported
    private var lastImportedPath: String? = null

    // Some devices need to take control of the native window
    private var takesWindowOwnership: Boolean = false

    // Device cutout - only used on API 28+
    private var topInsetHeight: Int = 0

    // Fullscreen - only used on API levels 16-18
    private var fullscreen: Boolean = true

    // Splashscreen is active
    private var splashScreen: Boolean = true

    // surface used on devices that need a view
    private var view: NativeSurfaceView? = null
    private class NativeSurfaceView(context: Context): SurfaceView(context),
        SurfaceHolder.Callback {
        val tag = "NativeSurfaceView"
        init { holder.addCallback(this) }
        override fun surfaceCreated(holder: SurfaceHolder) {
            Logger.v(tag, "surface created")
            setWillNotDraw(false)
        }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Logger.v(tag, String.format(Locale.US,
                "surface changed {\n  width:  %d\n  height: %d\n format: %s\n}",
                width, height, ScreenUtils.pixelFormatName(format))
            )
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Logger.v(tag, "surface destroyed")
        }
    }

    companion object {
        private const val TAG_MAIN = "MainActivity"
        private const val ACTION_SAF_FILEPICKER = 2
        private val BATTERY_FILTER = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        private val RUNTIME_VERSION = Build.VERSION.RELEASE
    }

    init {
        System.loadLibrary("luajit")
    }

    /*---------------------------------------------------------------
     *                        activity callbacks                    *
     *--------------------------------------------------------------*/

    /* Called when the activity is first created. */
    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.v(String.format(Locale.US,
            "Launching %s %s", BuildConfig.APP_NAME, MainApp.info))

        assets = Assets()
        clipboard = Clipboard(this)
        device = Device(this)
        timeout = Timeout()
        event = EventReceiver()

        super.onCreate(savedInstanceState)
        setTheme(R.style.Fullscreen)

        // Window background must be black for vertical and horizontal lines to be visible
        window.setBackgroundDrawableResource(android.R.color.black)

        val surfaceKind: String = if (device.needsView) {
            view = NativeSurfaceView(this)
            window.takeSurface(null)
            view?.holder?.addCallback(this)
            setContentView(view)
            takesWindowOwnership = true
            "SurfaceView"
        } else {
            "Native Content"
        }
        Logger.v(TAG_MAIN, "surface: $surfaceKind")

        registerReceiver(event, event.filter)
        if (!Permissions.hasStoragePermission(this)) {
            Permissions.requestStoragePermission(this)
        }
    }

    /* Called when the activity has become visible. */
    override fun onResume() {
        super.onResume()
        device.onResume()
        timeout.onResume(this)
    }

    /* Called when another activity is taking focus. */
    override fun onPause() {
        super.onPause()
        device.onPause()
        timeout.onPause(this)
        intent = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        drawSplashScreen(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Logger.v(TAG_MAIN, String.format(Locale.US,
            "surface changed {\n  width:  %d\n  height: %d\n format: %s\n}",
            width, height, ScreenUtils.pixelFormatName(format))
        )
        super.surfaceChanged(holder, format, width, height)
        drawSplashScreen(holder)
    }

    override fun onAttachedToWindow() {
        Logger.d(TAG_MAIN, "onAttachedToWindow()")
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cut: DisplayCutout? = window.decorView.rootWindowInsets.displayCutout
            if (cut != null) {
                val cutPixels = cut.safeInsetTop
                if (topInsetHeight != cutPixels) {
                    Logger.v(TAG_MAIN,
                        "top $cutPixels pixels are not available, reason: window inset")
                    topInsetHeight = cutPixels
                }
            }
        }
    }

    /* Called just before the activity is resumed by an intent */
    override fun onNewIntent(intent: Intent) {
        val scheme = intent.scheme
        Logger.d(TAG_MAIN, "onNewIntent(): $scheme")
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /* Called on permission result */
    override fun onRequestPermissionsResult(requestCode: Int, permissions:
        Array<String>, grantResults: IntArray) {
        Logger.d(TAG_MAIN, "onRequestPermissionResult()")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permissions.hasStoragePermission(this)) {
            Logger.i(TAG_MAIN, String.format(Locale.US,
                    "Permission granted for request code: %d", requestCode))
        } else {
            Logger.e(TAG_MAIN, String.format(Locale.US,
                    "Permission rejected for request code: %d", requestCode))
        }
    }

    /* Called on activity result, available from KitKat onwards */
    @TargetApi(19)
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == ACTION_SAF_FILEPICKER && resultCode == Activity.RESULT_OK) {
            val importPath = lastImportedPath ?: return
            resultData?.let {
                val clipData = it.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val path = clipData.getItemAt(i)
                        FileUtils.saveAsFile(this, path.uri, importPath)
                    }
                } else FileUtils.saveAsFile(this, resultData.data, importPath)
            }
        }
    }

    /* Called when the activity focus changes */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setFullscreenLayout()
    }

    /* Called when the activity is going to be destroyed */
    public override fun onDestroy() {
        Logger.v(TAG_MAIN, "onDestroy()")
        unregisterReceiver(event)
        super.onDestroy()
    }

    /*---------------------------------------------------------------
     *             override methods used by lua/JNI                *
     *--------------------------------------------------------------*/

    override fun canIgnoreBatteryOptimizations(): Boolean {
        return Permissions.isIgnoringBatteryOptimizations(this)
    }
    override fun canWriteSystemSettings(): Boolean {
        return Permissions.hasWriteSettingsPermission(this)
    }

    override fun dictLookup(text: String?, action: String?, nullablePackage: String?) {
        text?.let { lookupText ->
            action?.let { lookupAction ->
                val lookupIntent = Intent(IntentUtils.getByAction(lookupText, lookupAction, nullablePackage))
                if (!startActivityIfSafe(lookupIntent)) {
                    Logger.e(TAG_MAIN, "invalid lookup: can't find a package able to resolve $action")
                }
            } ?: Logger.e(TAG_MAIN, "invalid lookup: no action")
        } ?: Logger.e(TAG_MAIN, "invalid lookup: no text")
    }

    override fun download(url: String, name: String): Int {
        return ApkUpdater.download(this, url, name)
    }

    override fun einkUpdate(mode: Int) {
        if (takesWindowOwnership) {
            device.einkUpdate(view as View, mode)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            device.einkUpdate(rootView, mode)
        }
    }

    override fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int) {
        if (takesWindowOwnership) {
            device.einkUpdate(view as View, mode, delay, x, y, width, height)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            device.einkUpdate(rootView, mode, delay, x, y, width, height)
        }
    }

    override fun enableFrontlightSwitch(): Boolean {
        return device.enableFrontlightSwitch(this)
    }

    override fun extractAssets(): Boolean {
        val ok = assets.extract(this)
        splashScreen = false
        return ok
    }

    override fun getBatteryLevel(): Int {
        return getBatteryState(true)
    }

    override fun getClipboardText(): String {
        return clipboard.getClipboardText(this)
    }

    override fun getEinkPlatform(): String {
        return device.einkPlatform
    }

    override fun getExternalPath(): String {
        return device.externalStorage
    }

    override fun getExternalSdPath(): String {
        return FileUtils.getExtSdcardPath(this)
    }

    override fun getFilePathFromIntent(): String? {
        return intent?.let {
            if (it.action == Intent.ACTION_VIEW) {
                FileUtils.getPathFromUri(this, it.data)
            } else null
        }
    }

    override fun getFlavor(): String {
        return BuildConfig.FLAVOR_CHANNEL
    }

    override fun getLastImportedPath(): String? {
        val current = lastImportedPath
        lastImportedPath = null
        return current
    }

    override fun getLightDialogState(): Int {
        return device.getLightDialogState()
    }

    override fun getName(): String {
        return BuildConfig.APP_NAME
    }

    override fun getNetworkInfo(): String {
        return NetworkUtils.getNetworkInfo(this)
    }

    override fun getPlatformName(): String {
        return device.platform
    }

    override fun getProduct(): String {
        return device.product
    }

    override fun getScreenAvailableHeight(): Int {
        return ScreenUtils.getScreenAvailableHeight(this)
    }

    override fun getScreenAvailableWidth(): Int {
        return ScreenUtils.getScreenAvailableWidth(this)
    }

    override fun getScreenBrightness(): Int {
        return device.getScreenBrightness(this)
    }

    override fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // We need to handle the notch in Portrait
            // NOTE: getScreenAvailableHeight does it automatically, but it also excludes the nav bar, when there's one :/
            if (device.getScreenOrientation(this).and(1) == 0) {
                // getScreenOrientation returns LinuxFB rotation constants, Portrait rotations are always even
                ScreenUtils.getScreenHeight(this) - topInsetHeight
            } else {
                ScreenUtils.getScreenHeight(this)
            }
        } else {
            ScreenUtils.getScreenHeight(this)
        }
    }

    override fun getScreenMaxBrightness(): Int {
        return device.getScreenMaxBrightness()
    }

    override fun getScreenMinBrightness(): Int {
        return device.getScreenMinBrightness()
    }

    override fun getScreenMaxWarmth(): Int {
        return device.getScreenMaxWarmth()
    }

    override fun getScreenMinWarmth(): Int {
        return device.getScreenMinWarmth()
    }

    override fun getScreenOrientation(): Int {
        return device.getScreenOrientation(this)
    }

    override fun getScreenWarmth(): Int {
        return device.getScreenWarmth(this)
    }

    override fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // We need to handle the notch in Landscape
            // NOTE: getScreenAvailableWidth does it automatically, but it also excludes the nav bar, when there's one :/
            if (device.getScreenOrientation(this).and(1) == 1) {
                // getScreenOrientation returns LinuxFB rotation constants, Landscape rotations are always odd
                ScreenUtils.getScreenWidth(this) - topInsetHeight
            } else {
                ScreenUtils.getScreenWidth(this)
            }
        } else {
            ScreenUtils.getScreenWidth(this)
        }
    }

    override fun getStatusBarHeight(): Int {
        return ScreenUtils.getStatusBarHeight(this)
    }

    override fun getVersion(): String {
        return RUNTIME_VERSION
    }

    override fun hasClipboardText(): Boolean {
        return clipboard.hasClipboardText()
    }

    override fun hasExternalStoragePermission(): Boolean {
        return Permissions.hasStoragePermission(this)
    }

    override fun hasNativeRotation(): Boolean {
        return if (device.platform == "android") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                !(device.bugRotation)
            } else false
        } else false
    }

    override fun isCharging(): Boolean {
        return (getBatteryState(false) == 1)
    }

    override fun isChromeOS(): Boolean {
        return device.isChromeOS
    }

    @Suppress("ConstantConditionIf")
    override fun isDebuggable(): Boolean {
        return BuildConfig.DEBUG
    }

    override fun isEink(): Boolean {
        return device.hasEinkSupport
    }

    override fun isEinkFull(): Boolean {
        return device.hasFullEinkSupport
    }

    override fun isFullscreen(): Boolean {
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fullscreen
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            ScreenUtils.isFullscreenDeprecated(this)
        } else {
            false
        }
    }

    override fun isPackageEnabled(pkg: String): Boolean {
        return try {
            val pm = packageManager
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            val enabled = pm.getApplicationInfo(pkg, 0).enabled
            enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @SuppressLint("SdCardPath")
    override fun isPathInsideSandbox(path: String): Boolean {
        return when {
            path.startsWith("/sdcard") -> true
            path.startsWith(device.externalStorage) -> true
            else -> false
        }
    }

    override fun isTv(): Boolean {
        return device.isTv
    }

    override fun isWarmthDevice(): Boolean {
        return device.isWarmthDevice()
    }

    override fun needsWakelocks(): Boolean {
        return device.needsWakelocks
    }

    override fun openLink(url: String): Int {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        return if (startActivityIfSafe(intent)) 0 else 1
    }

    override fun openWifiSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_WIFI_SETTINGS
        }
        startActivityIfSafe(intent)
    }

    override fun performHapticFeedback(constant: Int, force: Int) {
        if (takesWindowOwnership) {
            device.hapticFeedback(this, constant, force > 0, view as View)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            device.hapticFeedback(this, constant, force > 0, rootView)
        }
    }

    override fun requestIgnoreBatteryOptimizations(rationale: String, okButton: String, cancelButton: String) {
        Permissions.requestIgnoreBatteryOptimizations(this, rationale, okButton, cancelButton)
    }

    override fun requestWriteSystemSettings(rationale: String, okButton: String, cancelButton: String) {
        Permissions.requestWriteSettingsPermission(this, rationale, okButton, cancelButton)
    }

    override fun safFilePicker(path: String?): Boolean {
        lastImportedPath = path
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, getSupportedMimetypes())
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            lastImportedPath?.let {
                try {
                    startActivityForResult(intent, ACTION_SAF_FILEPICKER)
                    true
                } catch (e: Exception) {
                    false
                }
            } ?: false
        } else {
            false
        }
    }

    override fun sendText(text: String?) {
        text?.let {
            startActivityIfSafe(IntentUtils.getSendIntent(it, null))
        }
    }

    override fun setClipboardText(text: String) {
        clipboard.setClipboardText(this, text)
    }

    override fun setFullscreen(enabled: Boolean) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fullscreen = enabled
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            ScreenUtils.setFullscreenDeprecated(this, enabled)
        }
    }

    override fun setIgnoreInput(enabled: Boolean) {
        runOnUiThread {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    override fun setScreenBrightness(brightness: Int) {
        device.setScreenBrightness(this, brightness)
    }

    override fun setScreenOffTimeout(ms: Int) {
        timeout.setTimeout(this, ms)
    }

    override fun setScreenOrientation(orientation: Int) {
        device.setScreenOrientation(this, orientation)
    }

    override fun setScreenWarmth(warmth: Int) {
        device.setScreenWarmth(this, warmth)
    }

    override fun showFrontlightDialog(title: String, dim: String, warmth: String,
                                      okButton: String, cancelButton: String) {
        device.showDialog(this, title, dim, warmth, okButton, cancelButton)
    }

    override fun showToast(message: String, longTimeout: Boolean) {
        val duration = if (longTimeout) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    override fun startEPDTestActivity() {
        val intent = Intent(this, EPDTestActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }

    override fun untar(filePath: String, outputPath: String): Boolean {
        return ArchiveUtils.untar(filePath, outputPath)
    }

    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    private fun drawSplashScreen(holder: SurfaceHolder) {
        if (splashScreen) {
            /* draw splash screen to surface */
            holder.lockCanvas()?.let { canvas ->
                try {
                    ContextCompat.getDrawable(this, R.drawable.splash_icon)?.let { splashDrawable ->
                        splashDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        splashDrawable.draw(canvas)
                    }
                } catch (e: Exception) {
                    Logger.w(TAG_MAIN, "Failed to draw splash screen:\n$e")
                }
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun getBatteryState(isPercent: Boolean): Int {
        val intent = applicationContext.registerReceiver(null, BATTERY_FILTER)
        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val percent = level * 100 / scale

            return if (isPercent) {
                percent
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                if (percent != 100) 1 else 0
            } else {
                0
            }
        } else {
            return 0
        }
    }

    private fun getSupportedMimetypes(): Array<String> {
        return arrayOf(
            "application/epub+zip",
            "application/fb2",
            "application/fb3",
            "application/msword",
            "application/oxps",
            "application/pdf",
            "application/rtf",
            "application/tcr",
            "application/vnd.amazon.mobi8-ebook",
            "application/vnd.comicbook+tar",
            "application/vnd.comicbook+zip",
            "application/vnd.ms-htmlhelp",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.palm",
            "application/x-cbz",
            "application/x-chm",
            "application/x-fb2",
            "application/x-fb3",
            "application/x-mobipocket-ebook",
            "application/x-tar",
            "application/xhtml+xml",
            "application/xml",
            "application/zip",
            "image/djvu",
            "image/gif",
            "image/jp2",
            "image/jpeg",
            "image/jxr",
            "image/png",
            "image/svg+xml",
            "image/tiff",
            "image/vnd.djvu",
            "image/vnd.ms-photo",
            "image/x-djvu",
            "image/x-portable-arbitrarymap",
            "image/x-portable-bitmap",
            "text/html",
            "text/plain"
        )
    }

    @Suppress("DEPRECATION")
    private fun setFullscreenLayout() {
        val decorView = window.decorView
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ->
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
            else -> decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun startActivityIfSafe(intent: Intent?): Boolean {
        if (intent == null) return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        val intentStr = IntentUtils.intentToString(intent)
        try {
            val pm = packageManager
            val act = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (act.size > 0) {
                Logger.d(TAG_MAIN, "starting activity with intent: $intentStr")
                startActivity(intent)
                return true
            } else {
                Logger.w(TAG_MAIN, "unable to find a package for $intentStr")
            }
            return false
        } catch (e: Exception) {
            Logger.e(TAG_MAIN, "error opening $intentStr\nException: $e")
            return false
        }
    }
}
