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

    // Optional custom surface for devices that need a specific SurfaceView
    // implementation (e.g. the iReader HWSurfaceView). Returning null lets
    // MainActivity fall back to its own NativeSurfaceView.
    fun createSurface(context: android.content.Context): android.view.SurfaceView? = null

    // Whether the surface should use an opaque pixel format (default transparent).
    fun usesOpaquePixelFormat(): Boolean = false

    // Prepare a hardware page-turn animation; default is a no-op.
    fun prepareRipple(effect: Int) {}

    fun setEpdMode(targetView: android.view.View,
                   mode: Int, delay: Long,
                   x: Int, y: Int, width: Int, height: Int, epdMode: String?)

    fun resume()
    fun pause()
}
