package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.extensions.read
import org.koreader.launcher.extensions.write
import java.io.File

/* Controller for Onyx Boox Palma 2 Pro with brightness and warmth support.
 * Uses onyx_bl_br for brightness and onyx_bl_ct for warmth (color temperature).
 * Dynamically reads max values from sysfs.
 */

class OnyxPalma2ProController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val MIN = 0
        private const val DEFAULT_BRIGHTNESS_MAX = 255
        private const val DEFAULT_WARMTH_MAX = 32

        private const val BRIGHTNESS_FILE = "/sys/class/backlight/onyx_bl_br/brightness"
        private const val BRIGHTNESS_MAX_FILE = "/sys/class/backlight/onyx_bl_br/max_brightness"
        private const val WARMTH_FILE = "/sys/class/backlight/onyx_bl_ct/brightness"
        private const val WARMTH_MAX_FILE = "/sys/class/backlight/onyx_bl_ct/max_brightness"
    }

    private val cachedBrightnessMax: Int by lazy {
        try {
            val value = File(BRIGHTNESS_MAX_FILE).read()
            if (value > 0) value else DEFAULT_BRIGHTNESS_MAX
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read max brightness, using default: $e")
            DEFAULT_BRIGHTNESS_MAX
        }
    }

    private val cachedWarmthMax: Int by lazy {
        try {
            val value = File(WARMTH_MAX_FILE).read()
            if (value > 0) value else DEFAULT_WARMTH_MAX
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read max warmth, using default: $e")
            DEFAULT_WARMTH_MAX
        }
    }

    override fun getPlatform(): String {
        return "onyx-palma2pro"
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
            File(BRIGHTNESS_FILE).read()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read brightness: $e")
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            File(WARMTH_FILE).read()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read warmth: $e")
            0
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness !in MIN..cachedBrightnessMax) {
            Log.w(TAG, "brightness value out of range: $brightness (max: $cachedBrightnessMax)")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        try {
            File(BRIGHTNESS_FILE).write(brightness)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness: $e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth !in MIN..cachedWarmthMax) {
            Log.w(TAG, "warmth value out of range: $warmth (max: $cachedWarmthMax)")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth")
        try {
            File(WARMTH_FILE).write(warmth)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set warmth: $e")
        }
    }

    override fun getMinWarmth(): Int {
        return MIN
    }

    override fun getMaxWarmth(): Int {
        return cachedWarmthMax
    }

    override fun getMinBrightness(): Int {
        return MIN
    }

    override fun getMaxBrightness(): Int {
        return cachedBrightnessMax
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun hasStandaloneWarmth(): Boolean {
        return true
    }
}
