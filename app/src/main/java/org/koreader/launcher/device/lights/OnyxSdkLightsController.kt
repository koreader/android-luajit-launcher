package org.koreader.launcher.device.lights

import android.app.Activity
import android.content.Context
import android.util.Log
import org.koreader.launcher.device.LightsInterface
import java.lang.reflect.Method

// ─── Constants mirroring FrontLightController / BaseBrightnessProvider ────────

private const val LIGHT_TYPE_FL = 1
private const val LIGHT_TYPE_CTM_WARM = 2
private const val LIGHT_TYPE_CTM_COLD = 3
private const val LIGHT_TYPE_CTM_ALL = 4
private const val LIGHT_TYPE_TEMP = 6
private const val LIGHT_TYPE_CTM_BR = 7

enum class OnyxBrightnessType { FL, WARM_AND_COLD, CTM, NONE }

// ─── Controller ───────────────────────────────────────────────────────────────

class OnyxSdkLightsController : LightsInterface {

    companion object {
        private const val TAG = "Lights"
        private const val MIN = 0
        private const val FALLBACK_MAX = 255
    }

    override fun getPlatform(): String = "onyx-sdk-lights"
    override fun hasFallback(): Boolean = false
    override fun needsPermission(): Boolean = false
    override fun hasStandaloneWarmth(): Boolean = false

    override fun hasWarmth(): Boolean = true

    // ── Read ──────────────────────────────────────────────────────────────────

    override fun getBrightness(activity: Activity): Int {
        OnyxDevice.init(activity)
        return when (OnyxDevice.brightnessType) {
            OnyxBrightnessType.FL -> OnyxDevice.getLightValue(LIGHT_TYPE_FL) ?: 0
            OnyxBrightnessType.WARM_AND_COLD -> OnyxDevice.getLightValue(LIGHT_TYPE_CTM_COLD) ?: 0
            OnyxBrightnessType.CTM -> OnyxDevice.getLightValue(LIGHT_TYPE_CTM_BR) ?: 0
            OnyxBrightnessType.NONE -> 0
        }
    }

