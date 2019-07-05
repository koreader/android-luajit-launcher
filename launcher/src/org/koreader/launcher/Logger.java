package org.koreader.launcher;

import android.util.Log;

public class Logger {
    public static void d(final String tag, final String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }
    public static void v(final String tag, final String message) {
        Log.v(tag, message);
    }
    public static void i(final String tag, final String message) {
        Log.i(tag, message);
    }
    public static void w(final String tag, final String message) {
        Log.w(tag, message);
    }
    public static void e(final String tag, final String message) {
        Log.e(tag, message);
    }
}
