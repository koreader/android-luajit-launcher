package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
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
        val brightnessFile = File(WHITE_FILE)
        return try {
            // .replace("\n", "") is needed, since it's automatically appended
            // without it exception is thrown
            // java.lang.NumberFormatException: For input string: "125\n"
            return brightnessFile.readText().replace("\n", "").toInt()
        } catch (e: Exception) {
            Log.w(TAG, Log.getStackTraceString(e))
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        val warmthFile = File(WARMTH_FILE)
        return try {
            // .replace("\n", "") is needed, since it's automatically appended
            // without it exception is thrown
            // java.lang.NumberFormatException: For input string: "125\n"
            return warmthFile.readText().replace("\n", "").toInt()
        } catch (e: Exception) {
            Log.w(TAG, Log.getStackTraceString(e))
            0
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        val brightnessFile = File(WHITE_FILE)
        try {
            brightnessFile.writeText(brightness.toString())
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        val warmthFile = File(WARMTH_FILE)
        Log.v(TAG, "Setting warmth to $warmth")
        try {
            warmthFile.writeText(warmth.toString())
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

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }
}
