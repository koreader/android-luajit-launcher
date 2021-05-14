/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device

import org.koreader.launcher.Logger
import org.koreader.launcher.device.epd.freescale.NTXNewEPDController
import org.koreader.launcher.device.epd.rockchip.RK3026EPDController
import org.koreader.launcher.device.epd.rockchip.RK3368EPDController
import org.koreader.launcher.device.epd.qualcomm.QualcommOnyxEPDController
import org.koreader.launcher.interfaces.EPDInterface
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
                DeviceInfo.EinkDevice.BOYUE_T80D,
                DeviceInfo.EinkDevice.BOYUE_T78D,
                DeviceInfo.EinkDevice.BOYUE_T103D,
                DeviceInfo.EinkDevice.BOYUE_K103,
                DeviceInfo.EinkDevice.BOYUE_K78W,
                DeviceInfo.EinkDevice.BOYUE_P6 -> {
                    logController("Rockchip RK3368")
                    RK3368EPDController()
                }
                DeviceInfo.EinkDevice.BOYUE_T65S,
                DeviceInfo.EinkDevice.CREMA,
                DeviceInfo.EinkDevice.JDREAD,
                DeviceInfo.EinkDevice.TOLINO,
                DeviceInfo.EinkDevice.NOOK_V520 -> {
                    logController("Freescale NTX")
                    NTXNewEPDController()
                }
                DeviceInfo.EinkDevice.ONYX_NOVA2 -> {
                    logController("Qualcomm")
                    QualcommOnyxEPDController()
                }
                else -> {
                    FakeEPDController()
                }
            }
        }

    private fun logController(name: String?) {
        Logger.i(String.format(Locale.US,
            "[EPDFactory]: Using %s EPD Controller", name))
    }
    private class FakeEPDController : EPDInterface {
        override fun resume() {}
        override fun pause() {}
        override fun setEpdMode(targetView: android.view.View,
                                mode: Int, delay: Long,
                                x: Int, y: Int, width: Int, height: Int, epdMode: String?) {
            Logger.w("[EPDController]: Unknown device, ignoring epd update")
        }
    }
}
