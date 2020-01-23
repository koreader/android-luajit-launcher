package org.koreader.service;

@SuppressWarnings("unused")

class Logger {
    private static final String TAG = "KOService";
    static void v(String message) { android.util.Log.v(TAG, message); }
    static void d(String message) { android.util.Log.d(TAG, message); }
    static void i(String message) { android.util.Log.i(TAG, message); }
    static void w(String message) { android.util.Log.w(TAG, message); }
    static void e(String message) { android.util.Log.e(TAG, message); }
}
