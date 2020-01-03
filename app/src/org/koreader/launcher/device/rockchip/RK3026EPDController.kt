/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device.rockchip

import org.koreader.launcher.EPDController

class RK3026EPDController : RK30xxEPDController(), EPDController {

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        requestEpdMode(targetView, epdMode!!, true)
    }
}
