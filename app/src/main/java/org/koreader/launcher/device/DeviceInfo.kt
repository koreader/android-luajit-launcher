/* Device info using Android build properties,
 * based on https://github.com/unwmun/refreshU
 *
 * Note: devices don't need to be declared here unless
 * they have known e-ink update routines, custom light settings  and/or bug workarounds. */

package org.koreader.launcher.device

import android.os.Build
import kotlin.collections.HashMap
import java.util.Locale

object DeviceInfo {
    val MANUFACTURER: String
    val BRAND: String
    val MODEL: String
    val DEVICE: String
    val PRODUCT: String
    val HARDWARE: String

    // known quirks
    val QUIRK_BROKEN_LIFECYCLE: Boolean
    val QUIRK_NEEDS_WAKELOCKS: Boolean
    val QUIRK_NO_HW_ROTATION: Boolean
    val QUIRK_NO_LIGHTS: Boolean

    val HAS_COLOR_SCREEN: Boolean

    enum class EinkDevice {
        NONE,
        BOYUE_K78W,
        BOYUE_K103,
        BOYUE_P6,
        BOYUE_P61,
        BOYUE_P78,
        BOYUE_S62,
        BOYUE_T61,
        BOYUE_T62,
        BOYUE_T65S,
        BOYUE_T78D,
        BOYUE_T80D,
        BOYUE_T80S,
        BOYUE_T103D,
        CREMA,
        CREMA_0650L,
        CREMA_CARTA_G,
        ENERGY,
        FIDIBOOK,
        HANVON_960,
        INKBOOK,
        JDREAD,
        MEEBOOK_P6,
        NABUK,
        NOOK,
        ONYX_C67,
        ONYX_DARWIN7,
        ONYX_DARWIN9,
        ONYX_EDISON,
        ONYX_FAUST3,
        ONYX_KON_TIKI2,
        ONYX_LEAF,
        ONYX_LEAF2,
        ONYX_LOMONOSOV,
        ONYX_MAGICBOOK,
        ONYX_MAX,
        ONYX_MONTECRISTO3,
        ONYX_NOTE,
        ONYX_NOTE3,
        ONYX_NOTE5,
        ONYX_NOTE_AIR,
        ONYX_NOTE_AIR2,
        ONYX_NOTE_PRO,
        ONYX_NOTE_X2,
        ONYX_NOVA2,
        ONYX_NOVA3,
        ONYX_NOVA3_COLOR,
        ONYX_NOVA_AIR,
        ONYX_NOVA_AIR_2,
        ONYX_NOVA_AIR_C,
        ONYX_NOVA_PRO,
        ONYX_PAGE,
        ONYX_POKE3,
        ONYX_POKE4,
        ONYX_POKE4LITE,
        ONYX_POKE_PRO,
        ONYX_TAB_ULTRA,
        ONYX_TAB_ULTRA_C,
        RIDI_PAPER_3,
        SONY_CP1,
        SONY_RP1,
        TAGUS_GEA,
        TOLINO
    }

    enum class LightsDevice {
        NONE,
        BOYUE_S62,
        CREMA_CARTA_G,
        MEEBOOK_P6,
        ONYX_C67,
        ONYX_DARWIN7,
        ONYX_DARWIN9,
        ONYX_EDISON,
        ONYX_FAUST3,
        ONYX_KON_TIKI2,
        ONYX_LEAF,
        ONYX_LEAF2,
        ONYX_LOMONOSOV,
        ONYX_MAGICBOOK,
        ONYX_MONTECRISTO3,
        ONYX_NOTE3,
        ONYX_NOTE_AIR,
        ONYX_NOTE_AIR2,
        ONYX_NOTE_PRO,
        ONYX_NOTE_X2,
        ONYX_NOVA2,
        ONYX_NOVA3,
        ONYX_NOVA3_COLOR,
        ONYX_NOVA_AIR,
        ONYX_NOVA_AIR_2,
        ONYX_NOVA_AIR_C,
        ONYX_NOVA_PRO,
        ONYX_PAGE,
        ONYX_POKE3,
        ONYX_POKE4,
        ONYX_POKE4LITE,
        ONYX_POKE_PRO,
        ONYX_TAB_ULTRA,
        ONYX_TAB_ULTRA_C,
        RIDI_PAPER_3,
        TAGUS_GEA,
        TOLINO_EPOS,
        TOLINO_SHINE3,
        TOLINO_VISION5
    }

