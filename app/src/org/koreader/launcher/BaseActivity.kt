package org.koreader.launcher

import java.io.IOException
import java.util.Locale
import java.util.concurrent.CountDownLatch

import android.app.Dialog
import android.app.DownloadManager
import android.app.NativeActivity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.text.format.Formatter
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast

import org.koreader.launcher.device.DeviceInfo
import org.koreader.launcher.helper.ScreenHelper
import java.io.File


abstract class BaseActivity : NativeActivity(), ILuaJNI {
    private var wakelock: PowerManager.WakeLock? = null
    private var isWakeLockAllowed: Boolean = false
    private var topInsetHeight: Int = 0
    var screen: ScreenHelper? = null
    
    companion object {
        private const val TAG = "BaseActivity"
        private const val WAKELOCK_ID = "wakelock:screen_bright"
        private val PRODUCT = DeviceInfo.PRODUCT
        private val RUNTIME_VERSION = Build.VERSION.RELEASE
        private val HAS_EINK_SUPPORT = DeviceInfo.EINK_SUPPORT
        private val HAS_FULL_EINK_SUPPORT = DeviceInfo.EINK_FULL_SUPPORT
        private val NEEDS_WAKELOCK_ENABLED = DeviceInfo.BUG_WAKELOCKS
        private val BATTERY_FILTER = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    }

