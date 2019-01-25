package com.unw.device.epdcontrol.rockchip;

import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by unw on 15. 3. 25..
 */
public abstract class RK30xxEPDController
{
    enum EINK_MODE
    {
        EPD_AUTO,
        EPD_FULL,
        EPD_A2,
        EPD_PART,
        EPD_FULL_DITHER,

        EPD_RESET,
        EPD_BLACK_WHITE,
        EPD_TEXT,
        EPD_BLOCK,
        EPD_FULL_WIN,

        EPD_OED_PART,
        EPD_DIRECT_PART,
        EPD_DIRECT_A2,
        EPD_STANDBY,
        EPD_POWEROFF
    }

    public static final String EPD_NULL = "EPD_NULL";

    public static final String EPD_AUTO = "EPD_AUTO";
    public static final String EPD_FULL = "EPD_FULL";
    public static final String EPD_A2 = "EPD_A2";
    public static final String EPD_PART = "EPD_PART";
    public static final String EPD_FULL_DITHER = "EPD_FULL_DITHER";

    public static final String EPD_RESET = "EPD_RESET";
    public static final String EPD_BLACK_WHITE = "EPD_BLACK_WHITE";
    public static final String EPD_TEXT = "EPD_TEXT";
    public static final String EPD_BLOCK = "EPD_BLOCK";
    public static final String EPD_FULL_WIN = "EPD_FULL_WIN";

    public static final String EPD_OED_PART = "EPD_OED_PART";
    public static final String EPD_DIRECT_PART = "EPD_DIRECT_PART";
    public static final String EPD_DIRECT_A2 = "EPD_DIRECT_A2";
    public static final String EPD_STANDBY = "EPD_STANDBY";
    public static final String EPD_POWEROFF = "EPD_POWEROFF";

    protected static Class<Enum> eInkEnum;
    protected static Method requestEpdModeMethod1;
    protected static Method requestEpdModeMethod2;

    protected static Field isInA2;


    static {
        try {
            eInkEnum = (Class<Enum>) Class.forName("android.view.View$EINK_MODE");

            requestEpdModeMethod1 = View.class.getMethod("requestEpdMode", eInkEnum);

            requestEpdModeMethod2 = View.class.getMethod("requestEpdMode", eInkEnum, boolean.class);

            isInA2 = View.class.getDeclaredField("mIsInA2");
            isInA2.setAccessible(true);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static boolean isInA2(View view)
    {
        try {
            boolean value = (Boolean)isInA2.get(view);
            Log.d("TAGGG", "isInA2 : " + value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean requestEpdMode(View view, String mode) {

        try {
            requestEpdModeMethod1.invoke(view, stringToEnum(mode));
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean requestEpdMode(View view, String mode, boolean flag) {
        try {
            requestEpdModeMethod2.invoke(view, stringToEnum(mode), flag);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Object stringToEnum(String str)
    {
        return Enum.valueOf(eInkEnum, str);
    }

}