    enum class QuirkDevice {
        NONE,
        EMULATOR,
        ONYX_MAX,
        ONYX_NOTE,
        ONYX_POKE2,
        SONY_CP1,
        SONY_RP1
    }

    // default values for generic devices.
    internal var EINK = EinkDevice.NONE
    internal var LIGHTS = LightsDevice.NONE
    private var QUIRK = QuirkDevice.NONE

    internal val BOYUE: Boolean
    internal val TOLINO: Boolean

    // device probe
    private val BOYUE_K78W: Boolean
    private val BOYUE_K103: Boolean
    private val BOYUE_P6: Boolean
    private val BOYUE_P61: Boolean
    private val BOYUE_P78: Boolean
    private val BOYUE_S62: Boolean
    private val BOYUE_T61: Boolean
    private val BOYUE_T62: Boolean
    private val BOYUE_T65S: Boolean
    private val BOYUE_T78D: Boolean
    private val BOYUE_T80D: Boolean
    private val BOYUE_T80S: Boolean
    private val BOYUE_T103D: Boolean
    private val CREMA: Boolean
    private val CREMA_0650L: Boolean
    private val CREMA_CARTA_G: Boolean
    private val EMULATOR_X86: Boolean
    private val ENERGY: Boolean
    private val FIDIBOOK: Boolean
    private val HANVON_960: Boolean
    private val INKBOOK: Boolean
    private val JDREAD: Boolean
    private val MEEBOOK_P6: Boolean
    private val NABUK_REGAL_HD: Boolean
    private val NOOK: Boolean
    private val ONYX_C67: Boolean
    private val ONYX_DARWIN7: Boolean
    private val ONYX_DARWIN9: Boolean
    private val ONYX_EDISON: Boolean
    private val ONYX_FAUST3: Boolean
    private val ONYX_KON_TIKI2: Boolean
    private val ONYX_LEAF: Boolean
    private val ONYX_LEAF2: Boolean
    private val ONYX_LOMONOSOV: Boolean
    private val ONYX_MAGICBOOK: Boolean
    private val ONYX_MONTECRISTO3: Boolean
    private val ONYX_MAX: Boolean
    private val ONYX_NOTE: Boolean
    private val ONYX_NOTE3: Boolean
    private val ONYX_NOTE5: Boolean
    private val ONYX_NOTE_AIR: Boolean
    private val ONYX_NOTE_AIR2: Boolean
    private val ONYX_NOTE_PRO: Boolean
    private val ONYX_NOTE_X2: Boolean
    private val ONYX_NOVA2: Boolean
    private val ONYX_NOVA3: Boolean
    private val ONYX_NOVA3_COLOR: Boolean
    private val ONYX_NOVA_AIR: Boolean
    private val ONYX_NOVA_AIR_2: Boolean
    private val ONYX_NOVA_AIR_C: Boolean
    private val ONYX_NOVA_PRO: Boolean
    private val ONYX_PAGE: Boolean
    private val ONYX_POKE2: Boolean
    private val ONYX_POKE3: Boolean
    private val ONYX_POKE4: Boolean
    private val ONYX_POKE4LITE: Boolean
    private val ONYX_POKE_PRO: Boolean
    private val ONYX_TAB_ULTRA: Boolean
    private val ONYX_TAB_ULTRA_C: Boolean
    private val RIDI_PAPER_3: Boolean
    private val SONY_CP1: Boolean
    private val SONY_RP1: Boolean
    private val TAGUS_GEA: Boolean
    private val TOLINO_EPOS: Boolean
    private val TOLINO_SHINE3: Boolean
    private val TOLINO_VISION5: Boolean