    override fun getWarmth(activity: Activity): Int {
        OnyxDevice.init(activity)
        return when (OnyxDevice.brightnessType) {
            OnyxBrightnessType.WARM_AND_COLD -> OnyxDevice.getLightValue(LIGHT_TYPE_CTM_WARM) ?: 0
            OnyxBrightnessType.CTM -> OnyxDevice.getLightValue(LIGHT_TYPE_TEMP) ?: 0
            else -> 0
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    override fun setBrightness(activity: Activity, brightness: Int) {
        OnyxDevice.init(activity)
        val max = getMaxBrightness()
        if (brightness !in MIN..max) {
            Log.w(TAG, "brightness out of range: $brightness (max=$max)")
            return
        }
        Log.d(TAG, "setBrightness=$brightness type=${OnyxDevice.brightnessType}")
        val lightType = when (OnyxDevice.brightnessType) {
            OnyxBrightnessType.FL -> LIGHT_TYPE_FL
            OnyxBrightnessType.WARM_AND_COLD -> LIGHT_TYPE_CTM_COLD
            OnyxBrightnessType.CTM -> LIGHT_TYPE_CTM_BR
            OnyxBrightnessType.NONE -> return
        }
        OnyxDevice.setLight(activity, lightType, brightness)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        OnyxDevice.init(activity)
        val max = getMaxWarmth()
        if (warmth !in MIN..max) {
            Log.w(TAG, "warmth out of range: $warmth (max=$max)")
            return
        }
        Log.d(TAG, "setWarmth=$warmth type=${OnyxDevice.brightnessType}")
        val lightType = when (OnyxDevice.brightnessType) {
            OnyxBrightnessType.WARM_AND_COLD -> LIGHT_TYPE_CTM_WARM
            OnyxBrightnessType.CTM -> LIGHT_TYPE_TEMP
            else -> return
        }
        OnyxDevice.setLight(activity, lightType, warmth)
    }

    // ── Range ─────────────────────────────────────────────────────────────────

    override fun getMinBrightness(): Int = MIN
    override fun getMinWarmth(): Int = MIN

    override fun getMaxBrightness(): Int = when (OnyxDevice.brightnessType) {
        OnyxBrightnessType.FL -> OnyxDevice.getMaxLightValue(LIGHT_TYPE_FL)
        OnyxBrightnessType.WARM_AND_COLD -> OnyxDevice.getMaxLightValue(LIGHT_TYPE_CTM_COLD)
        OnyxBrightnessType.CTM -> OnyxDevice.getMaxLightValue(LIGHT_TYPE_CTM_BR)
        OnyxBrightnessType.NONE -> null
    } ?: FALLBACK_MAX

    override fun getMaxWarmth(): Int = when (OnyxDevice.brightnessType) {
        OnyxBrightnessType.WARM_AND_COLD -> OnyxDevice.getMaxLightValue(LIGHT_TYPE_CTM_WARM)
        OnyxBrightnessType.CTM -> OnyxDevice.getMaxLightValue(LIGHT_TYPE_TEMP)
        else -> null
    } ?: FALLBACK_MAX

    override fun enableFrontlightSwitch(activity: Activity): Int = 1
}

// ─── Brightness type detector ─────────────────────────────────────────────────

/**
 * Encapsulates the detection heuristics for which brightness channels this
 * device supports.  Extracted from OnyxDevice.init() to keep each class focused.
 *
 * Priority (mirrors BrightnessController.initProviderMap()):
 *   checkCTM() / maxCTM > 0 → CTM (types 6 + 7)
 *   hasFLBrightness → FL (type 1)
 *   hasCTMBrightness / maxWarm > 0 → WARM_AND_COLD (types 2 + 3)
 */
internal object BrightnessDetector {

    private const val TAG = "OnyxDevice"

    /**
     * Returns the detected [OnyxBrightnessType], or `null` when detection
     * results in [OnyxBrightnessType.NONE] (i.e. nothing was found).
     */
    fun detect(
        context: Context,
        bridge: OnyxReflectionBridge,
    ): OnyxBrightnessType {
        val checkCTM = bridge.checkCTM()
        val hasFL = bridge.hasFLBrightness(context)
        val hasCTM = bridge.hasCTMBrightness(context)
        val maxCTM = bridge.getMaxLightValue(LIGHT_TYPE_TEMP) ?: 0
        val maxWarm = bridge.getMaxLightValue(LIGHT_TYPE_CTM_WARM) ?: 0

        val type = when {
            checkCTM || maxCTM > 0 -> OnyxBrightnessType.CTM
            hasFL -> OnyxBrightnessType.FL
            hasCTM || maxWarm > 0 -> OnyxBrightnessType.WARM_AND_COLD
            else -> OnyxBrightnessType.NONE
        }

        Log.d(
            TAG,
            "Detection: checkCTM=$checkCTM hasCTM=$hasCTM hasFL=$hasFL " +
                "maxCTM=$maxCTM maxWarm=$maxWarm → $type"
        )
        return type
    }
}

// ─── Front-light open/close logic ────────────────────────────────────────────

/**
 * Decides which physical channel to open/close and whether a value-zero write
 * should close the channel or just zero it.
 *
 * Extracted from OnyxDevice.setLight() to keep that function focused on
 * dispatching rather than policy.
 */
internal object FrontLightSwitch {

    /**
     * Maps a logical [lightType] to the physical channel used for open/close.
     * CTM brightness and temperature share the master CTM switch (type 4);
     * all other channels map to themselves.
     */
    fun channelFor(lightType: Int): Int = when (lightType) {
        LIGHT_TYPE_CTM_BR,
        LIGHT_TYPE_TEMP -> LIGHT_TYPE_CTM_ALL

        else -> lightType
    }

    /**
     * Returns true when setting [lightType] to zero should close the front
     * light, rather than simply writing value 0 to the parameter.
     *
     * Warmth parameters (TEMP / CTM_WARM) only control colour balance and
     * must not turn off the whole panel.
     */
    fun shouldCloseOnZero(lightType: Int): Boolean = when (lightType) {
        LIGHT_TYPE_FL,
        LIGHT_TYPE_CTM_BR,
        LIGHT_TYPE_CTM_COLD,
        LIGHT_TYPE_CTM_WARM -> true

        else -> false
    }
}

// ─── Pure reflection bridge ───────────────────────────────────────────────────

/**
 * Thin wrapper around `android.onyx.hardware.DeviceController` that exposes
 * only typed Kotlin calls.  Contains no state beyond the cached [Method]
 * references; all business logic lives in [OnyxDevice], [BrightnessDetector],
 * and [FrontLightSwitch].
 */
internal class OnyxReflectionBridge {

    companion object {
        private const val TAG = "OnyxDevice"
    }

    private val controller: Class<*>? = try {
        Class.forName("android.onyx.hardware.DeviceController")
    } catch (e: Exception) {
        Log.w(TAG, "DeviceController not found: $e"); null
    }

    private val mGetLightValue: Method? =
        method("getLightValue", Integer.TYPE) ?: method("getLightValues", Integer.TYPE)

    private val mGetMaxLightValue: Method? =
        method("getMaxLightValue", Integer.TYPE) ?: method("getMaxLightValues", Integer.TYPE)

    private val mSetLightValue: Method? =
        method("setLightValue", Integer.TYPE, Integer.TYPE)
            ?: method("setLightValues", Integer.TYPE, Integer.TYPE)

    private val mOpenFrontLight: Method? = method("openFrontLight", Integer.TYPE)
    private val mCloseFrontLight: Method? = method("closeFrontLight", Integer.TYPE)

    private val mIsLightOn: Method? =
        method("isLightOn", Context::class.java, Integer.TYPE)
            ?: method("isLightOn", Integer.TYPE)

    private val mHasFLBrightness: Method? = method("hasFLBrightness", Context::class.java)
    private val mHasCTMBrightness: Method? = method("hasCTMBrightness", Context::class.java)
    private val mCheckCTM: Method? = method("checkCTM")

    // ── Capability queries ────────────────────────────────────────────────────

    fun checkCTM(): Boolean = mCheckCTM?.invoke(controller) as? Boolean ?: false
    fun hasFLBrightness(ctx: Context): Boolean = mHasFLBrightness?.invoke(controller, ctx) as? Boolean ?: false
    fun hasCTMBrightness(ctx: Context): Boolean = mHasCTMBrightness?.invoke(controller, ctx) as? Boolean ?: false

    // ── Reads ─────────────────────────────────────────────────────────────────

    fun getLightValue(type: Int): Int? =
        mGetLightValue?.invoke(controller, type) as? Int

    fun getMaxLightValue(type: Int): Int? {
        val v = mGetMaxLightValue?.invoke(controller, type) as? Number
        return v?.toInt()?.takeIf { it != 0 }
    }

    fun isLightOn(context: Context, type: Int): Boolean = mIsLightOn?.let { m ->
        if (m.parameterTypes.size == 2) m.invoke(controller, context, type)
        else m.invoke(controller, type)
    } as? Boolean ?: false

    // ── Writes ────────────────────────────────────────────────────────────────

    fun openFrontLight(type: Int): Boolean =
        mOpenFrontLight?.invoke(controller, type) as? Boolean ?: false

    fun closeFrontLight(type: Int): Boolean =
        mCloseFrontLight?.invoke(controller, type) as? Boolean ?: false

    fun setLightValue(type: Int, value: Int): Boolean =
        mSetLightValue?.invoke(controller, type, value) as? Boolean ?: false

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun method(name: String, vararg params: Class<*>): Method? = try {
        controller?.getMethod(name, *params)
    } catch (e: Exception) {
        Log.w(TAG, "Method '$name' not found: $e"); null
    }
}

// ─── Stateful device facade ───────────────────────────────────────────────────

object OnyxDevice {

    private const val TAG = "OnyxDevice"

    private val bridge = OnyxReflectionBridge()

    var brightnessType: OnyxBrightnessType = OnyxBrightnessType.NONE
        private set

    /**
     * Initialises brightness-type detection.
     */
    fun init(context: Context) {
        val detected = BrightnessDetector.detect(context, bridge)
        if (detected != brightnessType) {
            brightnessType = detected
        }
    }

    // ── Reads (delegated to bridge) ───────────────────────────────────────────

    fun getLightValue(type: Int): Int? = bridge.getLightValue(type)
    fun getMaxLightValue(type: Int): Int? = bridge.getMaxLightValue(type)

    fun isLightOn(context: Context, type: Int): Boolean =
        bridge.isLightOn(context, type)

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Sets a light [type] to [value], opening or closing the physical channel
     * as required.  Channel-to-switch mapping and close-on-zero policy are
     * resolved by [FrontLightSwitch].
     */
    fun setLight(context: Context, type: Int, value: Int): Boolean {
        val channel = FrontLightSwitch.channelFor(type)

        return if (value == 0) {
            setLightOff(type, channel)
        } else {
            ensureLightOn(context, channel)
            val ok = bridge.setLightValue(type, value)
            Log.d(TAG, "setLightValue(type=$type, value=$value) → $ok")
            ok
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun setLightOff(type: Int, channel: Int): Boolean {
        if (FrontLightSwitch.shouldCloseOnZero(type)) {
            bridge.closeFrontLight(channel)
            Log.d(TAG, "closeFrontLight(channel=$channel)")
        }

        val ok = bridge.setLightValue(type, 0)
        Log.d(TAG, "setLightValue(type=$type, value=0) → $ok")
        return ok
    }

    private fun ensureLightOn(context: Context, channel: Int) {
        if (!bridge.isLightOn(context, channel)) {
            val ok = bridge.openFrontLight(channel)
            Log.d(TAG, "openFrontLight(channel=$channel) → $ok")
        }
    }
}
