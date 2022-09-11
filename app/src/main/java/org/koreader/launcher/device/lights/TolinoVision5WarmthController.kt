package org.koreader.launcher.device.lights

import android.app.Activity
import android.provider.Settings
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.extensions.read
import org.koreader.launcher.extensions.write
import java.io.File

/* Special controller for Tolino Epos/Epos2.
 * see https://github.com/koreader/koreader/pull/6332
 *
 * Thanks to @zwim
 */

class TolinoVision5WarmthController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val WARMTH_MAX = 10
        private const val MIN = 0
        private const val NTX_IO_FILE = "/dev/ntx_io"
        private const val NTX_WARMTH_ID = 248
        private const val NTX_IO_LUA_FILE = "frontend/device/kobo/ntx_io.lua"
        private const val ACTUAL_BRIGHTNESS_FILE = "/sys/class/backlight/mxc_msp430_fl.0/actual_brightness" // always readable, same for Epos2 and Vision4
        private const val COLOR_FILE = "/sys/class/backlight/lm3630a_led/color"
        private var currentWarmth: Int? = null
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
            Log.w(TAG, "$e")
            -1
        }

        // change the brightness through android. Be aware one step in Android is less than one step in the file
        if (startBrightness > BRIGHTNESS_MAX/2)
            setBrightness(activity, startBrightness - (BRIGHTNESS_MAX/100+1))
        else
            setBrightness(activity, startBrightness + (BRIGHTNESS_MAX/100+1))

        // we have to wait until the android changes seep through to the file,
        // 50ms is to less, 60ms seems to work, so use 80 to have some safety
        Thread.sleep(80)

        val actualBrightnessFromFile = try {
            actualBrightnessFile.readText().trim().toInt()
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            -1
        }

        setBrightness(activity, startBrightness)

        if (startBrightnessFromFile == actualBrightnessFromFile) {
            return try { // try to send keyevent to system to turn on frontlight, needs extended permissions
                Runtime.getRuntime().exec("su -c input keyevent KEYCODE_BUTTON_A && echo OK")
                1
            } catch (e: Exception) {
                e.printStackTrace()
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
            Log.w(TAG, e.toString())
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        if (currentWarmth == null) {
            currentWarmth = WARMTH_MAX - File(COLOR_FILE).read()
        }
        return currentWarmth ?: WARMTH_MAX
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        try {
            Settings.System.putInt(activity.applicationContext.contentResolver,
                "screen_brightness", brightness)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }

        Log.v(TAG, "Setting warmth to $warmth of $WARMTH_MAX")

        try {
            val ntxFile = File(NTX_IO_FILE)
            // check that the ntx_io file exists
            if (ntxFile.canWrite()) {
                // invert, because 0 is the "warmest"(red) and 10 is the "coldest"(blue)
                // this makes the bar for "warmth" most left blue while the right most is red
                val invertedWarmth = (warmth - WARMTH_MAX) * -1
                val luajitBinFile = File("./luajit")
                // check and fix the "luajit" binary file
                if (!luajitBinFile.canExecute()) {
                    val chmodProcess = Runtime.getRuntime().exec("chmod 775 $luajitBinFile")
                    val chmodProcessExitStatus = chmodProcess.waitFor()
                    Log.v(TAG, "\"chmod 775 $luajitBinFile\" exit status $chmodProcessExitStatus")
                }

                // execute ntx_io.lua script from kobo
                val luaBinProcess = Runtime.getRuntime().exec("$luajitBinFile $NTX_IO_LUA_FILE $NTX_WARMTH_ID $invertedWarmth")
                val luaBinProcessExitStatus = luaBinProcess.waitFor()
                Log.v(TAG, "\"$luajitBinFile $ntxFile $NTX_WARMTH_ID $invertedWarmth\" exit status $luaBinProcessExitStatus")
                currentWarmth = warmth
                return
            } else {
                Log.w(TAG, "\"/dev/ntx_io\" is not writeable")
            }
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }

        // fallback to the original "su" way

        val colorFile = File(COLOR_FILE)

        try {
            if (!colorFile.canWrite()) {
                Runtime.getRuntime().exec("su -c chmod 666 $COLOR_FILE")
            }
            colorFile.write(WARMTH_MAX - warmth)
            currentWarmth = warmth
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
}
