package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.LightsInterface

/* handle frontlight within the activity, without affecting other activities */

class BoyueS62RootController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 254
        private const val BRIGHTNESS_MIN = 0
    }

    override fun getPlatform(): String {
        return "boyue-s62-root"
    }

    override fun hasFallback(): Boolean {
        return true
    }

    override fun hasWarmth(): Boolean {
        return false
    }

    override fun needsPermission(): Boolean {
        return false
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun getBrightness(activity: Activity): Int {
        var brightness = 0
        activity.runOnUiThread {
            try {
                brightness = Settings.System.getInt(activity.applicationContext.contentResolver, "boyue_warm_light")
            } catch (e: Exception) {
                Log.w(TAG, "$e")
            }
        }
        Log.v(TAG, "getBrightness: $brightness")
        return brightness
    }

    override fun getWarmth(activity: Activity): Int {
        Log.w(TAG, "getWarmth: not implemented")
        return 0
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < BRIGHTNESS_MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value out of range: $brightness")
            return
        }
        Log.v(TAG, "setBrightness: $brightness")
        activity.runOnUiThread {
            try {
                //Runtime.getRuntime().exec("su -c settings put system boyue_warm_light " + brightness) // slow
                Runtime.getRuntime().exec("su -c echo " + brightness + " > /sys/class/backlight/rk28_bl_warm/brightness")
            } catch (e: Exception) {
                Log.w(TAG, "$e")
            }
          }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        Log.w(TAG, "ignoring setWarmth: not implemented")
    }

    override fun getMinWarmth(): Int {
        return 0
    }
    override fun getMaxWarmth(): Int {
        return 0
    }
    override fun getMinBrightness(): Int {
        return BRIGHTNESS_MIN
    }
    override fun getMaxBrightness(): Int {
        return BRIGHTNESS_MAX
    }

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }
}
