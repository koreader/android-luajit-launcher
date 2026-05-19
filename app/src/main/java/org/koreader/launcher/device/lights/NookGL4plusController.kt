package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.LightsInterface

/* Controller for Nook Glowlight 4 Plus (bnrv1300) on Android 8.1.
 * Brightness via Settings.System (SELinux blocks direct sysfs writes from the app process).
 * Warmth via 'su -c echo' to sysfs color file (SELinux blocks direct ioctl from the app process).
 * see https://github.com/koreader/koreader/issues/14574
 */
class NookGL4plusController : LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 100
        private const val WARMTH_MAX = 10
        private const val MIN = 0
        private const val COLOR_FILE = "/sys/class/backlight/lm3630a_led/color"
    }

    private var currentWarmth: Int? = null

    override fun getPlatform(): String = "nook"
    override fun hasFallback(): Boolean = false
    override fun hasWarmth(): Boolean = true
    override fun needsPermission(): Boolean = false
    override fun hasStandaloneWarmth(): Boolean = false

    override fun enableFrontlightSwitch(activity: Activity): Int = 1

    override fun getBrightness(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return currentWarmth ?: MIN
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value out of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, brightness)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value out of range: $warmth")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth of $WARMTH_MAX")
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo $warmth > $COLOR_FILE"))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                currentWarmth = warmth
            } else {
                Log.w(TAG, "su failed (exit $exitCode)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "setWarmth: $e")
        }
    }

    override fun getMinBrightness(): Int = MIN
    override fun getMaxBrightness(): Int = BRIGHTNESS_MAX
    override fun getMinWarmth(): Int = MIN
    override fun getMaxWarmth(): Int = WARMTH_MAX
}
