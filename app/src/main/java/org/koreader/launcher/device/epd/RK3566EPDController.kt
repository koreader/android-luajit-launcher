package org.koreader.launcher.device.epd

import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.rockchip.RK35xxEPDController

class RK3566EPDController : RK35xxEPDController(), EPDInterface {

    override fun getPlatform(): String {
        return "rockchip"
    }

    override fun getMode(): String {
        return "full-only"
    }

    override fun getWaveformFull(): Int {
        // 1 = EPD_FULL in MainActivity.einkUpdate()'s mode mapping (1..4).
        // EPD_AUTO(0) would be treated as "invalid" and dropped there,
        // making every full-refresh request a no-op.
        return 1
    }

    override fun getWaveformPartial(): Int {
        return EPD_AUTO
    }

    override fun getWaveformFullUi(): Int {
        return EPD_AUTO
    }

    override fun getWaveformPartialUi(): Int {
        return EPD_AUTO
    }

    override fun getWaveformFast(): Int {
        return EPD_AUTO
    }

    override fun getWaveformDelay(): Int {
        return EPD_AUTO
    }

    override fun getWaveformDelayUi(): Int {
        return EPD_AUTO
    }

    override fun getWaveformDelayFast(): Int {
        return EPD_AUTO
    }

    override fun needsView(): Boolean {
        return true
    }

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        requestEpdMode(targetView)
    }

    override fun resume() {}
    override fun pause() {}
}
