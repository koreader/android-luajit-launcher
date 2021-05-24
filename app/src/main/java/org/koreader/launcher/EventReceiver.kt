// A broadcast receiver that writes event codes to a named pipe, so they can be consumed from lua

package org.koreader.launcher

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.Keep
import java.io.File
import java.io.FileWriter
import kotlin.collections.HashMap

@Keep
class EventReceiver : BroadcastReceiver() {
    private val tag = this::class.java.simpleName
    private val eventMap = HashMap<String, Int>()
    private val fifoPath: String = File(MainApp.assets_path, "alooper.fifo").path

    init {
        eventMap[Intent.ACTION_POWER_CONNECTED] = 100
        eventMap[Intent.ACTION_POWER_DISCONNECTED] = 101
        eventMap[DownloadManager.ACTION_DOWNLOAD_COMPLETE] = 110
    }

    val filter: IntentFilter
        get() {
            val info = StringBuilder()
            val filter = IntentFilter()
            for ((key, _) in eventMap) {
                info.append("$key\n".padStart(2))
                filter.addAction(key)
            }

            Log.v(tag, "Filtering ${eventMap.size} events: \n$info")
            return filter
        }

    private fun write(code: Int?) {
        code?.let {
            try {
                // 32-bit event code, low byte first
                val msg  = CharArray(4)
                msg[0] = (it and 0xFF).toChar()
                msg[1] = ((it ushr 8) and 0xFF).toChar()
                msg[2] = ((it ushr 16) and 0xFF).toChar()
                msg[3] = ((it ushr 24) and 0xFF).toChar()
                val writer = FileWriter(fifoPath, true)
                writer.write(msg, 0, 4)
                writer.close()
            } catch (e: Exception) {
                Log.e(tag, "Cannot write to file $fifoPath: \n$e")
            }
        } ?: Log.e(tag, "Invalid code: must be a 32-bit integer")
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { event ->
            if (eventMap.containsKey(event.action)) {
                Log.v(tag, "Received event ${event.action}")
                write(eventMap[event.action])
            }
        }
    }
}
