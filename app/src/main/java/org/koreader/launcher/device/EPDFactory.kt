/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device

import android.util.Log
import org.koreader.launcher.device.epd.NookEPDController
import org.koreader.launcher.device.epd.TolinoEPDController
import org.koreader.launcher.device.epd.RK3026EPDController
import org.koreader.launcher.device.epd.RK3368EPDController
import org.koreader.launcher.device.epd.OnyxEPDController
import org.koreader.launcher.device.epd.OldTolinoEPDController
import org.koreader.launcher.device.epd.NGL4EPDController
import org.koreader.launcher.device.epd.RK3566EPDController

import java.util.*

object EPDFactory {
    val epdController: EPDInterface
        get() {
            return when (DeviceInfo.ID) {
                DeviceInfo.Id.BOYUE_T61,
                DeviceInfo.Id.BOYUE_T62,
                DeviceInfo.Id.BOYUE_T80S,
                DeviceInfo.Id.CREMA_0650L,
                DeviceInfo.Id.ENERGY,
                DeviceInfo.Id.FIDIBOOK,
                DeviceInfo.Id.INKBOOK,
                DeviceInfo.Id.INKBOOKFOCUS,
                DeviceInfo.Id.MEEBOOK_P6,
                DeviceInfo.Id.ONYX_C67,
                DeviceInfo.Id.ONYX_MAGICBOOK,
                DeviceInfo.Id.ONYX_MONTECRISTO3,
                -> {
                    logController("Rockchip RK3026")
                    RK3026EPDController()
                }

                DeviceInfo.Id.BOYUE_C64P,
                DeviceInfo.Id.BOYUE_K78W,
                DeviceInfo.Id.BOYUE_K103,
                DeviceInfo.Id.BOYUE_P6,
                DeviceInfo.Id.BOYUE_P61,
                DeviceInfo.Id.BOYUE_P78,
                DeviceInfo.Id.BOYUE_P101,
                DeviceInfo.Id.BOYUE_S62,
                DeviceInfo.Id.BOYUE_T78D,
                DeviceInfo.Id.BOYUE_T80D,
                DeviceInfo.Id.BOYUE_T103D,
                DeviceInfo.Id.HYREAD_GAZE_NOTE,
                -> {
                    logController("Rockchip RK3368")
                    RK3368EPDController()
                }

                DeviceInfo.Id.BOYUE_T65S,
                DeviceInfo.Id.CREMA_0710C,
                DeviceInfo.Id.JDREAD,
                DeviceInfo.Id.LINFINY_ENOTE,
                DeviceInfo.Id.NOOK,
                DeviceInfo.Id.SONY_CP1,
                DeviceInfo.Id.SONY_RP1,
                -> {
                    logController("Nook/NTX")
                    NookEPDController()
                }

                DeviceInfo.Id.MOOINKPLUS2C,
                DeviceInfo.Id.NOOK_GL4,
                DeviceInfo.Id.TOLINO_EPOS3,
                DeviceInfo.Id.TOLINO_VISION6,
                -> {
                    logController("NOOK_GL4")
                    NGL4EPDController()
                }

                DeviceInfo.Id.CREMA,
                DeviceInfo.Id.CREMA_CARTA_G,
                DeviceInfo.Id.HANVON_960,
                DeviceInfo.Id.ONYX_JDREAD,
                DeviceInfo.Id.RIDI_PAPER_3,
                DeviceInfo.Id.TOLINO,
                DeviceInfo.Id.TOLINO_EPOS1,
                DeviceInfo.Id.TOLINO_EPOS2,
                DeviceInfo.Id.TOLINO_PAGE2,
                DeviceInfo.Id.TOLINO_SHINE3,
                DeviceInfo.Id.TOLINO_VISION4,
                DeviceInfo.Id.TOLINO_VISION5,
                -> {
                    logController("Tolino/NTX")
                    TolinoEPDController()
                }

                DeviceInfo.Id.ONYX_DARWIN9,
                DeviceInfo.Id.ONYX_EDISON,
                DeviceInfo.Id.ONYX_GALILEO2,
                DeviceInfo.Id.ONYX_GO_103,
                DeviceInfo.Id.ONYX_GO6,
                DeviceInfo.Id.ONYX_GO_COLOR7,
                DeviceInfo.Id.ONYX_KON_TIKI2,
                DeviceInfo.Id.ONYX_LEAF,
                DeviceInfo.Id.ONYX_LEAF2,
                DeviceInfo.Id.ONYX_LIVINGSTONE3,
                DeviceInfo.Id.ONYX_LOMONOSOV,
                DeviceInfo.Id.ONYX_MAX,
                DeviceInfo.Id.ONYX_MAX2_PRO,
                DeviceInfo.Id.ONYX_NOTE,
                DeviceInfo.Id.ONYX_NOTE3,
                DeviceInfo.Id.ONYX_NOTE4,
                DeviceInfo.Id.ONYX_NOTE5,
                DeviceInfo.Id.ONYX_NOTE_AIR,
                DeviceInfo.Id.ONYX_NOTE_AIR2,
                DeviceInfo.Id.ONYX_NOTE_AIR_3C,
                DeviceInfo.Id.ONYX_NOTE_AIR_4C,
                DeviceInfo.Id.ONYX_NOTE_MAX,
                DeviceInfo.Id.ONYX_NOTE_PRO,
                DeviceInfo.Id.ONYX_NOTE_X2,
                DeviceInfo.Id.ONYX_NOVA,
                DeviceInfo.Id.ONYX_NOVA2,
                DeviceInfo.Id.ONYX_NOVA3,
                DeviceInfo.Id.ONYX_NOVA3_COLOR,
                DeviceInfo.Id.ONYX_NOVA_AIR,
                DeviceInfo.Id.ONYX_NOVA_AIR_2,
                DeviceInfo.Id.ONYX_NOVA_AIR_C,
                DeviceInfo.Id.ONYX_NOVA_PRO,
                DeviceInfo.Id.ONYX_PAGE,
                DeviceInfo.Id.ONYX_PALMA,
                DeviceInfo.Id.ONYX_PALMA2,
                DeviceInfo.Id.ONYX_POKE2,
                DeviceInfo.Id.ONYX_POKE3,
                DeviceInfo.Id.ONYX_POKE4,
                DeviceInfo.Id.ONYX_POKE5,
                DeviceInfo.Id.ONYX_POKE6,
                DeviceInfo.Id.ONYX_POKE4LITE,
                DeviceInfo.Id.ONYX_POKE_PRO,
                DeviceInfo.Id.ONYX_TAB_ULTRA,
                DeviceInfo.Id.ONYX_TAB_ULTRA_C,
                DeviceInfo.Id.ONYX_TAB_ULTRA_C_PRO,
                DeviceInfo.Id.STORYTEL_READER2,
                -> {
                    logController("Onyx/Qualcomm")
                    OnyxEPDController()
                }

                DeviceInfo.Id.NABUK,
                DeviceInfo.Id.ONYX_DARWIN7,
                DeviceInfo.Id.ONYX_FAUST3,
                DeviceInfo.Id.TAGUS_GEA,
                -> {
                    logController("Old Tolino/NTX")
                    OldTolinoEPDController()
                }

                DeviceInfo.Id.HYREAD_GAZE_NOTE_CC,
                DeviceInfo.Id.HYREAD_MINI6,
                DeviceInfo.Id.INKBOOKFOCUS_PLUS,
                DeviceInfo.Id.INKPALM_PLUS,
                DeviceInfo.Id.MEEBOOK_M6,
                DeviceInfo.Id.MEEBOOK_M6C,
                DeviceInfo.Id.MEEBOOK_M7,
                DeviceInfo.Id.MOAAN_MIX7,
                DeviceInfo.Id.OBOOK_P78D,
                DeviceInfo.Id.PUBU_PUBOOK,
                DeviceInfo.Id.XIAOMI_READER,
                -> {
                    logController("Rockchip RK3566")
                    RK3566EPDController()
                }

                else -> {
                    FakeEPDController()
                }
            }
        }

    private const val TAG = "EPD"

    private class FakeEPDController : EPDInterface {

        override fun getPlatform(): String {
            return "none"
        }

        override fun getWaveformFull(): Int {
            return 0
        }

        override fun getWaveformPartial(): Int {
            return 0
        }

        override fun getWaveformFullUi(): Int {
            return 0
        }

        override fun getWaveformPartialUi(): Int {
            return 0
        }

        override fun getWaveformFast(): Int {
            return 0
        }

        override fun getWaveformDelay(): Int {
            return 0
        }

        override fun getWaveformDelayUi(): Int {
            return 0
        }

        override fun getWaveformDelayFast(): Int {
            return 0
        }

        override fun getMode(): String {
            return "none"
        }

        override fun needsView(): Boolean {
            return false
        }

        override fun setEpdMode(targetView: android.view.View,
                                mode: Int, delay: Long,
                                x: Int, y: Int, width: Int, height: Int, epdMode: String?) {
            return
        }

        override fun resume() {}
        override fun pause() {}
    }

    private fun logController(name: String) {
        Log.i(TAG, String.format(Locale.US,
            "Using %s driver", name))
    }
}
