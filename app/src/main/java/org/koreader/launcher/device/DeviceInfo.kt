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
    val EINK_FREESCALE: Boolean
    val EINK_ROCKCHIP: Boolean
    val EINK_QCOM: Boolean
    val EINK_SUPPORT: Boolean
    val EINK_FULL_SUPPORT: Boolean
    val BUG_WAKELOCKS: Boolean
    val BUG_SCREEN_ROTATION: Boolean
    val NEEDS_VIEW: Boolean

    private val BOYUE_T61: Boolean
    private val BOYUE_T62: Boolean
    private val BOYUE_T65S: Boolean
    private val BOYUE_T80S: Boolean
    private val BOYUE_T80D: Boolean
    private val BOYUE_T78D: Boolean
    private val BOYUE_T103D: Boolean
    private val BOYUE_K103: Boolean
    private val BOYUE_K78W: Boolean
    private val BOYUE_P6: Boolean
    private val CREMA: Boolean
    private val CREMA_0650L: Boolean
    private val FIDIBOOK: Boolean
    private val ONYX_C67: Boolean
    private val ONYX_NOVA2: Boolean
    private val ENERGY: Boolean
    private val INKBOOK: Boolean
    private val TOLINO: Boolean
    private val TOLINO_EPOS: Boolean
    private val JDREAD: Boolean
    private val NOOK_V520: Boolean
    private val SONY_RP1: Boolean
    private val EMULATOR_X86: Boolean
    private val IS_BOYUE: Boolean

    // default values for generic devices.
    internal var EINK = EinkDevice.UNKNOWN
    internal var LIGHTS = LightsDevice.NONE
    private var BUG = BugDevice.NONE

    enum class EinkDevice {
        UNKNOWN,
        BOYUE_T61,
        BOYUE_T62,
        BOYUE_T65S,
        BOYUE_T80S,
        BOYUE_T80D,
        BOYUE_T78D,
        BOYUE_T103D,
        BOYUE_K103,
        BOYUE_K78W,
        BOYUE_P6,
        CREMA,
        CREMA_0650L,
        FIDIBOOK,
        ONYX_C67,
        ONYX_NOVA2,
        ENERGY,
        INKBOOK,
        JDREAD,
        TOLINO,
        NOOK_V520
    }

    enum class LightsDevice {
        NONE,
        TOLINO_EPOS,
        ONYX_NOVA2,
        ONYX_C67
    }

    enum class BugDevice {
        NONE,
        SONY_RP1,
        EMULATOR
    }

    init {
        MANUFACTURER = lowerCase(getBuildField("MANUFACTURER"))
        BRAND = lowerCase(getBuildField("BRAND"))
        MODEL = lowerCase(getBuildField("MODEL"))
        DEVICE = lowerCase(getBuildField("DEVICE"))
        PRODUCT = lowerCase(getBuildField("PRODUCT"))
        HARDWARE = lowerCase(getBuildField("HARDWARE"))
        IS_BOYUE = MANUFACTURER.contentEquals("boeye") || MANUFACTURER.contentEquals("boyue")

        // --------------- device probe --------------- //
        val deviceMap = HashMap<EinkDevice, Boolean>()
        val bugMap = HashMap<BugDevice, Boolean>()
        val lightsMap = HashMap<LightsDevice, Boolean>()

        // Boyue T62, manufacturer uses both "boeye" and "boyue" ids.
        BOYUE_T62 = (IS_BOYUE
                && (PRODUCT.startsWith("t62") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.startsWith("t62"))
        deviceMap[EinkDevice.BOYUE_T62] = BOYUE_T62

        // Boyue T61, uses RK3066 chipset
        BOYUE_T61 = (IS_BOYUE
                && (PRODUCT.startsWith("t61") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.startsWith("t61"))
        deviceMap[EinkDevice.BOYUE_T61] = BOYUE_T61

        // Boyue/JDRead T65S
        BOYUE_T65S = BRAND.contentEquals("boyue") && PRODUCT.contentEquals("t65s")
        deviceMap[EinkDevice.BOYUE_T65S] = BOYUE_T65S

        // Boyue Likebook Plus
        BOYUE_T80S = IS_BOYUE && PRODUCT.contentEquals("t80s")
        deviceMap[EinkDevice.BOYUE_T80S] = BOYUE_T80S

        // Boyue Likebook Mars
        BOYUE_T80D = IS_BOYUE
            && (PRODUCT.contentEquals("t80d") || PRODUCT.contentEquals("mars"))
        deviceMap[EinkDevice.BOYUE_T80D] = BOYUE_T80D

        // Boyue Likebook Muses
        BOYUE_T78D = IS_BOYUE
            && (PRODUCT.contentEquals("t78d") || PRODUCT.contentEquals("muses"))
        deviceMap[EinkDevice.BOYUE_T78D] = BOYUE_T78D

        // Boyue Likebook Mimas
        BOYUE_T103D = IS_BOYUE
            && (PRODUCT.contentEquals("t103d") || PRODUCT.contentEquals("mimas"))
        deviceMap[EinkDevice.BOYUE_T103D] = BOYUE_T103D

        // Boyue Likebook Alita
        BOYUE_K103 = IS_BOYUE
            && (PRODUCT.contentEquals("k103") || PRODUCT.contentEquals("alita"))
        deviceMap[EinkDevice.BOYUE_K103] = BOYUE_K103

        // Boyue Likebook Ares
        BOYUE_K78W = IS_BOYUE
            && (PRODUCT.contentEquals("k78w") || PRODUCT.contentEquals("ares"))
        deviceMap[EinkDevice.BOYUE_K78W] = BOYUE_K78W

        // Boyue Likebook P6
        BOYUE_P6 = IS_BOYUE && PRODUCT.contentEquals("p6")
        deviceMap[EinkDevice.BOYUE_P6] = BOYUE_P6

        // Crema Note (1010P)
        CREMA = BRAND.contentEquals("crema") && PRODUCT.contentEquals("note")
        deviceMap[EinkDevice.CREMA] = CREMA

        // Crema Carta+
        CREMA_0650L = BRAND.contentEquals("crema") && PRODUCT.contentEquals("keplerb")
        deviceMap[EinkDevice.CREMA_0650L] = CREMA_0650L

        // Onyx NOVA 2
        ONYX_NOVA2 = (MANUFACTURER.contentEquals("onyx")
                && PRODUCT.contentEquals("nova2")
                && DEVICE.contentEquals("nova2"))
        lightsMap[LightsDevice.ONYX_NOVA2] = ONYX_NOVA2
        deviceMap[EinkDevice.ONYX_NOVA2] = ONYX_NOVA2

        // Onyx C67
        ONYX_C67 = (MANUFACTURER.contentEquals("onyx")
                && (PRODUCT.startsWith("c67") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.startsWith("c67"))
        lightsMap[LightsDevice.ONYX_C67] = ONYX_C67
        deviceMap[EinkDevice.ONYX_C67] = ONYX_C67

        // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
        ENERGY = (BRAND.contentEquals("energysistem") || BRAND.contentEquals("energy_sistem"))
            && MODEL.startsWith("ereader")
        deviceMap[EinkDevice.ENERGY] = ENERGY

        // Artatech Inkbook Prime/Prime HD.
        INKBOOK = (MANUFACTURER.contentEquals("artatech")
                && BRAND.contentEquals("inkbook")
                && MODEL.startsWith("prime"))
        deviceMap[EinkDevice.INKBOOK] = INKBOOK

        // Tolino
        TOLINO = BRAND.contentEquals("tolino") && MODEL.contentEquals("imx50_rdp")
                || MODEL.contentEquals("tolino") && (DEVICE.contentEquals("tolino_vision2")
                || DEVICE.contentEquals("ntx_6sl"))
        deviceMap[EinkDevice.TOLINO] = TOLINO

        // Tolino Epos 2 and Tolino Vision 4 also have warmth lights
        TOLINO_EPOS = BRAND.contentEquals("rakutenkobo") && MODEL.contentEquals("tolino")
            && DEVICE.contentEquals("ntx_6sl")
        lightsMap[LightsDevice.TOLINO_EPOS] = TOLINO_EPOS

        // Nook Glowlight 3 et al.
        NOOK_V520 = (MANUFACTURER.contentEquals("barnesandnoble") || MANUFACTURER.contentEquals("freescale"))
                && (MODEL.contentEquals("bnrv510") || MODEL.contentEquals("bnrv520") || MODEL.contentEquals("bnrv700")
                || MODEL.contentEquals("evk_mx6sl") || MODEL.startsWith("ereader"))
        deviceMap[EinkDevice.NOOK_V520] = NOOK_V520

        // Fidibook
        FIDIBOOK = MANUFACTURER.contentEquals("fidibo") && MODEL.contentEquals("fidibook")
        deviceMap[EinkDevice.FIDIBOOK] = FIDIBOOK

        // JDRead1
        JDREAD = MANUFACTURER.contentEquals("onyx") && MODEL.contentEquals("jdread")
        deviceMap[EinkDevice.JDREAD] = JDREAD


        // Sony DPT-RP1
        SONY_RP1 = MANUFACTURER.contentEquals("sony") && MODEL.contentEquals("dpt-rp1")
        bugMap[BugDevice.SONY_RP1] = SONY_RP1

        // Android emulator for x86
        EMULATOR_X86 = MODEL.contentEquals("Android SDK built for x86")
        bugMap[BugDevice.EMULATOR] = EMULATOR_X86

        // find current eink device.
        val einkIter = deviceMap.keys.iterator()
        while (einkIter.hasNext()) {
            val eink = einkIter.next()
            val flag = deviceMap[eink]
            if (flag != null && flag) {
                EINK = eink
            }
        }

        // find known bugs
        val bugIter = bugMap.keys.iterator()
        while (bugIter.hasNext()) {
            val bug = bugIter.next()
            val flag = bugMap[bug]
            if (flag != null && flag) {
                BUG = bug
            }
        }

        // find devices with custom lights
        val lightsIter = lightsMap.keys.iterator()
        while (lightsIter.hasNext()) {
            val lights = lightsIter.next()
            val flag = lightsMap[lights]
            if (flag != null && flag) {
                LIGHTS = lights
            }
        }

        // freescale epd driver
        EINK_FREESCALE =
            BOYUE_T65S ||
            CREMA ||
            TOLINO ||
            NOOK_V520

        // rockchip epd driver
        EINK_ROCKCHIP =
            BOYUE_T61 ||
            BOYUE_T62 ||
            BOYUE_T78D ||
            BOYUE_T80D ||
            BOYUE_T103D ||
            BOYUE_K103 ||
            BOYUE_K78W ||
            BOYUE_P6 ||
            CREMA_0650L ||
            ENERGY ||
            INKBOOK ||
            ONYX_C67

        EINK_QCOM = ONYX_NOVA2

        // basic eink support
        EINK_SUPPORT = EINK_FREESCALE || EINK_ROCKCHIP || EINK_QCOM

        // full eink support
        EINK_FULL_SUPPORT = CREMA || TOLINO || ONYX_NOVA2

        // need wakelocks
        BUG_WAKELOCKS = BUG == BugDevice.SONY_RP1

        // 4.4+ device without native surface rotation
        BUG_SCREEN_ROTATION = BUG == BugDevice.EMULATOR

        // needs a surfaceView to do epd updates
        NEEDS_VIEW = EINK_FREESCALE || EINK_QCOM
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
