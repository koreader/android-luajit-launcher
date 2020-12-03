package org.koreader.launcher

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.util.concurrent.CountDownLatch

class Clipboard(activity: Activity) {

    private var clipboard: ClipboardManager = activity.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE)
        as ClipboardManager

    private inner class Box<T> {
        var value: T? = null
    }

    fun getClipboardText(activity: Activity): String {
        val result = Box<String>()
        val cd = CountDownLatch(1)
        activity.runOnUiThread {
            try {
                val data = clipboard.primaryClip
                if (data != null && data.itemCount > 0) {
                    val text = data.getItemAt(0).coerceToText(
                        activity.applicationContext)
                    if (text != null) {
                        result.value = text.toString()
                    } else {
                        result.value = ""
                    }
                }
            } catch (e: Exception) {
                Logger.w(e.toString())
                result.value = ""
            }
            cd.countDown()
        }
        try {
            cd.await()
        } catch (ex: InterruptedException) {
            return ""
        }
        return result.value ?: ""
    }

    fun hasClipboardText(): Boolean {
        val clipdata = clipboard.primaryClip
        return if (clipdata != null) {
            val number = clipdata.itemCount
            (number > 0)
        } else false
    }

    fun setClipboardText(activity: Activity, text: String) {
        activity.runOnUiThread {
            try {
                @Suppress("UsePropertyAccessSyntax")
                clipboard.setPrimaryClip(ClipData.newPlainText("KOReader_clipboard", text))
            } catch (e: Exception) {
                Logger.w(e.toString())
            }
        }
    }
}
