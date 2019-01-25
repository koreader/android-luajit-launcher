package com.unw.device.epdcontrol;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by unw on 15. 3. 31..
 */
public class DeviceInfo {

    private static final String TAG = "DeviceInfo";

    public enum Device {
        UNKNOWN,
        EINK_BOYUE_T61,
        EINK_BOYUE_T62,
        EINK_ONYX_C67,
    }

    public final static String MANUFACTURER;
    public final static String MODEL;
    public final static String DEVICE;
    public final static String PRODUCT;

    public static Device CURRENT_DEVICE = Device.UNKNOWN;

    public static final boolean EINK_BOYUE_T61;
    public static final boolean EINK_BOYUE_T62;
    public static final boolean EINK_ONYX_C67;

    static {
        MANUFACTURER = getBuildField("MANUFACTURER");
        MODEL = getBuildField("MODEL");
        DEVICE = getBuildField("DEVICE");
        PRODUCT = getBuildField("PRODUCT");

        HashMap<Device, Boolean> deviceMap = new HashMap<Device, Boolean>();

        Log.i(TAG, getDeviceInfo());
        // 자기들 회사 이름도 모르는지 boeye로 나옴
        EINK_BOYUE_T62 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && (PRODUCT.toLowerCase().startsWith("t62") || MODEL.contentEquals("rk30sdk"))
                && DEVICE.toLowerCase().startsWith("t62");
        deviceMap.put(Device.EINK_BOYUE_T62, EINK_BOYUE_T62);

        // T61은 RK3066 칩셋 사용
        EINK_BOYUE_T61 = ( MANUFACTURER.toLowerCase().contentEquals("boeye") || MANUFACTURER.toLowerCase().contentEquals("boyue") )
                && ( PRODUCT.toLowerCase().startsWith("t61") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("t61");
        deviceMap.put(Device.EINK_BOYUE_T61, EINK_BOYUE_T61);

        // Onyx C67
        EINK_ONYX_C67 = MANUFACTURER.toLowerCase().contentEquals("onyx")
                && ( PRODUCT.toLowerCase().startsWith("c67") || MODEL.contentEquals("rk30sdk") )
                && DEVICE.toLowerCase().startsWith("c67");
        deviceMap.put(Device.EINK_ONYX_C67, EINK_ONYX_C67);

        // ~ 기타등등


        // 현재 장비 찾기
        Iterator<Device> iter = deviceMap.keySet().iterator();

        while (iter.hasNext()) {
            Device device = iter.next();
            Boolean flag = deviceMap.get(device);

            if (flag) {
                CURRENT_DEVICE = device;
            }
        }

        Log.i(TAG, "Current device is : " + CURRENT_DEVICE.name());
    }

    private static String getBuildField(String fieldName) {

        try {
            return (String)Build.class.getField(fieldName).get(null);
        } catch (Exception e) {
            Log.d(TAG, "Exception while trying to check Build." + fieldName);
            return "";
        }
    }

    public static String getDeviceInfo(){
        StringBuilder builder = new StringBuilder("DeviceInfo: ")
                .append("MANUFACTURER=").append(MANUFACTURER)
                .append(", MODEL=").append(MODEL)
                .append(", DEVICE=").append(DEVICE)
                .append(", PRODUCT=").append(PRODUCT);

        return builder.toString();
    }
}
