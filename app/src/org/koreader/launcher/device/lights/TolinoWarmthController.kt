package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings

import org.koreader.launcher.interfaces.LightInterface
import org.koreader.launcher.utils.Logger

class TolinoWarmthController : LightInterface {
    companion object {
        private const val TAG = "lights"
        private const val BRIGHTNESS_MAX = 100
        private const val WARMTH_MAX = 10
        private const val MIN = 0
    }

    override fun hasWarmth(): Boolean {
        return true
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
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                "screen_brightness_color")
        } catch (e: Exception) {
            Logger.w(TAG, e.toString())
            0
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) return
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                "screen_brightness", brightness)
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) return
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                "screen_brightness_color", warmth)
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
