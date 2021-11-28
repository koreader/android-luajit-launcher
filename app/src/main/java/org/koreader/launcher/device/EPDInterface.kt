/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device

interface EPDInterface {
    fun getMode(): String
    fun getPlatform(): String

    fun getWaveformFull(): Int
    fun getWaveformPartial(): Int
    fun getWaveformFullUi(): Int
    fun getWaveformPartialUi(): Int
    fun getWaveformFast(): Int
    fun getWaveformDelay(): Int
    fun getWaveformDelayUi(): Int
    fun getWaveformDelayFast(): Int

    fun needsView(): Boolean

    fun setEpdMode(targetView: android.view.View,
                   mode: Int, delay: Long,
                   x: Int, y: Int, width: Int, height: Int, epdMode: String?)

    fun resume()
    fun pause()
}
