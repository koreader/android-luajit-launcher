package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.Ioctl
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.extensions.read
import java.io.File

/* Special controller for Tolino Vision5
 * see https://github.com/koreader/android-luajit-launcher/pull/382
 *
 * Original Controller by @zwim, see "./TolinoRootController.kt"
 */

class TolinoNtxController : Ioctl(), LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val WARMTH_MAX = 10 // can also be read by "/sys/class/backlight/lm3630a_led/max_color" on Vision5
        private const val MIN = 0
        private const val NTX_IO_FILE = "/dev/ntx_io"
        private const val NTX_WARMTH_ID = 248
        private const val COLOR_FILE = "/sys/class/backlight/lm3630a_led/color" // only readable by "system" in Vision5
    }

    // store the current warmth value, because in some models (Vision5) it cannot be fetched
    private var currentWarmth: Int? = null

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
        return false
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun getBrightness(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                "screen_brightness")
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        if (currentWarmth == null) {
            currentWarmth = WARMTH_MAX - File(COLOR_FILE).read()
        }
        return currentWarmth ?: WARMTH_MAX
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                "screen_brightness", brightness)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }

        Log.v(TAG, "Setting warmth to $warmth of $WARMTH_MAX")

        if (io(NTX_IO_FILE, NTX_WARMTH_ID, (warmth - WARMTH_MAX) * -1))
            currentWarmth = warmth
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
