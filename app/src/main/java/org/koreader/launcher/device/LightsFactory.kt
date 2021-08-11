package org.koreader.launcher.device

import android.util.Log
import org.koreader.launcher.device.lights.GenericController
import org.koreader.launcher.device.lights.OnyxWarmthController
import org.koreader.launcher.device.lights.OnyxC67Controller
import org.koreader.launcher.device.lights.TolinoWarmthController
import java.util.*

object LightsFactory {
    private const val TAG = "Lights"
    val lightsController: LightsInterface
        get() {
            return when (DeviceInfo.LIGHTS) {
                DeviceInfo.LightsDevice.TOLINO_EPOS -> {
                    logController("Tolino")
                    TolinoWarmthController()
                }
                DeviceInfo.LightsDevice.ONYX_KON_TIKI2,
                DeviceInfo.LightsDevice.ONYX_NOVA2 -> {
                    logController("Onyx/Qualcomm")
                    OnyxWarmthController()
                }
                DeviceInfo.LightsDevice.ONYX_C67 -> {
                    logController("ONYX C67")
                    OnyxC67Controller()
                }
                else -> {
                    logController("Generic")
                    GenericController()
                }
            }
        }

    private fun logController(name: String?) {
        Log.i(TAG, String.format(Locale.US,
            "Using %s driver", name))
    }
}
