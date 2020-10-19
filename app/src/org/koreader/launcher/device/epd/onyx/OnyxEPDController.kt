package org.koreader.launcher.device.epd.onyx

import java.lang.Thread

import android.util.Log
import android.view.View

import org.koreader.launcher.interfaces.EPDInterface

class OnyxEPDController : EPDInterface {
    fun preventSystemRefresh(){
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
                Class.forName("android.view.View").getMethod("refreshScreen",
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE
                    ).invoke(targetView, x, y, width, height, 6)
            }
        }.start()
    }
}
