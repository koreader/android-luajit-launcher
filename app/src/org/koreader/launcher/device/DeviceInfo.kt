/* Device info using Android build properties,
 * based on https://github.com/unwmun/refreshU
 *
 * Note: devices don't need to be declared here unless
 * they have known e-ink update routines and/or bug workarounds. */

package org.koreader.launcher.device

import android.os.Build
import java.util.*

object DeviceInfo {

    val PRODUCT: String
    val EINK_FREESCALE: Boolean
    val EINK_ROCKCHIP: Boolean
    val EINK_SUPPORT: Boolean
    val EINK_FULL_SUPPORT: Boolean
    val BUG_WAKELOCKS: Boolean

    private val MANUFACTURER: String
    private val BRAND: String
    private val MODEL: String
    private val DEVICE: String
    private val BOYUE_T61: Boolean
    private val BOYUE_T62: Boolean
    private val BOYUE_T65S: Boolean
    private val BOYUE_T80S: Boolean
    private val BOYUE_T80D: Boolean
    private val BOYUE_T78D: Boolean
    private val BOYUE_T103D: Boolean
    private val BOYUE_K103: Boolean
    private val BOYUE_K78W: Boolean
    private val CREMA: Boolean
    private val CREMA_0650L: Boolean
    private val ONYX_C67: Boolean
    private val ENERGY: Boolean
    private val INKBOOK: Boolean
    private val TOLINO: Boolean
    private val NOOK_V520: Boolean
    private val SONY_RP1: Boolean
    private val EMULATOR_X86: Boolean
    private val IS_BOYUE: Boolean

    // default values for generic devices.
    internal var EINK = EinkDevice.UNKNOWN
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
        CREMA,
        CREMA_0650L,
        ONYX_C67,
        ENERGY,
        INKBOOK,
        TOLINO,
        NOOK_V520
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
        IS_BOYUE = MANUFACTURER.contentEquals("boeye") || MANUFACTURER.contentEquals("boyue")

        // --------------- device probe --------------- //
        val deviceMap = HashMap<EinkDevice, Boolean>()
        val bugMap = HashMap<BugDevice, Boolean>()

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
        BOYUE_T80D = IS_BOYUE && PRODUCT.contentEquals("t80d")
        deviceMap[EinkDevice.BOYUE_T80D] = BOYUE_T80D

        // Boyue Likebook Muses
        BOYUE_T78D = IS_BOYUE && PRODUCT.contentEquals("t78d")
        deviceMap[EinkDevice.BOYUE_T78D] = BOYUE_T78D

        // Boyue Likebook Mimas
        BOYUE_T103D = IS_BOYUE && PRODUCT.contentEquals("t103d")
        deviceMap[EinkDevice.BOYUE_T103D] = BOYUE_T103D

        // Boyue Likebook Alita
        BOYUE_K103 = IS_BOYUE && PRODUCT.contentEquals("k103")
        deviceMap[EinkDevice.BOYUE_K103] = BOYUE_K103

        // Boyue Likebook Ares
        BOYUE_K78W = IS_BOYUE && PRODUCT.contentEquals("k78w")
        deviceMap[EinkDevice.BOYUE_K78W] = BOYUE_K78W

        // Crema Note (1010P)
        CREMA = BRAND.contentEquals("crema") && PRODUCT.contentEquals("note")
        deviceMap[EinkDevice.CREMA] = CREMA

        // Crema Carta+
        CREMA_0650L = BRAND.contentEquals("crema") && PRODUCT.contentEquals("keplerb")
        deviceMap[EinkDevice.CREMA_0650L] = CREMA_0650L

        // Onyx C67
        ONYX_C67 = (MANUFACTURER.contentEquals("onyx")
                && (PRODUCT.startsWith("c67") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.startsWith("c67"))
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

        // Nook Glowlight 3 et al.
        NOOK_V520 = (MANUFACTURER.contentEquals("barnesandnoble") || MANUFACTURER.contentEquals("freescale"))
                && (MODEL.contentEquals("bnrv510") || MODEL.contentEquals("bnrv520") || MODEL.contentEquals("bnrv700") || MODEL.startsWith("ereader"))
        deviceMap[EinkDevice.NOOK_V520] = NOOK_V520

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
            CREMA_0650L ||
            ENERGY ||
            INKBOOK ||
            ONYX_C67

        // basic eink support
        EINK_SUPPORT = EINK_FREESCALE || EINK_ROCKCHIP

        // full eink support
        EINK_FULL_SUPPORT = CREMA || TOLINO

        // need wakelocks
        BUG_WAKELOCKS = BUG == BugDevice.SONY_RP1
    }

    private fun getBuildField(fieldName: String): String {
        return try {
            Build::class.java.getField(fieldName).get(null) as? String ?: ""
        } catch (e: Exception) {
             ""
        }
    }

    private fun lowerCase(text: String): String {
        return text.toLowerCase(Locale.US)
    }
}
