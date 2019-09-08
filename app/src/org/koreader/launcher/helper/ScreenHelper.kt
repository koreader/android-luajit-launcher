package org.koreader.launcher.helper

import java.util.Locale
import java.util.concurrent.CountDownLatch

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager

import org.koreader.launcher.Logger


/* Screen helper.

   Some methods are intended to be executed on the UIThread.
   You'll need to pass an Activity as a parameter of your methods. */

class ScreenHelper(context: Context) : BaseHelper(context) {

    var appBrightness: Int = 0
    var appTimeout: Int = 0

    private var customBrightness = false
    private var fullscreen = true
    private var sysBrightness = readSettingScreenBrightness()
    private var sysTimeout = readSettingScreenOffTimeout()

    companion object {
        const val BRIGHTNESS_MIN: Int = 0
        const val BRIGHTNESS_MAX: Int = 255
        const val TIMEOUT_WAKELOCK: Int = -1
        const val TIMEOUT_SYSTEM: Int = 0
    }

    /* Screen brightness */
    val screenBrightness: Int
        get() = if (customBrightness) {
            if (appBrightness >= BRIGHTNESS_MIN && appBrightness <= BRIGHTNESS_MAX)
                appBrightness
            else
                sysBrightness
        } else {
            sysBrightness
        }

    /* Screen layout */
    val isFullscreen: Int
        get() = if (fullscreen) 1 else 0


    /* Screen size */
    fun getScreenWidth(activity: Activity): Int {
        return getScreenSize(activity).x
    }

    fun getScreenHeight(activity: Activity): Int {
        return getScreenSize(activity).y
    }

    fun getScreenAvailableHeight(activity: Activity): Int {
        return getScreenSizeWithConstraints(activity).y
    }

    // DEPRECATED: returns 0 on API16+
    fun getStatusBarHeight(activity: Activity): Int {
        val rectangle = Rect()
        val window = activity.window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top
    }

    fun setScreenBrightness(activity: Activity, brightness: Int) {
        var custom = true
        if (brightness < 0) {
            Logger.d(tag, "using system brightness")
            sysBrightness = readSettingScreenBrightness()
            custom = false
        } else {
            Logger.d(tag, "using custom brightness: $brightness")
        }
        val value = custom
        val level = brightness * 1.0f / BRIGHTNESS_MAX
        runOnUiThread(Runnable {
            try {
                val params = activity.window.attributes
                params.screenBrightness = level
                activity.window.attributes = params
                appBrightness = brightness
                customBrightness = value
            } catch (e: Exception) {
                Logger.w(tag, e.toString())
            }
        })
    }

    /**
     * set the new timeout state
     *
     * known timeout states are TIMEOUT_SYSTEM, TIMEOUT_WAKELOCK
     * and values greater than 0 (milliseconds of the new timeout).
     *
     * @param new_timeout - new timeout state:
     */

    fun setTimeout(new_timeout: Int) {
        // update appTimeout first
        appTimeout = new_timeout
        // custom timeout in milliseconds
        if (appTimeout > TIMEOUT_SYSTEM) {
            Logger.v(tag, String.format(Locale.US,
                    "set timeout for app: %d seconds",
                    appTimeout / 1000))
            writeSettingScreenOffTimeout(appTimeout)
            // default timeout (by using system settings with or without wakelocks)
        } else if (appTimeout == TIMEOUT_SYSTEM || appTimeout == TIMEOUT_WAKELOCK) {
            Logger.v(tag, String.format(Locale.US,
                    "set timeout for app: (state: %d), restoring defaults: %d seconds",
                    appTimeout, sysTimeout / 1000))
            writeSettingScreenOffTimeout(sysTimeout)
        }
    }

    /**
     * set timeout based on activity state
     *
     * @param resumed - is the activity resumed and focused?
     */

    fun setTimeout(resumed: Boolean) {
        try {
            if (resumed) {
                // back from paused: update android screen off timeout first
                sysTimeout = readSettingScreenOffTimeout()

                // apply a custom timeout for the application
                if (sysTimeout != appTimeout && appTimeout > TIMEOUT_SYSTEM) {
                    Logger.v(tag, String.format(Locale.US,
                            "restoring app timeout: %d -> %d seconds",
                            sysTimeout / 1000, appTimeout / 1000))

                    writeSettingScreenOffTimeout(appTimeout)
                }
            } else {
                // app paused: restore system timeout.
                if (sysTimeout != appTimeout && appTimeout > TIMEOUT_SYSTEM) {
                    Logger.v(tag, String.format(Locale.US,
                            "restoring system timeout: %d -> %d seconds",
                            appTimeout / 1000, sysTimeout / 1000))

                    writeSettingScreenOffTimeout(sysTimeout)
                }
            }
        } catch (e: Exception) {
            Logger.w(tag, e.toString())
        }

    }

    fun isFullscreenDeprecated(activity: Activity): Int {
        return if (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
            1
        else
            0
    }

    fun setFullscreen(enabled: Boolean) {
        fullscreen = enabled
    }

    fun setFullscreenDeprecated(activity: Activity, fullscreen: Boolean) {
        val cd = CountDownLatch(1)
        runOnUiThread(Runnable {
            try {
                val window = activity.window
                if (fullscreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            } catch (e: Exception) {
                Logger.w(tag, e.toString())
            }

            cd.countDown()
        })
        try {
            cd.await()
        } catch (ex: InterruptedException) {
            Logger.e(tag, ex.toString())
        }

    }

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

    private fun getScreenSizeWithConstraints(activity: Activity): Point {
        val size = Point()
        val display = activity.windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        size.set(metrics.widthPixels, metrics.heightPixels)
        return size
    }

    private fun readSettingScreenBrightness(): Int {
        return try {
            Settings.System.getInt(applicationContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            Logger.w(tag, e.toString())
            0
        }

    }

    private fun readSettingScreenOffTimeout(): Int {
        try {
            return Settings.System.getInt(applicationContext.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            Logger.w(tag, e.toString())
            return 0
        }

    }

    private fun writeSettingScreenOffTimeout(timeout: Int) {
        if (timeout <= 0) return

        try {
            Settings.System.putInt(applicationContext.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        } catch (e: Exception) {
            Logger.w(tag, e.toString())
        }

    }
}
