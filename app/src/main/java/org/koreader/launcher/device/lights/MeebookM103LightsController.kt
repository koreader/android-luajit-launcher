package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.LightsInterface

/* Lights controller for the Meebook M103.
 *
 * The frontlight exposes two independent channels:
 *  - cold (brightness): Settings.System "screen_brightness" (standard Android
 *    brightness, controllable via the window attribute);
 *  - warm: vendor-specific Settings.System "haoqing_warm_light" (0..254).
 *
 * Writing the warm channel requires the WRITE_SETTINGS permission, so
 * needsPermission() returns true.
 */

class MeebookM103LightsController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val BRIGHTNESS_MIN = 1 // zero would mean system-settings
        private const val WARMTH_MAX = 254
        private const val WARMTH_MIN = 0
        private const val WARMTH_KEY = "haoqing_warm_light"
    }

    override fun getPlatform(): String {
        return "meebook-m103"
    }

    override fun hasFallback(): Boolean {
        return false
    }

    override fun hasWarmth(): Boolean {
        return true
    }

    override fun needsPermission(): Boolean {
        return true
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun getBrightness(activity: Activity): Int {
        val brightness = (activity.window.attributes.screenBrightness * (BRIGHTNESS_MAX - BRIGHTNESS_MIN) / 1.0f).toInt() + BRIGHTNESS_MIN
        return if (brightness < 0) {
            try {
                Settings.System.getInt(activity.applicationContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS)
            } catch (e: Exception) {
                Log.w(TAG, e.toString())
                0
            }
        } else brightness
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver, WARMTH_KEY)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            WARMTH_MIN
        }
    }

    // brightness has to be between BRIGHTNESS_MIN and BRIGHTNESS_MAX or
    // negative for system settings.
    // Values between 0 and BRIGHTNESS_MIN are gracefully ignored.
    override fun setBrightness(activity: Activity, brightness: Int) {
        Log.v(TAG, "Setting brightness to $brightness")
        val level: Float? = when (brightness) {
            0 -> 0.0f
            else -> {
                if ((brightness < BRIGHTNESS_MIN || brightness > BRIGHTNESS_MAX))
                    null
                else
                    (brightness - BRIGHTNESS_MIN) * 1.0f / (BRIGHTNESS_MAX - BRIGHTNESS_MIN)
            }
        }
        level?.let { value ->
            activity.runOnUiThread {
                try {
                    val params = activity.window.attributes
                    params.screenBrightness = value
                    activity.window.attributes = params
                } catch (e: Exception) {
                    Log.w(TAG, e.toString())
                }
            }
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < WARMTH_MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value out of range: $warmth")
            return
        }

        Log.v(TAG, "Setting warmth to $warmth")
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver, WARMTH_KEY, warmth)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun getMinWarmth(): Int {
        return WARMTH_MIN
    }

    override fun getMaxWarmth(): Int {
        return WARMTH_MAX
    }

    override fun getMinBrightness(): Int {
        return BRIGHTNESS_MIN
    }

    override fun getMaxBrightness(): Int {
        return BRIGHTNESS_MAX
    }

    override fun hasStandaloneWarmth(): Boolean {
        return true
    }
}
