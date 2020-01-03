/* generic EPD Controller for Android devices
 *
 * interface for boyue rk33xx clones, like the Likebook Mimas, Mars, Muses...
 * based on https://github.com/koreader/koreader/issues/4595
 *
 * Thanks to @carlinux
 *
 * Only full mode was tested, so 'getEpdMode' returns that.
 * Device owners will want to expand this to return other well known modes:
 *
 * val EPD_A2 = 2
 * val EPD_PART = 3
 * val EPD_BLACK_WHITE = 6
 * val EPD_FORCE_FULL = 11
 * val EPD_REGAL = 15
 * val EPD_ADAPTATIVE = 17
 * val EPD_FAST = 18
 * val EPD_DITHER = 100
 */

package org.koreader.launcher.device.rockchip

import java.util.Locale
import android.util.Log

abstract class RK33xxEPDController {
    companion object {
        private const val TAG = "epd"
        private const val EPD_FULL = 1

        fun requestEpdMode(epdMode: String): Boolean {
            return try {
                Class.forName("android.view.View").getMethod("setByEinkUpdateMode",
                        Integer.TYPE).invoke(null, getEpdMode(epdMode))
                true
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                false
            }
        }

        private fun getEpdMode(epdMode: String): Int {
            val mode = EPD_FULL
            Log.v(TAG, String.format(Locale.US, "Requesting %s: %d", epdMode, mode))
            return mode
        }
    }
}
