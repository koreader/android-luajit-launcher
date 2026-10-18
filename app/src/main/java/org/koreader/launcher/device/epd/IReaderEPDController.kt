/* EPD controller for iReader (掌阅) devices.
 *
 * Tested on iReader Smart XS (掌阅 Smart XS); applies to Smart Air, Ocean 4
 * 长续航版 and other iReader e-ink readers.
 *
 * iReader ROM provides a JNI command interface to the e-paper controller:
 *
 *     android.eink.EPDCDevice.nativePostCommand(String)
 *
 * Commands (reverse-engineered from iReader's own EpdcScrollModeChanger APK
 * v3.2.0, verified on-device):
 *     "epdc-refresh gc16"  -> full refresh (GC16 waveform). VERIFIED WORKING.
 *     "epdc-refresh gc16p" -> NOT supported by iReader (device ignores it).
 *     "epdc-mode <mode>"   -> set scroll/refresh mode (0/2101/7/a21bpp).
 *
 * Notes:
 *  - No root required; the command is a normal framework JNI call.
 *  - No EinkManager service (iReader is not a standard Rockchip device).
 *  - android.eink.EPDCDevice is a hidden framework API on Android 9+, so we
 *    lift hidden-API restrictions via VMRuntime.setHiddenApiExemptions first
 *    (same trick used by the standalone iReader refresh app).
 *  - nativePostCommand may be static or an instance method; both are handled
 *    through reflection.
 *
 * Mode strategy ("full-only"): iReader's system handles partial refresh
 * natively (it is an e-ink OS). KOReader only routes FULL updates to
 * einkUpdate, and we translate those into "epdc-refresh gc16". Partial/UI/fast
 * updates are left to the system, avoiding over-refreshing.
 *
 * Waveform constants follow the KOReader convention used by NTX/Sunxi
 * (NO_MERGE | GC16 = 0x80000004, GU16 = 0x84, A2 = 0x10).
 */

package org.koreader.launcher.device.epd

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Locale
import org.koreader.launcher.device.EPDInterface

class IReaderEPDController : EPDInterface {

    companion object {
        private const val TAG = "EPD"

        const val IREADER_EINK_NO_MERGE = Integer.MIN_VALUE // 0x80000000
        const val IREADER_EINK_GC16_MODE = 0x04
        const val IREADER_EINK_GU16_MODE = 0x84
        const val IREADER_EINK_A2_MODE = 0x10

        // Verified on iReader Smart XS: gc16 works, gc16p does not respond.
        const val CMD_FULL_REFRESH = "epdc-refresh gc16"

        @Volatile private var reflectionReady = false
        private var postCommandFn: Method? = null
        private var postCommandTarget: Any? = null

        @SuppressLint("BlockedPrivateApi")
        private fun initReflection() {
            if (reflectionReady) return
            try {
                // android.eink.EPDCDevice is a hidden framework API on Android 9+;
                // lift the hidden-API restrictions so reflection is allowed.
                try {
                    val runtimeClass = Class.forName("dalvik.system.VMRuntime")
                    val runtime = runtimeClass.getDeclaredMethod("getRuntime").invoke(null)
                    runtimeClass.getDeclaredMethod(
                        "setHiddenApiExemptions", Array<String>::class.java
                    ).invoke(runtime, arrayOf("L"))
                    Log.i(TAG, "iReader EPD: hidden-api exemption set")
                } catch (e: Throwable) {
                    Log.w(TAG, "iReader EPD: hidden-api exemption failed (may be unnecessary): $e")
                }

                val cls = Class.forName("android.eink.EPDCDevice")
                val method = try {
                    cls.getMethod("nativePostCommand", String::class.java)
                } catch (_: NoSuchMethodException) {
                    cls.getDeclaredMethod("nativePostCommand", String::class.java)
                }
                method.isAccessible = true
                postCommandFn = method
                postCommandTarget = if (Modifier.isStatic(method.modifiers)) {
                    null
                } else {
                    try {
                        cls.getDeclaredConstructor().newInstance()
                    } catch (e: Throwable) {
                        Log.w(TAG, "iReader EPD: no accessible constructor, "
                            + "assuming static nativePostCommand: $e")
                        null
                    }
                }
                Log.i(TAG, "iReader EPD: EPDCDevice.nativePostCommand ready "
                    + "(static=${Modifier.isStatic(method.modifiers)})")
            } catch (e: Throwable) {
                Log.w(TAG, "iReader EPD: reflection path unavailable: $e")
            }
            reflectionReady = true
        }

        private fun postCommand(cmd: String): Boolean {
            initReflection()
            val fn = postCommandFn ?: return false
            return try {
                fn.invoke(postCommandTarget, cmd)
                Log.i(TAG, "iReader EPD: nativePostCommand(\"$cmd\")")
                true
            } catch (e: Throwable) {
                Log.w(TAG, "iReader EPD: nativePostCommand(\"$cmd\") failed: $e")
                false
            }
        }
    }

    override fun getPlatform(): String = "ireader"
    override fun getMode(): String = "full-only"

    override fun getWaveformFull(): Int = IREADER_EINK_NO_MERGE + IREADER_EINK_GC16_MODE
    override fun getWaveformPartial(): Int = IREADER_EINK_GU16_MODE
    override fun getWaveformFullUi(): Int = IREADER_EINK_NO_MERGE + IREADER_EINK_GC16_MODE
    override fun getWaveformPartialUi(): Int = IREADER_EINK_GU16_MODE
    override fun getWaveformFast(): Int = IREADER_EINK_A2_MODE

    override fun getWaveformDelay(): Int = 0
    override fun getWaveformDelayUi(): Int = 0
    override fun getWaveformDelayFast(): Int = 0

    override fun needsView(): Boolean = false

    override fun setEpdMode(
        targetView: View,
        mode: Int, delay: Long,
        x: Int, y: Int, width: Int, height: Int, epdMode: String?
    ) {
        // "full-only": only FULL updates reach here; the iReader system handles
        // partial/UI/fast natively. Send gc16 for full-screen refreshes only.
        val isFull = epdMode == "EPD_FULL"
            || mode == getWaveformFull()
            || mode == getWaveformFullUi()

        if (isFull) {
            postCommand(CMD_FULL_REFRESH)
        } else {
            Log.v(TAG, String.format(Locale.US,
                "iReader EPD: partial/fast left to system (epdMode=%s mode=%d)",
                epdMode, mode))
        }
    }

    override fun resume() {}

    override fun pause() {}
}
