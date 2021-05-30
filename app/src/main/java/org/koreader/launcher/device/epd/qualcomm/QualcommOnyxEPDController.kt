/* Tested on Onyx Boox Nova 2 */

package org.koreader.launcher.device.epd.qualcomm

import org.koreader.launcher.device.EPDInterface

class QualcommOnyxEPDController : QualcommEPDController(), EPDInterface {
    override fun resume() {}
    override fun pause() {}
    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        requestEpdMode(targetView, mode, delay, x, y, width, height)
    }
}
