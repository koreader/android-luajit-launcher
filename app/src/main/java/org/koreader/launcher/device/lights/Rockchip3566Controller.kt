package org.koreader.launcher.device.lights

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.device.lights.Rockchip3566Controller.Companion.LED_A_FILE
import org.koreader.launcher.device.lights.Rockchip3566Controller.Companion.LED_B_FILE
import org.koreader.launcher.device.lights.Rockchip3566Controller.Companion.SYS_PROP_BRIGHTNESS
import org.koreader.launcher.device.lights.Rockchip3566Controller.Companion.SYS_PROP_TEMPERATURE
import org.koreader.launcher.device.lights.Rockchip3566Controller.Companion.SYS_UTIL_RK3566_CLASS_NAME
import org.koreader.launcher.extensions.forNameOrNull
import org.koreader.launcher.extensions.getMethodOrNull
import org.koreader.launcher.extensions.read
import org.koreader.launcher.extensions.write
import org.koreader.launcher.util.getSystemProperty
import org.koreader.launcher.util.setSystemProperty
import java.io.File

/**
 * The light controller for the device [org.koreader.launcher.device.DeviceInfo.DEVICE_RK3566_EINK]
 * which is based on reverse engineering the main launcher "inkBOOKSettings.apk" for the model
 * [org.koreader.launcher.device.DeviceInfo.Id.INKBOOKFOCUS_PLUS].
 *
 * This controller tries to write to the LED files [LED_A_FILE] and [LED_B_FILE], as a fallback
 * reflection via [SYS_UTIL_RK3566_CLASS_NAME] is used if a write should be not possible.
 *
 * Whenever the brightness or warmth changes, the [computeLedValues] function needs to be called
 * which computes the correct values for LED a and b. Note that these values do not correspond
 * to the input which is set by [setBrightness] and [setWarmth].
 *
 * Additionally, to mimic the behavior of the system app as good as possible, the brightness
 * and warmth values are stored in the system properties [SYS_PROP_BRIGHTNESS] and [SYS_PROP_TEMPERATURE].
 *
 * However, the system app caches the brightness and temperature in RAM, therefore any changes
 * won't be automatically reflected in the system UI.
 */
class Rockchip3566Controller : LightsInterface {

    companion object Companion {
        private const val TAG = "Rockchip3566Controller"
        private const val BRIGHTNESS_MAX = 180
        private const val WARMTH_MAX = 200
        private const val MIN = 0

        private const val SYS_UTIL_RK3566_CLASS_NAME = "android.yitoa.rk3566.SysUtil"

        private const val SYS_UTIL_METHOD_SET_LED_A_BRIGHTNESS = "setBackLightLedaBrightness"
        private const val SYS_UTIL_METHOD_GET_MAX_LED_A_BRIGHTNESS = "getBackLightLedaMaxBrightness"
        private const val SYS_UTIL_METHOD_SET_LED_B_BRIGHTNESS = "setBackLightLedbBrightness"
        private const val SYS_UTIL_METHOD_GET_MAX_LED_B_BRIGHTNESS = "getBackLightLedbMaxBrightness"

        private const val SYS_PROP_BRIGHTNESS = "persist.inkbook.frontlight.brightness"
        private const val SYS_PROP_TEMPERATURE = "persist.inkbook.frontlight.temperature"

        // typo exists in system property
        private const val SYS_PROP_COLD_EFFICIENCY = "persist.inkbook.light.coldefficancy"
        private const val SYS_PROP_COLD_EFF_DEFAULT = 1

        // typo exists in system property
        private const val SYS_PROP_WARM_EFFICIENCY = "persist.inkbook.light.warmefficancy"
        private const val SYS_PROP_WARM_EFF_DEFAULT = 1

        // system properties for led a and b
        private const val SYS_PROP_LED_A = "persist.yitoa.backlight.leda.brightness"
        private const val SYS_PROP_LED_B = "persist.yitoa.backlight.ledb.brightness"

        // system files for backlight brightness led a and b
        private const val LED_A_FILE = "/sys/class/backlight/lm3630a_leda/brightness"
        private const val LED_B_FILE = "/sys/class/backlight/lm3630a_ledb/brightness"

        // system files for the max backlight brightness for led a and b
        private const val MAX_LED_A_FILE = "/sys/class/backlight/lm3630a_leda/max_brightness"
        private const val MAX_LED_B_FILE = "/sys/class/backlight/lm3630a_ledb/max_brightness"
        private const val DEFAULT_MAX = 200
    }

    private var sysUtilWrapper: ClassWrapper? = null

    private fun getSysUtilClass(context: Context): ClassWrapper? {
        if (sysUtilWrapper != null) {
            return sysUtilWrapper
        }
        val sysUtil = forNameOrNull(SYS_UTIL_RK3566_CLASS_NAME) ?: return null
        val instance = sysUtil
            .getConstructor(Context::class.java)
            .newInstance(context)
        val wrapper = ClassWrapper(sysUtil, instance)
        sysUtilWrapper = wrapper
        return wrapper
    }

    override fun getPlatform(): String {
        return "rockchip"
    }

    override fun hasFallback(): Boolean {
        return false
    }

    override fun hasWarmth(): Boolean {
        return true
    }

    override fun needsPermission(): Boolean {
        // no, as settings can be only manipulated via reflection
        return false
    }

    override fun getBrightness(activity: Activity): Int {
        return getSystemProperty(SYS_PROP_BRIGHTNESS, MIN.toString())
            ?.toIntOrNull() ?: MIN
    }

