/**
 * This file was created by unw on 15. 3. 31 as part of
 * https://github.com/unwmun/refreshU
 */

package org.koreader.device;

import android.os.Build;
import java.util.HashMap;
import java.util.Iterator;


public class DeviceInfo {

    public enum Device {
        // unkown devices
        UNKNOWN,
        // devices with supported driver
        BOYUE_T61,
        BOYUE_T62,
        BOYUE_T80S,
        BOYUE_T80D,
        BOYUE_T78D,
        BOYUE_T103D,
        ONYX_C67,
        ENERGY,
        INKBOOK,
        // devices using a generic workaround
        SONY_RP1,
        NOOK_V520,
        EMULATOR_X86,
    }

    public final static int EPD_FULL = 1;
    public final static int EPD_PART = 2;
    public final static int EPD_A2 = 3;
    public final static int EPD_AUTO = 4;

    public final static String MANUFACTURER;
    public final static String BRAND;
    public final static String MODEL;
    public final static String DEVICE;
    public final static String PRODUCT;

    public static final boolean EINK_BOYUE_T61;
    public static final boolean EINK_BOYUE_T62;
    public static final boolean EINK_BOYUE_T80S;
    public static final boolean EINK_BOYUE_T80D;
    public static final boolean EINK_BOYUE_T78D;
    public static final boolean EINK_BOYUE_T103D;
    public static final boolean EINK_ONYX_C67;
    public static final boolean EINK_ENERGY;
    public static final boolean EINK_INKBOOK;
    public static final boolean EINK_SONY_RP1;
    public static final boolean EINK_NOOK_V520;
    public static final boolean EINK_EMULATOR_X86;
    public static final boolean EINK_GENERIC;

    public static final boolean IS_EINK_SUPPORTED;
    public static Device CURRENT_DEVICE = Device.UNKNOWN;

    static {
        MANUFACTURER = getBuildField("MANUFACTURER");
        BRAND = getBuildField("BRAND");
        MODEL = getBuildField("MODEL");
        DEVICE = getBuildField("DEVICE");
        PRODUCT = getBuildField("PRODUCT");

        HashMap<Device, Boolean> deviceMap = new HashMap<Device, Boolean>();

        // Boyue T62, manufacturer uses both "boeye" and "boyue" ids.
        EINK_BOYUE_T62 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && (PRODUCT.toLowerCase().startsWith("t62") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.toLowerCase().startsWith("t62");
        deviceMap.put(Device.BOYUE_T62, EINK_BOYUE_T62);

        // Boyue T61, uses RK3066 chipset
        EINK_BOYUE_T61 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && ( PRODUCT.toLowerCase().startsWith("t61") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("t61");
        deviceMap.put(Device.BOYUE_T61, EINK_BOYUE_T61);

        // Boyue Likebook Plus
        EINK_BOYUE_T80S = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t80s");
        deviceMap.put(Device.BOYUE_T80S, EINK_BOYUE_T80S);

        // Boyue Likebook Mars
        EINK_BOYUE_T80D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t80d");
        deviceMap.put(Device.BOYUE_T80D, EINK_BOYUE_T80D);

        // Boyue Likebook Muses
        EINK_BOYUE_T78D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t78d");
        deviceMap.put(Device.BOYUE_T78D, EINK_BOYUE_T78D);

        // Boyue Likebook Mimas
        EINK_BOYUE_T103D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t103d");
        deviceMap.put(Device.BOYUE_T103D, EINK_BOYUE_T103D);

        // Onyx C67
        EINK_ONYX_C67 = MANUFACTURER.toLowerCase().contentEquals("onyx")
                && ( PRODUCT.toLowerCase().startsWith("c67") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("c67");
        deviceMap.put(Device.ONYX_C67, EINK_ONYX_C67);

        // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
        EINK_ENERGY = (BRAND.toLowerCase().contentEquals("energysistem") || BRAND.toLowerCase().contentEquals("energy_sistem"))
                && MODEL.toLowerCase().startsWith("ereader");
        deviceMap.put(Device.ENERGY, EINK_ENERGY);

        // Artatech Inkbook Prime/Prime HD.
        EINK_INKBOOK = MANUFACTURER.toLowerCase().contentEquals("artatech")
                && BRAND.toLowerCase().contentEquals("inkbook")
                && MODEL.toLowerCase().startsWith("prime");
        deviceMap.put(Device.INKBOOK, EINK_INKBOOK);

        // Sony DPT-RP1
        EINK_SONY_RP1 = MANUFACTURER.toLowerCase().contentEquals("sony")
                && MODEL.toLowerCase().contentEquals("dpt-rp1");
        deviceMap.put(Device.SONY_RP1, EINK_SONY_RP1);

        // Nook Glowlight 3
        EINK_NOOK_V520 = MANUFACTURER.toLowerCase().contentEquals("barnesandnoble")
                && MODEL.toLowerCase().contentEquals("bnrv520");
        deviceMap.put(Device.NOOK_V520, EINK_NOOK_V520);

        // Android Emulator for x86
        EINK_EMULATOR_X86 = MODEL.contentEquals("Android SDK built for x86");
        deviceMap.put(Device.EMULATOR_X86, EINK_EMULATOR_X86);

        // add your eink device here...


        // true if we use a generic workaround
        EINK_GENERIC = (
            EINK_SONY_RP1 ||
            EINK_NOOK_V520 ||
            EINK_EMULATOR_X86
        );

        // true if we found a supported device
        IS_EINK_SUPPORTED = (
            EINK_BOYUE_T61 ||
            EINK_BOYUE_T62 ||
            EINK_BOYUE_T78D ||
            EINK_BOYUE_T80D ||
            EINK_BOYUE_T103D ||
            EINK_ENERGY ||
            EINK_INKBOOK ||
            EINK_ONYX_C67 ||
            EINK_GENERIC
        );

        // find current device.
        Iterator<Device> iter = deviceMap.keySet().iterator();

        while (iter.hasNext()) {
            Device device = iter.next();
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
