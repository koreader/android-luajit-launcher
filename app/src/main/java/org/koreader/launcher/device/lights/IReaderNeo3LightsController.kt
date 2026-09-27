package org.koreader.launcher.device.lights

import android.app.Activity
import android.net.Uri
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import java.io.File

/**
 * iReader Neo 3 Ultra (RM06L) frontlight.
 *
 * Two world-writable sysfs channels on the LM3630A: A = warm, B = cool.
 * The system toggles the light through SystemUI's reading-light provider.
 *
 * Warmth is exposed in the same [0, 100] range as brightness (100 == MAX == 255),
 * so the KOReader UI shows both sliders on an identical scale.
 */
class IReaderNeo3LightsController : LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val WARM_PATH = "/sys/devices/platform/11009000.i2c/i2c-2/2-0036/a_brightness"
        private const val COOL_PATH = "/sys/devices/platform/11009000.i2c/i2c-2/2-0036/b_brightness"
        private const val MAX = 255          // hardware max per channel
        private const val WARMTH_MAX = 100   // warmth API range [0, 100]; 100 == MAX
        private const val PROVIDER = "content://com.szzy.ireader.systemui.provider"
    }

    // Remember the last warmth we applied. When the light is off both HW
    // channels read 0 and we can no longer derive the mix from them, so we
    // fall back to this cached value when restoring brightness.
    private var lastWarmth = WARMTH_MAX / 2

    override fun getPlatform(): String = "ireader"
    override fun hasFallback(): Boolean = false
    override fun hasWarmth(): Boolean = true
    override fun needsPermission(): Boolean = false
    override fun hasStandaloneWarmth(): Boolean = false

    override fun getMinBrightness(): Int = 0
    override fun getMaxBrightness(): Int = MAX
    override fun getMinWarmth(): Int = 0
    override fun getMaxWarmth(): Int = WARMTH_MAX

    override fun enableFrontlightSwitch(activity: Activity): Int = 1

    override fun getBrightness(activity: Activity): Int =
        maxOf(read(WARM_PATH), read(COOL_PATH))

    override fun getWarmth(activity: Activity): Int {
        val warm = read(WARM_PATH)
        val cool = read(COOL_PATH)
        if (warm + cool == 0) return lastWarmth
        return warm * WARMTH_MAX / (warm + cool)
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        applyChannels(activity, brightness.coerceIn(0, MAX), getWarmth(activity))
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        lastWarmth = warmth.coerceIn(0, WARMTH_MAX)
        applyChannels(activity, getBrightness(activity), lastWarmth)
    }

    private fun applyChannels(activity: Activity, brightness: Int, warmth: Int) {
        val b = brightness.coerceIn(0, MAX)
        val w = warmth.coerceIn(0, WARMTH_MAX)
        if (b <= 0) {
            // Switch the system to its "off" preset before zeroing the channels,
            // so the SystemUI provider doesn't re-apply its own level afterwards.
            notifySystem(activity, false)
            write(WARM_PATH, 0)
            write(COOL_PATH, 0)
            return
        }
        // Peak normalization: the dominant channel reaches the requested
        // brightness, so 100% is not dimmed at 50% warmth. The two LM3630A
        // channels are independent, so total output can reach 2 * MAX.
        val warmW = w * MAX / WARMTH_MAX
        val coolW = MAX - warmW
        val peak = maxOf(coolW, warmW, 1)
        // Ask the system to switch to its "custom" preset first, then write our
        // own levels on top, so the provider doesn't overwrite them.
        notifySystem(activity, true)
        write(WARM_PATH, b * warmW / peak)
        write(COOL_PATH, b * coolW / peak)
    }

    private fun notifySystem(activity: Activity, on: Boolean) {
        try {
            val method = if (on) "call_reading_light_custom" else "call_reading_light_off"
            activity.contentResolver.call(Uri.parse(PROVIDER), method, null, null)
        } catch (t: Throwable) {
            Log.w(TAG, "reading-light provider call failed", t)
        }
    }

    private fun read(path: String): Int = try {
        File(path).readText().trim().toIntOrNull() ?: 0
    } catch (t: Throwable) {
        0
    }

    private fun write(path: String, value: Int) {
        try {
            File(path).writeText("$value\n")
        } catch (t: Throwable) {
            Log.w(TAG, "failed to write $path", t)
        }
    }
}
