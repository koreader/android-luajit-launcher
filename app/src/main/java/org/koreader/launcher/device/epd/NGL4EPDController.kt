/* Tested on Nook Glowlight 4e */

package org.koreader.launcher.device.epd

import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.freescale.NTXEPDController
import android.util.Log
import java.util.*
import android.view.Surface;
import android.view.SurfaceView;

class NGL4EPDController : NTXEPDController(), EPDInterface {

    companion object {
        private const val NGL4TAG = "NGL4"

        // constants taken as is from sunxi-kobo.h (NGL4 prefix insered for names)
      
        const val NGL4_EINK_INIT_MODE = 0x01
        const val NGL4_EINK_DU_MODE = 0x02
        const val NGL4_EINK_GC16_MODE = 0x04
        const val NGL4_EINK_GC4_MODE = 0x08
        const val NGL4_EINK_A2_MODE = 0x10
        const val NGL4_EINK_GL16_MODE = 0x20
        const val NGL4_EINK_GLR16_MODE = 0x40
        const val NGL4_EINK_GLD16_MODE = 0x80
        const val NGL4_EINK_GU16_MODE = 0x84
        const val NGL4_EINK_GCK16_MODE = 0x90
        const val NGL4_EINK_GLK16_MODE = 0x94
        const val NGL4_EINK_CLEAR_MODE = 0x88
        const val NGL4_EINK_GC4L_MODE = 0x8C
        const val NGL4_EINK_GCC16_MODE = 0xA0
        const val NGL4_EINK_PARTIAL_MODE = 0x400
        const val NGL4_EINK_AUTO_MODE = 0x8000
        const val NGL4_EINK_NEGATIVE_MODE = 0x10000
        const val NGL4_EINK_REGAL_MODE = 0x80000
        const val NGL4_EINK_GAMMA_CORRECT = 0x200000
        const val NGL4_EINK_MONOCHROME = 0x400000
        const val NGL4_EINK_DITHERING_Y1 = 0x01800000
        const val NGL4_EINK_DITHERING_Y4 = 0x02800000
        const val NGL4_EINK_DITHERING_SIMPLE = 0x04800000
        const val NGL4_EINK_DITHERING_NTX_Y1 = 0x08800000
        const val NGL4_EINK_NO_MERGE = Integer.MIN_VALUE // 0x80000000

        const val NGL4_PAGE_DELAY = 0
        const val NGL4_UI_DELAY = 0
        const val NGL4_FAST_DELAY = 0
    }

    override fun getPlatform(): String {
        return "freescale"
    }

    override fun getMode(): String {
       return "full-only"
    }

    override fun getWaveformFull(): Int {
        return NGL4_EINK_NO_MERGE + NGL4_EINK_GC16_MODE
    }

    // the mode constants below are effectively not used because getMode returns anything other than "all"

    override fun getWaveformPartial(): Int {
        return NGL4_EINK_GU16_MODE
    }

    override fun getWaveformFullUi(): Int {
        return NGL4_EINK_NO_MERGE + EINK_WAVEFORM_MODE_GLR16
    }

    override fun getWaveformPartialUi(): Int {
       return NGL4_EINK_GLR16_MODE
    }

    override fun getWaveformFast(): Int {
        return NGL4_EINK_A2_MODE;
    }

    // the delays below are effectively not used because of the method involved

    override fun getWaveformDelay(): Int {
        return NGL4_PAGE_DELAY
    }

    override fun getWaveformDelayUi(): Int {
        return NGL4_UI_DELAY
    }

    override fun getWaveformDelayFast(): Int {
        return NGL4_FAST_DELAY
    }

    override fun needsView(): Boolean {
        return true
        //return false
    }

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {

        try {
          Log.i(NGL4TAG, String.format(Locale.US, "setEpdMode to addEpdc: type:%d x:%d y:%d w:%d h:%d",
            mode, x, y, width, height))

          val iArr = intArrayOf(x, y, width, height, mode)

          Class.forName("android.view.Surface").getDeclaredMethod("addEpdc", IntArray::class.java).invoke(
            (targetView as SurfaceView).holder.surface, iArr);
        } catch (e: Exception) {
            Log.e(NGL4TAG, e.toString())
            false
        }
    }

    override fun resume() {}
    override fun pause() {}
}
