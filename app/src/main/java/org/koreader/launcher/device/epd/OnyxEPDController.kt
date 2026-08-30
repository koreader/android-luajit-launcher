/* Tested on Onyx Boox Nova 2 */

package org.koreader.launcher.device.epd

import android.util.Log
import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.qualcomm.QualcommEPDController

class OnyxEPDController : QualcommEPDController(), EPDInterface {

    private companion object {
        const val TAG = "EPD"
    }

    private var debouncerPrevented = false

    private fun preventBooxSystemRefresh(): Boolean {
        return try {
            // Official BOOX API: ViewUpdateHelper.debouncer(false, 0, 0, 0, 0).
            Class.forName("android.onyx.ViewUpdateHelper").getMethod(
                "debouncer",
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            ).invoke(null, false, 0, 0, 0, 0)
            Log.i(TAG, "called ViewUpdateHelper.debouncer(false, 0, 0, 0, 0)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "ViewUpdateHelper.debouncer reflection failed: ${e.message}", e)
            false
        }
    }

    private fun resumeBooxSystemRefresh(): Boolean {
        return try {
            // Ask BOOX's EInkHelper to reapply the current debouncer setting.
            val helperClass = Class.forName("android.onyx.optimization.EInkHelper")
            val duration = (helperClass.getMethod("getAnimationDuration")
                .invoke(null) as Number).toInt()
            if (duration < 0) {
                Log.w(TAG, "BOOX E-Ink service is not ready")
                return false
            }
            helperClass.getMethod("setAnimationDuration", Integer.TYPE)
                .invoke(null, duration)
            Log.i(TAG, "reapplied BOOX debouncer setting (animationDuration=$duration)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "EInkHelper debouncer restore failed: ${e.message}", e)
            false
        }
    }

    override fun getPlatform(): String {
        return "qualcomm"
    }

    override fun getMode(): String {
        return "all"
    }

    override fun getWaveformFull(): Int {
        return EINK_WAVEFORM_UPDATE_FULL + EINK_WAVEFORM_MODE_WAIT + EINK_WAVEFORM_MODE_GC16
    }

    override fun getWaveformPartial(): Int {
        return EINK_WAVEFORM_UPDATE_PARTIAL + EINK_WAVEFORM_MODE_GC16
    }

    override fun getWaveformFullUi(): Int {
        return EINK_WAVEFORM_UPDATE_FULL + EINK_WAVEFORM_MODE_REAGL
    }

    override fun getWaveformPartialUi(): Int {
        return EINK_WAVEFORM_UPDATE_PARTIAL + EINK_WAVEFORM_MODE_GC16
    }

    override fun getWaveformFast(): Int {
        return EINK_WAVEFORM_UPDATE_PARTIAL + EINK_WAVEFORM_MODE_DU
    }

    override fun getWaveformDelay(): Int {
        return EINK_WAVEFORM_DELAY
    }

    override fun getWaveformDelayUi(): Int {
        return EINK_WAVEFORM_DELAY_UI
    }

    override fun getWaveformDelayFast(): Int {
        return EINK_WAVEFORM_DELAY_FAST
    }

    override fun needsView(): Boolean {
        return false
    }

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        if (!debouncerPrevented) {
            debouncerPrevented = preventBooxSystemRefresh()
        }
        // KOReader passes right/bottom, while current Onyx firmware expects width/height.
        requestEpdMode(targetView, mode, delay, x, y, width - x, height - y)
    }

    override fun resume() {
        if (getMode() == "all") {
            debouncerPrevented = false
        }
    }
    override fun pause() {
        if (getMode() == "all") {
            debouncerPrevented = false
        }
    }
}
