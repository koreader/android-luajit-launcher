package org.koreader.launcher.helper

import java.util.concurrent.CountDownLatch

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager

import org.koreader.launcher.Logger


class ClipboardHelper(context: Context) : BaseHelper(context) {

    private val clipboard = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE)
        as ClipboardManager

    var clipboardText: String
        get() {
            val result = Box<String>()
            val cd = CountDownLatch(1)
            runOnUiThread(Runnable {
                try {
                    if (clipboard.hasPrimaryClip()) {
                        val data = clipboard.primaryClip
                        if (data != null && data.itemCount > 0) {
                            val text = data.getItemAt(0).coerceToText(
                                    applicationContext)
                            if (text != null) {
                                result.value = text.toString()
                            } else {
                                result.value = ""
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.w(tag, e.toString())
                    result.value = ""
                }
                cd.countDown()
            })
            try {
                cd.await()
            } catch (ex: InterruptedException) {
                return ""
            }

            return if (result.value == null) {
                ""
            } else result.value.toString()
        }
        set(text) = runOnUiThread(Runnable {
            try {
                val clip = ClipData.newPlainText("KOReader_clipboard", text)
                clipboard.primaryClip = clip
            } catch (e: Exception) {
                Logger.w(tag, e.toString())
            }
        })

    fun hasClipboardText(): Int {
        return if (clipboardHasText()) 1 else 0
    }

    private fun clipboardHasText(): Boolean {
        return if (clipboard.hasPrimaryClip()) {
            val data = clipboard.primaryClip
            data != null && data.itemCount > 0
        } else false
    }

    private inner class Box<T> {
        internal var value: T? = null
    }
}
