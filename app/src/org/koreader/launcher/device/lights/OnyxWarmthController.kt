package org.koreader.launcher.device.lights

import java.io.File

import android.app.Activity
import android.provider.Settings

import org.koreader.launcher.interfaces.LightInterface
import org.koreader.launcher.utils.Logger

class OnyxWarmthController : LightInterface {
    companion object {
        private const val TAG = "lights"
        private const val BRIGHTNESS_MAX = 255
        private const val WARMTH_MAX = 255
        private const val MIN = 0
        private const val SETTINGS_WHITE = "screen_cold_brightness"
        private const val SETTINGS_WARM = "screen_warm_brightness"
        private const val WHITE_FILE = "/sys/class/backlight/white/brightness"
        private const val WARMTH_FILE = "/sys/class/backlight/warm/brightness"
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
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                SETTINGS_WHITE)
        } catch (e: Exception) {
            Logger.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        val colorFile = File(WARMTH_FILE)
        return try {
            // TODO: Set onyx value
            Settings.System.getInt(activity.applicationContext.contentResolver,
                SETTINGS_WARM)
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
            0
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Logger.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Logger.v(TAG, "Setting brightness to $brightness")
        val brightnessFile = File(WARMTH_FILE)
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                SETTINGS_WHITE, brightness)
            brightnessFile.writeText(brightness.toString())
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Logger.w(TAG, "warmth value of of range: $warmth")
            return
        }
        val warmthFile = File(WARMTH_FILE)
        // TODO: Add anroid settings!!
        Logger.v(TAG, "Setting warmth to $warmth")
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                SETTINGS_WARM, warmth)
            warmthFile.writeText(warmth.toString())
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
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
        return MIN
    }
}
