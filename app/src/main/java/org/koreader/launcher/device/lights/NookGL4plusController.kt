package org.koreader.launcher.device.lights

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.LightsInterface

/* Controller for Nook Glowlight 4 Plus (bnrv1300) on Android 8.1.
 * Brightness via Settings.System. Requires "Modify system settings" special app permission;
 *   grant with: adb shell appops set org.koreader.launcher WRITE_SETTINGS allow
 * Warmth via com.nook.partner GlowLightService (exported, no permission required, no root).
 *   Sends action_set_color_temperature (0-100 scale); the service rescales to 0-10 for
 *   the lm3630a_led hardware and calls PowerManager.setFrontlightBrightnessColor() using
 *   its own DEVICE_POWER privilege.
 * see https://github.com/koreader/koreader/issues/14574
 */
class NookGL4plusController : LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 100
        private const val WARMTH_MAX = 10
        private const val MIN = 0
        private const val GLOWLIGHT_PACKAGE = "com.nook.partner"
        private const val GLOWLIGHT_SERVICE = "com.nook.partner.service.GlowLightService"
        private const val ACTION_SET_COLOR_TEMPERATURE = "action_set_color_temperature"
        private const val EXTRA_COLOR_TEMPERATURE = "extra_color_temperature"
    }

    @Volatile private var currentWarmth: Int = MIN

    override fun getPlatform(): String = "nook"
    override fun hasFallback(): Boolean = false
    override fun hasWarmth(): Boolean = true
    override fun needsPermission(): Boolean = false
    override fun hasStandaloneWarmth(): Boolean = false
    override fun enableFrontlightSwitch(activity: Activity): Int = 1

    override fun getBrightness(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            MIN
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver,
                "screen_brightness_color")
        } catch (e: Exception) {
            currentWarmth
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
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
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value out of range: $warmth")
            return
        }
        if (warmth == getWarmth(activity)) return
        Log.v(TAG, "Setting warmth to $warmth of $WARMTH_MAX")
        setWarmthViaService(activity, warmth)
    }

    private fun setWarmthViaService(activity: Activity, warmth: Int): Boolean {
        return try {
            val intent = Intent(ACTION_SET_COLOR_TEMPERATURE).apply {
                component = ComponentName(GLOWLIGHT_PACKAGE, GLOWLIGHT_SERVICE)
                putExtra(EXTRA_COLOR_TEMPERATURE, warmth * 10)
            }
            val result = activity.startService(intent)
            if (result != null) {
                currentWarmth = warmth
                true
            } else {
                Log.w(TAG, "GlowLightService unavailable (com.nook.partner disabled?)")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "setWarmth via service failed: $e")
            false
        }
    }

    override fun getMinBrightness(): Int = MIN
    override fun getMaxBrightness(): Int = BRIGHTNESS_MAX
    override fun getMinWarmth(): Int = MIN
    override fun getMaxWarmth(): Int = WARMTH_MAX
}
