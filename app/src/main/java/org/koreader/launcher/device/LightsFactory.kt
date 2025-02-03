package org.koreader.launcher.device

import android.util.Log
import org.koreader.launcher.device.lights.*
import java.util.*

object LightsFactory {
    private const val TAG = "Lights"
    val lightsController: LightsInterface
        get() {
            return when (DeviceInfo.ID) {
                DeviceInfo.Id.BOYUE_S62,
                -> {
                    logController("Boyue S62")
                    BoyueS62RootController()
                }
                DeviceInfo.Id.ONYX_GO_COLOR7,
                DeviceInfo.Id.ONYX_NOTE_AIR_3C,
                DeviceInfo.Id.ONYX_NOVA_AIR,
                DeviceInfo.Id.ONYX_PAGE,
                DeviceInfo.Id.ONYX_POKE5,
                DeviceInfo.Id.ONYX_TAB_ULTRA_C,
                DeviceInfo.Id.ONYX_TAB_ULTRA_C_PRO,
                -> {
                    logController("Onyx Adb")
                    OnyxAdbLightsController()
                }
                DeviceInfo.Id.ONYX_C67,
                DeviceInfo.Id.ONYX_MAGICBOOK,
                DeviceInfo.Id.ONYX_MONTECRISTO3,
                -> {
                    logController("Onyx C67")
                    OnyxC67Controller()
                }
                DeviceInfo.Id.ONYX_JDREAD,
                DeviceInfo.Id.ONYX_NOVA3_COLOR,
                DeviceInfo.Id.TAGUS_GEA,
                -> {
                    logController("Onyx Color")
                    OnyxColorController()
                }
                DeviceInfo.Id.ONYX_DARWIN7,
                DeviceInfo.Id.ONYX_EDISON,
                DeviceInfo.Id.ONYX_FAUST3,
                DeviceInfo.Id.ONYX_KON_TIKI2,
                DeviceInfo.Id.ONYX_LEAF,
                DeviceInfo.Id.ONYX_LOMONOSOV,
                DeviceInfo.Id.ONYX_NOTE3,
                DeviceInfo.Id.ONYX_NOTE_AIR,
                DeviceInfo.Id.ONYX_NOTE_PRO,
                DeviceInfo.Id.ONYX_NOVA3,
                DeviceInfo.Id.ONYX_NOVA_PRO,
                DeviceInfo.Id.ONYX_POKE2,
                DeviceInfo.Id.ONYX_POKE3,
                DeviceInfo.Id.ONYX_POKE_PRO,
                -> {
                    logController("Onyx/Qualcomm")
                    OnyxWarmthController()
                }
                DeviceInfo.Id.ONYX_DARWIN9,
                DeviceInfo.Id.ONYX_LEAF2,
                DeviceInfo.Id.ONYX_LIVINGSTONE3,
                DeviceInfo.Id.ONYX_NOTE4,
                DeviceInfo.Id.ONYX_NOTE_AIR2,
                DeviceInfo.Id.ONYX_NOTE_X2,
                DeviceInfo.Id.ONYX_NOVA,
                DeviceInfo.Id.ONYX_NOVA2,
                DeviceInfo.Id.ONYX_NOVA_AIR_2,
                DeviceInfo.Id.ONYX_NOVA_AIR_C,
                DeviceInfo.Id.ONYX_POKE4,
                DeviceInfo.Id.ONYX_POKE4LITE,
                DeviceInfo.Id.ONYX_TAB_ULTRA,
                DeviceInfo.Id.STORYTEL_READER2,
                -> {
                    logController("Onyx/Sdk")
                    OnyxSdkLightsController()
                }
                DeviceInfo.Id.CREMA_0710C,
                DeviceInfo.Id.CREMA_CARTA_G,
                DeviceInfo.Id.MEEBOOK_P6,
                DeviceInfo.Id.RIDI_PAPER_3,
                DeviceInfo.Id.TOLINO_EPOS1,
                DeviceInfo.Id.TOLINO_SHINE3,
                DeviceInfo.Id.TOLINO_VISION4,
                DeviceInfo.Id.TOLINO_VISION5,
                -> {
                    logController("TolinoNTX")
                    TolinoNtxController()
                }
                DeviceInfo.Id.TOLINO_PAGE2,
                -> {
                    logController("TolinoNTXNoWarmth")
                    TolinoNtxNoWarmthController()
                }
                DeviceInfo.Id.NOOK_GL4,
                DeviceInfo.Id.TOLINO_EPOS2,
                -> {
                    logController("TolinoRoot")
                    TolinoRootController()
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
