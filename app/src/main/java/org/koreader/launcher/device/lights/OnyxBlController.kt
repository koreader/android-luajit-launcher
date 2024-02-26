package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.extensions.read
import org.koreader.launcher.extensions.write
import java.io.File

class OnyxBlController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val MIN = 0
        private const val WHITE_FILE = "/sys/class/backlight/onyx_bl_br/brightness"
        private const val WARMTH_FILE = "/sys/class/backlight/onyx_bl_ct/brightness"
        private const val MAX_WHITE_FILE = "/sys/class/backlight/onyx_bl_br/max_brightness"
        private const val MAX_WARMTH_FILE = "/sys/class/backlight/onyx_bl_ct/max_brightness"
    }

    override fun getPlatform(): String {
        return "onyx-bl"
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
        if (brightness < MIN || brightness > getMaxBrightness()) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        File(WHITE_FILE).write(brightness)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > getMaxWarmth()) {
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
        return File(MAX_WARMTH_FILE).read()
    }

    override fun getMinBrightness(): Int {
        return MIN
    }

    override fun getMaxBrightness(): Int {
        return File(MAX_WHITE_FILE).read()
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun hasStandaloneWarmth(): Boolean {
        return true
    }
}
