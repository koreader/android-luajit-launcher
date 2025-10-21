package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.os.Looper
import android.os.Handler
import org.koreader.launcher.device.Ioctl
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.device.DeviceInfo

// Light and warmth controller for B300 Tolino devices (Epos 3, Vision 6, Shine 4)
class TolinoB300Controller : Ioctl(), LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 100
        private const val WARMTH_MAX = 10
        private const val MIN = 0
        private const val SCREEN_BRIGHTNESS = "screen_brightness"
        private const val SCREEN_BRIGHTNESS_COLOR = "screen_brightness_color"
    }

    private fun needsInvertedWarmth(): Boolean {
        return DeviceInfo.ID == DeviceInfo.Id.TOLINO_VISION6 ||
               DeviceInfo.ID == DeviceInfo.Id.TOLINO_SHINE4
    }

    private fun showToastOnUiThread(activity: Activity, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(activity.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun ensureWriteSettingsPermission(activity: Activity): Boolean {
        if (!Settings.System.canWrite(activity.applicationContext)) {
            showToastOnUiThread(
                activity,
                "Please enable 'Modify system settings' for KOReader in Android settings."
            )
            Log.w(TAG, "WRITE_SETTINGS permission not granted.")
            return false
        }
        return true
    }

    override fun getPlatform(): String {
        return "tolino"
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

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun getBrightness(activity: Activity): Int {
        return try {
            Settings.System.getInt(activity.applicationContext.contentResolver, SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            val raw = Settings.System.getInt(
                activity.applicationContext.contentResolver,
                SCREEN_BRIGHTNESS_COLOR
            )
            if (needsInvertedWarmth()) WARMTH_MAX - raw else raw
        } catch (e: Exception) {
            Log.w(TAG, e.toString())
            0
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (!ensureWriteSettingsPermission(activity)) return
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        try {
            Settings.System.putInt(
                    activity.applicationContext.contentResolver,
                    SCREEN_BRIGHTNESS,
                    brightness
            )
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (!ensureWriteSettingsPermission(activity)) return
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        val warmthToSet = if (needsInvertedWarmth()) WARMTH_MAX - warmth else warmth
        Log.v(TAG, "Setting warmth to $warmth (actual: $warmthToSet)")
        try {
            Settings.System.putInt(
                activity.applicationContext.contentResolver,
                SCREEN_BRIGHTNESS_COLOR,
                warmthToSet
            )
            // workaround, toggle brightness to force warmth refresh
            val currentBrightness: Int = getBrightness(activity)
            Settings.System.putInt(
                activity.applicationContext.contentResolver,
                SCREEN_BRIGHTNESS,
                currentBrightness + 1
            )
            Settings.System.putInt(
                activity.applicationContext.contentResolver,
                SCREEN_BRIGHTNESS,
                currentBrightness
            )
        } catch (e: Exception) {
            Log.w(TAG, "$e")
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

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }
}
