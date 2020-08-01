package org.koreader.launcher.device.lights

import java.io.File

import android.app.Activity
import android.provider.Settings

import org.koreader.launcher.interfaces.LightInterface
import org.koreader.launcher.utils.Logger

/* Special controller for Tolino Epos/Epos2.
 * see https://github.com/koreader/koreader/pull/6332
 *
 * Thanks to @zwim
 */

class TolinoWarmthController : LightInterface {
    companion object {
        private const val TAG = "lights"
        private const val BRIGHTNESS_MAX = 255
        private const val WARMTH_MAX = 10
        private const val MIN = 0
        private const val COLOR_FILE = "/sys/class/backlight/tlc5947_bl/color"
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
                "screen_brightness")
        } catch (e: Exception) {
            Logger.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        val colorFile = File(COLOR_FILE)
        return try {
            WARMTH_MAX - colorFile.readText().toInt()
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
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                "screen_brightness", brightness)
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > BRIGHTNESS_MAX) {
            Logger.w(TAG, "warmth value of of range: $warmth")
            return
        }
        val colorFile = File(COLOR_FILE)
        Logger.v(TAG, "Setting warmth to $warmth")
        try {
            colorFile.setLastModified(System.currentTimeMillis())
        } catch (e: Exception) {
            Runtime.getRuntime().exec("su -c chmod 666 $COLOR_FILE")
        }

        try {
            colorFile.writeText((WARMTH_MAX - warmth).toString())
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
}
