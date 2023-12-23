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
        ONYX_GENERIC,
        ONYX_SDK,
        ONYX_WARMTH,
        ONYX_C67,
        ONYX_DARWIN7,
        ONYX_FAUST3,
        ONYX_MAGICBOOK,
        ONYX_MAX,
        ONYX_MONTECRISTO3,
        ONYX_NOTE,
        ONYX_NOVA,
        ONYX_NOVA3_COLOR,
        ONYX_NOVA_AIR,
        ONYX_NOVA_AIR_C,
        ONYX_TAB_ULTRA_C,
        RIDI_PAPER_3,
        SONY_CP1,
        SONY_RP1,
        ONYX_TAGUS_GEA,
        TOLINO
    }

    enum class LightsDevice {
        NONE,
        BOYUE_S62,
        CREMA_CARTA_G,
        MEEBOOK_P6,
        ONYX_SDK,
        ONYX_WARMTH,
        ONYX_C67,
        ONYX_DARWIN7,
        ONYX_FAUST3,
        ONYX_MAGICBOOK,
        ONYX_MONTECRISTO3,
        ONYX_NOVA3_COLOR,
        ONYX_NOVA_AIR_C,
        RIDI_PAPER_3,
        ONYX_TAGUS_GEA,
        TOLINO_EPOS,
        TOLINO_SHINE3,
        TOLINO_VISION4,
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
    private val ONYX_GENERIC: Boolean
    private val ONYX_SDK: Boolean
    private val ONYX_WARMTH: Boolean
    private val ONYX_C67: Boolean
    private val ONYX_DARWIN7: Boolean
    private val ONYX_FAUST3: Boolean
    private val ONYX_MAGICBOOK: Boolean
    private val ONYX_MONTECRISTO3: Boolean
    private val ONYX_MAX: Boolean
    private val ONYX_NOTE: Boolean
    private val ONYX_NOVA3_COLOR: Boolean
    private val ONYX_NOVA_AIR_C: Boolean
    private val ONYX_POKE2: Boolean
    private val ONYX_TAB_ULTRA_C: Boolean
    private val ONYX_TAGUS_GEA: Boolean
    private val RIDI_PAPER_3: Boolean
    private val SONY_CP1: Boolean
    private val SONY_RP1: Boolean
    private val TOLINO_EPOS: Boolean
    private val TOLINO_SHINE3: Boolean
    private val TOLINO_VISION4: Boolean
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

        // Group of Onyx Generic
        ONYX_GENERIC = (MANUFACTURER.contentEquals("onyx") || BRAND.contentEquals("onyx"))
            && (MODEL.contentEquals("palma") || PRODUCT.contentEquals("note5")
        )

        // Group of Onyx Sdk
        ONYX_SDK = (MANUFACTURER.contentEquals("onyx") || BRAND.contentEquals("onyx"))
            && (PRODUCT.contentEquals("leaf2")
            || PRODUCT.contentEquals("leaf2_p") || PRODUCT.contentEquals("mc_darwin9")
            || PRODUCT.contentEquals("darwin9") || MODEL.contentEquals("noteair2")
            || MODEL.contentEquals("noteair2p") || MODEL.contentEquals("notex2")
            || PRODUCT.contentEquals("nova") || PRODUCT.contentEquals("nova2")
            || MODEL.contentEquals("novaair2") || MODEL.contentEquals("page")
            || MODEL.contentEquals("poke4") || MODEL.contentEquals("poke5p")
            || MODEL.contentEquals("poke4lite") || MODEL.contentEquals("tabultra")
        )

        // Group of Onyx Warmth
        ONYX_WARMTH = (MANUFACTURER.contentEquals("onyx") || BRAND.contentEquals("onyx"))
            && (PRODUCT.contentEquals("edison")
            || PRODUCT.contentEquals("kon_tiki2") || PRODUCT.contentEquals("leaf")
            || DEVICE.contentEquals("lomonosov") || PRODUCT.contentEquals("note3")
            || PRODUCT.contentEquals("noteair") || PRODUCT.contentEquals("notepro")
            || PRODUCT.contentEquals("nova3") || MODEL.contentEquals("novaair")
            || MODEL.contentEquals("novapro") || PRODUCT.contentEquals("poke3")
            || PRODUCT.contentEquals("poke_pro")
        )

        // Onyx C67
        ONYX_C67 = MANUFACTURER.contentEquals("onyx")
            && (PRODUCT.startsWith("c67") || MODEL.contentEquals("rk30sdk"))
            && DEVICE.startsWith("c67")

        // ONYX DARWIN 7
        ONYX_DARWIN7 = MANUFACTURER.contentEquals("onyx")
            && (PRODUCT.contentEquals("mc_darwin7") || PRODUCT.contentEquals("darwin7"))
            && (DEVICE.contentEquals("mc_darwin7") || DEVICE.contentEquals("darwin7"))

        // Onyx Faust 3
        ONYX_FAUST3 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("mc_faust3")
            && DEVICE.contentEquals("mc_faust3")

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

        // Onyx Nova 3 Color
        ONYX_NOVA3_COLOR = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("nova3color")

        // Onyx Nova Air C
        ONYX_NOVA_AIR_C = BRAND.contentEquals("onyx")
            && MODEL.contentEquals("novaairc")

        // Onyx Poke 2
        ONYX_POKE2 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("poke2")

        // Onyx Tab Ultra C
        ONYX_TAB_ULTRA_C = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("tabultrac")

        // Onyx Tagus Gea
        ONYX_TAGUS_GEA = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("tagus_pokep")

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

        // Tolino (catch them all)
        TOLINO = BRAND.contentEquals("tolino") && MODEL.contentEquals("imx50_rdp")
            || MODEL.contentEquals("tolino") && (DEVICE.contentEquals("tolino_vision2")
            || DEVICE.contentEquals("ntx_6sl"))

        // Tolino Epos 2 also have warmth lights
        TOLINO_EPOS = BRAND.contentEquals("rakutenkobo")
            && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")
            && !HARDWARE.contentEquals("e60k00")
            && !HARDWARE.contentEquals("e60q50")
            && !HARDWARE.contentEquals("e70k00")

        // Tolino Shine 3 also has warmth lights, but with ntx_io file
        TOLINO_SHINE3 = BRAND.contentEquals("rakutenkobo")
            && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")
            && HARDWARE.contentEquals("e60k00")

        // Tolino Vision 4 also has warmth lights, but with ntx_io file
        TOLINO_VISION4 = BRAND.contentEquals("rakutenkobo")
            && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")
            && HARDWARE.contentEquals("e60q50")

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
        deviceMap[EinkDevice.ONYX_GENERIC] = ONYX_GENERIC
        deviceMap[EinkDevice.ONYX_SDK] = ONYX_SDK
        deviceMap[EinkDevice.ONYX_WARMTH] = ONYX_WARMTH
        deviceMap[EinkDevice.ONYX_C67] = ONYX_C67
        deviceMap[EinkDevice.ONYX_DARWIN7] = ONYX_DARWIN7
        deviceMap[EinkDevice.ONYX_FAUST3] = ONYX_FAUST3
        deviceMap[EinkDevice.ONYX_MAGICBOOK] = ONYX_MAGICBOOK
        deviceMap[EinkDevice.ONYX_MAX] = ONYX_MAX
        deviceMap[EinkDevice.ONYX_MONTECRISTO3] = ONYX_MONTECRISTO3
        deviceMap[EinkDevice.ONYX_NOTE] = ONYX_NOTE
        deviceMap[EinkDevice.ONYX_NOVA3_COLOR] = ONYX_NOVA3_COLOR
        deviceMap[EinkDevice.ONYX_NOVA_AIR_C] = ONYX_NOVA_AIR_C
        deviceMap[EinkDevice.ONYX_TAB_ULTRA_C] = ONYX_TAB_ULTRA_C
        deviceMap[EinkDevice.RIDI_PAPER_3] = RIDI_PAPER_3
        deviceMap[EinkDevice.SONY_CP1] = SONY_CP1
        deviceMap[EinkDevice.SONY_RP1] = SONY_RP1
        deviceMap[EinkDevice.ONYX_TAGUS_GEA] = ONYX_TAGUS_GEA
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
        lightsMap[LightsDevice.ONYX_SDK] = ONYX_SDK
        lightsMap[LightsDevice.ONYX_WARMTH] = ONYX_WARMTH
        lightsMap[LightsDevice.ONYX_C67] = ONYX_C67
        lightsMap[LightsDevice.ONYX_DARWIN7] = ONYX_DARWIN7
        lightsMap[LightsDevice.ONYX_FAUST3] = ONYX_FAUST3
        lightsMap[LightsDevice.ONYX_MAGICBOOK] = ONYX_MAGICBOOK
        lightsMap[LightsDevice.ONYX_MONTECRISTO3] = ONYX_MONTECRISTO3
        lightsMap[LightsDevice.ONYX_NOVA3_COLOR] = ONYX_NOVA3_COLOR
        lightsMap[LightsDevice.ONYX_NOVA_AIR_C] = ONYX_NOVA_AIR_C
        lightsMap[LightsDevice.RIDI_PAPER_3] = RIDI_PAPER_3
        lightsMap[LightsDevice.ONYX_TAGUS_GEA] = ONYX_TAGUS_GEA
        lightsMap[LightsDevice.TOLINO_EPOS] = TOLINO_EPOS
        lightsMap[LightsDevice.TOLINO_SHINE3] = TOLINO_SHINE3
        lightsMap[LightsDevice.TOLINO_VISION4] = TOLINO_VISION4
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
            EinkDevice.ONYX_TAB_ULTRA_C,
            EinkDevice.ONYX_NOVA_AIR_C -> true
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
