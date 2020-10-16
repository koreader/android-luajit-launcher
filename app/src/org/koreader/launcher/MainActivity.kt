package org.koreader.launcher

import java.util.Locale

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

import org.koreader.launcher.device.DeviceInfo
import org.koreader.launcher.device.EPDFactory
import org.koreader.launcher.device.LightsFactory
import org.koreader.launcher.utils.FileUtils
import org.koreader.launcher.utils.Logger
import org.koreader.launcher.utils.Permissions

/* MainActivity.java
 *
 * Implements e-ink updates
 * Implements WRITE_EXTERNAL_STORAGE/WRITE_SETTINGS permissions
 *
 * Takes care of most activity callbacks that are view-aware.
 * It handles custom timeout based on activity state too.
 */

@Suppress("ConstantConditionIf")
class MainActivity : BaseActivity() {

    private var initialized: Boolean = false
    private var permissionCounter: Int = 0

    // EPD driver for this device
    private val epd = EPDFactory.epdController

    // Light controller for this device
    private val lights = LightsFactory.lightsController
    private var lightDialogState = LIGHT_DIALOG_CLOSED

    // Some e-ink devices need to take control of the native window from the java side
    private var takesWindowOwnership: Boolean = false

    // Hardware orientation for this device (matches android logo)
    private var screenIsLandscape: Boolean = false

    // Use this setting to avoid screen dimming
    private var isScreenAlwaysOn: Boolean = false

    // Use this setting to override screen off timeout system setting
    private var isCustomTimeout: Boolean = false

    // When custom timeouts are used these are the current timeouts for activity and system
    private var systemTimeout: Int = 0
    private var appTimeout: Int = 0

    private var lastImportedPath: String? = null

    companion object {
        private const val TAG_MAIN = "MainActivity"

        // constants from https://developer.android.com/reference/android/content/res/Configuration
        private const val ANDROID_LANDSCAPE = 0
        private const val ANDROID_PORTRAIT = 1
        private const val ANDROID_REVERSE_LANDSCAPE = 8
        private const val ANDROID_REVERSE_PORTRAIT = 9

        // constants from https://github.com/koreader/koreader-base/blob/master/ffi/framebuffer.lua
        private const val LINUX_PORTRAIT = 0
        private const val LINUX_LANDSCAPE = 1
        private const val LINUX_REVERSE_PORTRAIT = 2
        private const val LINUX_REVERSE_LANDSCAPE = 3

        private const val LIGHT_DIALOG_CLOSED = -1
        private const val LIGHT_DIALOG_OPENED = 0
        private const val LIGHT_DIALOG_CANCEL = 1
        private const val LIGHT_DIALOG_OK = 2

        private const val SCREEN_ON_ENABLED = -1
        private const val SCREEN_ON_DISABLED = 0
        private const val TIMEOUT_MIN = 2 * 60 * 1000
        private const val TIMEOUT_MAX = 45 * 60 * 1000

        private const val ACTION_SAF_FILEPICKER = 2
    }

