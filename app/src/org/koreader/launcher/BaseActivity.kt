package org.koreader.launcher

import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.Locale

import android.app.Dialog
import android.app.DownloadManager
import android.app.NativeActivity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.text.format.Formatter
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/* BaseActivity.java - convenience wrapper on top of NativeActivity that
 * implements most of the kotlin/java methods exposed to lua. */

abstract class BaseActivity : NativeActivity(), JNILuaInterface,
    ActivityCompat.OnRequestPermissionsResultCallback{

    private var brightness: Int = 0
    private var customBrightness: Boolean = false
    private var fullscreen: Boolean = true // only used on API levels 16-18
    private var splashScreen: Boolean = true
    private var topInsetHeight: Int = 0 // only used on API 28+
    private lateinit var clipboard: ClipboardManager

    companion object {
        private const val TAG = "BaseActivity"
        private val BATTERY_FILTER = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        private val PRODUCT = DeviceInfo.PRODUCT
        private val RUNTIME_VERSION = Build.VERSION.RELEASE
        private val HAS_EINK_SUPPORT = DeviceInfo.EINK_SUPPORT
        private val HAS_FULL_EINK_SUPPORT = DeviceInfo.EINK_FULL_SUPPORT
        private val NEEDS_WAKELOCK_ENABLED = DeviceInfo.BUG_WAKELOCKS
    }

    /* dialog used while extracting assets from zip */
    private var dialog: FramelessProgressDialog? = null
    private class FramelessProgressDialog private constructor(context: Context):
        Dialog(context, R.style.FramelessDialog) {
        companion object {
            fun show(context: Context, title: CharSequence): FramelessProgressDialog {
                val dialog = FramelessProgressDialog(context)
                dialog.setTitle(title)
                dialog.setCancelable(false)
                dialog.setOnCancelListener(null)
                dialog.window?.setGravity(Gravity.BOTTOM)
                val progressBar = ProgressBar(context)
                try {
                    ContextCompat.getDrawable(context, R.drawable.discrete_spinner)
                        ?.let { spinDrawable -> progressBar.indeterminateDrawable = spinDrawable }
                } catch (e: Exception) {
                    Logger.w("Failed to set progress drawable:\n$e")
                }
                /* The next line will add the ProgressBar to the dialog. */
                dialog.addContentView(progressBar, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                )
                dialog.show()
                return dialog
            }
        }
    }

    private inner class Box<T> {
        internal var value: T? = null
    }

    /*---------------------------------------------------------------
     *                        activity callbacks                    *
     *--------------------------------------------------------------*/

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setTheme(R.style.Fullscreen)
        // Window background must be black for vertical and horizontal lines to be visible
        window.setBackgroundDrawableResource(android.R.color.black)

        clipboard = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE)
            as ClipboardManager
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        drawSplashScreen(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        super.surfaceChanged(holder, format, width, height)
        if (holder != null) {
            drawSplashScreen(holder)
        }
    }

    override fun onAttachedToWindow() {
        Logger.d(TAG, "onAttachedToWindow()")
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cut: android.view.DisplayCutout? = window.decorView.rootWindowInsets.displayCutout
            if (cut != null) {
                val cutPixels = cut.safeInsetTop
                if (topInsetHeight != cutPixels) {
                    Logger.v(TAG,
                        "top $cutPixels pixels are not available, reason: window inset")
                    topInsetHeight = cutPixels
                }
            }
        }
    }

    override fun onDestroy() {
        Logger.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    /*---------------------------------------------------------------
     *             implement methods used by lua/JNI                *
     *--------------------------------------------------------------*/

    /* assets */
    override fun extractAssets(): Int {
        val output = filesDir.absolutePath
        val check = try {
            val zipFile = AssetsUtils.getZipFromAsset(this)
            if (zipFile != null) {
                var ok = true
                Logger.i("Check file in asset module: $zipFile")
                // upgrade or downgrade files from zip
                if (!AssetsUtils.isSameVersion(this, zipFile)) {
                    showProgress() // spinning circle starts
                    val startTime = System.nanoTime()
                    Logger.i("Installing new package to $output")
                    val stream = assets.open("module/$zipFile")
                    ok = AssetsUtils.unzip(stream, output, true)
                    val endTime = System.nanoTime()
                    val elapsedTime = endTime - startTime
                    Logger.v("update installed in " + elapsedTime / 1000000000 + " seconds")
                    dismissProgress() // spinning circle stops
                }
                if (ok) 1 else 0
            } else {
                // check if the app has other, non-zipped, raw assets
                Logger.i("Zip file not found, trying raw assets...")
                if (AssetsUtils.copyRawAssets(this)) 1 else 0
            }
        } catch (e: IOException) {
            Logger.e(TAG, "error extracting assets:\n$e")
            dismissProgress()
            0
        }
        splashScreen = false
        return check
    }

    /* build */
    override fun getFlavor(): String {
        return resources.getString(R.string.app_flavor)
    }

    override fun getName(): String {
        return resources.getString(R.string.app_name)
    }

    override fun isDebuggable(): Int {
        return if (BuildConfig.DEBUG) 1 else 0
    }

    /* clipboard */
    override fun getClipboardText(): String {
        val result = Box<String>()
        val cd = CountDownLatch(1)
        runOnUiThread {
            try {
                val data = clipboard.primaryClip
                if (data != null && data.itemCount > 0) {
                    val text = data.getItemAt(0).coerceToText(
                        applicationContext)
                    if (text != null) {
                        result.value = text.toString()
                    } else {
                        result.value = ""
                    }
                }
            } catch (e: Exception) {
                Logger.w(TAG, e.toString())
                result.value = ""
            }
            cd.countDown()
        }
        try {
            cd.await()
        } catch (ex: InterruptedException) {
            return ""
        }
        return result.value ?: ""
    }

    override fun hasClipboardText(): Int {
        val clipdata = clipboard.primaryClip
        return if (clipdata != null) {
            val number = clipdata.itemCount
            if (number > 0) 1 else 0
        } else 0
    }

    override fun setClipboardText(text: String) {
        runOnUiThread {
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText("KOReader_clipboard", text))
            } catch (e: Exception) {
                Logger.w(TAG, e.toString())
            }
        }
    }

    /* device */
    override fun getEinkPlatform(): String {
        return if (DeviceInfo.EINK_FREESCALE) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                || (DeviceInfo.EINK == DeviceInfo.EinkDevice.CREMA)) {
                "freescale"
            } else {
                "freescale-legacy"
            }
        } else if (DeviceInfo.EINK_ROCKCHIP) {
            "rockchip"
        } else {
            "none"
        }
    }

    override fun getExternalPath(): String {
        return MainApp.storage_path
    }

    override fun getProduct(): String {
        return PRODUCT
    }

    override fun getVersion(): String {
        return RUNTIME_VERSION
    }

    override fun isEink(): Int {
        return if (HAS_EINK_SUPPORT) 1 else 0
    }

    override fun isEinkFull(): Int {
        return if (HAS_FULL_EINK_SUPPORT) 1 else 0
    }

    override fun needsWakelocks(): Int {
        return if (NEEDS_WAKELOCK_ENABLED) 1 else 0
    }

    /* intents */
    override fun openLink(url: String): Int {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        return if (startActivityIfSafe(intent)) 0 else 1
    }

    override fun dictLookup(text: String?, action: String?, nullablePackage: String?) {
       text?.let { lookupText ->
            action?.let { lookupAction ->
                val lookupIntent = Intent(IntentUtils.getByAction(lookupText, lookupAction, nullablePackage))
                if (!startActivityIfSafe(lookupIntent)) {
                    Logger.e(TAG, "invalid lookup: can't find a package able to resolve $action")
                }
            } ?: Logger.e(TAG, "invalid lookup: no action")
        } ?: Logger.e(TAG, "invalid lookup: no text")
    }

    /* network */

    @Suppress("DEPRECATION")
    override fun download(url: String, name: String): Int {
        val manager: DownloadManager? = applicationContext.getSystemService(
            Context.DOWNLOAD_SERVICE) as? DownloadManager
        val file = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS).toString() + "/" + name)

        if (file.exists()) {
            Logger.w(TAG, "File already exists: skipping download")
            return 1
        }
        val request = DownloadManager.Request(Uri.parse(url))
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)

        /* Try to download the request. This *should* not fail, but it fails
           on some AOSP devices that don't need to pass google CTS. */
        try {
            manager?.enqueue(request)
            return 0
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    override fun getNetworkInfo(): String {
        val wifi: WifiManager? = applicationContext.getSystemService(
            Context.WIFI_SERVICE) as? WifiManager
        return if (wifi != null) {
            val wi = wifi.connectionInfo
            val dhcp = wifi.dhcpInfo
            val ip = wi.ipAddress
            val gw = dhcp.gateway
            val ipAddress = formatIp(ip)
            val gwAddress = formatIp(gw)
            String.format(Locale.US, "%s;%s;%s", wi.ssid, ipAddress, gwAddress)
        } else ""
    }

    override fun isWifiEnabled(): Int {
        val wifi: WifiManager? = applicationContext.getSystemService(
            Context.WIFI_SERVICE) as? WifiManager
        return if (wifi?.isWifiEnabled == true) 1 else 0
    }

    @Suppress("DEPRECATION")
    override fun setWifiEnabled(enabled: Boolean) {
        val wifi: WifiManager? = applicationContext.getSystemService(
            Context.WIFI_SERVICE) as? WifiManager
        wifi?.isWifiEnabled = enabled
    }

    /* package manager */
    override fun isPackageEnabled(pkg: String): Int {
        return try {
            val pm = packageManager
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            val enabled = pm.getApplicationInfo(pkg, 0).enabled
            if (enabled) 1 else 0
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }

    /* power */
    override fun isCharging(): Int {
        return getBatteryState(false)
    }

    override fun getBatteryLevel(): Int {
        return getBatteryState(true)
    }

    /* screen */
    override fun getScreenBrightness(): Int {
        return if (customBrightness) {
            brightness
        } else {
            SystemSettings.getSystemBrightness(this)
            //ScreenUtils.readSettingScreenBrightness(this)
        }
    }

    override fun getScreenAvailableHeight(): Int {
        return ScreenUtils.getScreenAvailableHeight(this)
    }

    override fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ScreenUtils.getScreenHeight(this) - topInsetHeight
        } else {
            ScreenUtils.getScreenHeight(this)
        }
    }

    override fun getScreenWidth(): Int {
        return ScreenUtils.getScreenWidth(this)
    }

    override fun getStatusBarHeight(): Int {
        return ScreenUtils.getStatusBarHeight(this)
    }

    override fun isFullscreen(): Int {
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (fullscreen) 1 else 0
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            ScreenUtils.isFullscreenDeprecated(this)
        } else {
            1
        }
    }

    override fun setFullscreen(enabled: Boolean) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2 ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fullscreen = enabled
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            ScreenUtils.setFullscreenDeprecated(this, enabled)
        }
    }

    override fun setScreenBrightness(brightness: Int) {
        ScreenUtils.setScreenBrightness(this, brightness)
    }

    /* widgets */
    override fun showToast(message: String) {
        showToast(message, false)
    }

    override fun showToast(message: String, longTimeout: Boolean) {
        runOnUiThread {
            if (longTimeout) {
                val toast = Toast.makeText(this@BaseActivity,
                        message, Toast.LENGTH_LONG)
                toast.show()
            } else {
                val toast = Toast.makeText(this@BaseActivity,
                        message, Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    /* dialogs */
    private fun showProgress() {
        runOnUiThread {
            dialog = FramelessProgressDialog.show(this@BaseActivity, "") }
    }

    private fun dismissProgress() {
        runOnUiThread {
            dialog?.dismiss()
        }
    }

    /* draw splash screen to surface */
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
                    Logger.w(TAG, "Failed to draw splash screen:\n$e")
                }
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    /* network */

    /* Formatter.formatIpAddress is deprecated in API15 because
       it does not support IPv6. Is still handy to format ip and
       gateway addresses given by most LAN routers */

    @Suppress("DEPRECATION")
    private fun formatIp(number: Int): String {
        return if (number > 0) {
            Formatter.formatIpAddress(number)
        } else {
            number.toString()
        }
    }

    /* power */
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

    /* start activity if we find a package able to handle a given intent */
    private fun startActivityIfSafe(intent: Intent?): Boolean {
        if (intent == null) return false
        val intentStr = IntentUtils.intentToString(intent)
        try {
            val pm = packageManager
            val act = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (act.size > 0) {
                Logger.d(TAG, "starting activity with intent: $intentStr")
                startActivity(intent)
                return true
            } else {
                Logger.w(TAG, "unable to find a package for $intentStr")
            }
            return false
        } catch (e: Exception) {
            Logger.e(TAG, "error opening $intentStr\nException: $e")
            return false
        }
    }
}
