/* EPD controller for Nook Glowlight 4 Plus (bnrv1300, "Emperor" platform, Android 8.1).
 *
 * Uses view.invalidate(int) via reflection — the same hook used by com.nook.partner's
 * EpdDisplayControllerImpl. Constants from sunxi-kobo.h (same as NGL4EPDController).
 *
 * needsView() returns false: we do NOT use NativeSurfaceView. The view passed to setEpdMode
 * is the content view (window.decorView.findViewById(android.R.id.content)), which is
 * sufficient for the driver hook.
 */

package org.koreader.launcher.device.epd

import android.util.Log
import java.util.*
import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.freescale.NTXEPDController

class NookEmperorEPDController : EPDInterface {

    companion object {
        private const val TAG = "EPD"

        const val EMPEROR_EINK_GC16_MODE = 0x04
        const val EMPEROR_EINK_GU16_MODE = 0x84
        const val EMPEROR_EINK_NO_MERGE = Integer.MIN_VALUE // 0x80000000
    }

    override fun getPlatform(): String = "freescale"
    override fun getMode(): String = "full-only"

    override fun getWaveformFull(): Int = EMPEROR_EINK_NO_MERGE + EMPEROR_EINK_GC16_MODE
    override fun getWaveformPartial(): Int = EMPEROR_EINK_GU16_MODE
    override fun getWaveformFullUi(): Int = EMPEROR_EINK_NO_MERGE + NTXEPDController.EINK_WAVEFORM_MODE_GLR16
    override fun getWaveformPartialUi(): Int = EMPEROR_EINK_GU16_MODE
    override fun getWaveformFast(): Int = EMPEROR_EINK_GU16_MODE

    override fun getWaveformDelay(): Int = 0
    override fun getWaveformDelayUi(): Int = 0
    override fun getWaveformDelayFast(): Int = 0

    // Do not use NativeSurfaceView — avoids surface setup crash on Emperor hardware.
    // The content view (decorView) is sufficient for view.invalidate(int) driver hook.
    override fun needsView(): Boolean = false

    override fun setEpdMode(
        targetView: android.view.View,
        mode: Int, delay: Long,
        x: Int, y: Int, width: Int, height: Int, epdMode: String?
    ) {
        try {
            Class.forName("android.view.View")
                .getMethod("invalidate", Integer.TYPE)
                .invoke(targetView, mode)
            Log.i(TAG, String.format(Locale.US, "Emperor EPD invalidate: mode=0x%x", mode))
        } catch (e: Exception) {
            Log.e(TAG, "Emperor EPD invalidate failed: $e")
        }
    }

    override fun resume() {}
    override fun pause() {}
}
