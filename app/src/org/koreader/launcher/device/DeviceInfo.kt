/* Device info using Android build properties,
 * based on https://github.com/unwmun/refreshU
 *
 * Note: devices don't need to be declared here unless
 * they have known eink update routines and/or bug workarounds. */

package org.koreader.launcher.device

import java.util.HashMap

import android.os.Build

object DeviceInfo {
    private const val T61: String = "t61"
    private const val T62: String = "t62"
    private const val RK30SDK: String = "rk30sdk"
    private val MANUFACTURER: String
    private val BRAND: String
    private val MODEL: String
    private val DEVICE: String
    private val BOYUE_T61: Boolean
    private val BOYUE_T62: Boolean
    private val BOYUE_T80S: Boolean
    private val BOYUE_T80D: Boolean
    private val BOYUE_T78D: Boolean
    private val BOYUE_T103D: Boolean
    private val CREMA: Boolean
    private val ONYX_C67: Boolean
    private val ENERGY: Boolean
    private val INKBOOK: Boolean
    private val TOLINO: Boolean
    private val NOOK_V520: Boolean
    private val SONY_RP1: Boolean
    private val EMULATOR_X86: Boolean
    private val IS_BOYUE: Boolean
    val PRODUCT: String
    val EINK_FREESCALE: Boolean
    val EINK_ROCKCHIP: Boolean
    val EINK_SUPPORT: Boolean
    val EINK_FULL_SUPPORT: Boolean
    val BUG_WAKELOCKS: Boolean

    // default values for generic devices.
    internal var EINK = EinkDevice.UNKNOWN
    private var BUG = BugDevice.NONE

    enum class EinkDevice {
        UNKNOWN,
        BOYUE_T61,
        BOYUE_T62,
        BOYUE_T80S,
        BOYUE_T80D,
        BOYUE_T78D,
        BOYUE_T103D,
        CREMA,
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
        // --------------- device probe --------------- //
        val deviceMap = HashMap<EinkDevice, Boolean>()
        val bugMap = HashMap<BugDevice, Boolean>()

        // we use the standard android build properties for device identification
        MANUFACTURER = getBuildField("MANUFACTURER")
        BRAND = getBuildField("BRAND")
        MODEL = getBuildField("MODEL")
        DEVICE = getBuildField("DEVICE")
        PRODUCT = getBuildField("PRODUCT")

        IS_BOYUE = MANUFACTURER.toLowerCase().contentEquals("boeye")
                || MANUFACTURER.toLowerCase().contentEquals("boyue")

        // Boyue T62, manufacturer uses both "boeye" and "boyue" ids.
        BOYUE_T62 = (IS_BOYUE
                && (PRODUCT.toLowerCase().startsWith(T62) || MODEL.contentEquals(RK30SDK))
                && DEVICE.toLowerCase().startsWith(T62))
        deviceMap[EinkDevice.BOYUE_T62] = BOYUE_T62

        // Boyue T61, uses RK3066 chipset
        BOYUE_T61 = (IS_BOYUE
                && (PRODUCT.toLowerCase().startsWith(T61) || MODEL.contentEquals(RK30SDK))
                && DEVICE.toLowerCase().startsWith(T61))
        deviceMap[EinkDevice.BOYUE_T61] = BOYUE_T61

        // Boyue Likebook Plus
        BOYUE_T80S = IS_BOYUE && PRODUCT.toLowerCase().contentEquals("t80s")
        deviceMap[EinkDevice.BOYUE_T80S] = BOYUE_T80S

        // Boyue Likebook Mars
        BOYUE_T80D = IS_BOYUE && PRODUCT.toLowerCase().contentEquals("t80d")
        deviceMap[EinkDevice.BOYUE_T80D] = BOYUE_T80D

        // Boyue Likebook Muses
        BOYUE_T78D = IS_BOYUE && PRODUCT.toLowerCase().contentEquals("t78d")
        deviceMap[EinkDevice.BOYUE_T78D] = BOYUE_T78D

        // Boyue Likebook Mimas
        BOYUE_T103D = IS_BOYUE && PRODUCT.toLowerCase().contentEquals("t103d")
        deviceMap[EinkDevice.BOYUE_T103D] = BOYUE_T103D

        // Crema Note (1010P)
        CREMA = BRAND.toLowerCase().contentEquals("crema")
                &&  PRODUCT.toLowerCase().contentEquals("note")
        deviceMap[EinkDevice.CREMA] = CREMA

        // Onyx C67
        ONYX_C67 = (MANUFACTURER.toLowerCase().contentEquals("onyx")
                && (PRODUCT.toLowerCase().startsWith("c67")
                || MODEL.contentEquals("rk30sdk"))
                && DEVICE.toLowerCase().startsWith("c67"))
        deviceMap[EinkDevice.ONYX_C67] = ONYX_C67

        // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
        ENERGY = (BRAND.toLowerCase().contentEquals("energysistem")
                || BRAND.toLowerCase().contentEquals("energy_sistem"))
                && MODEL.toLowerCase().startsWith("ereader")
        deviceMap[EinkDevice.ENERGY] = ENERGY

        // Artatech Inkbook Prime/Prime HD.
        INKBOOK = (MANUFACTURER.toLowerCase().contentEquals("artatech")
                && BRAND.toLowerCase().contentEquals("inkbook")
                && MODEL.toLowerCase().startsWith("prime"))
        deviceMap[EinkDevice.INKBOOK] = INKBOOK

        // Tolino
        TOLINO = BRAND.toLowerCase().contentEquals("tolino")
                && MODEL.toLowerCase().contentEquals("imx50_rdp")
                || MODEL.toLowerCase().contentEquals("tolino")
                && (DEVICE.toLowerCase().contentEquals("tolino_vision2")
                || DEVICE.toLowerCase().contentEquals("ntx_6sl"))
        deviceMap[EinkDevice.TOLINO] = TOLINO

        // Nook Glowlight 3
        NOOK_V520 = MANUFACTURER.toLowerCase().contentEquals("barnesandnoble")
                && MODEL.toLowerCase().contentEquals("bnrv520")
        deviceMap[EinkDevice.NOOK_V520] = NOOK_V520

        // Sony DPT-RP1
        SONY_RP1 = MANUFACTURER.toLowerCase().contentEquals("sony")
                && MODEL.toLowerCase().contentEquals("dpt-rp1")
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
        EINK_FREESCALE = CREMA ||
                TOLINO ||
                NOOK_V520

        // rockchip epd driver
        EINK_ROCKCHIP = BOYUE_T61 ||
                BOYUE_T62 ||
                BOYUE_T78D ||
                BOYUE_T80D ||
                BOYUE_T103D ||
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
            Build::class.java.getField(fieldName).get(null) as String
        } catch (e: Exception) {
            ""
        }

    }
}
