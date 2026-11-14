package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.LightsInterface

/* Lights controller for the Meebook M103.
 *
 * Frontlight has two independent channels:
 *  - cold (brightness): Settings.System "screen_brightness" (standard Android
 *    brightness, range 0..255) — works;
 *  - warm: vendor-specific Settings.System "haoqing_warm_light" (0..254) —
 *    NOT controllable by a non-privileged app: the key isn't in
 *    Settings.System.PUBLIC_SETTINGS, so the ROM's SettingsProvider rejects
 *    writes from a targetSdk 30 app, and the warm LED's sysfs node
 *    (/sys/class/backlight/backlight_warm) is root-only. Warmth is therefore
 *    reported as unsupported (hasWarmth() == false).
 *
 * Writing the brightness channel requires the WRITE_SETTINGS permission, so
 * needsPermission() returns true.
 */

class MeebookM103LightsController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val BRIGHTNESS_MIN = 0
        private const val WARMTH_MAX = 254
        private const val WARMTH_MIN = 0
        private const val WARMTH_KEY = "haoqing_warm_light"
    }

    override fun getPlatform(): String {
        return "meebook-m103"
    }

    override fun hasFallback(): Boolean {
        return false
    }

    override fun hasWarmth(): Boolean {
        return false
    }

    override fun needsPermission(): Boolean {
        return true
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun getBrightness(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver, WARMTH_KEY)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            WARMTH_MIN
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < BRIGHTNESS_MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value out of range: $brightness")
            return
        }

        Log.v(TAG, "Setting brightness to $brightness")
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, brightness)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < WARMTH_MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value out of range: $warmth")
            return
        }

        Log.v(TAG, "Setting warmth to $warmth")
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver, WARMTH_KEY, warmth)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun getMinWarmth(): Int {
        return WARMTH_MIN
    }

    override fun getMaxWarmth(): Int {
        return WARMTH_MAX
    }

    override fun getMinBrightness(): Int {
        return BRIGHTNESS_MIN
    }

    override fun getMaxBrightness(): Int {
        return BRIGHTNESS_MAX
    }

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }
}
