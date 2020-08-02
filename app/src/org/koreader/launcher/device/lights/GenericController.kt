package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings

import org.koreader.launcher.interfaces.LightInterface
import org.koreader.launcher.utils.Logger

/* handle frontlight within the activity, without affecting other activities */

class GenericController : LightInterface {
    companion object {
        private const val TAG = "lights"
        private const val BRIGHTNESS_MAX = 255
        private const val BRIGHTNESS_MIN = 0
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

    override fun getBrightness(activity: Activity): Int {
        val brightness = (activity.window.attributes.screenBrightness * 255 / 1.0f).toInt()
        return if (brightness < 0) {
             try {
                Settings.System.getInt(activity.applicationContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS)
            } catch (e: Exception) {
                Logger.w(TAG, e.toString())
                0
            }
        } else brightness
    }

    override fun getWarmth(activity: Activity): Int {
        Logger.w(TAG, "getWarmth: not implemented")
        return 0
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        Logger.v(TAG, "Setting brightness to $brightness")
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
