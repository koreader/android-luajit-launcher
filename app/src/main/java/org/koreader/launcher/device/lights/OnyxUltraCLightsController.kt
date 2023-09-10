package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import android.content.Context
import java.lang.Class.forName
import java.lang.reflect.Method
import android.content.Intent

class OnyxUltraCLightsController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 25
        private const val WARMTH_MAX = 24
        private const val MIN = 0
    }

    override fun getPlatform(): String {
        return "onyx-ultrac"
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
        return FrontLightUltraC.getCold(activity)
    }

    override fun getWarmth(activity: Activity): Int {
        return FrontLightUltraC.getWarm(activity)
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "Brightness value out of range: $brightness")
            return
        }

        val actualBrightness = getBrightness(activity).takeIf { it != -1 } ?: return

        val intent = when {
            brightness > actualBrightness -> Intent("onyx.action.BRIGHTNESS_UP")
            brightness < actualBrightness -> Intent("onyx.action.RIGHTNESS_DOWN")
            else -> {
                return
            }
        }

        val diff = kotlin.math.abs(brightness - actualBrightness)

        repeat(diff) {
            activity.sendBroadcast(intent)
        }

        Log.v(TAG, "Setting brightness to $brightness")
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }

        val actualBrightness = getWarmth(activity).takeIf { it != -1 } ?: return

        val intent = when {
            warmth > actualBrightness -> Intent("onyx.action.TEMPERATURE_UP")
            warmth < actualBrightness -> Intent("onyx.action.TEMPERATURE_DOWN")
            else -> {
                return
            }
        }

        val diff = kotlin.math.abs(warmth - actualBrightness)

        repeat(diff) {
            activity.sendBroadcast(intent)
        }

        Log.v(TAG, "Setting warmth to $warmth")
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

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }
}

object FrontLightUltraC {
    private const val TAG = "Lights"

    private val flController: Class<*>? = try {
        forName("android.onyx.hardware.DeviceController")
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val getCoolWarmBrightness: Method? = try {
        flController!!.getMethod("getBrightnessConfig", Context::class.java, Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private const val LIGHT_CONFIG_WARM: Int = 6
    private const val LIGHT_CONFIG_BRIGHTNESS: Int = 7

    fun getWarm(context: Context?): Int {
        return try {
            getCoolWarmBrightness!!.invoke(flController!!, context, LIGHT_CONFIG_WARM) as Int
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun getCold(context: Context?): Int {
        return try {
            getCoolWarmBrightness!!.invoke(flController!!, context, LIGHT_CONFIG_BRIGHTNESS) as Int
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
