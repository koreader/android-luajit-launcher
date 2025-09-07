package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.Ioctl
import org.koreader.launcher.device.LightsInterface

// Light and warmth controller for B300 Tolino devices (Epos 3, Vision 6, Shine 4)
// Need testers for Shine 4, I'm operating under the assumption that this works.
// Vision 6 has inverted warmth from personal testing.
class TolinoB300Controller : Ioctl(), LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 100
        private const val WARMTH_MAX = 10
        private const val MIN = 0
        private const val SCREEN_BRIGHTNESS = "screen_brightness"
        private const val SCREEN_BRIGHTNESS_COLOR = "screen_brightness_color"
    }

    override fun getPlatform(): String {
        return "tolino"
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
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver, SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            Settings.System.getInt(
                    activity.applicationContext.contentResolver,
                    SCREEN_BRIGHTNESS_COLOR
            )
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        try {
            Settings.System.putInt(
                    activity.applicationContext.contentResolver,
                    SCREEN_BRIGHTNESS,
                    brightness
            )
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth")
        try {
            Settings.System.putInt(
                    activity.applicationContext.contentResolver,
                    SCREEN_BRIGHTNESS_COLOR,
                    warmth
            )

            // crappy toggle brightness to force warmth refresh
            val currentBrightness: Int =
            Settings.System.getInt(
                    activity.applicationContext.contentResolver,
                    SCREEN_BRIGHTNESS
            )
            Settings.System.putInt(
                    activity.applicationContext.contentResolver,
                    SCREEN_BRIGHTNESS,
                    currentBrightness + 1
            )
            Settings.System.putInt(
                    activity.applicationContext.contentResolver,
                    SCREEN_BRIGHTNESS,
                    currentBrightness
            )
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun getMinWarmth(): Int {
        return MIN
    }

    override fun getMaxWarmth(): Int {
        return WARMTH_MAX
    }

    override fun getMinBrightness(): Int {
        return MIN
    }

    override fun getMaxBrightness(): Int {
        return BRIGHTNESS_MAX
    }

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }
}
