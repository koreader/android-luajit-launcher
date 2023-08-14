package org.koreader.launcher.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.CountDownLatch

val Activity.platform: String
    get() = if (packageManager.hasSystemFeature("org.chromium.arc.device_management")) {
        "chrome"
    } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        && packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
        "android_tv"
    } else {
        "android"
    }
/* Haptic feedback */
fun Activity.hapticFeedback(constant: Int, force: Boolean, view: View) {
    runOnUiThread {
        if (force) {
            view.performHapticFeedback(constant, 2)
        } else {
            view.performHapticFeedback(constant)
        }
    }
}

fun Activity.aardAction(text: String) {
    val aardIntent: Intent = Intent().apply {
        action = "aard2.lookup"
        putExtra(SearchManager.QUERY, text)
    }
    startDictionaryActivity(this, aardIntent)
}

fun Activity.colordictAction(text: String, domain: String? = null) {
    val colordictIntent: Intent = Intent().apply {
        action = "aard2.lookup"
        putExtra("EXTRA_QUERY", text)
        putExtra("EXTRA_FULLSCREEN", true)
    }
    startDictionaryActivity(this, colordictIntent, domain)
}

fun Activity.filePicker(id: Int): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val safIntent: Intent = Intent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, supported_extensions)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(safIntent, id)
        true
    } else {
        false
    }
}

fun Activity.getAvailableHeight(): Int {
    return getScreenSizeWithConstraints(this).y
}

fun Activity.getAvailableWidth(): Int {
    return getScreenSizeWithConstraints(this).x
}

fun Activity.getBarHeight(): Int {
    val rectangle = Rect()
    window.decorView.getWindowVisibleDisplayFrame(rectangle)
    return rectangle.top
}

fun Activity.getHeight(): Int {
    return getScreenSize(this).y
}

fun Activity.getSdcardPath(): String? {
    val context = this.applicationContext
    val packageName = context.packageName
    return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        try {
            val volumes: Array<out java.io.File> = ContextCompat.getExternalFilesDirs(context, null)
            volumes[1].absolutePath.replace("/Android/data/$packageName/files", "")
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
}

fun Activity.getWidth(): Int {
    return getScreenSize(this).x
}

@Suppress("DEPRECATION")
fun Activity.isFullscreenDeprecated(): Boolean {
    return (window.attributes.flags and
        WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
}

@Suppress("DEPRECATION")
fun Activity.networkInfo(): String {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager

    var connectionType: Int = ACTIVE_NETWORK_NONE
    val connected: Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork?.let { net ->
                connectivityManager.getNetworkCapabilities(net)?.let {
                    when {
                        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            connectionType = ACTIVE_NETWORK_WIFI
                            true
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            connectionType = ACTIVE_NETWORK_MOBILE
                            true
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                            connectionType = ACTIVE_NETWORK_ETHERNET
                            true
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> {
                            connectionType = ACTIVE_NETWORK_BLUETOOTH
                            true
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                            connectionType = ACTIVE_NETWORK_VPN
                            true
                        }
                        else -> false
                    }
                } ?: false
            } ?: false
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            connectionType = ACTIVE_NETWORK_WIFI
                            true
                        }
                        ConnectivityManager.TYPE_MOBILE -> {
                            connectionType = ACTIVE_NETWORK_MOBILE
                            true
                        }
                        ConnectivityManager.TYPE_ETHERNET -> {
                            connectionType = ACTIVE_NETWORK_ETHERNET
                            true
                        }
                        ConnectivityManager.TYPE_BLUETOOTH -> {
                            connectionType = ACTIVE_NETWORK_BLUETOOTH
                            true
                        }
                        ConnectivityManager.TYPE_VPN -> {
                            connectionType = ACTIVE_NETWORK_VPN
                            true
                        }
                        else -> false
                    }
                }
            } ?: false
        }
    return String.format(Locale.US, "%d;%d", if (connected) 1 else 0, connectionType)
}

fun Activity.processTextAction(text: String, domain: String? = null) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        sendAction(text, domain)
    } else {
        val processTextIntent: Intent = Intent().apply {
            action = Intent.ACTION_PROCESS_TEXT
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, text)
            type = "text/plain"
        }
        startDictionaryActivity(this, processTextIntent, domain)
    }
}

