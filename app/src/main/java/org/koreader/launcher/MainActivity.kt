package org.koreader.launcher

import android.Manifest
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
import android.graphics.PixelFormat
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.koreader.launcher.device.Device
import org.koreader.launcher.dialog.LightDialog
import org.koreader.launcher.extensions.*
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch

class MainActivity : NativeActivity(), LuaInterface,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private val tag = this::class.java.simpleName

    private lateinit var assets: Assets
    private lateinit var device: Device
    private lateinit var event: EventReceiver
    private lateinit var timeout: Timeout
    private lateinit var updater: ApkUpdater
    private lateinit var lightDialog: LightDialog

    private var isResumed = false

    // Path of last file imported
    private var lastImportedPath: String? = null

    // Device cutout - only used on API 28+
    private var insetsHeight: Int = 0

    // Fullscreen - only used on API levels 16-18
    private var fullscreen: Boolean = true

    // Splashscreen is active
    private var splashScreen: Boolean = true

    // surface used on devices that need a view
    private var view: NativeSurfaceView? = null
    private class NativeSurfaceView(context: Context): SurfaceView(context),
        SurfaceHolder.Callback {
        init { holder.addCallback(this) }
        override fun surfaceCreated(holder: SurfaceHolder) { setWillNotDraw(false) }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    // Hardware orientation for this device (usually match android boot logo)
    private var screenIsLandscape: Boolean = false

    companion object {
        private const val TAG_SURFACE = "Surface"
        private const val MANDATORY_PERMISSIONS_ID = 1
        private const val ACTION_SAF_FILEPICKER_ID = 2
        private val BATTERY_FILTER = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        @JvmStatic
        private fun pixelFormatName(format: Int): String {
            return when (format) {
                PixelFormat.OPAQUE -> "OPAQUE"
                PixelFormat.RGBA_1010102 -> "RGBA_1010102"
                PixelFormat.RGBA_8888 -> "RGBA_8888"
                PixelFormat.RGBA_F16 -> "RGBA_F16"
                PixelFormat.RGBX_8888 -> "RGBX_8888"
                PixelFormat.RGB_888 -> "RGB_888"
                PixelFormat.RGB_565 -> "RGB_565"
                PixelFormat.TRANSLUCENT -> "TRANSLUCENT"
                PixelFormat.TRANSPARENT -> "TRANSPARENT"
                else -> String.format(Locale.US, "Unknown: %d", format)
            }
        }
    }

    init {
        Log.i(tag, "loading libluajit")
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
        lightDialog = LightDialog()

        setTheme(R.style.Fullscreen)

        // Window background must be black for vertical and horizontal lines to be visible
        window.setBackgroundDrawableResource(android.R.color.black)

        val surfaceKind: String = if (device.needsView) {
            view = NativeSurfaceView(this)
            window.takeSurface(null)
            view?.holder?.addCallback(this)
            setContentView(view)
            "SurfaceView"
        } else {
            "Native Content"
        }
        Log.v(TAG_SURFACE, "Using $surfaceKind implementation")

        @Suppress("DEPRECATION")
        screenIsLandscape = windowManager.defaultDisplay.width > windowManager.defaultDisplay.height

        Log.v(tag, String.format(Locale.US,
            "native orientation: %s", if (this.screenIsLandscape) "landscape" else "portrait"))

        registerReceiver(event, event.filter)

        if (!hasMandatoryPermissions()) {
            requestMandatoryPermissions()
        }
    }

    /* Called when the activity has become visible. */
    override fun onResume() {
        super.onResume()
        device.epd.resume()
        timeout.onResume(this)
        isResumed = true
    }

    /* Called when another activity is taking focus. */
    override fun onPause() {
        super.onPause()
        device.epd.pause()
        timeout.onPause(this)
        isResumed = false
        intent = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        drawSplashScreen(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.v(TAG_SURFACE, String.format(Locale.US,
            "surface changed {\n  width:  %d\n  height: %d\n format: %s\n}",
            width, height, pixelFormatName(format))
        )

        super.surfaceChanged(holder, format, width, height)
        drawSplashScreen(holder)
    }

    override fun onAttachedToWindow() {
        Log.d(TAG_SURFACE, "onAttachedToWindow()")
        super.onAttachedToWindow()

        if (Build.VERSION.SDK_INT >= 33) {
            val cut = windowManager.defaultDisplay.cutout
            if (cut != null && cut.boundingRects.isNotEmpty()) {
                insetsHeight = cut.safeInsetTop + cut.safeInsetBottom
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cut: DisplayCutout? = window.decorView.rootWindowInsets.displayCutout
            if (cut != null) {
                val cutPixels = cut.safeInsetTop
                if (insetsHeight != cutPixels) {
                    Log.v(TAG_SURFACE,
                        "top $cutPixels pixels are not available, reason: window inset")
                    insetsHeight = cutPixels
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MANDATORY_PERMISSIONS_ID) {
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(tag, "mandatory permission rejected: ${permissions[i]}. Bye!")
                    Toast.makeText(this, resources.getString(R.string.error_no_permissions),
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    /* Called on activity result, available from KitKat onwards */
    @TargetApi(19)
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == ACTION_SAF_FILEPICKER_ID && resultCode == Activity.RESULT_OK) {
            val importPath = lastImportedPath ?: return
            resultData?.let {
                val clipData = it.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val path = clipData.getItemAt(i)
                        path.uri.toFile(this, importPath)
                    }
                } else resultData.data?.toFile(this, importPath)
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
        MainApp.crashReport(applicationContext)
    }

    @Suppress("unused")
    fun hasRequiredPermissions(): Boolean {
        return hasMandatoryPermissions()
    }

    /*---------------------------------------------------------------
     *             override methods used by lua/JNI                *
     *--------------------------------------------------------------*/

    @Suppress("NewApi")
    override fun canIgnoreBatteryOptimizations(): Boolean {
        return if (MainApp.isAtLeastApi(Build.VERSION_CODES.M)) {
            val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else false
    }

    @Suppress("NewApi")
    override fun canWriteSystemSettings(): Boolean {
        return if (MainApp.isAtLeastApi(Build.VERSION_CODES.M)) {
            Settings.System.canWrite(this)
        } else true
    }

    override fun dictLookup(text: String?, action: String?, nullablePackage: String?) {
        text?.let { lookupText ->
            action?.let { lookupAction ->
                when (lookupAction) {
                    "aard2" -> aardAction(lookupText)
                    "colordict" -> colordictAction(lookupText, nullablePackage)
                    "quickdic" -> quickdicAction(lookupText)
                    "search" -> searchAction(lookupText, nullablePackage)
                    "send" -> sendAction(lookupText, nullablePackage)
                    "text" -> processTextAction(lookupText, nullablePackage)
                    else -> Log.w(tag, "Unsupported action $lookupAction")
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
        val rootView = view ?: window.decorView.findViewById<View>(android.R.id.content)
        val modeName = when (mode) {
            1 -> "EPD_FULL"
            2 -> "EPD_PART"
            3 -> "EPD_A2"
            4 -> "EPD_AUTO"
            else -> "invalid"
        }

        if (modeName != "invalid") {
            Log.v(tag, String.format(Locale.US,
                "requesting epd update, type: %s", modeName))

            device.epd.setEpdMode(rootView, 0, 0, 0, 0, 0, 0, modeName)
        }
    }

    override fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int) {
        val rootView = view ?: window.decorView.findViewById<View>(android.R.id.content)
        Log.v(tag, String.format(Locale.US,
            "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
            mode, delay, x, y, width, height))

        device.epd.setEpdMode(rootView, mode, delay, x, y, width, height, null)
    }

    override fun enableFrontlightSwitch(): Boolean {
        return device.lights.enableFrontlightSwitch(this) == 1
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
        val cd = CountDownLatch(1)
        val result = Box<String>()
        runOnUiThread {
            result.value = try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData: ClipData? = clipboard.primaryClip
                clipData?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            } finally {
                cd.countDown()
            }
        }

        return try {
            cd.await()
            result.value ?: ""
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
            ""
        }
    }

    override fun getDeviceProperties(): String {
        return device.properties
    }

    override fun getEinkConstants(): String {
        return String.format(Locale.US, "%d;%d;%d;%d;%d;%d;%d;%d",
            device.epd.getWaveformFull(),
            device.epd.getWaveformPartial(),
            device.epd.getWaveformFullUi(),
            device.epd.getWaveformPartialUi(),
            device.epd.getWaveformFast(),
            device.epd.getWaveformDelay(),
            device.epd.getWaveformDelayUi(),
            device.epd.getWaveformDelayFast())
    }

    override fun getEinkPlatform(): String {
        return device.einkPlatform
    }

    override fun getExternalPath(): String {
        return MainApp.storage_path
    }

    override fun getExternalSdPath(): String {
        return getSdcardPath() ?: "null"
    }

    override fun getFilePathFromIntent(): String? {
        return intent?.let {
            if (it.action == Intent.ACTION_VIEW) {
                it.data?.absolutePath(this)
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
        return lightDialog.state
    }

    override fun getName(): String {
        return MainApp.name
    }

    override fun getNetworkInfo(): String {
        return networkInfo()
    }

    override fun getPlatformName(): String {
        return platform
    }

    override fun getScreenAvailableHeight(): Int {
        return getAvailableHeight()
    }

    override fun getScreenAvailableWidth(): Int {
        return getAvailableWidth()
    }

    override fun getScreenBrightness(): Int {
        return device.lights.getBrightness(this)
    }

    override fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // We need to handle the notch in Portrait
            // NOTE: getScreenAvailableHeight does it automatically, but it also excludes the nav bar, when there's one :/
            if (getOrientationCompat(screenIsLandscape).and(1) == 0) {
                // getScreenOrientation returns LinuxFB rotation constants, Portrait rotations are always even
                getHeight() - insetsHeight
            } else {
                getHeight()
            }
        } else {
            getHeight()
        }
    }

    override fun getScreenMaxBrightness(): Int {
        return device.lights.getMaxBrightness()
    }

    override fun getScreenMinBrightness(): Int {
        return device.lights.getMinBrightness()
    }

    override fun getScreenMaxWarmth(): Int {
        return device.lights.getMaxWarmth()
    }

    override fun getScreenMinWarmth(): Int {
        return device.lights.getMinWarmth()
    }

    override fun getScreenOrientation(): Int {
        return getOrientationCompat(screenIsLandscape)
    }

    override fun getScreenWarmth(): Int {
        return device.lights.getWarmth(this)
    }

    override fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // We need to handle the notch in Landscape
            // NOTE: getScreenAvailableWidth does it automatically, but it also excludes the nav bar, when there's one :/
            if (getOrientationCompat(screenIsLandscape).and(1) == 1) {
                // getScreenOrientation returns LinuxFB rotation constants, Landscape rotations are always odd
                getWidth() - insetsHeight
            } else {
                getWidth()
            }
        } else {
            getWidth()
        }
    }

    override fun getStatusBarHeight(): Int {
        return getBarHeight()
    }

    override fun getVersion(): String {
        return Build.VERSION.RELEASE
    }

    override fun hasBrokenLifecycle(): Boolean {
        return device.bugLifecycle
    }

    override fun hasClipboardText(): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.let {
            (it.itemCount > 0)
        }?: false
    }

    override fun hasLights(): Boolean {
        return device.hasLights
    }

    override fun hasNativeRotation(): Boolean {
        return if (platform == "android") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                !(device.bugRotation)
            } else false
        } else false
    }

    override fun hasOTAUpdates(): Boolean {
        return when (platform) {
            "chrome" -> false
            else -> MainApp.has_ota_updates
        }
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
        return when (platform) {
            "chrome" -> true
            else -> false
        }
    }

    override fun isColorScreen(): Boolean {
        return device.hasColorScreen
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
            isFullscreenDeprecated()
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
            path.startsWith(MainApp.storage_path) -> true
            else -> false
        }
    }

    override fun isActivityResumed(): Boolean {
        return isResumed
    }

    override fun isTv(): Boolean {
        return when (platform) {
            "android_tv" -> true
            else -> false
        }
    }

    override fun isWarmthDevice(): Boolean {
        return device.lights.hasWarmth()
    }

    override fun needsWakelocks(): Boolean {
        return device.needsWakelocks
    }

    override fun openLink(url: String): Boolean {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        return try {
            startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun openWifiSettings() {
        openWifi()
    }

    override fun performHapticFeedback(constant: Int, force: Int) {
        val rootView = view ?: window.decorView.findViewById<View>(android.R.id.content)
        hapticFeedback(constant, force > 0, rootView)
    }

    @Suppress("NewApi")
    override fun requestIgnoreBatteryOptimizations(rationale: String, okButton: String, cancelButton: String) {
        if (MainApp.isAtLeastApi(Build.VERSION_CODES.M)) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            requestSpecialPermission(intent, rationale, okButton, cancelButton)
        }
    }

    @Suppress("NewApi")
    override fun requestWriteSystemSettings(rationale: String, okButton: String, cancelButton: String) {
        if (MainApp.isAtLeastApi(Build.VERSION_CODES.M)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            requestSpecialPermission(intent, rationale, okButton, cancelButton)
        }
    }

    override fun safFilePicker(path: String?): Boolean {
        lastImportedPath = path
        return path?.let { _ ->
            filePicker(ACTION_SAF_FILEPICKER_ID)
        } ?: false
    }

    override fun sendText(text: String, reason: String?, title: String?, mimetype: String?) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = mimetype ?: "text/plain"
        }
        title?.let {
            sendIntent.putExtra(Intent.EXTRA_TITLE, it)
        }

        val chooser = Intent.createChooser(sendIntent, reason)
        try {
            startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setClipboardText(text: String) {
        runOnUiThread {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("KOReader_clipboard", text))
        }
    }

    override fun setFullscreen(enabled: Boolean) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fullscreen = enabled
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            setFullscreenDeprecated(enabled)
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
        device.lights.setBrightness(this, brightness)
    }

    override fun setScreenOffTimeout(ms: Int) {
        timeout.setTimeout(this, ms)
    }

    override fun setScreenOrientation(orientation: Int) {
        setOrientationCompat(screenIsLandscape, orientation)
    }

    override fun setScreenWarmth(warmth: Int) {
        device.lights.setWarmth(this, warmth)
    }

    override fun showFrontlightDialog(title: String, dim: String, warmth: String,
                                      okButton: String, cancelButton: String) {
        lightDialog.show(this, device.lights, title, dim, warmth, okButton, cancelButton)
    }

    override fun showToast(message: String, longTimeout: Boolean) {
        val duration = if (longTimeout) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    override fun startTestActivity() {
        val intent = Intent(this, TestActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }

    override fun untar(filePath: String, outputPath: String): Boolean {
        return File(filePath).uncompress(outputPath)
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

    @Suppress("NewApi")
    private fun hasMandatoryPermissions(): Boolean {
        return if (MainApp.isAtLeastApi(Build.VERSION_CODES.R)) {
            Environment.isExternalStorageManager()
        } else {
            (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        }
    }

    @Suppress("NewApi")
    private fun requestMandatoryPermissions() {
        if (MainApp.isAtLeastApi(Build.VERSION_CODES.R)) {
            val intent = Intent().apply {
                action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                data = Uri.fromParts("package", packageName, null)
            }
            requestSpecialPermission(intent,
                resources.getString(R.string.permission_manage_storage),
                null, null)
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MANDATORY_PERMISSIONS_ID)
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

    private class Box<T> {
        var value: T? = null
    }
}
