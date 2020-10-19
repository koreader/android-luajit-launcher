/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.interfaces

interface EPDInterface {
    fun setEpdMode(targetView: android.view.View,
                   mode: Int, delay: Long,
                   x: Int, y: Int, width: Int, height: Int, epdMode: String?)
}