    // Surface used on devices that need a view
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
                "surface changed {\n  width:  %d\n  height: %d\n}", width, height))
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Logger.v(tag, "surface destroyed")
        }
    }

    /*---------------------------------------------------------------
     *                        activity callbacks                    *
     *--------------------------------------------------------------*/

    /* Called when the activity is first created. */
    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.v(TAG_MAIN, "onCreate()")

        if (Permissions.hasStoragePermission(this@MainActivity)) {
            initialized = true
        } else {
            Permissions.requestStoragePermission(this@MainActivity)
        }

        super.onCreate(savedInstanceState)
        if (DeviceInfo.NEEDS_VIEW) {
            Logger.v(TAG_MAIN, "onNativeSurfaceViewImpl()")
            view = NativeSurfaceView(this)
            window.takeSurface(null)
            view?.holder?.addCallback(this)
            setContentView(view)
            takesWindowOwnership = true
        } else {
            /* native content without further processing */
            Logger.v(TAG_MAIN, "onNativeWindowImpl()")
        }
        screenIsLandscape = isHwLandscape()
        systemTimeout = getSystemScreenOffTimeout()
    }

    /* Called when the activity has become visible. */
    override fun onResume() {
        Logger.d(TAG_MAIN, "onResume()")
        super.onResume()
        applyCustomTimeout(true)
    }

    /* Called when another activity is taking focus. */
    override fun onPause() {
        Logger.d(TAG_MAIN, "onPause()")
        super.onPause()
        applyCustomTimeout(false)
        intent = null
    }

    /* Called just before the activity is resumed by an intent
     *
     * If the intent is action.MAIN then scheme will be null
     * If the intent is action.VIEW then the scheme can be file or content.
     */

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
        if (Permissions.hasPermissionGranted(this@MainActivity, permissions[0])) {
            Logger.v(TAG_MAIN, String.format(Locale.US,
                    "Permission granted for request code: %d", requestCode))
            runLuaJIT()
        } else {
            Logger.e(TAG_MAIN, String.format(Locale.US,
                    "Permission rejected for request code: %d", requestCode))

            if (permissionCounter < 1) {
                permissionCounter++
                val msg = resources.getString(R.string.warning_storage_permission)
                showPermissionDialog(msg, false)
            } else {
                val msg = resources.getString(R.string.error_storage_permission)
                showPermissionDialog(msg, true)
            }
        }
    }

    /* Called on activity result, available from KitKat onwards */
    @TargetApi(19)
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == ACTION_SAF_FILEPICKER && resultCode == Activity.RESULT_OK) {
            lastImportedPath ?: return
            resultData?.let {
                val clipData = it.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val path = clipData.getItemAt(i)
                        FileUtils.saveAsFile(this@MainActivity, path.uri, lastImportedPath)
                    }
                } else FileUtils.saveAsFile(this@MainActivity, resultData.data, lastImportedPath)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (initialized) {
            super.onWindowFocusChanged(hasFocus)
            if (hasFocus) setFullscreenLayout()
        }
    }

    override fun getFilePathFromIntent(): String? {
        return intent?.let {
            if (it.action == Intent.ACTION_VIEW) {
                FileUtils.getPathFromUri(this@MainActivity, it.data)
            } else null
        }
    }

    override fun getLastImportedPath(): String? {
        val current = lastImportedPath
        lastImportedPath = null
        return current
    }

    override fun isPathInsideSandbox(path: String?): Int {
        return path?.let {
            if (it.startsWith(MainApp.storage_path)) 1 else 0
            } ?: 0
    }

    /* Called when the activity is going to be destroyed */
    public override fun onDestroy() {
        Logger.d(TAG_MAIN, "onDestroy()")
        super.onDestroy()
    }

    /*---------------------------------------------------------------
     *             override methods used by lua/JNI                *
     *--------------------------------------------------------------*/

    override fun enableFrontlightSwitch(): Int {
        return lights.enableFrontlightSwitch(this@MainActivity)
    }

    override fun getScreenBrightness(): Int {
        return lights.getBrightness(this@MainActivity)
    }

    override fun setScreenBrightness(brightness: Int) {
        lights.setBrightness(this@MainActivity, brightness)
    }

    override fun getScreenMinBrightness(): Int {
        return lights.getMinBrightness()
    }

    override fun getScreenMaxBrightness(): Int {
        return lights.getMaxBrightness()
    }

    override fun getScreenWarmth(): Int {
        return lights.getWarmth(this@MainActivity)
    }

    override fun setScreenWarmth(warmth: Int) {
        lights.setWarmth(this@MainActivity, warmth)
    }

    override fun getScreenMinWarmth(): Int {
        return lights.getMinWarmth()
    }

    override fun getScreenMaxWarmth(): Int {
        return lights.getMaxWarmth()
    }

    override fun isWarmthDevice(): Int {
        return if (lights.hasWarmth()) 1 else 0
    }

    override fun getLightDialogState(): Int {
        return lightDialogState
    }

    override fun showFrontlightDialog(title: String, dim: String, warmth: String, okButton: String, cancelButton: String): Int {
        val hasWarmth = lights.hasWarmth()
        setFrontlightDialogState(LIGHT_DIALOG_OPENED)
        runOnUiThread {
            val dimText = TextView(this@MainActivity)
            val dimSeekBar = SeekBar(this@MainActivity)
            dimText.text = dim
            dimText.gravity = Gravity.CENTER_HORIZONTAL
            dimText.textSize = 18f
            dimSeekBar.max = lights.getMaxBrightness()
            dimSeekBar.progress = lights.getBrightness(this@MainActivity)
            dimSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    lights.setBrightness(this@MainActivity, p1)
                }
            })
            val linearLayout = LinearLayout(this@MainActivity)
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.addView(dimText)
            linearLayout.addView(dimSeekBar)
            if (hasWarmth) {
                val warmthText = TextView(this@MainActivity)
                val warmthSeekBar = SeekBar(this@MainActivity)
                warmthText.text = warmth
                warmthText.gravity = Gravity.CENTER_HORIZONTAL
                warmthText.textSize = 18f
                warmthSeekBar.max = lights.getMaxWarmth()
                warmthSeekBar.progress = lights.getWarmth(this@MainActivity)
                warmthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        lights.setWarmth(this@MainActivity, p1)
                    }
                })
                linearLayout.addView(warmthText)
                linearLayout.addView(warmthSeekBar)
            }

            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(title)
                .setCancelable(false)
                .setPositiveButton(okButton) {
                    _, _ -> setFrontlightDialogState(LIGHT_DIALOG_OK)
                }
                .setNegativeButton(cancelButton) {
                    _, _ -> setFrontlightDialogState(LIGHT_DIALOG_CANCEL)
                }

            val dialog: AlertDialog = builder.create()
            dialog.setView(linearLayout)
            dialog.show()
        }
        return 0
    }

   /* Native orientation is available for Android 4.4+ devices.
      Supported devices can also be blacklisted in DeviceInfo if the behaviour is buggy.

      FIXME: It is actually disabled on 9.0+ devices with a notch.

        we need to set viewports for non portrait modes

        - on landscape: notch, 0, height, width - notch
        - on reverse landscape: 0, 0, height, width - notch
        - on reverse portrait: 0, 0, height - notch, width

        + adjust touch too?¿?
   */

    override fun hasNativeRotation(): Int {
        return if (MainApp.platform_type == "android") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // FIXME: hw rotation is disabled in devices with a Notch.
                if ((topInsetHeight > 0) || (DeviceInfo.BUG_SCREEN_ROTATION)) 0 else 1
            } else 0
        } else 0
    }

    override fun getScreenOrientation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> if (screenIsLandscape) LINUX_PORTRAIT else LINUX_REVERSE_LANDSCAPE
            Surface.ROTATION_180 -> if (screenIsLandscape) LINUX_REVERSE_LANDSCAPE else LINUX_REVERSE_PORTRAIT
            Surface.ROTATION_270 -> if (screenIsLandscape) LINUX_REVERSE_PORTRAIT else LINUX_LANDSCAPE
            else -> if (screenIsLandscape) LINUX_LANDSCAPE else LINUX_PORTRAIT
        }
    }

    override fun setScreenOrientation(orientation: Int) {
        val newOrientation = if (screenIsLandscape) {
            when (orientation) {
                ANDROID_LANDSCAPE -> ANDROID_PORTRAIT
                ANDROID_PORTRAIT -> ANDROID_LANDSCAPE
                ANDROID_REVERSE_LANDSCAPE -> ANDROID_REVERSE_PORTRAIT
                ANDROID_REVERSE_PORTRAIT -> ANDROID_REVERSE_LANDSCAPE
                else -> orientation
            }
        } else {
            when (orientation) {
                ANDROID_LANDSCAPE -> ANDROID_REVERSE_LANDSCAPE
                ANDROID_REVERSE_LANDSCAPE -> ANDROID_LANDSCAPE
                else -> orientation
            }
        }
        requestedOrientation = newOrientation
    }

    override fun isTv(): Int {
        return if (MainApp.platform_type == "android_tv") 1 else 0
    }

    override fun isChromeOS(): Int {
        return if (MainApp.platform_type == "chrome") 1 else 0
    }

    override fun getPlatformName(): String {
        return MainApp.platform_type
    }

    override fun canWriteSystemSettings(): Int {
        return if (Permissions.hasWriteSettingsPermission(this@MainActivity)) 1 else 0
    }

    /* ignore input toggle */
    override fun setIgnoreInput(enabled: Boolean) {
        runOnUiThread {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    // update the entire screen (rockchip)
    override fun einkUpdate(mode: Int) {
        val modeName = when (mode) {
            1 -> "EPD_FULL"
            2 -> "EPD_PART"
            3 -> "EPD_A2"
            4 -> "EPD_AUTO"
            else -> "invalid"
        }

        if (modeName != "invalid") {
            Logger.v(TAG_MAIN, String.format(Locale.US,
                "requesting epd update, type: %s", modeName))

            if (takesWindowOwnership and (view != null)) {
                epd.setEpdMode(view as View, 0, 0, 0, 0, 0, 0, modeName)
            } else {
                val rootView = window.decorView.findViewById<View>(android.R.id.content)
                epd.setEpdMode(rootView, 0, 0, 0, 0, 0, 0, modeName)
            }
        }
    }

    // update a region or the entire screen (freescale)
    override fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int) {

        Logger.v(TAG_MAIN, String.format(Locale.US,
                "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
                mode, delay, x, y, width, height))

        if (takesWindowOwnership) {
            epd.setEpdMode(view as View, mode, delay, x, y, width, height, null)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            epd.setEpdMode(rootView, mode, delay, x, y, width, height, null)
        }
    }

    override fun getScreenOffTimeout(): Int {
        return getSystemScreenOffTimeout()
    }

    override fun getSystemTimeout(): Int {
        // return last known value
        return systemTimeout
    }

    override fun hasExternalStoragePermission(): Int {
        return if (Permissions.hasStoragePermission(this@MainActivity)) 1 else 0
    }

    override fun safFilePicker(path: String?): Int {
        lastImportedPath = path
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            val filter = arrayOf(
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
            intent.putExtra(Intent.EXTRA_MIME_TYPES, filter)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            lastImportedPath?.let {
                try {
                    startActivityForResult(intent, ACTION_SAF_FILEPICKER)
                    1
                } catch (e: Exception) {
                    0
                }
            } ?: 0
        } else {
            0
        }
    }

    override fun performHapticFeedback(constant: Int, force: Int) {
        if (!takesWindowOwnership) {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            hapticFeedback(constant, force > 0, rootView)
        }
    }

    override fun requestWriteSystemSettings() {
        Permissions.requestWriteSettingsPermission(this@MainActivity)
    }

    override fun setScreenOffTimeout(timeout: Int) {


        when {
            timeout > SCREEN_ON_DISABLED -> {
                // custom timeout
                isCustomTimeout = true
                appTimeout = safeTimeout(timeout)
                val mins = toMin(appTimeout)
                setScreenOn(false)
                Logger.d(TAG_MAIN,
                    "applying activity custom timeout: $mins minutes")
                setSystemScreenOffTimeout(appTimeout)
            }
            timeout == SCREEN_ON_ENABLED -> {
                // always on
                isCustomTimeout = false
                appTimeout = 0
                setScreenOn(true)
            }
            else -> {
                // default
                appTimeout = 0
                isCustomTimeout = false
                setScreenOn(false)
            }
        }
    }

    override fun startEPDTestActivity() {
        val intent = Intent(this@MainActivity, EPDTestActivity::class.java)
        startActivity(intent)
    }

    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    @Suppress("DEPRECATION")
    private fun isHwLandscape(): Boolean {
        val display = windowManager.defaultDisplay
        return display.width > display.height
    }

    private fun hapticFeedback(constant: Int, force: Boolean, view: View) {
        runOnUiThread {
            if (force) {
                view.performHapticFeedback(constant, 2)
            } else {
                view.performHapticFeedback(constant)
            }
        }
    }

    /* set a fullscreen layout */
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

    private fun setFrontlightDialogState(state: Int) {
        lightDialogState = state
    }

    /* keep screen awake toggle */
    private fun setScreenOn(enable: Boolean) {
        if (enable != isScreenAlwaysOn) {
            Logger.d(TAG_MAIN, "screen on: switching to $enable")
            isScreenAlwaysOn = enable
            runOnUiThread {
                if (enable) {
                    Logger.d(TAG_MAIN, "add FLAG_KEEP_SCREEN_ON")
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    Logger.d(TAG_MAIN, "clear FLAG_KEEP_SCREEN_ON")
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    /* toggle system/activity timeouts based on activity state */
    private fun applyCustomTimeout(enable: Boolean) {
        if (enable)
            systemTimeout = getSystemScreenOffTimeout()

        var message = ""
        val resumed = ((enable && isCustomTimeout) && (appTimeout > 0))
        val paused = ((!enable && isCustomTimeout) && (systemTimeout > 0))

        val newTimeout: Int? = when {
            resumed -> {
                setScreenOn(false)
                message = "applying activity custom timeout"
                setSystemScreenOffTimeout(safeTimeout(appTimeout))
                appTimeout
            }

            paused -> {
                message = "restoring system timeout"
                setSystemScreenOffTimeout(systemTimeout)
                systemTimeout
            }

            else -> null
        }
        if (newTimeout != null) {
            val mins = toMin(newTimeout)
            Logger.d(TAG_MAIN, "$message: $mins minutes")
        }
    }

    private fun safeTimeout(timeout: Int): Int {
        return when {
            timeout < TIMEOUT_MIN -> TIMEOUT_MIN
            timeout > TIMEOUT_MAX -> TIMEOUT_MAX
            else -> timeout
        }
    }

    private fun toMin(milliseconds: Int): Int {
        return if (milliseconds > 0) milliseconds / (1000 * 60) else 0
    }

    private fun showPermissionDialog(message: String, isError: Boolean) {
        val builder = AlertDialog.Builder(this@MainActivity)
        val title = resources.getString(if (isError) R.string.error else R.string.warning)
        builder.setTitle(title).setMessage(message).setPositiveButton("OK") { _, _ ->
            if (isError) {
                finish()
            } else {
                Permissions.requestStoragePermission(this@MainActivity)
            }
        }
        builder.create().show()
    }

    private fun runLuaJIT() {
        initialized = true
        onWindowFocusChanged(true)
    }

    private fun getSystemScreenOffTimeout(): Int {
        return try {
            Settings.System.getInt(applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            Logger.w(TAG_MAIN, "$e")
            0
        }
    }

    private fun setSystemScreenOffTimeout(timeout: Int) {
        if (timeout <= 0) return
        try {
            Settings.System.putInt(applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        } catch (e: Exception) {
            Logger.w(TAG_MAIN, "$e")
        }
    }
}
