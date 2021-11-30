package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import android.content.Context
import java.lang.Class.forName
import java.lang.reflect.Method
import kotlin.math.*


data class WarmColdSetting(val warm: Int = 0, val cold: Int = 0)
data class WarmthBrightnessSetting(var warmth: Int = 0, var brightness: Int = 0)

class OnyxSDMController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val BRIGHTNESS_MAX = 100
        private const val WARMTH_MAX = 100
        private const val MIN = 0

        private var currentWarmCold = WarmColdSetting()
        private var currentWarmthBrightness = WarmthBrightnessSetting()
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
            currentWarmCold = FrontLight.getWarmCold(activity)
            currentWarmthBrightness = Adapter.findWarmthBrightnessApproximationForWarmCold(currentWarmCold)
            return currentWarmthBrightness.brightness
        } catch (e: Exception) {
            Log.w(TAG, Log.getStackTraceString(e))
            0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        return try {
            currentWarmCold = FrontLight.getWarmCold(activity)
            currentWarmthBrightness = Adapter.findWarmthBrightnessApproximationForWarmCold(currentWarmCold)
            return currentWarmthBrightness.warmth
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
        try {
            currentWarmthBrightness.brightness = brightness
            currentWarmCold = Adapter.convertWarmthBrightnessToWarmCold(currentWarmthBrightness)
            Log.v(TAG, "Setting brightness to $brightness")
            FrontLight.setWarmCold(currentWarmCold, activity)
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
            currentWarmthBrightness.warmth = warmth
            currentWarmCold = Adapter.convertWarmthBrightnessToWarmCold(currentWarmthBrightness)
            FrontLight.setWarmCold(currentWarmCold, activity)
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

object Adapter{
    private val maxWarmColdSetting = WarmColdSetting(255, 255)
    private val MAX_BRIGHTNESS_LUX = 112

    fun convertWarmthBrightnessToWarmCold(warmthBrightness: WarmthBrightnessSetting): WarmColdSetting {
        val desiredBrightnessLux = convertBrightnessSettingToLux(warmthBrightness.brightness)
        val warmBrightnessLux = desiredBrightnessLux * warmthBrightness.warmth / 100
        val warmSetting = convertLuxToWarmOrColdSetting(warmBrightnessLux, maxWarmColdSetting.warm)
        val coldBrightnessLux = desiredBrightnessLux - warmBrightnessLux
        val coldSetting = convertLuxToWarmOrColdSetting(coldBrightnessLux, maxWarmColdSetting.cold)
        return WarmColdSetting(warmSetting, coldSetting)
    }

    fun findWarmthBrightnessApproximationForWarmCold(warmCold: WarmColdSetting): WarmthBrightnessSetting {
        val warmBrightnessLux = convertWarmOrColdSettingToLux(warmCold.warm)
        val coldBrightnessLux = convertWarmOrColdSettingToLux(warmCold.cold)
        val brightnessLux = warmBrightnessLux + coldBrightnessLux
        val warmthPercent = min(100.0, warmBrightnessLux * 100 / brightnessLux).roundToInt()
        val brightness = convertLuxToBrigthnessSetting(brightnessLux)
        return WarmthBrightnessSetting(warmthPercent, brightness)
    }

    private fun convertLuxToWarmOrColdSetting(brightnessLux: Double, maxResult: Int): Int {
        if (brightnessLux <= 0.05) return 0
        val minNonZeroResult = 1
        return Math.max(minNonZeroResult, min(maxResult, (34 * ln(17 * brightnessLux)).roundToInt()))
    }

    private fun convertWarmOrColdSettingToLux(setting: Int): Double {
        return if (setting == 0) 0.0 else Math.E.pow(setting.toDouble() / 34) / 17
    }

    private fun convertBrightnessSettingToLux(slider: Int): Double {
        if (slider == 0) return 0.0
        return if (slider <= 5) {
            max(0.0, 0.0223609 * slider.toDouble().pow(2.0) - 0.0406061 * slider + 0.0861964)
        } else min(MAX_BRIGHTNESS_LUX.toDouble(), 0.5 * Math.E.pow(0.0535 * slider))
    }

    private fun convertLuxToBrigthnessSetting(lux: Double): Int {
        if (lux == 0.0) return 0
        if (lux <= 0.5) {
            return (0.90797 + 0.214277 * sqrt(max(0.0, 974 * lux - 66))).roundToInt()
        }
        val MAX_BRIGHTNESS_SETTING = 100.0
        return max(1.0, min(MAX_BRIGHTNESS_SETTING, (18.6916 * ln(2 * lux)).roundToInt().toDouble())).roundToInt()
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


    fun getWarmCold(context: Context?): WarmColdSetting {
        try {
            return WarmColdSetting(
                getCoolWarmBrightness.invoke(fLController, context, BRIGHTNESS_CONFIG_WARM_IDX) as Int,
                getCoolWarmBrightness.invoke(fLController, context, BRIGHTNESS_CONFIG_COLD_IDX) as Int
            )
        } catch (e: Exception) {
            Log.w(TAG, "$e")
            return WarmColdSetting()
        }
    }

    fun setWarmCold(setting: WarmColdSetting, context: Context?) {
        try {
            setWarmBrightness.invoke(fLController, context, setting.warm)
            setColdBrightness.invoke(fLController, context, setting.cold)
        } catch (e: Exception) {
            Log.w(TAG, "$e")
        }


    }


}
