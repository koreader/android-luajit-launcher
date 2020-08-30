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

    override fun getFrontlightSwitchState(activity: Activity): Int {
        // ATTENTION: getBrightness, setBrightness use the Android range 0..255
        // in the brightness files the used range is 0..100

        // As we don`t know, if the frontlightSwitch was pressed just before this method
        // was called, we have to wait until the android changes seep through to the file,
        // 50ms is to less, 60ms seems to work, so use 80 to have some safety
        Thread.sleep(80)

        val startBrightness = getBrightness(activity)

        val actualBrightnessFile = File(ACTUAL_BRIGHTNESS_FILE)
        val startBrightnessFromFile = try {
            actualBrightnessFile.readText().trim().toInt()
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
            -1
        }

        // change the brightness through android. Be aware one step in Android is less than one step in the file
        var actualBrightness = startBrightness
        if (startBrightness > BRIGHTNESS_MAX/2)
            actualBrightness -= (BRIGHTNESS_MAX/100+1).toInt()
        else
            actualBrightness +=  (BRIGHTNESS_MAX/100+1).toInt()
        setBrightness(activity, actualBrightness)

        // we have to wait until the android changes seep through to the file,
        // 50ms is to less, 60ms seems to work, so use 80 to have some safety
        Thread.sleep(80)

        val actualBrightnessFromFile = try {
            actualBrightnessFile.readText().trim().toInt()
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
        }

        setBrightness(activity, startBrightness)

        if (actualBrightnessFromFile == startBrightnessFromFile) {
            Logger.w(TAG, "frontlight Switch off")
            return 0 // switch is off
        }
        else {
            Logger.w(TAG, "frontlight Switch on")
            return 1 // switch is on
        }
}

    // try to toggle on frontlight switch on Tolinos, returns the former switch state
    override fun enableFrontlightSwitch(activity: Activity): Int {

        if (getFrontlightSwitchState(activity) == 0) {
            try { // try to send keyevent to system to turn on frontlight, needs extended permissions
                Runtime.getRuntime().exec("su -c input keyevent KEYCODE_BUTTON_A");
            } catch (e: Exception) {
                Logger.w("Exception in enableFrontlightSwitch", e.toString());
            }
            return 0 // former switch state
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
        if (warmth < MIN || warmth > WARMTH_MAX) {
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
