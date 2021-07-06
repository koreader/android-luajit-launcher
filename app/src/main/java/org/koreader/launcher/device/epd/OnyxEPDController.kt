/* Tested on Onyx Boox Nova 2 */

package org.koreader.launcher.device.epd

import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.epd.qualcomm.QualcommEPDController

class OnyxEPDController : QualcommEPDController(), EPDInterface {

    override fun getPlatform(): String {
        return "qualcomm"
    }

    override fun getMode(): String {
        return "full-only"
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
