package org.koreader.launcher.helper

import org.koreader.launcher.Logger


abstract class BaseHelper(context: android.content.Context) {

    // application context
    val applicationContext: android.content.Context = context.applicationContext

    // handler to forward messages to the main thread
    val handler = android.os.Handler(this.applicationContext.mainLooper)

    // subclass tag
    val tag: String = this.javaClass.simpleName

    init { Logger.d(tag, "Starting") }

    fun runOnUiThread(runnable: Runnable) {
        handler.post(runnable)
    }
}
