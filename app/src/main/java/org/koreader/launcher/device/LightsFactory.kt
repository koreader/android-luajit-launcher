package org.koreader.launcher.device

import android.util.Log
import org.koreader.launcher.device.lights.GenericController
import org.koreader.launcher.device.lights.OnyxWarmthController
import org.koreader.launcher.device.lights.OnyxC67Controller
import org.koreader.launcher.device.lights.TolinoWarmthController
import org.koreader.launcher.interfaces.LightInterface
import java.util.*

object LightsFactory {
    private const val TAG = "Lights"
    val lightsController: LightInterface
        get() {
            return when (DeviceInfo.LIGHTS) {
                DeviceInfo.LightsDevice.TOLINO_EPOS -> {
                    logController("Tolino")
                    TolinoWarmthController()
                }
                DeviceInfo.LightsDevice.ONYX_NOVA2 -> {
                    logController("Onyx Nova 2")
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
