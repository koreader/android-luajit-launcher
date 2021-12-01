package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import android.content.Context
import java.lang.Class.forName
import java.lang.reflect.Method

class OnyxSDMController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val WARMTH_MAX = 255
        private const val MIN = 0

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
            FrontLight.getCold(activity)
        } catch (e: Exception) {
            Log.w(TAG, Log.getStackTraceString(e))
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            FrontLight.getWarm(activity)
        } catch (e: Exception) {
            Log.w(TAG, Log.getStackTraceString(e))
            0
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        try {
            FrontLight.setCold(brightness, activity)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth")
        try {
            FrontLight.setWarm(warmth, activity)
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

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }
}

object FrontLight {

    private val fLController = forName("android.onyx.hardware.DeviceController")

    private val setWarmBrightness: Method = fLController.getMethod("setWarmLightDeviceValue", Context::class.java, Integer.TYPE)
    private val setColdBrightness: Method = fLController.getMethod("setColdLightDeviceValue", Context::class.java, Integer.TYPE)

    private val getCoolWarmBrightness: Method = fLController.getMethod("getBrightnessConfig", Context::class.java, Integer.TYPE)
    private const val BRIGHTNESS_CONFIG_WARM_IDX: Int = 2
    private const val BRIGHTNESS_CONFIG_COLD_IDX: Int = 3

    private const val TAG = "lights"

    fun getWarm(context: Context?): Int {
        return try {
            getCoolWarmBrightness.invoke(fLController, context, BRIGHTNESS_CONFIG_WARM_IDX) as Int
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            0
        }
    }

    fun getCold(context: Context?): Int {
        return try {
            getCoolWarmBrightness.invoke(fLController, context, BRIGHTNESS_CONFIG_COLD_IDX) as Int
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            0
        }
    }

    fun setWarm(value: Int, context: Context?) {
        try {
            setWarmBrightness.invoke(fLController, context, value)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

    fun setCold(value: Int, context: Context?) {
        try {
            setColdBrightness.invoke(fLController, context, value)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }
    }

}
