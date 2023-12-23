package org.koreader.launcher.device

import android.util.Log
import org.koreader.launcher.device.lights.*
import java.util.*

object LightsFactory {
    private const val TAG = "Lights"
    val lightsController: LightsInterface
        get() {
            return when (DeviceInfo.LIGHTS) {
                DeviceInfo.LightsDevice.TOLINO_EPOS -> {
                    logController("TolinoRoot")
                    TolinoRootController()
                }
                DeviceInfo.LightsDevice.CREMA_CARTA_G,
                DeviceInfo.LightsDevice.MEEBOOK_P6,
                DeviceInfo.LightsDevice.RIDI_PAPER_3,
                DeviceInfo.LightsDevice.TOLINO_SHINE3,
                DeviceInfo.LightsDevice.TOLINO_VISION4,
                DeviceInfo.LightsDevice.TOLINO_VISION5 -> {
                    logController("TolinoNTX")
                    TolinoNtxController()
                }
                DeviceInfo.LightsDevice.ONYX_DARWIN7,
                DeviceInfo.LightsDevice.ONYX_FAUST3,
                DeviceInfo.LightsDevice.ONYX_WARMTH -> {
                    logController("Onyx/Qualcomm")
                    OnyxWarmthController()
                }
                DeviceInfo.LightsDevice.ONYX_NOVA_AIR_C,
                DeviceInfo.LightsDevice.ONYX_SDK -> {
                    logController("Onyx/Sdk")
                    OnyxSdkLightsController()
                }
                DeviceInfo.LightsDevice.ONYX_NOVA3_COLOR,
                DeviceInfo.LightsDevice.ONYX_TAGUS_GEA -> {
                    logController("Onyx color")
                    OnyxColorController()
                }
                DeviceInfo.LightsDevice.ONYX_C67,
                DeviceInfo.LightsDevice.ONYX_MAGICBOOK,
                DeviceInfo.LightsDevice.ONYX_MONTECRISTO3 -> {
                    logController("Onyx C67")
                    OnyxC67Controller()
                }
                DeviceInfo.LightsDevice.BOYUE_S62 -> {
                    logController("Boyue S62")
                    BoyueS62RootController()
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
