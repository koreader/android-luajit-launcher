package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.extensions.read
import org.koreader.launcher.extensions.write
import java.io.File

/* Controller for some Onyx Color devices.
 * Tested on a Onyx Nova3 Color.
 *
 * Thanks to @ilyats
 */

class OnyxColorController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val BRIGHTNESS_MIN = 0
        private const val BRIGHTNESS_FILE = "/sys/class/backlight/pwm-backlight.0/brightness"
        private const val ACTUAL_BRIGHTNESS_FILE = "/sys/class/backlight/pwm-backlight.0/actual_brightness"
    }

    override fun getPlatform(): String {
        return "onyx-color"
    }

    override fun hasFallback(): Boolean {
        return false
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
        return File(ACTUAL_BRIGHTNESS_FILE).read()
    }

    override fun getWarmth(activity: Activity): Int {
        Log.w(TAG, "getWarmth: not implemented")
        return 0
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < BRIGHTNESS_MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        File(BRIGHTNESS_FILE).write(brightness)
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
}
