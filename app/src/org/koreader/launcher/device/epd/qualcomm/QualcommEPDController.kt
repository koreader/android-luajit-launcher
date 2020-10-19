package org.koreader.launcher.device.epd.qualcomm

import java.lang.Thread

import android.view.View

import org.koreader.launcher.interfaces.EPDInterface

// More information including epd mode values
// https://github.com/koreader/android-luajit-launcher/pull/250#issuecomment-711443457
class QualcommEPDController : EPDInterface {
    fun preventSystemRefresh(){
        // Sets UpdateMode and UpdateScheme to None
        // this function is called EpdController.setSystemUpdateModeAndScheme in onyxsdk
        Class.forName("android.view.View").getMethod("setWaveformAndScheme",
            Integer.TYPE,
            Integer.TYPE,
            Integer.TYPE).invoke(null, 5, 1, 0)
    }

    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
        preventSystemRefresh()
        object: Thread(){
            override fun run(){
                Thread.sleep(delay)
                // EpdController.refreshScreenRegion in onyxsdk
                Class.forName("android.view.View").getMethod("refreshScreen",
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE
                    ).invoke(targetView, x, y, width, height, mode)
            }
        }.start()
    }
}
