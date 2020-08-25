package org.koreader.launcher.device

import org.koreader.launcher.interfaces.LightInterface
import java.util.Locale

import org.koreader.launcher.device.lights.GenericController
import org.koreader.launcher.device.lights.TolinoWarmthController
import org.koreader.launcher.device.lights.OnyxWarmthController
import org.koreader.launcher.utils.Logger

object LightsFactory {
    val lightsController: LightInterface
        get() {
            return when (DeviceInfo.LIGHTS) {
                DeviceInfo.LightsDevice.TOLINO_EPOS -> {
                    logController("Tolino")
                    TolinoWarmthController()
                }
                DeviceInfo.LightsDevice.ONYX_NOVA2 -> {
                    logController("ONYX_NOVA2")
                    OnyxWarmthController()
                }
                else -> {
                    logController("Generic")
                    GenericController()
                }
            }
        }

    private fun logController(name: String?) {
        Logger.i(String.format(Locale.US,
            "[LightsFactory]: Using %s Lights Controller", name))
    }
}
