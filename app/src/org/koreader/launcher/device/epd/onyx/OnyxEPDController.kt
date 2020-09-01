
/* generic EPD Controller for Android,
 *
 * interface for newer ntx devices, like Nooks and Tolinos.
 * based on https://github.com/koreader/koreader/issues/3517
 *
 * Thanks to @char11
 *
 *	val EINK_DITHER_COLOR_Y1 = 268435456;
 *	val EINK_DITHER_COLOR_Y4 = 0;
 *	val EINK_DITHER_MODE_DITHER = 256;
 *	val EINK_DITHER_MODE_NODITHER = 0;
 *	val EINK_MONOCHROME_MODE_MONOCHROME = 2048;
 *	val EINK_MONOCHROME_MODE_NOMONOCHROME = 0;
 *	val EINK_UPDATE_MODE_FULL = 32;
 *	val EINK_UPDATE_MODE_PARTIAL = 0;
 *	val EINK_WAIT_MODE_NOWAIT = 0;
 *	val EINK_WAIT_MODE_WAIT = 64;
 *	val EINK_WAVEFORM_MODE_A2 = 5;
 *	val EINK_WAVEFORM_MODE_AUTO = 4;
 *	val EINK_WAVEFORM_MODE_DU = 1;
 *	val EINK_WAVEFORM_MODE_GC16 = 2;
 *	val EINK_WAVEFORM_MODE_GC4 = 3;
 *	val EINK_WAVEFORM_MODE_GL16 = 6;
 *	val EINK_WAVEFORM_MODE_GLD16 = 8;
 *	val EINK_WAVEFORM_MODE_GLR16 = 7;
 *	val EINK_WAVEFORM_MODE_INIT = 0;
 */

package org.koreader.launcher.device.epd.onyx

import java.lang.Thread

import android.util.Log
import android.view.View

import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode

import org.koreader.launcher.interfaces.EPDInterface

class OnyxEPDController : EPDInterface {
    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        val updateMode = when(mode){
            1 -> UpdateMode.DU
            2 -> UpdateMode.GC
            3 -> UpdateMode.GC4
            4 -> UpdateMode.GC
            5 -> UpdateMode.ANIMATION_QUALITY
            7 -> UpdateMode.ANIMATION_QUALITY
            else -> UpdateMode.GC
        }
        object : Thread(){
            override fun run(){
                EpdController.enableScreenUpdate(targetView, true)
                Thread.sleep(delay + 100)
                EpdController.refreshScreenRegion(targetView, x, y, x + width, y + height, updateMode)
                Thread.sleep(100)
                EpdController.enableScreenUpdate(targetView, false)
//                 object : Thread(){
//                     override fun run(){
//                         Thread.sleep(1000)
//                         EpdController.enableScreenUpdate(targetView, true)
//                     }
//                 }.start()
            }
        }.start()
    }
}
