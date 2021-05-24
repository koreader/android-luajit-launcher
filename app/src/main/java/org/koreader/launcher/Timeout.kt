package org.koreader.launcher

import android.app.Activity
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import java.util.*

class Timeout {
    private var appTimeout: Int = 0
    private var systemTimeout: Int = 0
    private var alwaysOn: Boolean = false
    private var customTimeout: Boolean = false

    companion object {
        private const val TAG = "TimeoutHelper"
        private const val SCREEN_ON_ENABLED = -1
        private const val SCREEN_ON_DISABLED = 0
        private const val TIMEOUT_MIN = 2 * 60 * 1000
        private const val TIMEOUT_MAX = 45 * 60 * 1000
    }

    fun onResume(activity: Activity) {
        apply(activity, true)
    }

    fun onPause(activity: Activity) {
        apply(activity, false)
    }

    private fun getSystemScreenOffTimeout(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            0
        }
    }

    fun setTimeout(activity: Activity, ms: Int) {
        when {
            // custom timeout
            ms > SCREEN_ON_DISABLED -> {
                customTimeout = true
                appTimeout = safeTimeout(ms)
                val mins = toMin(appTimeout)
                Log.v(TAG, "applying activity custom timeout: $mins minutes")
                setSystemScreenOffTimeout(activity, appTimeout)
                setScreenOn(activity, false)
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

    /* apply a timeout based on activity state

       If a custom timeout is enabled then we apply it each time the
       activity is resumed. We reapply system timeout when the activity is paused.

       Because the user can change the system timeout from android settings while
       the activity is in the background we read the updated setting on each resume.

       SCREEN_ON flag is managed automatically by the system, and does not require this.
    */

    private fun apply(activity: Activity, resumed: Boolean) {
        val logMsg = if (resumed) "onResume" else "onPause"
        if (resumed) {
            systemTimeout = getSystemScreenOffTimeout(activity)
            Log.v(TAG, String.format(Locale.US,
                "%s: updating system timeout: %s", logMsg, toMin(systemTimeout)))
        }
        if (resumed && customTimeout) {
            if (appTimeout > 0) {
                Log.v(TAG, String.format(Locale.US,
                    "%s: applying custom timeout: %s", logMsg, toMin(appTimeout)))

                val safe = safeTimeout(appTimeout)
                setSystemScreenOffTimeout(activity, safe)
            } else {
                Log.w(TAG, "$logMsg: custom timeout is 0, ignoring")
            }
        } else if (!resumed && customTimeout) {
            if (systemTimeout > 0) {
                Log.v(TAG, String.format(Locale.US,
                    "applying system timeout: %s", toMin(systemTimeout)))

                setSystemScreenOffTimeout(activity, systemTimeout)
            } else {
                Log.w(TAG, "$logMsg: system timeout is 0, ignoring")
            }
        } else {
            Log.v(TAG, logMsg)
        }
    }

    private fun safeTimeout(ms: Int): Int {
        return when {
            ms < TIMEOUT_MIN -> TIMEOUT_MIN
            ms > TIMEOUT_MAX -> TIMEOUT_MAX
            else -> ms
        }
    }

    private fun setScreenOn(activity: Activity, enable: Boolean) {
        if (enable != alwaysOn) {
            Log.v(TAG, "screen on: switching to $enable")
            alwaysOn = enable
            val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            val action = if (enable) "add" else "clear"
            activity.runOnUiThread {
                Log.v(TAG, "$action FLAG_KEEP_SCREEN_ON")
                if (enable) activity.window.addFlags(flag) else activity.window.clearFlags(flag)
            }
        }
    }

    private fun setSystemScreenOffTimeout(activity: Activity, timeout: Int) {
        if (timeout <= 0) return
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    private fun toMin(ms: Int): Int {
        return when {
            ms > 0 -> ms / (1000 * 60)
            else -> 0
        }
    }
}
