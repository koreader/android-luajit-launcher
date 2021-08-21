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


    enum class EinkDevice {
        NONE,
        BOYUE_K78W,
        BOYUE_K103,
        BOYUE_P6,
        BOYUE_P61,
        BOYUE_P78,
        BOYUE_T61,
        BOYUE_T62,
        BOYUE_T65S,
        BOYUE_T78D,
        BOYUE_T80D,
        BOYUE_T80S,
        BOYUE_T103D,
        CREMA,
        CREMA_0650L,
        ENERGY,
        FIDIBOOK,
        HANVON_960,
        INKBOOK,
        JDREAD,
        NABUK,
        NOOK,
        ONYX_C67,
        ONYX_KON_TIKI2,
        ONYX_NOVA2,
        TOLINO
    }

    enum class LightsDevice {
        NONE,
        ONYX_C67,
        ONYX_NOVA2,
        ONYX_KON_TIKI2,
        TOLINO_EPOS
    }

    enum class QuirkDevice {
        NONE,
        EMULATOR,
        ONYX_POKE2,
        SONY_RP1
    }

    // default values for generic devices.
    internal var EINK = EinkDevice.NONE
    internal var LIGHTS = LightsDevice.NONE
    private var QUIRK = QuirkDevice.NONE

    // device probe
    private val IS_BOYUE: Boolean
    private val BOYUE_K78W: Boolean
    private val BOYUE_K103: Boolean
    private val BOYUE_P6: Boolean
    private val BOYUE_P61: Boolean
    private val BOYUE_P78: Boolean
    private val BOYUE_T61: Boolean
    private val BOYUE_T62: Boolean
    private val BOYUE_T65S: Boolean
    private val BOYUE_T78D: Boolean
    private val BOYUE_T80D: Boolean
    private val BOYUE_T80S: Boolean
    private val BOYUE_T103D: Boolean
    private val CREMA: Boolean
    private val CREMA_0650L: Boolean
    private val EMULATOR_X86: Boolean
    private val ENERGY: Boolean
    private val FIDIBOOK: Boolean
    private val HANVON_960: Boolean
    private val INKBOOK: Boolean
    private val JDREAD: Boolean
    private val NABUK_REGAL_HD: Boolean
    private val NOOK: Boolean
    private val ONYX_C67: Boolean
    private val ONYX_KON_TIKI2: Boolean
    private val ONYX_NOVA2: Boolean
    private val ONYX_POKE2: Boolean
    private val SONY_RP1: Boolean
    private val TOLINO: Boolean
    private val TOLINO_EPOS: Boolean

    init {
        MANUFACTURER = lowerCase(getBuildField("MANUFACTURER"))
        BRAND = lowerCase(getBuildField("BRAND"))
        MODEL = lowerCase(getBuildField("MODEL"))
        DEVICE = lowerCase(getBuildField("DEVICE"))
        PRODUCT = lowerCase(getBuildField("PRODUCT"))
        HARDWARE = lowerCase(getBuildField("HARDWARE"))
        IS_BOYUE = MANUFACTURER.contentEquals("boeye")
            || MANUFACTURER.contentEquals("boyue")

        // Boyue Likebook Ares
        BOYUE_K78W = IS_BOYUE
            && (PRODUCT.contentEquals("k78w") || PRODUCT.contentEquals("ares"))

        // Boyue Likebook Alita
        BOYUE_K103 = IS_BOYUE
            && (PRODUCT.contentEquals("k103") || PRODUCT.contentEquals("alita"))

        // Boyue Likebook P6
        BOYUE_P6 = IS_BOYUE
            && PRODUCT.contentEquals("p6")

        // Boyue Lemon
        BOYUE_P61 = IS_BOYUE
            && PRODUCT.contentEquals("p61-k12-l")

        // Boyue Likebook P78
        BOYUE_P78 = IS_BOYUE
            && PRODUCT.contentEquals("p78")

        // Boyue T61
        BOYUE_T61 = (IS_BOYUE
            && (PRODUCT.startsWith("t61") || MODEL.contentEquals("rk30sdk"))
            && DEVICE.startsWith("t61"))

        // Boyue T62
        BOYUE_T62 = (IS_BOYUE
            && (PRODUCT.startsWith("t62") || MODEL.contentEquals("rk30sdk"))
            && DEVICE.startsWith("t62"))

        // Boyue/JDRead T65S
        BOYUE_T65S = IS_BOYUE
            && PRODUCT.contentEquals("t65s")

        // Boyue Likebook Muses
        BOYUE_T78D = IS_BOYUE
            && (PRODUCT.contentEquals("t78d") || PRODUCT.contentEquals("muses"))

        // Boyue Likebook Mars
        BOYUE_T80D = IS_BOYUE
            && (PRODUCT.contentEquals("t80d") || PRODUCT.contentEquals("mars"))

        // Boyue Likebook Plus
        BOYUE_T80S = IS_BOYUE
            && PRODUCT.contentEquals("t80s")

        // Boyue Likebook Mimas
        BOYUE_T103D = IS_BOYUE
            && (PRODUCT.contentEquals("t103d") || PRODUCT.contentEquals("mimas"))

        // Crema Note (1010P)
        CREMA = BRAND.contentEquals("crema")
            && PRODUCT.contentEquals("note")

        // Crema Carta+
        CREMA_0650L = BRAND.contentEquals("crema")
            && PRODUCT.contentEquals("keplerb")

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

        // Nabuk Regal HD
        NABUK_REGAL_HD = MANUFACTURER.contentEquals("onyx")
            && MODEL.contentEquals("nabukreg_hd")

        // Nook (catch them all)
        NOOK = (MANUFACTURER.contentEquals("barnesandnoble") || MANUFACTURER.contentEquals("freescale"))
            && (MODEL.contentEquals("bnrv510") || MODEL.contentEquals("bnrv520") || MODEL.contentEquals("bnrv700")
            || MODEL.contentEquals("evk_mx6sl") || MODEL.startsWith("ereader"))

        // Onyx NOVA 2
        ONYX_NOVA2 = (MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("nova2")
            && DEVICE.contentEquals("nova2"))

        // Onyx C67
        ONYX_C67 = (MANUFACTURER.contentEquals("onyx")
            && (PRODUCT.startsWith("c67") || MODEL.contentEquals("rk30sdk"))
            && DEVICE.startsWith("c67"))

        // Onyx Kon-Tiki 2
        ONYX_KON_TIKI2 = (MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("kon_tiki2")
            && DEVICE.contentEquals("kon_tiki2"))

        // Onyx Poke 2
        ONYX_POKE2 = MANUFACTURER.contentEquals("onyx")
            && PRODUCT.contentEquals("poke2")

        // Sony DPT-RP1
        SONY_RP1 = MANUFACTURER.contentEquals("sony")
            && MODEL.contentEquals("dpt-rp1")

        // Tolino (catch them all)
        TOLINO = BRAND.contentEquals("tolino") && MODEL.contentEquals("imx50_rdp")
            || MODEL.contentEquals("tolino") && (DEVICE.contentEquals("tolino_vision2")
            || DEVICE.contentEquals("ntx_6sl"))

        // Tolino Epos 2 and Tolino Vision 4 also have warmth lights
        TOLINO_EPOS = BRAND.contentEquals("rakutenkobo")
            && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")

        // devices with known bugs
        val bugMap = HashMap<QuirkDevice, Boolean>()
        bugMap[QuirkDevice.EMULATOR] = EMULATOR_X86
        bugMap[QuirkDevice.ONYX_POKE2] = ONYX_POKE2
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
        deviceMap[EinkDevice.BOYUE_T61] = BOYUE_T61
        deviceMap[EinkDevice.BOYUE_T62] = BOYUE_T62
        deviceMap[EinkDevice.BOYUE_T65S] = BOYUE_T65S
        deviceMap[EinkDevice.BOYUE_T78D] = BOYUE_T78D
        deviceMap[EinkDevice.BOYUE_T80D] = BOYUE_T80D
        deviceMap[EinkDevice.BOYUE_T80S] = BOYUE_T80S
        deviceMap[EinkDevice.BOYUE_T103D] = BOYUE_T103D
        deviceMap[EinkDevice.CREMA] = CREMA
        deviceMap[EinkDevice.CREMA_0650L] = CREMA_0650L
        deviceMap[EinkDevice.ENERGY] = ENERGY
        deviceMap[EinkDevice.FIDIBOOK] = FIDIBOOK
        deviceMap[EinkDevice.INKBOOK] = INKBOOK
        deviceMap[EinkDevice.JDREAD] = JDREAD
        deviceMap[EinkDevice.NABUK] = NABUK_REGAL_HD
        deviceMap[EinkDevice.NOOK] = NOOK
        deviceMap[EinkDevice.ONYX_C67] = ONYX_C67
        deviceMap[EinkDevice.ONYX_KON_TIKI2] = ONYX_KON_TIKI2
        deviceMap[EinkDevice.ONYX_NOVA2] = ONYX_NOVA2

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
        lightsMap[LightsDevice.ONYX_C67] = ONYX_C67
        lightsMap[LightsDevice.ONYX_KON_TIKI2] = ONYX_KON_TIKI2
        lightsMap[LightsDevice.ONYX_NOVA2] = ONYX_NOVA2
        lightsMap[LightsDevice.TOLINO_EPOS] = TOLINO_EPOS

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
            QuirkDevice.SONY_RP1 -> true
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
