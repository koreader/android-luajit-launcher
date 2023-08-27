package org.koreader.launcher.device.epd.qualcomm

import android.util.Log
import android.view.View
import java.util.*

// More information including epd mode values
// https://github.com/koreader/android-luajit-launcher/pull/250#issuecomment-711443457
abstract class QualcommEPDController {
    companion object {
        private const val TAG = "EPD"
        const val EINK_WAVEFORM_UPDATE_FULL = 32
        const val EINK_WAVEFORM_UPDATE_PARTIAL = 0
        const val EINK_WAVEFORM_MODE_WAIT = 64
        const val EINK_WAVEFORM_MODE_DU = 1
        const val EINK_WAVEFORM_MODE_GC16 = 2
        const val EINK_WAVEFORM_MODE_REAGL = 6
        const val EINK_WAVEFORM_DELAY = 250
        const val EINK_WAVEFORM_DELAY_UI = 100
        const val EINK_WAVEFORM_DELAY_FAST = 0
        const val UI_GC_MODE = 98 // This flag seems to be able to trigger a refresh for all modes when using the repaintEverything method.

        // The refreshScreen method is causing a problem in ULTRAC, possibly others
        // When we switch to one of the modes: Balanced Mode, Fast Mode, or Ultrafast Mode,
        // we end up stuck in a fast mode that persists even if we return to Regal or HD Mode.
        // This refresh mode is annoying when reading a cbz file,
        // as it displays a white screen before each refresh, disrupting the reading experience.
        // References:
        // - https://github.com/onyx-intl/OnyxAndroidDemo/blob/master/doc/EPD-Screen-Update.md
        // - https://help.boox.com/hc/en-us/articles/10701257029780-Refresh-Modes
        // It seems that using the repaintEverything method solves the issue.
        // The SDK also employs this method for full refresh.
        fun requestEpdMode(targetView: View,
                                mode: Int, delay: Long,
                                x: Int, y: Int, width: Int, height: Int) : Boolean
        {
            return try {
                // We need to always call this, not sure why, if it's not called before
                // system will refresh after us, it'll refresh anyway if user set
                // Normal mode, or Regal mode works flawlessly otherwise

                // The setWaveformAndScheme on preventSystemRefresh() method does not exist for SDM devices (Bengal).
                // We will no longer use preventSystemRefresh() here.
                //preventSystemRefresh()

                val repaintEverything = Class.forName("android.view.View").getMethod("repaintEverything",
                                        Integer.TYPE)

                object: Thread(){
                    override fun run(){
                        Log.e(TAG, "Delay: " + delay)
                        try {
                            // 500 ms hardcoded that comes from framebuffer_android.lua seems to be to much here IMHO
                            // After some tests with a file the refresh fails to occur some value between 100-150 ms
                            // We can use de EINK_WAVEFORM_DELAY 250 ms if using repaintEverything
                            // Sad we need some delay anyway
                            sleep(EINK_WAVEFORM_DELAY.toLong())
                            repaintEverything.invoke(targetView, UI_GC_MODE)
                            Log.i(TAG, String.format(Locale.US,
                                "requested eink refresh, type: %d x:%d y:%d w:%d h:%d",
                                mode, x, y, width, height))
                        } catch (e: Exception) {
                            Log.e(TAG, e.toString())
                        }
                    }
                }.start()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
