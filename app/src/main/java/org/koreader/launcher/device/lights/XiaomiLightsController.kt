package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import android.content.Context
import java.lang.Class.forName
import java.lang.reflect.Method

class XiaomiLightsController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 32
        private const val WARMTH_MAX = 32
        private const val MIN = 0
    }

    override fun getPlatform(): String {
        return "xiaomi-lights"
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

    override fun getBrightness(activity: Activity): Int {
        return try {
            val enabled = getLightStatus!!.invoke(ledController!!, activity) as Boolean
            if (!enabled) {
                return 0
            }
            getLightLevel!!.invoke(ledController, activity) as Int
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            val enabled = getWarmStatus!!.invoke(ledController!!, activity) as Boolean
            if (!enabled) {
                return 0
            }
            getWarmLevel!!.invoke(ledController, activity) as Int
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < MIN || brightness > BRIGHTNESS_MAX) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
//    fun setLight(value: Int, context: Context?) {
//        try {
//            setLedValue(value, getWarm(context), context)
//            setLightLevel!!.invoke(ledController!!, context, value)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < MIN || warmth > WARMTH_MAX) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth")
//        try {
//            setLedValue(getLight(context), value, context)
//            setWarmLevel!!.invoke(ledController!!, context, value)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
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
        return true
    }

    private val ledController: Class<*>? = try {
        forName("android.os.MoanLedControl")
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val setLedValue: Method? = try {
        ledController!!.getMethod("setLedValue", Context::class.java, Integer.TYPE, Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

//    private fun setLedValue(lightValue: Int, warmValue: Int, context: Context?) {
//        setLedValue!!.invoke(ledController!!, context, lightValue, warmValue)
//    }

    private val setWarmLevel: Method? = try {
        ledController!!.getMethod("setWarmLevel", Context::class.java, Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val getWarmLevel: Method? = try {
        ledController!!.getMethod("getWarmLevel", Context::class.java)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val getWarmStatus: Method? = try {
        ledController!!.getMethod("getWarmStatus", Context::class.java)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val setLightLevel: Method? = try {
        ledController!!.getMethod("setLightLevel", Context::class.java, Integer.TYPE)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val getLightLevel: Method? = try {
        ledController!!.getMethod("getLightLevel", Context::class.java)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }

    private val getLightStatus: Method? = try {
        ledController!!.getMethod("getLightStatus", Context::class.java)
    } catch (e: Exception) {
        Log.w(TAG, "$e")
        null
    }
}
