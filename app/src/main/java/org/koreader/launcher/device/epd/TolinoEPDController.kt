/* Tested on Tolino Vision2/Shine3 */

package org.koreader.launcher.device.epd

import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.freescale.NTXEPDController

class TolinoEPDController : NTXEPDController(), EPDInterface {

    override fun getPlatform(): String {
        return "freescale"
    }

    override fun getMode(): String {
        return "all"
    }

    override fun needsView(): Boolean {
        return true
    }

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        requestEpdMode(targetView, mode, delay, x, y, width, height)
    }

    override fun resume() {}
    override fun pause() {}
}
