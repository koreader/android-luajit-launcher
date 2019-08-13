package org.koreader.launcher;

/* wrapper on top of android.util.Log;
 *
 * Uses the application name as the logger tag.
 * Discards DEBUG messages on release builds.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Logger {
    private enum LogLevel { VERBOSE, DEBUG, INFO, WARNING, ERROR }

    public static void e(final String message) {
        doLog(formatMessage(null, message), LogLevel.ERROR);
    }
    public static void e(final String tag, final String message) {
        doLog(formatMessage(tag, message), LogLevel.ERROR);
    }
    public static void w(final String message) {
        doLog(formatMessage(null, message), LogLevel.WARNING);
    }
    public static void w(final String tag, final String message) {
        doLog(formatMessage(tag, message), LogLevel.WARNING);
    }
    public static void i(final String message) {
        doLog(formatMessage(null, message), LogLevel.INFO);
    }
    public static void i(final String tag, final String message) {
        doLog(formatMessage(tag, message), LogLevel.INFO);
    }
    public static void d(final String message) {
        if (BuildConfig.DEBUG) doLog(formatMessage(null, message), LogLevel.DEBUG);
    }
    public static void d(final String tag, final String message) {
        if (BuildConfig.DEBUG) doLog(formatMessage(tag, message), LogLevel.DEBUG);
    }
    public static void v(final String message) {
        doLog(formatMessage(null, message), LogLevel.VERBOSE);
    }
    public static void v(final String tag, final String message) {
        doLog(formatMessage(tag, message), LogLevel.VERBOSE);
    }

    /* format the message, using or not a subtag. */
    private static String formatMessage(final String subtag, final String message) {
        if (subtag != null) {
            return "[" + subtag + "] " + message;
        } else {
            return message;
        }
    }

    /* log using application name as the logger tag */
    private static void doLog(final String message, LogLevel level) {
        switch (level) {
            case ERROR:
                android.util.Log.e(MainApp.getName(), message);
                break;
            case WARNING:
                android.util.Log.w(MainApp.getName(), message);
                break;
            case INFO:
                android.util.Log.i(MainApp.getName(), message);
                break;
            case DEBUG:
                android.util.Log.d(MainApp.getName(), message);
                break;
            case VERBOSE:
                android.util.Log.v(MainApp.getName(), message);
                break;
            default:
                break;
        }
    }
}
