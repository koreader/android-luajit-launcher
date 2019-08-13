/* Device info using Android build properties,
 * based on https://github.com/unwmun/refreshU
 *
 * Note: devices don't need to be declared here unless
 * they have known eink update routines and/or bug workarounds. */

package org.koreader.launcher.device;

import java.util.HashMap;
import java.util.Iterator;

import android.os.Build;

@SuppressWarnings({"CanBeFinal", "WhileLoopReplaceableByForEach"})
public class DeviceInfo {

    public enum EinkDevice {
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
        NOOK_V520,
    }

    public enum BugDevice {
        NONE,
        SONY_RP1,
        EMULATOR,
    }

    public static final String PRODUCT;
    public static final boolean EINK_FREESCALE;
    public static final boolean EINK_ROCKCHIP;
    public static final boolean EINK_SUPPORT;
    public static final boolean EINK_FULL_SUPPORT;
    public static final boolean BUG_WAKELOCKS;

    private static final String MANUFACTURER;
    private static final String BRAND;
    private static final String MODEL;
    private static final String DEVICE;
    private static final boolean BOYUE_T61;
    private static final boolean BOYUE_T62;
    private static final boolean BOYUE_T80S;
    private static final boolean BOYUE_T80D;
    private static final boolean BOYUE_T78D;
    private static final boolean BOYUE_T103D;
    private static final boolean CREMA;
    private static final boolean ONYX_C67;
    private static final boolean ENERGY;
    private static final boolean INKBOOK;
    private static final boolean TOLINO;
    private static final boolean NOOK_V520;
    private static final boolean SONY_RP1;
    private static final boolean EMULATOR_X86;
    private static final boolean IS_BOYUE;

    // default values for generic devices.
    static EinkDevice EINK = EinkDevice.UNKNOWN;
    private static BugDevice BUG = BugDevice.NONE;

