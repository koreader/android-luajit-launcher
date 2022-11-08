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
                DeviceInfo.LightsDevice.TOLINO_VISION5 -> {
                    logController("TolinoNTX")
                    TolinoNtxController()
                }
                DeviceInfo.LightsDevice.ONYX_DARWIN7,
                DeviceInfo.LightsDevice.ONYX_FAUST3,
                DeviceInfo.LightsDevice.ONYX_KON_TIKI2,
                DeviceInfo.LightsDevice.ONYX_LEAF,
                DeviceInfo.LightsDevice.ONYX_LOMONOSOV,
                DeviceInfo.LightsDevice.ONYX_NOTE3,
                DeviceInfo.LightsDevice.ONYX_NOTE_AIR,
                DeviceInfo.LightsDevice.ONYX_NOTE_PRO,
                DeviceInfo.LightsDevice.ONYX_NOVA3,
                DeviceInfo.LightsDevice.ONYX_NOVA_AIR,
                DeviceInfo.LightsDevice.ONYX_NOVA_PRO,
                DeviceInfo.LightsDevice.ONYX_POKE3,
                DeviceInfo.LightsDevice.ONYX_POKE_PRO -> {
                    logController("Onyx/Qualcomm")
                    OnyxWarmthController()
                }
                DeviceInfo.LightsDevice.ONYX_LEAF2,
                DeviceInfo.LightsDevice.ONYX_NOTE_AIR2,
                DeviceInfo.LightsDevice.ONYX_NOVA2,
                DeviceInfo.LightsDevice.ONYX_NOVA_AIR_C,
                DeviceInfo.LightsDevice.ONYX_POKE4 -> {
                    logController("Onyx/Sdk")
                    OnyxSdkLightsController()
                }
                DeviceInfo.LightsDevice.ONYX_NOVA3_COLOR -> {
                    logController("Onyx color")
                    OnyxColorController()
                }
                DeviceInfo.LightsDevice.ONYX_C67,
                DeviceInfo.LightsDevice.ONYX_MAGICBOOK,
                DeviceInfo.LightsDevice.ONYX_MONTECRISTO3 -> {
                    logController("Onyx C67")
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
