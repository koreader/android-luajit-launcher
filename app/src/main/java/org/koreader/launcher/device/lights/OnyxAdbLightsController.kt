package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import android.content.Context
import java.lang.Class.forName
import java.lang.reflect.Method

class OnyxAdbLightsController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val MIN = 0
    }

    override fun getPlatform(): String {
        return "onyx-adb-lights"
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
        return FrontLightAdb.getBrightness(activity)
    }

    override fun getWarmth(activity: Activity): Int {
        return FrontLightAdb.getWarmth(activity)
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > getMaxBrightness()) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        FrontLightAdb.setBrightness(brightness, activity)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > getMaxWarmth()) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth")
        FrontLightAdb.setWarmth(warmth, activity)
    }

    override fun getMinWarmth(): Int {
        return MIN
    }

    override fun getMaxWarmth(): Int {
        return FrontLightAdb.getMaxWarmth()
    }

    override fun getMinBrightness(): Int {
        return MIN
    }

    override fun getMaxBrightness(): Int {
        return FrontLightAdb.getMaxBrightness()
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }
}

object FrontLightAdb {
    private const val TAG = "Lights"

    private val flController: Class<*>? = try {
        forName("android.onyx.hardware.DeviceController")
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private fun getMethod(name: String, vararg parameterTypes: Class<*>): Method? {
        return try {
            flController?.getMethod(name, *parameterTypes)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            null
        }
    }

    private val setLightValueMethod: Method? = getMethod("setLightValue", Integer.TYPE, Integer.TYPE)
    private val getLightValueMethod: Method? = getMethod("getLightValue", Integer.TYPE)
    private val getMaxLightValueMethod: Method? = getMethod("getMaxLightValue", Integer.TYPE)
    private val checkCTMMethod: Method? = getMethod("checkCTM")

    private fun getMaxLightValue(lightType: Int): Int {
        return (getMaxLightValueMethod?.invoke(flController, lightType) as? Int ?: 0).let {
            if (it == 0) 100 else it
        }
    }

    fun checkType(): Int {
        if (checkCTMMethod?.invoke(flController) as? Boolean == true) {
            return 1
        } else {
            return 0
        }
    }

    private val type = checkType()

    fun getBrightnessType(): Int {
        if (type == 1) {
            return 7
        } else {
            return 3
        }
    }

    fun getWarmthType(): Int {
        if (type == 1) {
            return 6
        } else {
            return 2
        }
    }

    private var brightnessType = getBrightnessType()
    private var warmthType = getWarmthType()
    private val brightnessMax = getMaxLightValue(brightnessType)
    private val warmthMax = getMaxLightValue(warmthType)

    fun getMaxBrightness(): Int {
        return brightnessMax
    }

    fun getMaxWarmth(): Int {
        return warmthMax
    }

    private fun getValue(method: Method?, lightType: Int): Int {
        return method?.invoke(flController, lightType) as? Int ?: 0
    }

    private fun setValue(method: Method?, lightType: Int, value: Int) {
        try {
            method?.invoke(flController, lightType, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting light value", e)
        }
    }

    fun getWarmth(context: Context?): Int {
        return getValue(getLightValueMethod, warmthType)
    }

    fun getBrightness(context: Context?): Int {
        return getValue(getLightValueMethod, brightnessType)
    }

    fun setWarmth(value: Int, context: Context?) {
        setValue(setLightValueMethod, warmthType, value)
    }

    fun setBrightness(value: Int, context: Context?) {
        setValue(setLightValueMethod, brightnessType, value)
    }
}
