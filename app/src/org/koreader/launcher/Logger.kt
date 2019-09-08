package org.koreader.launcher

object Logger {
    private enum class LogLevel { ERROR, WARNING, INFO, DEBUG, VERBOSE }

    fun e(message: String) {
        doLog(formatMessage(null, message), LogLevel.ERROR)
    }
    fun e(tag: String, message: String) {
        doLog(formatMessage(tag, message), LogLevel.ERROR)
    }

    fun w(message: String) {
        doLog(formatMessage(null, message), LogLevel.WARNING)
    }
    fun w(tag: String, message: String) {
        doLog(formatMessage(tag, message), LogLevel.WARNING)
    }

    fun i(message: String) {
        doLog(formatMessage(null, message), LogLevel.INFO)
    }
    fun i(tag: String, message: String) {
        doLog(formatMessage(tag, message), LogLevel.INFO)
    }

    /* Discard DEBUG messages on release builds. */
    fun d(message: String) {
        if (BuildConfig.DEBUG) doLog(formatMessage(null, message), LogLevel.DEBUG)
    }
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) doLog(formatMessage(tag, message), LogLevel.DEBUG)
    }

    fun v(message: String) {
        doLog(formatMessage(null, message), LogLevel.VERBOSE)
    }
    fun v(tag: String, message: String) {
        doLog(formatMessage(tag, message), LogLevel.VERBOSE)
    }

    /* format the message, using or not a subtag. */
    private fun formatMessage(subtag: String?, message: String): String {
        return if (subtag != null) "[$subtag] $message" else message
    }

    /* log using application name as the logger tag */
    private fun doLog(message: String, level: LogLevel) {
        when (level) {
            Logger.LogLevel.ERROR -> android.util.Log.e(MainApp.name, message)
            Logger.LogLevel.WARNING -> android.util.Log.w(MainApp.name, message)
            Logger.LogLevel.INFO -> android.util.Log.i(MainApp.name, message)
            Logger.LogLevel.DEBUG -> android.util.Log.d(MainApp.name, message)
            Logger.LogLevel.VERBOSE -> android.util.Log.v(MainApp.name, message)
        }
    }
}
