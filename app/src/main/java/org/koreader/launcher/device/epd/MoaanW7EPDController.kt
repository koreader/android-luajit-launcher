package org.koreader.launcher.device.epd

import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.sunxi.SunxiEPDController

class MoaanW7EPDController : SunxiEPDController(), EPDInterface {

    override fun getPlatform(): String {
        return "sunxi"
    }

    override fun getMode(): String {
        return "all"
    }

    override fun getWaveformFull(): Int {
        return SUNXI_EINK_NO_MERGE + SUNXI_EINK_GC16_MODE
    }

    override fun getWaveformPartial(): Int {
        return SUNXI_EINK_GU16_MODE
    }

    override fun getWaveformFullUi(): Int {
        return SUNXI_EINK_NO_MERGE + SUNXI_EINK_GLR16_MODE
    }

    override fun getWaveformPartialUi(): Int {
        return SUNXI_EINK_GU16_MODE
    }

    override fun getWaveformFast(): Int {
        return SUNXI_EINK_A2_MODE
    }

    override fun getWaveformDelay(): Int {
        return 0
    }

    override fun getWaveformDelayUi(): Int {
        return 0
    }

    override fun getWaveformDelayFast(): Int {
        return 0
    }

    override fun needsView(): Boolean {
        return false
    }

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        requestEpdMode(targetView, mode, epdMode)
    }

    override fun resume() {}
    override fun pause() {}
}
