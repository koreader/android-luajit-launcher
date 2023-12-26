package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import android.content.Context
import java.lang.Class.forName
import java.lang.reflect.Method

class OnyxSdkLightsController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 255
        private const val WARMTH_MAX = 255
        private const val MIN = 0
    }

    override fun getPlatform(): String {
        return "onyx-sdk-lights"
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
        return FrontLight.getCold(activity)
    }

    override fun getWarmth(activity: Activity): Int {
        return FrontLight.getWarm(activity)
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        FrontLight.setCold(brightness, activity)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth")
        FrontLight.setWarm(warmth, activity)
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

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }
}

object FrontLight {
    private const val TAG = "lights"

    private val flController: Class<*>? = try {
        forName("android.onyx.hardware.DeviceController")
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val setWarmBrightness: Method? = try {
        flController!!.getMethod("setWarmLightDeviceValue", Context::class.java, Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }
    private val setColdBrightness: Method? = try {
        flController!!.getMethod("setColdLightDeviceValue", Context::class.java, Integer.TYPE)
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
    private const val BRIGHTNESS_CONFIG_WARM_IDX: Int = 2
    private const val BRIGHTNESS_CONFIG_COLD_IDX: Int = 3

    fun getWarm(context: Context?): Int {
        return try {
            getCoolWarmBrightness!!.invoke(flController!!, context, BRIGHTNESS_CONFIG_WARM_IDX) as Int
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun getCold(context: Context?): Int {
        return try {
            getCoolWarmBrightness!!.invoke(flController!!, context, BRIGHTNESS_CONFIG_COLD_IDX) as Int
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun setWarm(value: Int, context: Context?) {
        try {
            setWarmBrightness!!.invoke(flController!!, context, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setCold(value: Int, context: Context?) {
        try {
            setColdBrightness!!.invoke(flController!!, context, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
