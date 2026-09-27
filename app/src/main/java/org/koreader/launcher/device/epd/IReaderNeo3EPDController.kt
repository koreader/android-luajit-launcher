package org.koreader.launcher.device.epd

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import android.view.View
import org.koreader.launcher.device.DeviceInfo
import org.koreader.launcher.device.EPDInterface

/**
 * iReader Neo 3 Ultra (RM06L) eink controller.
 *
 * The device plays a hardware PAGE_H "water ripple" when
 * `EPDCDevice.nativePostCommand("next-effect-type N")` is posted right
 * before the next surface commit, hence the HWSurfaceView below.
 */
class IReaderNeo3EPDController : EPDInterface {
    companion object {
        private const val TAG = "EPD"
        // 0x01000002: FBSurface.invalidate mode the OEM uses to commit a full frame.
        private const val INVALIDATE_MODE = 16777218

        // HWSurfaceView is needed for EPDCDevice.nativePostCommand PAGE_H ripple
        fun usesOpaquePixelFormat(): Boolean =
            DeviceInfo.ID == DeviceInfo.Id.IREADER_NEO3_ULTRA

        fun createSurface(context: Context): SurfaceView? {
            if (DeviceInfo.ID != DeviceInfo.Id.IREADER_NEO3_ULTRA) return null
            return try {
                val cls = Class.forName("android.eink.view.HWSurfaceView")
                cls.getConstructor(Context::class.java).newInstance(context) as SurfaceView
            } catch (t: Throwable) {
                Log.w(TAG, "HWSurfaceView unavailable, fallback NativeSurfaceView", t)
                null
            }
        }

        fun prepareRipple(effect: Int) {
            if (effect == 0) return
            try {
                val epdc = Class.forName("android.eink.EPDCDevice")
                val post = epdc.getMethod("nativePostCommand", String::class.java)
                post.invoke(null, "next-effect-type $effect")
                Log.i(TAG, "next-effect-type $effect")
            } catch (t: Throwable) {
                Log.e(TAG, "nativePostCommand failed", t)
            }
        }
    }

    override fun getPlatform(): String = "ireader"
    override fun getMode(): String = "all"
    override fun needsView(): Boolean = true

    override fun getWaveformFull(): Int = 1
    override fun getWaveformPartial(): Int = 2
    override fun getWaveformFullUi(): Int = 3
    override fun getWaveformPartialUi(): Int = 4
    override fun getWaveformFast(): Int = 5

    override fun getWaveformDelay(): Int = 0
    override fun getWaveformDelayUi(): Int = 0
    override fun getWaveformDelayFast(): Int = 0

    override fun resume() {}
    override fun pause() {}

    override fun setEpdMode(
        targetView: View,
        mode: Int,
        delay: Long,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        epdMode: String?
    ) {
        // Commit the frame prepared by prepareRipple() through the EPD surface.
        try {
            val fb = targetView.javaClass.getMethod("getFBSurface").invoke(targetView)
                ?: return
            try {
                fb.javaClass.getMethod(
                    "invalidate",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                ).invoke(fb, x, y, width, height, INVALIDATE_MODE)
            } catch (_: Throwable) {
                fb.javaClass.getMethod("invalidate").invoke(fb)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "FBSurface invalidate skipped: ${t.message}")
        }
    }
}
