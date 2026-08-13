/* RK357x EPD Controller for Rockchip RK3572/RK3576 based devices
 *
 * Applies to devices sharing the RK3576 BSP (e.g. the iflytek ebook
 * Cube 2 with its RK3572 SoC).
 *
 * Refresh channel (reversed from 讯飞阅读 cube2_release.1104.apk, 2026-08):
 *   getSystemService("ebook") -> android.os.EbookManager -> sendOneFullFrame()
 *
 * The ROM's e-ink SDK (com.iflytek.eink.EinkManagerRK3576Impl) uses the
 * "ebook" system service for EPD refresh (EbookManager.setRefreshMode(int) /
 * sendOneFullFrame()), and the per-device config class
 * DisplaySettingsCube2Delegate manages refresh quality via the
 * persist.ebook.* property family.
 *
 * Full refresh only: partial refreshes are handled by the system's
 * EinkCanvas (libeinkhwui.so, native Android 16 EPD composition),
 * so this controller reports "full-only" mode, letting KOReader
 * schedule full refreshes (FULL_REFRESH_COUNT / chapters / images)
 * while the system keeps doing partial updates on its own.
 *
 * Package: org.koreader.launcher.device.epd
 */

package org.koreader.launcher.device.epd

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import org.koreader.launcher.device.EPDInterface

class RK357xEPDController : EPDInterface {

    override fun getPlatform(): String {
        return "rk357x"
    }

    override fun getMode(): String {
        // Full refresh only: partial refreshes are handled by the
        // system's EinkCanvas (native EPD composition).
        return "full-only"
    }

    override fun getWaveformFull(): Int {
        return EPD_AUTO
    }

    override fun getWaveformPartial(): Int {
        return EPD_AUTO
    }

    override fun getWaveformFullUi(): Int {
        return EPD_AUTO
    }

    override fun getWaveformPartialUi(): Int {
        return EPD_AUTO
    }

    override fun getWaveformFast(): Int {
        return EPD_AUTO
    }

    override fun getWaveformDelay(): Int {
        return EPD_AUTO
    }

    override fun getWaveformDelayUi(): Int {
        return EPD_AUTO
    }

    override fun getWaveformDelayFast(): Int {
        return EPD_AUTO
    }

    override fun needsView(): Boolean {
        return true
    }

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?) {
        requestFullFrame(targetView.context)
    }

    override fun resume() {}
    override fun pause() {}

    companion object {
        private const val TAG = "EPD"

        const val EPD_AUTO = 0

        @SuppressLint("WrongConstant")
        fun requestFullFrame(context: Context): Boolean {
            return try {
                val ebookManagerClass = Class.forName("android.os.EbookManager")
                val ebookManager: Any? = context.getSystemService("ebook")

                val sendOneFullFrame = ebookManagerClass.getDeclaredMethod("sendOneFullFrame")
                sendOneFullFrame.invoke(ebookManager)

                true
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                Log.e(TAG, e.stackTraceToString())
                false
            }
        }
    }
}
