package org.koreader.launcher.device.epd

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import org.koreader.launcher.device.EPDInterface

/* EPD Controller for Lenovo SmartPaper (RK3566).
 *
 * Uses the hidden system service android.os.EinkManager via reflection.
 * The service is obtained via Context.getSystemService("eink").
 *
 * Note: sendOneFullFrame() is blocked by SELinux on Lenovo devices,
 * so we use screenRefresh(true, 1) instead, which correctly triggers
 * a full-screen GC16 refresh (black/white flash to clear ghosting).
 */

class LenovoSmartPaperEPDController : EPDInterface {

    companion object {
        private const val TAG = "EPD"

        // Waveform value for full-screen refresh. Must match the EPD_FULL constant
        // in MainActivity.einkUpdate() (value 1) so the refresh request is not
        // discarded as "invalid". The actual waveform used by EinkManager is
        // determined inside setEpdMode(), not by this constant.
        private const val WAVEFORM_FULL = 1
    }

    override fun getPlatform(): String {
        return "rockchip"
    }

    override fun getMode(): String {
        return "full-only"
    }

    override fun getWaveformFull(): Int {
        return WAVEFORM_FULL
    }

    override fun getWaveformPartial(): Int {
        return WAVEFORM_FULL
    }

    override fun getWaveformFullUi(): Int {
        return WAVEFORM_FULL
    }

    override fun getWaveformPartialUi(): Int {
        return WAVEFORM_FULL
    }

    override fun getWaveformFast(): Int {
        return WAVEFORM_FULL
    }

    override fun getWaveformDelay(): Int {
        return WAVEFORM_FULL
    }

    override fun getWaveformDelayUi(): Int {
        return WAVEFORM_FULL
    }

    override fun getWaveformDelayFast(): Int {
        return WAVEFORM_FULL
    }

    override fun needsView(): Boolean {
        return true
    }

    @SuppressLint("WrongConstant")
    override fun setEpdMode(targetView: View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        try {
            val einkManagerClass = Class.forName("android.os.EinkManager")
            val einkManager: Any? = targetView.context.getSystemService("eink")

            if (einkManager != null) {
                // Use screenRefresh(true, 1) — the only working full-refresh
                // path on Lenovo devices. sendOneFullFrame() is blocked by SELinux.
                val screenRefresh = einkManagerClass.getMethod(
                    "screenRefresh", Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                screenRefresh.invoke(einkManager, true, 1)
            } else {
                Log.w(TAG, "EinkManager service not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "setEpdMode failed: ${e.message}")
            Log.e(TAG, e.stackTraceToString())
        }
    }

    override fun resume() {}
    override fun pause() {}
}
