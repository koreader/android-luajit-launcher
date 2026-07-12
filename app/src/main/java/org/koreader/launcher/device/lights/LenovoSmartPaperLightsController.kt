package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import kotlin.math.roundToInt

/* Lights Controller for Lenovo SmartPaper.
 *
 * Brightness and color temperature are encoded into a single float
 * and written to Settings.System "screen_brightness_float", using the
 * same encoding as Lenovo's system QuickSettingManager:
 *
 *   float value = (brightness / 100.0f) + (colorTemp / 10000.0f)
 *
 * Brightness raw range: 0–99
 * Color temperature raw range: 0–24 (via PowerManager.BRIGHTNESS_COLORTEMP_MAX)
 *
 * Requires WRITE_SETTINGS permission (special permission, must be granted
 * manually via Settings.ACTION_MANAGE_WRITE_SETTINGS).
 */

class LenovoSmartPaperLightsController : LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_RAW_MAX = 99
        private const val DEFAULT_COLORTEMP_MAX = 24
        private const val MIN = 0

        // The UI exposes 0–24 steps for both brightness and warmth.
        // This matches the color temp range and provides fine-grained
        // brightness control within the 0–99 raw range.
        private const val UI_MAX = 24
    }

    private var colorTempMax: Int = DEFAULT_COLORTEMP_MAX

    init {
        colorTempMax = getMaxColorTemp()
        Log.i(TAG, "LenovoSmartPaper lights: brightness=0..$BRIGHTNESS_RAW_MAX, " +
            "warmth=0..$colorTempMax, UI steps=0..$UI_MAX")
    }

    override fun getPlatform(): String {
        return "lenovo"
    }

    override fun hasFallback(): Boolean {
        return false
    }

    override fun hasWarmth(): Boolean {
        return true
    }

    override fun needsPermission(): Boolean {
        return true
    }

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    override fun getBrightness(activity: Activity): Int {
        return try {
            val brightnessFloat = Settings.System.getFloat(
                activity.contentResolver, "screen_brightness_float", 0f)
            // Extract brightness raw value (0–99). The "rounding" logic purposefully follows original SystemUI.
            val raw = (brightnessFloat * 10000f).roundToInt() / 100
            raw.coerceIn(MIN, BRIGHTNESS_RAW_MAX)
        } catch (e: Exception) {
            Log.w(TAG, "getBrightness failed: ${e.message}")
            MIN
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            val brightnessFloat = Settings.System.getFloat(
                activity.contentResolver, "screen_brightness_float", 0f)
            // Extract color temperature raw value (0–24)
            val raw = (brightnessFloat * 10000f).roundToInt() % 100
            raw.coerceIn(MIN, colorTempMax)
        } catch (e: Exception) {
            Log.w(TAG, "getWarmth failed: ${e.message}")
            MIN
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    override fun setBrightness(activity: Activity, brightness: Int) {
        val clamped = brightness.coerceIn(MIN, BRIGHTNESS_RAW_MAX)
        val warmth = getWarmth(activity)
        writeCombinedFloat(activity, clamped, warmth)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        val clamped = warmth.coerceIn(MIN, colorTempMax)
        val brightness = getBrightness(activity)
        writeCombinedFloat(activity, brightness, clamped)
    }

    private fun writeCombinedFloat(activity: Activity, brightness: Int, warmth: Int) {
        try {
            val value = (brightness / 100.0f) + (warmth / 10000.0f)
            val rounded = (value * 10000f).roundToInt() / 10000f
            Settings.System.putFloat(
                activity.contentResolver, "screen_brightness_float", rounded)
            Log.d(TAG, "write brightness=$brightness warmth=$warmth → float=$rounded")
        } catch (e: SecurityException) {
            Log.e(TAG, "WRITE_SETTINGS permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "setBrightness failed: ${e.message}")
        }
    }

    // ── Range ─────────────────────────────────────────────────────────────────

    override fun getMinBrightness(): Int = MIN
    override fun getMinWarmth(): Int = MIN
    override fun getMaxBrightness(): Int = BRIGHTNESS_RAW_MAX
    override fun getMaxWarmth(): Int = colorTempMax

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getMaxColorTemp(): Int {
        return try {
            android.os.PowerManager::class.java
                .getField("BRIGHTNESS_COLORTEMP_MAX")
                .get(null) as Int
        } catch (e: Exception) {
            Log.w(TAG, "Could not read BRIGHTNESS_COLORTEMP_MAX, using default $DEFAULT_COLORTEMP_MAX", e)
            DEFAULT_COLORTEMP_MAX
        }
    }
}
