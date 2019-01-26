/**
 * This file was created by unw on 15. 3. 31 as part of
 * https://github.com/unwmun/refreshU
 */

package org.koreader.device;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;


public class DeviceInfo {

    public enum Device {
        UNKNOWN,
        EINK_BOYUE_T61,
        EINK_BOYUE_T62,
        EINK_ONYX_C67,
        EINK_ENERGY,
    }

    public final static String MANUFACTURER;
    public final static String BRAND;
    public final static String MODEL;
    public final static String DEVICE;
    public final static String PRODUCT;

    public static final boolean EINK_BOYUE_T61;
    public static final boolean EINK_BOYUE_T62;
    public static final boolean EINK_ONYX_C67;
    public static final boolean EINK_ENERGY;

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
        deviceMap.put(Device.EINK_BOYUE_T62, EINK_BOYUE_T62);

        // Boyue T61, uses RK3066 chipset
        EINK_BOYUE_T61 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && ( PRODUCT.toLowerCase().startsWith("t61") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("t61");
        deviceMap.put(Device.EINK_BOYUE_T61, EINK_BOYUE_T61);

        // Onyx C67
        EINK_ONYX_C67 = MANUFACTURER.toLowerCase().contentEquals("onyx")
                && ( PRODUCT.toLowerCase().startsWith("c67") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("c67");
        deviceMap.put(Device.EINK_ONYX_C67, EINK_ONYX_C67);

        // Energy Sistem eReaders. Tested on Energy Ereader Pro 4
        EINK_ENERGY = BRAND.toLowerCase().contentEquals("energysistem")
                && MODEL.toLowerCase().startsWith("ereader");
        deviceMap.put(Device.EINK_ENERGY, EINK_ENERGY);

        // add your eink device here...


        // true if we found a supported device
        IS_EINK_SUPPORTED = (EINK_BOYUE_T62 || EINK_BOYUE_T61 || EINK_ONYX_C67 || EINK_ENERGY);

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
            Log.w("luajit-launcher", "Exception while trying to check Build." + fieldName);
            return "";
        }
    }
}
