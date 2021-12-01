/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device

import android.util.Log
import org.koreader.launcher.device.epd.NookEPDController
import org.koreader.launcher.device.epd.TolinoEPDController
import org.koreader.launcher.device.epd.RK3026EPDController
import org.koreader.launcher.device.epd.RK3368EPDController
import org.koreader.launcher.device.epd.OnyxEPDController
import java.util.*

object EPDFactory {
    val epdController: EPDInterface
        get() {
            return when (DeviceInfo.EINK) {

                DeviceInfo.EinkDevice.BOYUE_T61,
                DeviceInfo.EinkDevice.BOYUE_T62,
                DeviceInfo.EinkDevice.BOYUE_T80S,
                DeviceInfo.EinkDevice.CREMA_0650L,
                DeviceInfo.EinkDevice.FIDIBOOK,
                DeviceInfo.EinkDevice.ONYX_C67,
                DeviceInfo.EinkDevice.ENERGY,
                DeviceInfo.EinkDevice.INKBOOK -> {
                    logController("Rockchip RK3026")
                    RK3026EPDController()
                }

                DeviceInfo.EinkDevice.BOYUE_K78W,
                DeviceInfo.EinkDevice.BOYUE_K103,
                DeviceInfo.EinkDevice.BOYUE_P6,
                DeviceInfo.EinkDevice.BOYUE_P61,
                DeviceInfo.EinkDevice.BOYUE_P78,
                DeviceInfo.EinkDevice.BOYUE_T78D,
                DeviceInfo.EinkDevice.BOYUE_T80D,
                DeviceInfo.EinkDevice.BOYUE_T103D -> {
                    logController("Rockchip RK3368")
                    RK3368EPDController()
                }

                DeviceInfo.EinkDevice.BOYUE_T65S,
                DeviceInfo.EinkDevice.JDREAD,
                DeviceInfo.EinkDevice.NOOK -> {
                    logController("Nook/NTX")
                    NookEPDController()
                }

                DeviceInfo.EinkDevice.CREMA,
                DeviceInfo.EinkDevice.HANVON_960,
                DeviceInfo.EinkDevice.NABUK,
                DeviceInfo.EinkDevice.TOLINO -> {
                    logController("Tolino/NTX")
                    TolinoEPDController()
                }

                DeviceInfo.EinkDevice.ONYX_KON_TIKI2,
		        DeviceInfo.EinkDevice.ONYX_NOTE_AIR2,
                DeviceInfo.EinkDevice.ONYX_NOVA2 -> {
                    logController("Onyx/Qualcomm")
                    OnyxEPDController()
                }

                else -> {
                    FakeEPDController()
                }
            }
        }

    private const val TAG = "EPD"

    private class FakeEPDController : EPDInterface {

        override fun getPlatform(): String {
            return "none"
        }

        override fun getMode(): String {
            return "none"
        }

        override fun needsView(): Boolean {
            return false
        }

        override fun setEpdMode(targetView: android.view.View,
                                mode: Int, delay: Long,
                                x: Int, y: Int, width: Int, height: Int, epdMode: String?) {
            return
        }

        override fun resume() {}
        override fun pause() {}
    }

    private fun logController(name: String) {
        Log.i(TAG, String.format(Locale.US,
            "Using %s driver", name))
    }
}
