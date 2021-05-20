package org.koreader.launcher

import android.util.Log

/* wrapper on top of android.util.Log;
 *
 * Uses app name as the log name if no tag is supplied
 * Discards DEBUG messages on release builds.
 */

object Logger {
    private enum class LogLevel { VERBOSE, DEBUG, INFO, WARNING, ERROR }

    fun e(message: String) {
        doLog(null, message, LogLevel.ERROR)
    }
    fun e(tag: String, message: String) {
        doLog(tag, message, LogLevel.ERROR)
    }

    fun w(message: String) {
        doLog(null, message, LogLevel.WARNING)
    }
    fun w(tag: String, message: String) {
        doLog(tag, message, LogLevel.WARNING)
    }

    fun i(message: String) {
        doLog(null, message, LogLevel.INFO)
    }
    fun i(tag: String, message: String) {
        doLog(tag, message, LogLevel.INFO)
    }

    fun d(message: String) {
        if (MainApp.is_debug)
            doLog(null, message, LogLevel.DEBUG)
    }
    fun d(tag: String, message: String) {
        if (MainApp.is_debug)
            doLog(tag, message, LogLevel.DEBUG)
    }

    fun v(message: String) {
        doLog(null, message, LogLevel.VERBOSE)
    }
    fun v(tag: String, message: String) {
        doLog(tag, message, LogLevel.VERBOSE)
    }

    private fun doLog(tag: String?, message: String, level: LogLevel) {
        val name = tag ?: MainApp.name
        when (level) {
            LogLevel.ERROR -> Log.e(name, message)
            LogLevel.WARNING -> Log.w(name, message)
            LogLevel.INFO -> Log.i(name, message)
            LogLevel.DEBUG -> Log.d(name, message)
            LogLevel.VERBOSE -> Log.v(name, message)
        }
    }
}
