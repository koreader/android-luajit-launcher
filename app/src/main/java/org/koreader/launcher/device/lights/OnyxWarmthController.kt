package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.extensions.read
import org.koreader.launcher.extensions.write
import java.io.File

class OnyxWarmthController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val WARMTH_MAX = 255
        private const val MIN = 0
        private const val WHITE_FILE = "/sys/class/backlight/white/brightness"
        private const val WARMTH_FILE = "/sys/class/backlight/warm/brightness"
    }

    override fun getPlatform(): String {
        return "onyx-warmth"
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

    override fun getBrightness(activity: Activity): Int {
        return File(WHITE_FILE).read()
    }

    override fun getWarmth(activity: Activity): Int {
        return File(WARMTH_FILE).read()
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        File(WHITE_FILE).write(brightness)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }

        Log.v(TAG, "Setting warmth to $warmth")
        File(WARMTH_FILE).write(warmth)
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

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }
}
