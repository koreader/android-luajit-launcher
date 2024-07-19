package org.koreader.launcher.device.lights

import android.util.Log
import java.lang.Class.forName
import java.lang.reflect.Method

object OnyxSdkDeviceController {
    enum class Light(val code: Int) {
        COLD(7),
        WARM(6)
    }

    private const val TAG = "lights"

    private val flController: Class<*>? = try {
        forName("android.onyx.hardware.DeviceController")
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val getMaxLightValueMethod: Method? = try {
        flController!!.getMethod("getMaxLightValue", Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val getLightValueMethod: Method? = try {
        flController!!.getMethod("getLightValue", Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val setLightValueMethod: Method? = try {
        flController!!.getMethod("setLightValue", Integer.TYPE, Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    fun getMaxLightValue(light: Light): Int {
        return try {
            getMaxLightValueMethod!!.invoke(flController!!, light.code) as Int
        } catch (e: Exception) {
            Log.e(TAG, "error getting the max light $light", e)
            0
        }
    }

    fun getLightValue(light: Light): Int {
        return try {
            getLightValueMethod!!.invoke(flController!!, light.code) as Int
        } catch (e: Exception) {
            Log.e(TAG, "error getting the light $light", e)
            0
        }
    }

    fun setLightValue(light: Light, value: Int) {
        try {
            setLightValueMethod!!.invoke(flController!!, light.code, value)
        } catch (e: Exception) {
            Log.e(TAG, "error setting the light $light to $value", e)
        }
    }
}