    override fun getWarmth(activity: Activity): Int {
        return getSystemProperty(SYS_PROP_TEMPERATURE, MIN.toString())
            ?.toIntOrNull() ?: MIN
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        setBrightnessInternal(activity, brightness)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        setTemperatureInternal(activity, warmth)
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

    private fun setBrightnessInternal(context: Context, brightness: Int) {
        val defaultTemp = 0
        val temperature =
            getSystemProperty(SYS_PROP_TEMPERATURE, 0.toString())
                ?.toIntOrNull() ?: defaultTemp
        setLight(context, brightness, temperature)
    }

    private fun setTemperatureInternal(context: Context, temperature: Int) {
        val defaultTemp = 0
        val brightness =
            getSystemProperty(SYS_PROP_BRIGHTNESS, 0.toString())
                ?.toIntOrNull() ?: defaultTemp
        setLight(context, brightness, temperature)
    }

    private fun setLight(context: Context, brightness: Int, temperature: Int) {
        // a 0 value is never persisted for the configurable properties, which is done
        // in order to keep the last known value when the light is turned off
        if (brightness > 0) {
            setSystemProperty(SYS_PROP_BRIGHTNESS, brightness.toString())
        }
        if (temperature > 0) {
            setSystemProperty(SYS_PROP_TEMPERATURE, temperature.toString())
        }

        // retrieves the efficiency factors
        val coldEff =
            getSystemProperty(SYS_PROP_COLD_EFFICIENCY, SYS_PROP_COLD_EFF_DEFAULT.toString())
                ?.toIntOrNull() ?: SYS_PROP_COLD_EFF_DEFAULT
        val warmEff =
            getSystemProperty(SYS_PROP_WARM_EFFICIENCY, SYS_PROP_WARM_EFF_DEFAULT.toString())
                ?.toIntOrNull() ?: SYS_PROP_WARM_EFF_DEFAULT

        val sysUtil: ClassWrapper? by lazy { getSysUtilClass(context) }

        // retrieves the max leda and ledb
        val maxLedA = File(MAX_LED_A_FILE).readOrElse {
            (sysUtil?.clazz?.getMethodOrNull(
                methodName = SYS_UTIL_METHOD_GET_MAX_LED_A_BRIGHTNESS
            )?.invoke(sysUtil!!.instance) as? Int) ?: DEFAULT_MAX
        }
        val maxLedB = File(MAX_LED_B_FILE).readOrElse {
            (sysUtil?.clazz?.getMethodOrNull(
                methodName = SYS_UTIL_METHOD_GET_MAX_LED_B_BRIGHTNESS
            )?.invoke(sysUtil!!.instance) as? Int) ?: DEFAULT_MAX
        }
        val (ledA, ledB) = computeLedValues(
            brightness,
            temperature,
            coldEff,
            warmEff,
            maxLedA,
            maxLedB
        )

        // set the leda and ledb based on writing to the file directly
        // as a fallback, call the reflection function
        if (File(LED_A_FILE).write(ledA)) {
            setSystemProperty(SYS_PROP_LED_A, ledA.toString())
        } else {
            sysUtil
                ?.clazz?.getMethodOrNull(
                    methodName = SYS_UTIL_METHOD_SET_LED_A_BRIGHTNESS,
                    Int::class.java
                )
                ?.invoke(sysUtil!!.instance, ledA)
        }
        if (File(LED_B_FILE).write(ledB)) {
            setSystemProperty(SYS_PROP_LED_B, ledB.toString())
        } else {
            sysUtil
                ?.clazz?.getMethodOrNull(
                    methodName = SYS_UTIL_METHOD_SET_LED_B_BRIGHTNESS,
                    Int::class.java
                )
                ?.invoke(sysUtil!!.instance, ledB)
        }
        Log.i(
            TAG, "setLight=(brightness: ${brightness}, temperature: $temperature), " +
                "setLeds=(leda: ${ledA}, ledb: $ledB)"
        )

        // com.android.systemui has registered a broadcast receiver to this intent action
        context.sendBroadcast(Intent("com.android.systemui.statusbar.action.light_change").apply {
            // the system ui will adapt the light icon based on the boolean state
            putExtra("state", brightness > 1)
        })
    }

    /**
     * This computation function is taken from the inkBOOKSettings.apk, it is called right before
     * the setters of android.yitoa.rk3566.SysUtil are called.
     *
     * @return a pair where [Pair.first] represents leda and [Pair.second] ledb.
     */
    private fun computeLedValues(
        brightness: Int,
        temperature: Int,
        coldEff: Int,
        warmEff: Int,
        ledAMax: Int,
        ledBMax: Int,
    ): Pair<Int, Int> {
        return when {
            temperature <= 95 -> {
                val ledB = brightness.coerceAtMost(ledBMax)
                val ledA = ((brightness * temperature / 100.0) * coldEff / warmEff)
                    .toInt()
                    .coerceAtMost(ledAMax)
                ledA to ledB
            }

            temperature <= 105 -> {
                val ledB = brightness.coerceAtMost(ledBMax)
                val ledA = (coldEff * brightness / warmEff)
                    .coerceAtMost(ledAMax)
                ledA to ledB
            }

            else -> {
                val ledA = brightness.coerceAtMost(ledAMax)
                val ledB = (((ledBMax - temperature) * brightness / 100.0) * coldEff / warmEff)
                    .toInt()
                    .coerceAtMost(ledBMax)
                ledA to ledB
            }
        }
    }

    private data class ClassWrapper(
        val clazz: Class<*>,
        val instance: Any,
    )
}

private fun File.readOrElse(block: () -> Int): Int {
    val value = read()
    return if (value == 0) {
        block()
    } else {
        value
    }
}
