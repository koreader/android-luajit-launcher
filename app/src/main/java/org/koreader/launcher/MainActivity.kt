package org.koreader.launcher

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.NativeActivity
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.DisplayCutout
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.koreader.launcher.device.Device
import org.koreader.launcher.utils.ArchiveUtils
import org.koreader.launcher.utils.FileUtils
import org.koreader.launcher.utils.IntentUtils
import org.koreader.launcher.utils.NetworkUtils
import org.koreader.launcher.utils.Permissions
import org.koreader.launcher.utils.ScreenUtils
import java.util.Locale

class MainActivity : NativeActivity(), LuaInterface,
    ActivityCompat.OnRequestPermissionsResultCallback{

    private val tag = this::class.java.simpleName

    private lateinit var assets: Assets
    private lateinit var device: Device
    private lateinit var event: EventReceiver
    private lateinit var timeout: Timeout
    private lateinit var updater: ApkUpdater

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
        init { holder.addCallback(this) }
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.v(TAG_SURFACE, "surface created")
            setWillNotDraw(false)
        }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.v(TAG_SURFACE, String.format(Locale.US,
                "surface changed {\n  width:  %d\n  height: %d\n format: %s\n}",
                width, height, ScreenUtils.pixelFormatName(format))
            )
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.v(TAG_SURFACE, "surface destroyed")
        }
    }

    companion object {
        private const val TAG_SURFACE = "Surface"
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
        super.onCreate(savedInstanceState)
        assets = Assets()
        device = Device(this)
        timeout = Timeout()
        event = EventReceiver()
        updater = ApkUpdater()

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
        Log.v(TAG_SURFACE, "Using $surfaceKind implementation")

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
        Log.v(TAG_SURFACE, String.format(Locale.US,
            "surface changed {\n  width:  %d\n  height: %d\n format: %s\n}",
            width, height, ScreenUtils.pixelFormatName(format))
        )
        super.surfaceChanged(holder, format, width, height)
        drawSplashScreen(holder)
    }

    override fun onAttachedToWindow() {
        Log.d(TAG_SURFACE, "onAttachedToWindow()")
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cut: DisplayCutout? = window.decorView.rootWindowInsets.displayCutout
            if (cut != null) {
                val cutPixels = cut.safeInsetTop
                if (topInsetHeight != cutPixels) {
                    Log.v(TAG_SURFACE,
                        "top $cutPixels pixels are not available, reason: window inset")
                    topInsetHeight = cutPixels
                }
            }
        }
    }

    /* Called just before the activity is resumed by an intent */
    override fun onNewIntent(intent: Intent) {
        val scheme = intent.scheme
        Log.d(tag, "onNewIntent(): $scheme")
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /* Called on permission result */
    override fun onRequestPermissionsResult(requestCode: Int, permissions:
        Array<String>, grantResults: IntArray) {
        Log.d(tag, "onRequestPermissionResult()")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permissions.hasStoragePermission(this)) {
            Log.i(tag, String.format(Locale.US,
                    "Permission granted for request code: %d", requestCode))
        } else {
            Log.e(tag, String.format(Locale.US,
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
        Log.v(tag, "onDestroy()")
        unregisterReceiver(event)
        super.onDestroy()
    }

    /*---------------------------------------------------------------
     *                         native callbacks                     *
     *--------------------------------------------------------------*/

    /* Called when the main thread is about to exit because of an error */
    @Suppress("unused")
    fun onNativeCrash() {
        MainApp.crashReport(this.applicationContext)
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
                val intent: Intent? = when (lookupAction) {
                    // generic actions used by a lot of apps
                    "send" ->  Intent(IntentUtils.getSendTextIntent(lookupText, nullablePackage))
                    "search" -> Intent(IntentUtils.getSearchTextIntent(text, nullablePackage))
                    "text" ->  Intent(IntentUtils.getProcessTextIntent(text, nullablePackage))
                    // actions for specific apps
                    "aard2" ->  Intent(IntentUtils.getAard2Intent(text))
                    "colordict" -> Intent(IntentUtils.getColordictIntent(text, nullablePackage))
                    "quickdic" -> Intent(IntentUtils.getQuickdicIntent(text))
                    else -> null
                }
                intent?.let {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    if (!startActivityIfSafe(intent)) {
                        Log.e(tag, "invalid lookup: can't find a package able to resolve $lookupAction")
                    }
                }
            } ?: Log.e(tag, "invalid lookup: no action")
        } ?: Log.e(tag, "invalid lookup: no text")
    }

    override fun download(url: String, name: String): Int {
        return updater.download(this, url, name)
    }

    override fun dumpLogs() {
        MainApp.dumpLogcat()
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
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text as String?
        return text ?: ""
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
        return MainApp.flavor
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
        return MainApp.name
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
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.let {
            (it.itemCount > 0)
        }?: false
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

    override fun hasOTAUpdates(): Boolean {
        return MainApp.has_ota_updates
    }

    override fun hasRuntimeChanges(): Boolean {
        return MainApp.supports_runtime_changes
    }

    override fun installApk() {
        updater.install(this)
    }

    override fun isCharging(): Boolean {
        return (getBatteryState(false) == 1)
    }

    override fun isChromeOS(): Boolean {
        return device.isChromeOS
    }

    override fun isDebuggable(): Boolean {
        return MainApp.is_debug
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

    override fun isActivityResumed(): Boolean {
        return device.isResumed
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
        return path?.let { _ ->
            IntentUtils.safIntent?.let {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivityForResult(it, ACTION_SAF_FILEPICKER)
                true
            }?: false
        } ?: false
    }

    override fun sendText(text: String?) {
        text?.let {
            startActivityIfSafe(IntentUtils.getSendTextIntent(it, null))
        }
    }

    override fun setClipboardText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("KOReader_clipboard", text))
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
                    Log.w(tag, "Failed to draw splash screen:\n$e")
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
        return intent?.let {
            return try {
                val pm = packageManager
                val act = pm.queryIntentActivities(it, PackageManager.MATCH_DEFAULT_ONLY)
                if (act.size > 0) {
                    startActivity(it)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: false
    }
}
