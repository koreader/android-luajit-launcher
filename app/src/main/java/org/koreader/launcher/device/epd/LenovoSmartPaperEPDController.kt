/* EPD Controller for Lenovo Smart Paper (RK3566).
 * Tested on Lenovo Smart Paper. sendOneFullFrame() does not work on this device,
 * so setEpdMode() uses screenRefresh(true, 1) for full refresh instead.
 */

package org.koreader.launcher.device.epd

import android.util.Log
import android.view.View
import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.rockchip.RK35xxEPDController

class LenovoSmartPaperEPDController : RK35xxEPDController(), EPDInterface {

    companion object {
        private const val TAG = "EPD"

        // RK3566 EPD waveform modes (see RK35xxEPDController)
        private const val EPD_FULL = 2      // EPD_FULL_GC16
        private const val EPD_FULL_GL16 = 3 // EPD_FULL_GL16 (Balanced)
        private const val EPD_PART = 7      // EPD_PART_GC16
        private const val EPD_A2 = 12       // EPD_A2 (fast refresh)
        private const val EPD_DELAY = 0
    }

    override fun getPlatform(): String {
        return "rockchip"
    }

    override fun getMode(): String {
        return "full-only"
    }

    override fun getWaveformFull(): Int {
        return EPD_FULL
    }

    override fun getWaveformPartial(): Int {
        return EPD_PART
    }

    override fun getWaveformFullUi(): Int {
        return EPD_FULL_GL16
    }

    override fun getWaveformPartialUi(): Int {
        return EPD_PART
    }

    override fun getWaveformFast(): Int {
        return EPD_A2
    }

    override fun getWaveformDelay(): Int {
        return EPD_DELAY
    }

    override fun getWaveformDelayUi(): Int {
        return EPD_DELAY
    }

    override fun getWaveformDelayFast(): Int {
        return EPD_DELAY
    }

    override fun needsView(): Boolean {
        return true
    }

    override fun setEpdMode(
        targetView: View,
        mode: Int, delay: Long,
        x: Int, y: Int, width: Int, height: Int, epdMode: String?
    ) {
        try {
            val einkManagerClass = Class.forName("android.os.EinkManager")
            val einkManager: Any? = targetView.context.getSystemService("eink")

            if (einkManager != null) {
                val screenRefresh = einkManagerClass.getMethod(
                    "screenRefresh", Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType
                )
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