    static {
        // --------------- device probe --------------- //
        HashMap<EinkDevice, Boolean> deviceMap = new HashMap<>();
        HashMap<BugDevice, Boolean> bugMap = new HashMap<>();

        // we use the standard android build properties for device identification
        MANUFACTURER = getBuildField("MANUFACTURER");
        BRAND = getBuildField("BRAND");
        MODEL = getBuildField("MODEL");
        DEVICE = getBuildField("DEVICE");
        PRODUCT = getBuildField("PRODUCT");

        IS_BOYUE = MANUFACTURER.toLowerCase().contentEquals("boeye") ||
            MANUFACTURER.toLowerCase().contentEquals("boyue");

        // Boyue T62, manufacturer uses both "boeye" and "boyue" ids.
        BOYUE_T62 = IS_BOYUE
                && (PRODUCT.toLowerCase().startsWith("t62") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.toLowerCase().startsWith("t62");
        deviceMap.put(EinkDevice.BOYUE_T62, BOYUE_T62);

        // Boyue T61, uses RK3066 chipset
        BOYUE_T61 = IS_BOYUE
                && ( PRODUCT.toLowerCase().startsWith("t61") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("t61");
        deviceMap.put(EinkDevice.BOYUE_T61, BOYUE_T61);

        // Boyue Likebook Plus
        BOYUE_T80S = IS_BOYUE
                && PRODUCT.toLowerCase().contentEquals("t80s");
        deviceMap.put(EinkDevice.BOYUE_T80S, BOYUE_T80S);

        // Boyue Likebook Mars
        BOYUE_T80D = IS_BOYUE
                && PRODUCT.toLowerCase().contentEquals("t80d");
        deviceMap.put(EinkDevice.BOYUE_T80D, BOYUE_T80D);

        // Boyue Likebook Muses
        BOYUE_T78D = IS_BOYUE
                && PRODUCT.toLowerCase().contentEquals("t78d");
        deviceMap.put(EinkDevice.BOYUE_T78D, BOYUE_T78D);

        // Boyue Likebook Mimas
        BOYUE_T103D = IS_BOYUE
                && PRODUCT.toLowerCase().contentEquals("t103d");
        deviceMap.put(EinkDevice.BOYUE_T103D, BOYUE_T103D);

        // Crema Note (1010P)
        CREMA = BRAND.toLowerCase().contentEquals("crema")
                && PRODUCT.toLowerCase().contentEquals("note");
        deviceMap.put(EinkDevice.CREMA, CREMA);

        // Onyx C67
        ONYX_C67 = MANUFACTURER.toLowerCase().contentEquals("onyx")
                && ( PRODUCT.toLowerCase().startsWith("c67") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("c67");
        deviceMap.put(EinkDevice.ONYX_C67, ONYX_C67);

        // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
        ENERGY = (BRAND.toLowerCase().contentEquals("energysistem") || BRAND.toLowerCase().contentEquals("energy_sistem"))
                && MODEL.toLowerCase().startsWith("ereader");
        deviceMap.put(EinkDevice.ENERGY, ENERGY);

        // Artatech Inkbook Prime/Prime HD.
        INKBOOK = MANUFACTURER.toLowerCase().contentEquals("artatech")
                && BRAND.toLowerCase().contentEquals("inkbook")
                && MODEL.toLowerCase().startsWith("prime");
        deviceMap.put(EinkDevice.INKBOOK, INKBOOK);

        // Tolino
        TOLINO = (BRAND.toLowerCase().contentEquals("tolino") && (MODEL.toLowerCase().contentEquals("imx50_rdp")))
                || (MODEL.toLowerCase().contentEquals("tolino")
                && (DEVICE.toLowerCase().contentEquals("tolino_vision2") || DEVICE.toLowerCase().contentEquals("ntx_6sl")));
        deviceMap.put(EinkDevice.TOLINO, TOLINO);

        // Nook Glowlight 3
        NOOK_V520 = MANUFACTURER.toLowerCase().contentEquals("barnesandnoble")
                && MODEL.toLowerCase().contentEquals("bnrv520");
        deviceMap.put(EinkDevice.NOOK_V520, NOOK_V520);

        // Sony DPT-RP1
        SONY_RP1 = MANUFACTURER.toLowerCase().contentEquals("sony")
                && MODEL.toLowerCase().contentEquals("dpt-rp1");
        bugMap.put(BugDevice.SONY_RP1, SONY_RP1);

        // Android emulator for x86
        EMULATOR_X86 = MODEL.contentEquals("Android SDK built for x86");
        bugMap.put(BugDevice.EMULATOR, EMULATOR_X86);

        // find current eink device.
        Iterator<EinkDevice> einkIter = deviceMap.keySet().iterator();
        while (einkIter.hasNext()) {
            EinkDevice eink = einkIter.next();
            Boolean flag = deviceMap.get(eink);
            if ((flag != null) && flag) {
                EINK = eink;
            }
        }

        // find known bugs
        Iterator<BugDevice> bugIter = bugMap.keySet().iterator();
        while (bugIter.hasNext()) {
            BugDevice bug = bugIter.next();
            Boolean flag = bugMap.get(bug);
            if ((flag != null) && flag) {
                 BUG = bug;
            }
        }

        // freescale epd driver
        EINK_FREESCALE = (
            CREMA ||
            TOLINO ||
            NOOK_V520
        );

        // rockchip epd driver
        EINK_ROCKCHIP = (
            BOYUE_T61 ||
            BOYUE_T62 ||
            BOYUE_T78D ||
            BOYUE_T80D ||
            BOYUE_T103D ||
            ENERGY ||
            INKBOOK ||
            ONYX_C67
        );

        // basic eink support
        EINK_SUPPORT = (
            EINK_FREESCALE ||
            EINK_ROCKCHIP
        );

        // full eink support
        EINK_FULL_SUPPORT = (
            CREMA ||
            TOLINO
        );

        // need wakelocks
        BUG_WAKELOCKS = (
            (BUG == BugDevice.SONY_RP1)
        );
    }

    private static String getBuildField(String fieldName) {
        try {
            return (String)Build.class.getField(fieldName).get(null);
        } catch (Exception e) {
            return "";
        }
    }
}
