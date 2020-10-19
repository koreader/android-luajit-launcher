package org.koreader.launcher.device.epd.onyx

import java.lang.Thread

import android.util.Log
import android.view.View

import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import com.onyx.android.sdk.api.device.epd.UpdateScheme
import com.onyx.android.sdk.device.SDMDevice
import org.koreader.launcher.interfaces.EPDInterface

class OnyxEPDController : EPDInterface {
    override fun setEpdMode(targetView: android.view.View,
                            mode: Int, delay: Long,
                            x: Int, y: Int, width: Int, height: Int, epdMode: String?)
    {
//        EpdController.setSystemUpdateModeAndScheme(UpdateMode.None, UpdateScheme.None, 0)
        Class.forName("android.view.View").getMethod("setWaveformAndScheme", Integer.TYPE, Integer.TYPE, Integer.TYPE).invoke(targetView, 5, 1, 0)
        object: Thread(){
            override fun run(){
                Thread.sleep(delay)
                Class.forName("android.view.View").getMethod("refreshScreen",
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE
                    ).invoke(targetView, x, y, width, height, 98)
            }
        }.start()
    }
}