fun Activity.quickdicAction(text: String) {
    val quickdicIntent: Intent = Intent().apply {
        action = "com.hughes.action.ACTION_SEARCH_DICT"
        putExtra(SearchManager.QUERY, text)
    }
    startDictionaryActivity(this, quickdicIntent)
}

fun Activity.openWifi() {
    val openWifiIntent = Intent().apply {
        action = Settings.ACTION_WIFI_SETTINGS
    }
    startActivityCompat(this, openWifiIntent)
}

fun Activity.pruneCacheDir() {
    try {
        getExternalFilesDir(null)?.let {
            it.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Activity.requestSpecialPermission(intent: Intent, rationale: String,
                             okButton: String?, cancelButton: String?) {
    runOnUiThread {
        val ok = okButton ?: "OK"
        val builder = AlertDialog.Builder(this)
            .setMessage(rationale)
            .setCancelable(false)
            .setPositiveButton(ok) { _, _ ->
                startActivity(intent)
                finishAffinity()
            }

        if (cancelButton != null) {
            builder.setNegativeButton(cancelButton) { _, _ -> }
        }
        builder.create().show()
    }
}

fun Activity.searchAction(text: String, domain: String? = null) {
    val searchIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEARCH
        putExtra(SearchManager.QUERY, text)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startDictionaryActivity(this, searchIntent, domain)
}

fun Activity.sendAction(text: String, domain: String? = null) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    startDictionaryActivity(this, sendIntent, domain)
}

@Suppress("DEPRECATION")
fun Activity.setFullscreenDeprecated(fullscreen: Boolean) {
    val cd = CountDownLatch(1)
    runOnUiThread {
        try {
            if (fullscreen) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cd.countDown()
    }
    try {
        cd.await()
    } catch (ex: InterruptedException) {
        ex.printStackTrace()
    }
}

@Suppress("DEPRECATION")
private fun getScreenSize(activity: Activity): Point {
    val size = Point()
    val display = activity.windowManager.defaultDisplay

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        size.set(metrics.widthPixels, metrics.heightPixels)
    } else {
        display.getSize(size)
    }
    return size
}

@Suppress("DEPRECATION")
private fun getScreenSizeWithConstraints(activity: Activity): Point {
    val size = Point()
    val display = activity.windowManager.defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    size.set(metrics.widthPixels, metrics.heightPixels)
    return size
}

@SuppressLint("QueryPermissionsNeeded")
private fun startDictionaryActivity(context: Context, intent: Intent, domain: String? = null) {
    domain?.let {
        intent.setPackage(it)
        try {
            val pm = context.packageManager
            val act = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (act.size > 0) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } ?: run {
        startActivityCompat(context, intent)
    }
}

private fun startActivityCompat(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/* Orientation */
@Suppress("DEPRECATION")
fun Activity.getOrientationCompat(isLandscape: Boolean): Int {
    return when (windowManager.defaultDisplay.rotation) {
        Surface.ROTATION_90 -> if (isLandscape) LINUX_PORTRAIT else LINUX_REVERSE_LANDSCAPE
        Surface.ROTATION_180 -> if (isLandscape) LINUX_REVERSE_LANDSCAPE else LINUX_REVERSE_PORTRAIT
        Surface.ROTATION_270 -> if (isLandscape) LINUX_REVERSE_PORTRAIT else LINUX_LANDSCAPE
        else -> if (isLandscape) LINUX_LANDSCAPE else LINUX_PORTRAIT
    }
}

fun Activity.setOrientationCompat(isLandscape: Boolean, orientation: Int) {
    val newOrientation = if (isLandscape) {
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

// constants from https://github.com/koreader/android-luajit-launcher/blob/master/assets/android.lua
private const val ACTIVE_NETWORK_NONE = 0
private const val ACTIVE_NETWORK_WIFI = 1
private const val ACTIVE_NETWORK_MOBILE = 2
private const val ACTIVE_NETWORK_ETHERNET = 3
private const val ACTIVE_NETWORK_BLUETOOTH = 4
private const val ACTIVE_NETWORK_VPN = 5

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

private val supported_extensions = arrayOf(
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
