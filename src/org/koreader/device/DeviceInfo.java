/**
 * This file was created by unw on 15. 3. 31 as part of
 * https://github.com/unwmun/refreshU
 */

package org.koreader.device;

import android.os.Build;
import java.util.HashMap;
import java.util.Iterator;


public class DeviceInfo {
    // has a working eink driver
    public static final boolean IS_EINK_SUPPORTED;

    // supported eink devices
    public static final boolean EINK_BOYUE_T61;
    public static final boolean EINK_BOYUE_T62;
    public static final boolean EINK_BOYUE_T80S;
    public static final boolean EINK_BOYUE_T80D;
    public static final boolean EINK_BOYUE_T78D;
    public static final boolean EINK_BOYUE_T103D;
    public static final boolean EINK_ONYX_C67;
    public static final boolean EINK_ENERGY;
    public static final boolean EINK_INKBOOK;
    public static final boolean EINK_GENERIC;

    // has a known bug that we need to handle
    public static final boolean HAS_BUGS;

    // known bugs
    public static final boolean BUG_NEEDS_WAKELOCKS = false;

    // EPD modes for rockchip devices
    // to-do: move them elsewhere...
    public final static int EPD_FULL = 1;
    public final static int EPD_PART = 2;
    public final static int EPD_A2 = 3;
    public final static int EPD_AUTO = 4;

    public enum Eink {
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
        GENERIC,
    }

    public enum Bug {
        NONE,
        NEEDS_WAKELOCKS,
    }

    /** perform a check on this specific device,
      starting with an unknown epd driver and no quirks */

    public static Eink EPD = Eink.UNKNOWN;
    public static Bug BUG = Bug.NONE;

    public final static String MANUFACTURER;
    public final static String BRAND;
    public final static String MODEL;
    public final static String DEVICE;
    public final static String PRODUCT;

    static {
        MANUFACTURER = getBuildField("MANUFACTURER");
        BRAND = getBuildField("BRAND");
        MODEL = getBuildField("MODEL");
        DEVICE = getBuildField("DEVICE");
        PRODUCT = getBuildField("PRODUCT");

        HashMap<Eink, Boolean> deviceMap = new HashMap<Eink, Boolean>();
        HashMap<Bug, Boolean> bugMap = new HashMap<Bug, Boolean>();

        // Boyue T62, manufacturer uses both "boeye" and "boyue" ids.
        EINK_BOYUE_T62 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && (PRODUCT.toLowerCase().startsWith("t62") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.toLowerCase().startsWith("t62");
        deviceMap.put(Eink.BOYUE_T62, EINK_BOYUE_T62);

        // Boyue T61, uses RK3066 chipset
        EINK_BOYUE_T61 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && ( PRODUCT.toLowerCase().startsWith("t61") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("t61");
        deviceMap.put(Eink.BOYUE_T61, EINK_BOYUE_T61);

        // Boyue Likebook Plus
        EINK_BOYUE_T80S = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t80s");
        deviceMap.put(Eink.BOYUE_T80S, EINK_BOYUE_T80S);

        // Boyue Likebook Mars
        EINK_BOYUE_T80D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t80d");
        deviceMap.put(Eink.BOYUE_T80D, EINK_BOYUE_T80D);

        // Boyue Likebook Muses
        EINK_BOYUE_T78D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t78d");
        deviceMap.put(Eink.BOYUE_T78D, EINK_BOYUE_T78D);

        // Boyue Likebook Mimas
        EINK_BOYUE_T103D = (MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue"))
                && PRODUCT.toLowerCase().contentEquals("t103d");
        deviceMap.put(Eink.BOYUE_T103D, EINK_BOYUE_T103D);

        // Onyx C67
        EINK_ONYX_C67 = MANUFACTURER.toLowerCase().contentEquals("onyx")
                && ( PRODUCT.toLowerCase().startsWith("c67") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("c67");
        deviceMap.put(Eink.ONYX_C67, EINK_ONYX_C67);

        // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
        EINK_ENERGY = (BRAND.toLowerCase().contentEquals("energysistem") || BRAND.toLowerCase().contentEquals("energy_sistem"))
                && MODEL.toLowerCase().startsWith("ereader");
        deviceMap.put(Eink.ENERGY, EINK_ENERGY);

        // Artatech Inkbook Prime/Prime HD.
        EINK_INKBOOK = MANUFACTURER.toLowerCase().contentEquals("artatech")
                && BRAND.toLowerCase().contentEquals("inkbook")
                && MODEL.toLowerCase().startsWith("prime");
        deviceMap.put(Eink.INKBOOK, EINK_INKBOOK);

        // Sony DPT-RP1 (generic epd, needs wakelocks)
        EINK_GENERIC = MANUFACTURER.toLowerCase().contentEquals("sony")
                && MODEL.toLowerCase().contentEquals("dpt-rp1");
        deviceMap.put(Eink.GENERIC, EINK_GENERIC);
        bugMap.put(Bug.NEEDS_WAKELOCKS, EINK_GENERIC);

        // add your device here...


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

        // true if we found a known bug
        HAS_BUGS = (
            BUG_NEEDS_WAKELOCKS
        );

        // find e-ink driver for current device
        Iterator<Eink> einkIterator = deviceMap.keySet().iterator();

        while (einkIterator.hasNext()) {
            Eink eink = einkIterator.next();
            Boolean flag = deviceMap.get(eink);

            if (flag) {
                EPD = eink;
            }
        }

        // find known bugs for current device
        Iterator<Bug> bugIterator = bugMap.keySet().iterator();

        while (bugIterator.hasNext()) {
            Bug bug = bugIterator.next();
            Boolean flag = bugMap.get(bug);

            if (flag) {
                BUG = bug;
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
