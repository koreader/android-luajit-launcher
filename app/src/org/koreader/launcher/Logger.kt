package org.koreader.launcher

import android.util.Log

/* wrapper on top of android.util.Log;
 *
 * Uses the application name as the logger tag.
 * Discards DEBUG messages on release builds.
 */

@Suppress("ConstantConditionIf")
object Logger {
    private enum class LogLevel { VERBOSE, DEBUG, INFO, WARNING, ERROR }

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

    fun d(message: String) {
        if (BuildConfig.DEBUG)
            doLog(formatMessage(null, message), LogLevel.DEBUG)
    }
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG)
            doLog(formatMessage(tag, message), LogLevel.DEBUG)
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
            LogLevel.ERROR -> Log.e(BuildConfig.APP_NAME, message)
            LogLevel.WARNING -> Log.w(BuildConfig.APP_NAME, message)
            LogLevel.INFO -> Log.i(BuildConfig.APP_NAME, message)
            LogLevel.DEBUG -> Log.d(BuildConfig.APP_NAME, message)
            LogLevel.VERBOSE -> Log.v(BuildConfig.APP_NAME, message)
        }
    }
}
