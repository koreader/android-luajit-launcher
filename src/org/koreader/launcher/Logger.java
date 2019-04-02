package org.koreader.launcher;

import android.util.Log;

public class Logger {
    public static void d(final String TAG, final String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }
    public static void v(final String TAG, final String message) {
        Log.v(TAG, message);
    }
    public static void i(final String TAG, final String message) {
        Log.i(TAG, message);
    }
    public static void w(final String TAG, final String message) {
        Log.w(TAG, message);
    }
    public static void e(final String TAG, final String message) {
        Log.e(TAG, message);
    }
}