    /* the fullscreen dialog with a spinning circle used while uncompressing assets */
    private var dialog: FramelessProgressDialog? = null
    private class FramelessProgressDialog private constructor(context: Context) :
        Dialog(context, R.style.FramelessDialog) {
        companion object {
            fun show(context: Context, title: CharSequence): FramelessProgressDialog {
                val dialog = FramelessProgressDialog(context)
                dialog.setTitle(title)
                dialog.setCancelable(false)
                dialog.setOnCancelListener(null)
                /* The next line will add the ProgressBar to the dialog. */
                dialog.addContentView(ProgressBar(context), ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT))
                dialog.show()

                return dialog
            }
        }
    }
    private fun showProgress(title: String) {
        runOnUiThread { dialog = FramelessProgressDialog.show(this@BaseActivity, title) }
    }

    private fun dismissProgress() {
        runOnUiThread {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
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
        screen = ScreenHelper(this)
    }

    override fun onAttachedToWindow() {
        Logger.d(TAG, "onAttachedToWindow()")
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout: android.view.DisplayCutout? = window.decorView.rootWindowInsets.displayCutout
            if (cutout != null) {
                val cutoutPixels = cutout.safeInsetTop
                if (topInsetHeight != cutoutPixels) {
                    Logger.v(TAG, String.format(Locale.US,
                            "top %dpx are not available, reason: window inset", cutoutPixels))
                    topInsetHeight = cutoutPixels
                }
            }
        }
    }

    override fun onDestroy() {
        Logger.d(TAG, "onDestroy()")
        screen = null
        super.onDestroy()
    }

    /*---------------------------------------------------------------
     *               override methods used by lua/JNI               *
     *--------------------------------------------------------------*/

    /* assets ----------------------- */
    override fun extractAssets(): Int {
        val output = filesDir.absolutePath
        try {
            // is there any zip file inside the asset module?
            val zipFile = AssetsUtils.getZipFile(this)
            if (zipFile != null) {
                // zipfile found! it will be extracted or not based on its version name
                Logger.i("Check file in asset module: $zipFile")
                if (!AssetsUtils.isSameVersion(this, zipFile)) {
                    showProgress("")
                    val startTime = System.nanoTime()
                    Logger.i("Installing new package to $output")
                    val stream = assets.open("module/$zipFile")
                    AssetsUtils.unzip(stream, output)
                    val endTime = System.nanoTime()
                    val elapsedTime = endTime - startTime
                    Logger.v("update installed in " + elapsedTime / 1000000000 + " seconds")
                    dismissProgress()
                }
                // extracted without errors.
                return 1
            } else {
                // check if the app has other, non-zipped, raw assets
                Logger.i("Zip file not found, trying raw assets...")
                return if (AssetsUtils.copyUncompressedAssets(this)) 1 else 0
            }
        } catch (e: IOException) {
            Logger.e(TAG, "error extracting assets:\n$e")
            dismissProgress()
            return 0
        }

    }

    /* battery ---------------- */
    override val isCharging: Int
        get() = getBatteryState(false)

    override val batteryLevel: Int
        get() = getBatteryState(true)

    private fun getBatteryState(isPercent: Boolean): Int {
        val intent = applicationContext.registerReceiver(null, BATTERY_FILTER)
        return if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val percent = level * 100 / scale
            if (isPercent) percent else if (plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB)
                if (percent != 100) 1
                else 0
            else 0
        } else 0
    }

    /* build ------------------ */
    override val flavor: String
        get() = resources.getString(R.string.app_flavor)

    override val name: String
        get() = resources.getString(R.string.app_name)

    override val isDebuggable: Int
        get() = if (BuildConfig.DEBUG) 1 else 0

    /* clipboard -------------------- */
    override var clipboardText: String
        get() {
            val result = Box<String>()
            val cd = CountDownLatch(1)
            val clipboard: ClipboardManager = Utils.getClipboardManager(this)
            result.value = ""
            runOnUiThread {
                try {
                    val data = clipboard.primaryClip
                    if (data != null && data.itemCount > 0) {
                        val text = data.getItemAt(0).coerceToText(applicationContext)
                        if (text != null) {
                            result.value = text.toString()
                        }
                    }

                } catch (e: Exception) {
                    Logger.w(TAG, e.toString())
                }
                cd.countDown()
            }
            try {
                cd.await()
            } catch (ex: InterruptedException) {
                return ""
            }
            return result.value.toString()
        }
        set(text) = runOnUiThread {
            try {
                val clipboard: ClipboardManager = Utils.getClipboardManager(this)
                val clip = ClipData.newPlainText("KOReader_clipboard", text)
                clipboard.primaryClip = clip
            } catch (e: Exception) {
                Logger.w(TAG, e.toString())
            }
        }

    override fun hasClipboardText(): Int {
        val clipboard = Utils.getClipboardManager(this)
        val bool: Boolean = if (clipboard.hasPrimaryClip()) {
            val data = clipboard.primaryClip
            data?.let { it.itemCount > 0 } ?: false
        } else false

        return if (bool) 1 else 0
    }

    /* device ------------------ */
    override val product: String
        get() = PRODUCT

    override val version: String
        get() = RUNTIME_VERSION

    override val isEink: Int
        get() = if (HAS_EINK_SUPPORT) 1 else 0

    override val isEinkFull: Int
        get() = if (HAS_FULL_EINK_SUPPORT) 1 else 0

    override val einkPlatform: String
        get() = if (DeviceInfo.EINK_FREESCALE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                "freescale"
            } else {
                "freescale-legacy"
            }
        } else if (DeviceInfo.EINK_ROCKCHIP) {
            "rockchip"
        } else {
            "none"
        }

    override fun needsWakelocks(): Int {
        return if (NEEDS_WAKELOCK_ENABLED) 1 else 0
    }

    /* intents ---------------------------- */
    override fun openLink(url: String): Int {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        return if (startActivityIfSafe(intent)) 0 else 1
    }

    override fun dictLookup(text: String, pkg: String, action: String) {
        val intent = Intent(IntentUtils.getByAction(text, pkg, action))
        if (!startActivityIfSafe(intent)) {
            Logger.e(TAG,
                "dictionary lookup: can't find a package able to resolve action $action")
        }
    }

    private fun startActivityIfSafe(intent: Intent?): Boolean {
        val intentStr: String? = IntentUtils.intentToString(intent)
        try {
            val pm = packageManager
            val act = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (act.size > 0) {
                Logger.d(TAG, "starting activity with intent: $intentStr")
                startActivity(intent)
                return true
            } else Logger.w(TAG, "unable to find a package for $intentStr")
            return false
        } catch (e: Exception) {
            Logger.e(TAG,"Error while looking for a package to open " + intentStr +
                    "\nException: " + e.toString())
            return false
        }
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

    /* screen */
    override var screenBrightness: Int
        get() = screen!!.screenBrightness
        set(brightness) = screen!!.setScreenBrightness(this, brightness)

    override var screenOffTimeout: Int
        get() = screen!!.appTimeout
        set(timeout) = screen!!.setTimeout(timeout)

    override val screenAvailableHeight: Int
        get() = screen!!.getScreenAvailableHeight(this)

    override val screenHeight: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            screen!!.getScreenHeight(this) - topInsetHeight
        } else {
            screen!!.getScreenHeight(this)
        }

    override val screenWidth: Int
        get() = screen!!.getScreenWidth(this)

    override val statusBarHeight: Int
        get() = screen!!.getStatusBarHeight(this)

    override val isFullscreen: Int
        get() = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2
            || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            screen!!.isFullscreen
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            screen!!.isFullscreenDeprecated(this)
        } else {
            1
        }
    override fun setFullscreen(enabled: Boolean) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2
            || Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            screen!!.setFullscreen(enabled)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            screen!!.setFullscreenDeprecated(this, enabled)
        }
    }

    /* network */
    override val networkInfo: String
        get() = getNetworkInfo(this)

    override fun download(url: String, name: String): Int {
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
        val manager = Utils.getDownloadManager(this)
        manager.enqueue(request)
        return 0
    }
    private fun getNetworkInfo(context: Context): String {
        val wifi: WifiManager = Utils.getWifiManager(context)
        val wi = wifi.connectionInfo
        val dhcp = wifi.dhcpInfo
        val ip = wi.ipAddress
        val gw = dhcp.gateway
        val ipAddr = formatIp(ip)
        val gwAddr = formatIp(gw)
        return String.format(Locale.US, "%s;%s;%s", wi.ssid, ipAddr, gwAddr)
    }
    private fun formatIp(number: Int): String {
        return if (number > 0) {
            Formatter.formatIpAddress(number)
        } else {
            number.toString()
        }
    }

    /* wifi */
    override var wifiEnabled: Int
        get() = if (Utils.getWifiManager(this).isWifiEnabled) 1 else 0
        set(enabled) = toggleWifi(enabled == 1)

    override fun setWifiEnabled(enabled: Boolean) {
        toggleWifi(enabled)
    }
    private fun toggleWifi(enabled: Boolean) {
        Utils.getWifiManager(this).isWifiEnabled = enabled
    }

    /* wakelocks */
    override fun setWakeLock(enabled: Boolean) {
        setWakelockState(enabled)
    }
    fun setWakelockState(enabled: Boolean) {
        /* release wakelock first, if present and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock != null) wakelockRelease()
        /* update wakelock settings */
        isWakeLockAllowed = enabled
        /* acquire wakelock if we don't have one and wakelocks are allowed */
        if (isWakeLockAllowed && wakelock == null) wakelockAcquire()
    }
    private fun wakelockAcquire() {
        if (isWakeLockAllowed) {
            wakelockRelease()
            val pm = Utils.getPowerManager(this)
            wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, WAKELOCK_ID)
            Logger.v(TAG, "acquiring $WAKELOCK_ID")
            // release the wakelock after 30 minutes running in the foreground without inputs.
            // it will be acquired again on the next resume callback.
            wakelock!!.acquire((30 * 60 * 1000).toLong())
        }
    }
    private fun wakelockRelease() {
        if (isWakeLockAllowed && wakelock != null) {
            Logger.v(TAG, "releasing $WAKELOCK_ID")
            wakelock!!.release()
            wakelock = null
        }
    }

    /* widgets */
    override fun showToast(message: String) {
        showToast(message, false)
    }
    override fun showToast(message: String, is_long: Boolean) {
        runOnUiThread {
            val toast = Toast.makeText(this@BaseActivity, message,
                if (is_long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
            toast.show()
        }
    }
}
