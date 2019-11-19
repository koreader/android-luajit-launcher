/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher

import java.util.Locale

import org.koreader.launcher.device.rockchip.RK3026EPDController
import org.koreader.launcher.device.rockchip.RK3066EPDController
import org.koreader.launcher.device.rockchip.RK3368EPDController
import org.koreader.launcher.device.freescale.NTXNewEPDController

internal object EPDFactory {
    val epdController: EPDController
        get() {
            return when (DeviceInfo.EINK) {
                DeviceInfo.EinkDevice.BOYUE_T61,
                DeviceInfo.EinkDevice.BOYUE_T62,
                DeviceInfo.EinkDevice.BOYUE_T80S,
                DeviceInfo.EinkDevice.ONYX_C67,
                DeviceInfo.EinkDevice.ENERGY,
                DeviceInfo.EinkDevice.INKBOOK -> {
                    logController("Rockchip RK3026")
                    RK3026EPDController()
                }
                DeviceInfo.EinkDevice.BOYUE_T80D,
                DeviceInfo.EinkDevice.BOYUE_T78D,
                DeviceInfo.EinkDevice.BOYUE_T103D -> {
                    logController("Rockchip RK3368")
                    RK3368EPDController()
                }
                DeviceInfo.EinkDevice.CREMA,
                DeviceInfo.EinkDevice.TOLINO,
                DeviceInfo.EinkDevice.NOOK_V520 -> {
                    logController("Freescale NTX")
                    NTXNewEPDController()
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
    private class FakeEPDController : EPDController {
        override fun setEpdMode(targetView: android.view.View,
                                mode: Int, delay: Long,
                                x: Int, y: Int, width: Int, height: Int, epdMode: String?) {
            Logger.w("[EPDController]: Unknown device, ignoring epd update")
        }
    }
}