    init {
        MANUFACTURER = lowerCase(getBuildField("MANUFACTURER"))
        BRAND = lowerCase(getBuildField("BRAND"))
        MODEL = lowerCase(getBuildField("MODEL"))
        DEVICE = lowerCase(getBuildField("DEVICE"))
        PRODUCT = lowerCase(getBuildField("PRODUCT"))
        HARDWARE = lowerCase(getBuildField("HARDWARE"))
        BOYUE = MANUFACTURER.contentEquals("boeye")
            || MANUFACTURER.contentEquals("boyue")

        // Boyue Likebook Ares
        BOYUE_K78W = BOYUE
            && (PRODUCT.contentEquals("k78w") || PRODUCT.contentEquals("ares"))

        // Boyue Likebook Alita
        BOYUE_K103 = BOYUE
            && (PRODUCT.contentEquals("k103") || PRODUCT.contentEquals("alita"))

        // Boyue Likebook P6
        BOYUE_P6 = BOYUE && PRODUCT.contentEquals("p6")

        // Boyue Lemon
        BOYUE_P61 = BOYUE && PRODUCT.contentEquals("p61-k12-l")

        // Boyue Likebook P78
        BOYUE_P78 = BOYUE && PRODUCT.contentEquals("p78")

        // Boyue Likebook LemonRead S62A
        BOYUE_S62 = BOYUE && PRODUCT.contentEquals("s62")

        // Boyue T61
        BOYUE_T61 = (BOYUE
            && (PRODUCT.startsWith("t61") || MODEL.contentEquals("rk30sdk"))
            && DEVICE.startsWith("t61"))

        // Boyue T62
        BOYUE_T62 = (BOYUE
            && (PRODUCT.startsWith("t62") || MODEL.contentEquals("rk30sdk"))
            && DEVICE.startsWith("t62"))

        // Boyue/JDRead T65S
        BOYUE_T65S = BOYUE && PRODUCT.contentEquals("t65s")

        // Boyue Likebook Muses
        BOYUE_T78D = BOYUE
            && (PRODUCT.contentEquals("t78d") || PRODUCT.contentEquals("muses"))

        // Boyue Likebook Mars
        BOYUE_T80D = BOYUE
            && (PRODUCT.contentEquals("t80d") || PRODUCT.contentEquals("mars"))

        // Boyue Likebook Plus
        BOYUE_T80S = BOYUE && PRODUCT.contentEquals("t80s")

        // Boyue Likebook Mimas
        BOYUE_T103D = BOYUE
            && (PRODUCT.contentEquals("t103d") || PRODUCT.contentEquals("mimas"))

        // Crema Note (1010P)
        CREMA = BRAND.contentEquals("crema")
            && PRODUCT.contentEquals("note")

        // Crema Carta+
        CREMA_0650L = BRAND.contentEquals("crema")
            && PRODUCT.contentEquals("keplerb")

        // Crema Carta G
        CREMA_CARTA_G = BRAND.contentEquals("crema")
            && MODEL.contentEquals("crema-0670c")

        // Android emulator for x86
        EMULATOR_X86 = MODEL.contentEquals("Android SDK built for x86")

        // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
        ENERGY = (BRAND.contentEquals("energysistem") || BRAND.contentEquals("energy_sistem"))
            && MODEL.startsWith("ereader")

        // Fidibook
        FIDIBOOK = MANUFACTURER.contentEquals("fidibo")
            && MODEL.contentEquals("fidibook")

        // Hanvon 960
        HANVON_960 = BRAND.contentEquals("freescale")
            && PRODUCT.contentEquals("evk_6sl_eink")

        // Artatech Inkbook Prime/Prime HD.
        INKBOOK = (MANUFACTURER.contentEquals("artatech")
            && BRAND.contentEquals("inkbook")
            && MODEL.startsWith("prime"))

        // JDRead1
        JDREAD = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("jdread")

        // Meebook P6
        MEEBOOK_P6 = MANUFACTURER.contentEquals("haoqing")
            && MODEL.contentEquals("p6")

        // Nabuk Regal HD
        NABUK_REGAL_HD = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("nabukreg_hd")

        // Nook (catch them all)
        NOOK = (MANUFACTURER.contentEquals("barnesandnoble") || MANUFACTURER.contentEquals("freescale"))
            && (MODEL.contentEquals("bnrv510") || MODEL.contentEquals("bnrv520") || MODEL.contentEquals("bnrv700")
            || MODEL.contentEquals("evk_mx6sl") || MODEL.startsWith("ereader"))

        // Onyx C67
        ONYX_C67 = MANUFACTURER.contentEquals("onyx")
            && (PRODUCT.startsWith("c67") || MODEL.contentEquals("rk30sdk"))
            && DEVICE.startsWith("c67")

        // ONYX DARWIN 7
        ONYX_DARWIN7 = MANUFACTURER.contentEquals("onyx")
            && (PRODUCT.contentEquals("mc_darwin7") || PRODUCT.contentEquals("darwin7"))
            && (DEVICE.contentEquals("mc_darwin7") || DEVICE.contentEquals("darwin7"))

        // ONYX DARWIN 9
        ONYX_DARWIN9 = MANUFACTURER.contentEquals("onyx")
            && (PRODUCT.contentEquals("mc_darwin9") || PRODUCT.contentEquals("darwin9"))
            && (DEVICE.contentEquals("mc_darwin9") || DEVICE.contentEquals("darwin9"))

        // Onyx Edison
        ONYX_EDISON = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("edison")
            && DEVICE.contentEquals("edison")

        // Onyx Faust 3
        ONYX_FAUST3 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("mc_faust3")
            && DEVICE.contentEquals("mc_faust3")

        // Onyx Kon-Tiki 2
        ONYX_KON_TIKI2 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("kon_tiki2")
            && DEVICE.contentEquals("kon_tiki2")

        // Onyx Leaf
        ONYX_LEAF = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("leaf")
            && DEVICE.contentEquals("leaf")

        // Onyx Leaf 2 && Onyx Leaf 2 Plus
        ONYX_LEAF2 = MANUFACTURER.contentEquals("onyx")
            && (PRODUCT.contentEquals("leaf2") || PRODUCT.contentEquals("leaf2_p"))
            && (DEVICE.contentEquals("leaf2") || DEVICE.contentEquals("leaf2_p"))

        // Onyx Lomonosov
        ONYX_LOMONOSOV = MANUFACTURER.contentEquals("onyx")
            && DEVICE.contentEquals("lomonosov")

        // Onyx MagicBook
        ONYX_MAGICBOOK = MANUFACTURER.contentEquals("onyx")
            && BRAND.contentEquals("magicbook")

        // Onyx Max
        ONYX_MAX = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("max")
            && DEVICE.contentEquals("max")

        // Onyx Montecristo 3
        ONYX_MONTECRISTO3 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("mc_kepler_c")
            && DEVICE.contentEquals("mc_kepler_c")

        // Onyx Note
        ONYX_NOTE = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("note")
            && DEVICE.contentEquals("note")

        // Onyx Note 3
        ONYX_NOTE3 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("note3")
            && DEVICE.contentEquals("note3")

        // Onyx Note 5
        ONYX_NOTE5 = BRAND.contentEquals("onyx")
            && PRODUCT.contentEquals("note5")
            && DEVICE.contentEquals("note5")

        // Onyx Note Air
        ONYX_NOTE_AIR = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("noteair")
            && DEVICE.contentEquals("noteair")

        // Onyx Note Air 2 && Note Air 2 Plus
        ONYX_NOTE_AIR2 = BRAND.contentEquals("onyx")
            && (MODEL.contentEquals("noteair2") || MODEL.contentEquals("noteair2p"))

        // Onyx Note Pro
        ONYX_NOTE_PRO = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("notepro")
            && DEVICE.contentEquals("notepro")

        // Onyx Note X2
        ONYX_NOTE_X2 = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("notex2")

        // Onyx Nova 2
        ONYX_NOVA2 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("nova2")
            && DEVICE.contentEquals("nova2")

        // Onyx Nova 3
        ONYX_NOVA3 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("nova3")
            && DEVICE.contentEquals("nova3")

        // Onyx Nova 3 Color
        ONYX_NOVA3_COLOR = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("nova3color")

        // Onyx Nova Air
        ONYX_NOVA_AIR = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("novaair")

        // Onyx Nova Air 2
        ONYX_NOVA_AIR_2 = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("novaair2")

        // Onyx Nova Air C
        ONYX_NOVA_AIR_C = BRAND.contentEquals("onyx")
            && MODEL.contentEquals("novaairc")

        // Onyx Nova Pro
        ONYX_NOVA_PRO = BRAND.contentEquals("onyx")
            && MODEL.contentEquals("novapro")

        // Onyx Page
        ONYX_PAGE = BRAND.contentEquals("onyx")
            && MODEL.contentEquals("page")

        // Onyx Poke 2
        ONYX_POKE2 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("poke2")

        // Onyx Poke 3
        ONYX_POKE3 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("poke3")
            && DEVICE.contentEquals("poke3")

        // Onyx Poke 4
        ONYX_POKE4 = BRAND.contentEquals("onyx")
            && MODEL.contentEquals("poke4")

        // Onyx Poke 4 lite
        ONYX_POKE4LITE = BRAND.contentEquals("onyx")
            && MODEL.contentEquals("poke4lite")

        // Onyx Poke Pro
        ONYX_POKE_PRO = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("poke_pro")

        // Onyx Tab Ultra
        ONYX_TAB_ULTRA = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("tabultra")

        // Onyx Tab Ultra C
        ONYX_TAB_ULTRA_C = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("tabultrac")

        // Ridi Paper 3
        RIDI_PAPER_3 = BRAND.contentEquals("ridi")
            && MODEL.contentEquals("ridipaper")
            && PRODUCT.contentEquals("rp1")

        // Sony DPT-CP1
        SONY_CP1 = MANUFACTURER.contentEquals("sony")
            && MODEL.contentEquals("dpt-cp1")

        // Sony DPT-RP1
        SONY_RP1 = MANUFACTURER.contentEquals("sony")
            && MODEL.contentEquals("dpt-rp1")

        // Tagus Gea
        TAGUS_GEA = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("tagus_pokep")

        // Tolino (catch them all)
        TOLINO = BRAND.contentEquals("tolino") && MODEL.contentEquals("imx50_rdp")
            || MODEL.contentEquals("tolino") && (DEVICE.contentEquals("tolino_vision2")
            || DEVICE.contentEquals("ntx_6sl"))

        // Tolino Epos 2 and Tolino Vision 4 also have warmth lights
        TOLINO_EPOS = BRAND.contentEquals("rakutenkobo")
            && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")
            && !HARDWARE.contentEquals("e60k00")
            && !HARDWARE.contentEquals("e70k00")

        // Tolino Shine 3 also has warmth lights, but with ntx_io file
        TOLINO_SHINE3 = BRAND.contentEquals("rakutenkobo")
            && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")
            && HARDWARE.contentEquals("e60k00")

        // Tolino Vision 5 also has warmth lights, but with ntx_io file
        TOLINO_VISION5 = BRAND.contentEquals("rakutenkobo")
            && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")
            && HARDWARE.contentEquals("e70k00")

        // devices with known bugs
        val bugMap = HashMap<QuirkDevice, Boolean>()
        bugMap[QuirkDevice.EMULATOR] = EMULATOR_X86
        bugMap[QuirkDevice.ONYX_MAX] = ONYX_MAX
        bugMap[QuirkDevice.ONYX_NOTE] = ONYX_NOTE
        bugMap[QuirkDevice.ONYX_POKE2] = ONYX_POKE2
        bugMap[QuirkDevice.SONY_CP1] = SONY_CP1
        bugMap[QuirkDevice.SONY_RP1] = SONY_RP1

        bugMap.keys.iterator().run {
            while (this.hasNext()) {
                val bug = this.next()
                val flag = bugMap[bug]
                if (flag != null && flag) {
                    QUIRK = bug
                }
            }
        }

        // e-ink devices
        val deviceMap = HashMap<EinkDevice, Boolean>()
        deviceMap[EinkDevice.BOYUE_K103] = BOYUE_K103
        deviceMap[EinkDevice.BOYUE_K78W] = BOYUE_K78W
        deviceMap[EinkDevice.BOYUE_P6] = BOYUE_P6
        deviceMap[EinkDevice.BOYUE_P61] = BOYUE_P61
        deviceMap[EinkDevice.BOYUE_P78] = BOYUE_P78
        deviceMap[EinkDevice.BOYUE_S62] = BOYUE_S62
        deviceMap[EinkDevice.BOYUE_T61] = BOYUE_T61
        deviceMap[EinkDevice.BOYUE_T62] = BOYUE_T62
        deviceMap[EinkDevice.BOYUE_T65S] = BOYUE_T65S
        deviceMap[EinkDevice.BOYUE_T78D] = BOYUE_T78D
        deviceMap[EinkDevice.BOYUE_T80D] = BOYUE_T80D
        deviceMap[EinkDevice.BOYUE_T80S] = BOYUE_T80S
        deviceMap[EinkDevice.BOYUE_T103D] = BOYUE_T103D
        deviceMap[EinkDevice.CREMA] = CREMA
        deviceMap[EinkDevice.CREMA_0650L] = CREMA_0650L
        deviceMap[EinkDevice.CREMA_CARTA_G] = CREMA_CARTA_G
        deviceMap[EinkDevice.ENERGY] = ENERGY
        deviceMap[EinkDevice.FIDIBOOK] = FIDIBOOK
        deviceMap[EinkDevice.INKBOOK] = INKBOOK
        deviceMap[EinkDevice.JDREAD] = JDREAD
        deviceMap[EinkDevice.MEEBOOK_P6] = MEEBOOK_P6
        deviceMap[EinkDevice.NABUK] = NABUK_REGAL_HD
        deviceMap[EinkDevice.NOOK] = NOOK
        deviceMap[EinkDevice.ONYX_C67] = ONYX_C67
        deviceMap[EinkDevice.ONYX_DARWIN7] = ONYX_DARWIN7
        deviceMap[EinkDevice.ONYX_DARWIN9] = ONYX_DARWIN9
        deviceMap[EinkDevice.ONYX_EDISON] = ONYX_EDISON
        deviceMap[EinkDevice.ONYX_FAUST3] = ONYX_FAUST3
        deviceMap[EinkDevice.ONYX_KON_TIKI2] = ONYX_KON_TIKI2
        deviceMap[EinkDevice.ONYX_LEAF] = ONYX_LEAF
        deviceMap[EinkDevice.ONYX_LEAF2] = ONYX_LEAF2
        deviceMap[EinkDevice.ONYX_LOMONOSOV] = ONYX_LOMONOSOV
        deviceMap[EinkDevice.ONYX_MAGICBOOK] = ONYX_MAGICBOOK
        deviceMap[EinkDevice.ONYX_MAX] = ONYX_MAX
        deviceMap[EinkDevice.ONYX_MONTECRISTO3] = ONYX_MONTECRISTO3
        deviceMap[EinkDevice.ONYX_NOTE] = ONYX_NOTE
        deviceMap[EinkDevice.ONYX_NOTE3] = ONYX_NOTE3
        deviceMap[EinkDevice.ONYX_NOTE5] = ONYX_NOTE5
        deviceMap[EinkDevice.ONYX_NOTE_AIR] = ONYX_NOTE_AIR
        deviceMap[EinkDevice.ONYX_NOTE_AIR2] = ONYX_NOTE_AIR2
        deviceMap[EinkDevice.ONYX_NOTE_PRO] = ONYX_NOTE_PRO
        deviceMap[EinkDevice.ONYX_NOTE_X2] = ONYX_NOTE_X2
        deviceMap[EinkDevice.ONYX_NOVA2] = ONYX_NOVA2
        deviceMap[EinkDevice.ONYX_NOVA3] = ONYX_NOVA3
        deviceMap[EinkDevice.ONYX_NOVA3_COLOR] = ONYX_NOVA3_COLOR
        deviceMap[EinkDevice.ONYX_NOVA_AIR] = ONYX_NOVA_AIR
        deviceMap[EinkDevice.ONYX_NOVA_AIR_2] = ONYX_NOVA_AIR_2
        deviceMap[EinkDevice.ONYX_NOVA_AIR_C] = ONYX_NOVA_AIR_C
        deviceMap[EinkDevice.ONYX_NOVA_PRO] = ONYX_NOVA_PRO
        deviceMap[EinkDevice.ONYX_PAGE] = ONYX_PAGE
        deviceMap[EinkDevice.ONYX_POKE3] = ONYX_POKE3
        deviceMap[EinkDevice.ONYX_POKE4] = ONYX_POKE4
        deviceMap[EinkDevice.ONYX_POKE4LITE] = ONYX_POKE4LITE
        deviceMap[EinkDevice.ONYX_POKE_PRO] = ONYX_POKE_PRO
        deviceMap[EinkDevice.ONYX_TAB_ULTRA] = ONYX_TAB_ULTRA
        deviceMap[EinkDevice.ONYX_TAB_ULTRA_C] = ONYX_TAB_ULTRA_C
        deviceMap[EinkDevice.RIDI_PAPER_3] = RIDI_PAPER_3
        deviceMap[EinkDevice.SONY_CP1] = SONY_CP1
        deviceMap[EinkDevice.SONY_RP1] = SONY_RP1
        deviceMap[EinkDevice.TAGUS_GEA] = TAGUS_GEA
        deviceMap[EinkDevice.TOLINO] = TOLINO

        deviceMap.keys.iterator().run {
            while (this.hasNext()) {
                val eink = this.next()
                val flag = deviceMap[eink]
                if (flag != null && flag) {
                    EINK = eink
                }
            }
        }

        // devices with custom lights
        val lightsMap = HashMap<LightsDevice, Boolean>()
        lightsMap[LightsDevice.BOYUE_S62] = BOYUE_S62
        lightsMap[LightsDevice.CREMA_CARTA_G] = CREMA_CARTA_G
        lightsMap[LightsDevice.MEEBOOK_P6] = MEEBOOK_P6
        lightsMap[LightsDevice.ONYX_C67] = ONYX_C67
        lightsMap[LightsDevice.ONYX_DARWIN7] = ONYX_DARWIN7
        lightsMap[LightsDevice.ONYX_DARWIN9] = ONYX_DARWIN9
        lightsMap[LightsDevice.ONYX_EDISON] = ONYX_EDISON
        lightsMap[LightsDevice.ONYX_FAUST3] = ONYX_FAUST3
        lightsMap[LightsDevice.ONYX_KON_TIKI2] = ONYX_KON_TIKI2
        lightsMap[LightsDevice.ONYX_LEAF] = ONYX_LEAF
        lightsMap[LightsDevice.ONYX_LEAF2] = ONYX_LEAF2
        lightsMap[LightsDevice.ONYX_LOMONOSOV] = ONYX_LOMONOSOV
        lightsMap[LightsDevice.ONYX_MAGICBOOK] = ONYX_MAGICBOOK
        lightsMap[LightsDevice.ONYX_MONTECRISTO3] = ONYX_MONTECRISTO3
        lightsMap[LightsDevice.ONYX_NOTE3] = ONYX_NOTE3
        lightsMap[LightsDevice.ONYX_NOTE_AIR] = ONYX_NOTE_AIR
        lightsMap[LightsDevice.ONYX_NOTE_AIR2] = ONYX_NOTE_AIR2
        lightsMap[LightsDevice.ONYX_NOTE_X2] = ONYX_NOTE_X2
        lightsMap[LightsDevice.ONYX_NOVA2] = ONYX_NOVA2
        lightsMap[LightsDevice.ONYX_NOVA3] = ONYX_NOVA3
        lightsMap[LightsDevice.ONYX_NOVA3_COLOR] = ONYX_NOVA3_COLOR
        lightsMap[LightsDevice.ONYX_NOVA_AIR] = ONYX_NOVA_AIR
        lightsMap[LightsDevice.ONYX_NOVA_AIR_2] = ONYX_NOVA_AIR_2
        lightsMap[LightsDevice.ONYX_NOVA_AIR_C] = ONYX_NOVA_AIR_C
        lightsMap[LightsDevice.ONYX_NOVA_PRO] = ONYX_NOVA_PRO
        lightsMap[LightsDevice.ONYX_PAGE] = ONYX_PAGE
        lightsMap[LightsDevice.ONYX_POKE3] = ONYX_POKE3
        lightsMap[LightsDevice.ONYX_POKE4] = ONYX_POKE4
        lightsMap[LightsDevice.ONYX_POKE4LITE] = ONYX_POKE4LITE
        lightsMap[LightsDevice.ONYX_POKE_PRO] = ONYX_POKE_PRO
        lightsMap[LightsDevice.ONYX_TAB_ULTRA] = ONYX_TAB_ULTRA
        lightsMap[LightsDevice.ONYX_TAB_ULTRA_C] = ONYX_TAB_ULTRA_C
        lightsMap[LightsDevice.RIDI_PAPER_3] = RIDI_PAPER_3
        lightsMap[LightsDevice.TAGUS_GEA] = TAGUS_GEA
        lightsMap[LightsDevice.TOLINO_EPOS] = TOLINO_EPOS
        lightsMap[LightsDevice.TOLINO_SHINE3] = TOLINO_SHINE3
        lightsMap[LightsDevice.TOLINO_VISION5] = TOLINO_VISION5

        lightsMap.keys.iterator().run {
            while (this.hasNext()) {
                val lights = this.next()
                val flag = lightsMap[lights]
                if (flag != null && flag) {
                    LIGHTS = lights
                }
            }
        }

        // has broken lifecycle
        QUIRK_BROKEN_LIFECYCLE = when (QUIRK) {
            QuirkDevice.ONYX_POKE2 -> true
            else -> false
        }

        // need wakelocks
        QUIRK_NEEDS_WAKELOCKS = when (QUIRK) {
            QuirkDevice.SONY_RP1 -> true
            else -> false
        }

        // 4.4+ device without native surface rotation
        QUIRK_NO_HW_ROTATION = when (QUIRK) {
            QuirkDevice.EMULATOR -> true
            else -> false
        }

        // Android devices without lights
        QUIRK_NO_LIGHTS = when (QUIRK) {
            QuirkDevice.ONYX_MAX,
            QuirkDevice.ONYX_NOTE,
            QuirkDevice.SONY_CP1,
            QuirkDevice.SONY_RP1 -> true
            else -> false
        }

        HAS_COLOR_SCREEN = when (EINK) {
            EinkDevice.NONE,
            EinkDevice.ONYX_NOVA3_COLOR,
            EinkDevice.ONYX_NOVA_AIR_C,
            EinkDevice.ONYX_TAB_ULTRA_C -> true
            else -> false
        }
    }

    private fun getBuildField(fieldName: String): String {
        return try {
            Build::class.java.getField(fieldName).get(null) as? String ?: ""
        } catch (e: Exception) {
             ""
        }
    }

    private fun lowerCase(text: String): String {
        return text.lowercase(Locale.US)
    }
}
