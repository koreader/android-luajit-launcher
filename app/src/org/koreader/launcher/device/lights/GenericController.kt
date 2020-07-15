package org.koreader.launcher.device.lights

import android.app.Activity

import org.koreader.launcher.interfaces.LightInterface
import org.koreader.launcher.utils.Logger

class GenericController : LightInterface {
    companion object {
        private const val TAG = "lights"
        private const val BRIGHTNESS_MAX = 255
        private const val BRIGHTNESS_MIN = 0
    }

    override fun hasWarmth(): Boolean {
        return false
    }

    override fun getBrightness(activity: Activity): Int {
        return (activity.window.attributes.screenBrightness * 255 / 1.0f).toInt()
    }

    override fun getWarmth(activity: Activity): Int {
        Logger.w(TAG, "getWarmth: not implemented")
        return 0
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < BRIGHTNESS_MIN || brightness > BRIGHTNESS_MAX) return
        val level = brightness * 1.0f / BRIGHTNESS_MAX
        activity.runOnUiThread {
            try {
                val params = activity.window.attributes
                params.screenBrightness = level
                activity.window.attributes = params
            } catch (e: Exception) {
                Logger.w(TAG, e.toString())
            }
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        Logger.w(TAG, "ignoring setWarmth: not implemented")
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
