/* Alternative EPD controller, tested on Crema Grande.
 */

package org.koreader.launcher.device.epd.freescale

import android.util.Log
import java.io.PrintWriter

abstract class NTXAltEPDController {

    companion object {
        private const val TAG = "EPD"
        const val EINK_WAVEFORM_UPDATE_FULL = 32
        const val EINK_WAVEFORM_UPDATE_PARTIAL = 0
        const val EINK_WAVEFORM_MODE_DU = 1
        const val EINK_WAVEFORM_MODE_GC16 = 2
        const val EINK_WAVEFORM_MODE_GL16 = 6
        const val EINK_WAVEFORM_MODE_GLR16 = 7
        const val EINK_WAVEFORM_DELAY = 0

        fun requestEpdMode(): Boolean {
            return try {
                val writer = PrintWriter("sys/class/graphics/fb0/full_refresh_request")
                writer.println("1") // 1 - Update after screen changes, 2 - Update immediately
                writer.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                false
            }
        }
    }
}
