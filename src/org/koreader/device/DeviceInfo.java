/**
 * Device info using Android build properties,
 * based on https://github.com/unwmun/refreshU
 *
 * Note: devices don't need to be declared here unless
 * they have known eink update routines and/or bug workarounds.
 */

package org.koreader.device;

import android.os.Build;
import java.util.HashMap;
import java.util.Iterator;


public class DeviceInfo {

    public final static String MANUFACTURER;
    public final static String BRAND;
    public final static String MODEL;
    public final static String DEVICE;
    public final static String PRODUCT;

    public enum EinkDevice {
        UNKNOWN,
        BOYUE_T61,
        BOYUE_T62,
        BOYUE_T80S,
        BOYUE_T80D,
        BOYUE_T78D,
        BOYUE_T103D,
        ONYX_C67,
        ENERGY,
        INKBOOK,
        TOLINO,
        NOOK_V520,
    }

    public static EinkDevice CURRENT_DEVICE = EinkDevice.UNKNOWN;

    public static final boolean BOYUE_T61;
    public static final boolean BOYUE_T62;
    public static final boolean BOYUE_T80S;
    public static final boolean BOYUE_T80D;
    public static final boolean BOYUE_T78D;
    public static final boolean BOYUE_T103D;
    public static final boolean ONYX_C67;
    public static final boolean ENERGY;
    public static final boolean INKBOOK;
    public static final boolean TOLINO;
    public static final boolean NOOK_V520;
    public static final boolean SONY_RP1;
    public static final boolean EMULATOR_X86;

    public static final boolean EINK_FREESCALE;
    public static final boolean EINK_ROCKCHIP;
    public static final boolean EINK_SUPPORT;
    public static final boolean EINK_FULL_SUPPORT;
    public static final boolean BUG_WAKELOCKS;

    static {
        // we use the standard android build properties for device id.
        MANUFACTURER = getBuildField("MANUFACTURER");
        BRAND = getBuildField("BRAND");
        MODEL = getBuildField("MODEL");
        DEVICE = getBuildField("DEVICE");
        PRODUCT = getBuildField("PRODUCT");

        HashMap<EinkDevice, Boolean> deviceMap = new HashMap<EinkDevice, Boolean>();

        // Boyue T62, manufacturer uses both "boeye" and "boyue" ids.
        BOYUE_T62 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && (PRODUCT.toLowerCase().startsWith("t62") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.toLowerCase().startsWith("t62");
        deviceMap.put(EinkDevice.BOYUE_T62, BOYUE_T62);

        // Boyue T61, uses RK3066 chipset
        BOYUE_T61 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && ( PRODUCT.toLowerCase().startsWith("t61") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("t61");
        deviceMap.put(EinkDevice.BOYUE_T61, BOYUE_T61);

        // Boyue Likebook Plus
        BOYUE_T80S = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t80s");
        deviceMap.put(EinkDevice.BOYUE_T80S, BOYUE_T80S);

        // Boyue Likebook Mars
        BOYUE_T80D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t80d");
        deviceMap.put(EinkDevice.BOYUE_T80D, BOYUE_T80D);

        // Boyue Likebook Muses
        BOYUE_T78D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t78d");
        deviceMap.put(EinkDevice.BOYUE_T78D, BOYUE_T78D);

        // Boyue Likebook Mimas
        BOYUE_T103D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t103d");
        deviceMap.put(EinkDevice.BOYUE_T103D, BOYUE_T103D);

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

        // Android emulator for x86
        EMULATOR_X86 = MODEL.contentEquals("Android SDK built for x86");

        // freescale epd driver
        EINK_FREESCALE = (
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

        // basic support
        EINK_SUPPORT = (
            EINK_FREESCALE ||
            EINK_ROCKCHIP
        );

        // full support
        EINK_FULL_SUPPORT = (
            TOLINO
        );

        // need wakelocks
        BUG_WAKELOCKS = (
            SONY_RP1
        );

        // find current device.
        Iterator<EinkDevice> iter = deviceMap.keySet().iterator();
        while (iter.hasNext()) {
            EinkDevice device = iter.next();
            Boolean flag = deviceMap.get(device);

            if (flag) {
                CURRENT_DEVICE = device;
            }
        }
    }

    private static String getBuildField(String fieldName) {
        try {
            return (String)Build.class.getField(fieldName).get(null);
        } catch (Exception e) {
            return "";
        }
    }
}
