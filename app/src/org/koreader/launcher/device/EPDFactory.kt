/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device

import java.util.Locale

import org.koreader.launcher.Logger
import org.koreader.launcher.device.rockchip.RK3026EPDController
import org.koreader.launcher.device.rockchip.RK3066EPDController
import org.koreader.launcher.device.rockchip.RK3368EPDController
import org.koreader.launcher.device.freescale.NTXNewEPDController


object EPDFactory {

    private class FakeEPDController : EPDController {
        override fun setEpdMode(targetView: android.view.View, mode: Int,
            delay: Long, x: Int, y: Int, width: Int, height: Int, epdMode: String) {
            Logger.w("[EPDController]: Unknown device, ignoring epd update")
        }
    }

    val epdController: EPDController
        get() {
            val epdController: EPDController
            val controllerName: String
            when (DeviceInfo.EINK) {
                DeviceInfo.EinkDevice.BOYUE_T61,
                DeviceInfo.EinkDevice.BOYUE_T80S,
                DeviceInfo.EinkDevice.ONYX_C67,
                DeviceInfo.EinkDevice.ENERGY,
                DeviceInfo.EinkDevice.INKBOOK -> {
                    controllerName = "Rockchip RK3026"
                    epdController = RK3026EPDController()
                }
                DeviceInfo.EinkDevice.BOYUE_T62 -> {
                    controllerName = "Rockchip RK3066"
                    epdController = RK3066EPDController()
                }
                DeviceInfo.EinkDevice.BOYUE_T80D,
                DeviceInfo.EinkDevice.BOYUE_T78D,
                DeviceInfo.EinkDevice.BOYUE_T103D -> {
                    controllerName = "Rockchip RK3368"
                    epdController = RK3368EPDController()
                }
                DeviceInfo.EinkDevice.CREMA,
                DeviceInfo.EinkDevice.TOLINO,
                DeviceInfo.EinkDevice.NOOK_V520 -> {
                    controllerName = "Freescale NTX"
                    epdController = NTXNewEPDController()
                }
                else -> {
                    controllerName = "none"
                    epdController = FakeEPDController()
                }
            }
            if (controllerName != "none") {
                Logger.i(String.format(Locale.US,
                        "[EPDFactory]: Using %s EPD Controller", controllerName))
            }
            return epdController
        }
}
