package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.interfaces.LightInterface
import java.io.File

class OnyxC67Controller : LightInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val BRIGHTNESS_MIN = 0
        private const val BRIGHTNESS_FILE = "/sys/class/backlight/rk28_bl/brightness"
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
        val brightnessFile = File(BRIGHTNESS_FILE)
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
        Log.w(TAG, "getWarmth: not implemented")
        return 0
    }


    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < BRIGHTNESS_MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        val brightnessFile = File(BRIGHTNESS_FILE)
        try {
            brightnessFile.writeText(brightness.toString())
        } catch (e: Exception) {
            Log.w(TAG, "$e")
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
}
