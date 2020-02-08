package org.koreader.launcher.utils

import android.util.Log

import org.koreader.launcher.BuildConfig
import org.koreader.launcher.MainApp

/* wrapper on top of android.util.Log;
 *
 * Uses the application name as the logger tag.
 * Discards DEBUG messages on release builds.
 */

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
            LogLevel.ERROR -> Log.e(MainApp.name, message)
            LogLevel.WARNING -> Log.w(MainApp.name, message)
            LogLevel.INFO -> Log.i(MainApp.name, message)
            LogLevel.DEBUG -> Log.d(MainApp.name, message)
            LogLevel.VERBOSE -> Log.v(MainApp.name, message)
        }
    }
}
