package org.koreader.launcher.helpers

import java.util.Locale
import android.app.Activity
import android.provider.Settings
import android.view.WindowManager
import org.koreader.launcher.utils.Logger

class TimeoutHelper(activity: Activity) {

    // activity timeout in ms
    private var appTimeout: Int = 0

    // system timeout in ms
    private var systemTimeout: Int = 0

    // Use this setting to avoid screen dimming
    private var alwaysOn: Boolean = false

    // Use this setting to override screen off timeout while the app is in the foreground
    private var customTimeout: Boolean = false

    companion object {
        private const val TAG = "TimeoutHelper"
        private const val SCREEN_ON_ENABLED = -1
        private const val SCREEN_ON_DISABLED = 0
        private const val TIMEOUT_MIN = 2 * 60 * 1000
        private const val TIMEOUT_MAX = 45 * 60 * 1000
    }

    init {
        this.systemTimeout = getSystemScreenOffTimeout(activity)
        Logger.v(TAG, String.format(Locale.US,
            "system timeout: %s", toMin(this.systemTimeout)))
    }

    fun onResume(activity: Activity) {
        Logger.v(TAG, "timeout onResume")
        apply(activity, true)
    }

    fun onPause(activity: Activity) {
        Logger.v(TAG, "timeout onPause")
        apply(activity, false)
    }

    private fun apply(activity: Activity, resumed: Boolean) {
        if (resumed) {
            systemTimeout = getSystemScreenOffTimeout(activity)
            Logger.v(TAG, String.format(Locale.US,
                "updating system timeout: %s", toMin(systemTimeout)))
        }
        val applyResumed = ((resumed && customTimeout) && (appTimeout > 0))
        val applyPaused = ((!resumed && customTimeout) && (systemTimeout > 0))

        val template = "%s: %s - %d minutes"
        var message = ""
        val newTimeout: Int? = when {
            applyResumed -> {
                setScreenOn(activity, false)
                val safe = safeTimeout(appTimeout)
                message = String.format(template, "onResume()",
                    "apply activity timeout", toMin(safe))
                setSystemScreenOffTimeout(activity, safe)
                safe
            }
            applyPaused -> {
                message = String.format(template, "onPause()",
                    "restore system timeout", toMin(systemTimeout))
                setSystemScreenOffTimeout(activity, systemTimeout)
                systemTimeout
            }
            else -> null
        }
        newTimeout?.let {
            Logger.v(TAG, message)
        }
    }

    fun setTimeout(activity: Activity, ms: Int) {
        when {
            // custom timeout
            ms > SCREEN_ON_DISABLED -> {
                customTimeout = true
                appTimeout = safeTimeout(ms)
                val mins = toMin(appTimeout)
                Logger.v(TAG, "applying activity custom timeout: $mins minutes")
                setSystemScreenOffTimeout(activity, appTimeout)
            }
            // screen always on
            ms == SCREEN_ON_ENABLED -> {
                customTimeout = false
                appTimeout = 0
                setScreenOn(activity, true)
            }
            // default timeout
            else -> {
                customTimeout = false
                appTimeout = 0
                setScreenOn(activity, false)
            }
        }
    }

    private fun setScreenOn(activity: Activity, enable: Boolean) {
        if (enable != alwaysOn) {
            Logger.v(TAG, "screen on: switching to $enable")
            alwaysOn = enable
            val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            val action = if (enable) "add" else "clear"
            activity.runOnUiThread {
                Logger.v(TAG, "$action FLAG_KEEP_SCREEN_ON")
                if (enable) activity.window.addFlags(flag) else activity.window.clearFlags(flag)
            }
        }
    }

    fun getSystemTimeout(): Int {
        return systemTimeout
    }

    fun getSystemScreenOffTimeout(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            Logger.w(TAG, e.toString())
            0
        }
    }

    private fun setSystemScreenOffTimeout(activity: Activity, timeout: Int) {
        if (timeout <= 0) return
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
        }
    }

    private fun safeTimeout(ms: Int): Int {
        return when {
            ms < TIMEOUT_MIN -> TIMEOUT_MIN
            ms > TIMEOUT_MAX -> TIMEOUT_MAX
            else -> ms
        }
    }

    private fun toMin(ms: Int): Int {
        return when {
            ms > 0 -> ms / (1000 * 60)
            else -> 0
        }
    }
}
