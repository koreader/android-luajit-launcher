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
        private const val ACTUAL_BRIGHTNESS_FILE = "/sys/class/backlight/mxc_msp430_fl.0/actual_brightness" // always readable, same for Epos2 and Vision4
        private const val COLOR_FILE_EPOS2 = "/sys/class/backlight/tlc5947_bl/color"
        private const val COLOR_FILE_VISION4HD = "/sys/class/backlight/lm3630a_led/color"
        private val COLOR_FILE = if (File(COLOR_FILE_VISION4HD).exists())
            COLOR_FILE_VISION4HD
        else
            COLOR_FILE_EPOS2
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

    // try to toggle on frontlight switch on Tolinos, returns the former switch state
    override fun enableFrontlightSwitch(activity: Activity): Int {
        // ATTENTION: getBrightness, setBrightness use the Android range 0..255
        // in the brightness files the used range is 0..100
        val startBrightness = getBrightness(activity)

        val actualBrightnessFile = File(ACTUAL_BRIGHTNESS_FILE)
        val startBrightnessFromFile = try {
            actualBrightnessFile.readText().trim().toInt()
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
            -1
        }

        // change the brightness through android. Be aware one step in Android is less than one step in the file
        if (startBrightness > BRIGHTNESS_MAX/2)
            setBrightness(activity, startBrightness - (BRIGHTNESS_MAX/100+1).toInt())
        else
            setBrightness(activity, startBrightness + (BRIGHTNESS_MAX/100+1).toInt())

        // we have to wait until the android changes seep through to the file,
        // 50ms is to less, 60ms seems to work, so use 80 to have some safety
        Thread.sleep(80)

        val actualBrightnessFromFile = try {
            actualBrightnessFile.readText().trim().toInt()
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
            -1
        }

        setBrightness(activity, startBrightness)

        if (startBrightnessFromFile == actualBrightnessFromFile) {
            return try { // try to send keyevent to system to turn on frontlight, needs extended permissions
                Runtime.getRuntime().exec("su -c input keyevent KEYCODE_BUTTON_A && echo OK")
                1
            } catch (e: Exception) {
                Logger.w("Exception in enableFrontlightSwitch", e.toString());
                0
            }
        }

        return 1
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
            if (!colorFile.canWrite()) {
                Runtime.getRuntime().exec("su -c chmod 666 $COLOR_FILE")
            }
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
