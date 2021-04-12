// A broadcast receiver that writes event codes to a named pipe,
// so they can be consumed by a lua loop.

package org.koreader.launcher

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.io.File
import java.io.FileWriter
import kotlin.collections.HashMap

class EventReceiver : BroadcastReceiver() {

    private val tag = "Broadcast Receiver"
    private val eventMap = HashMap<String, Int>()
    private val fifoPath: String = File(MainApp.assets_path, "alooper.fifo").path

    init {
        eventMap[Intent.ACTION_POWER_CONNECTED] = 100
        eventMap[Intent.ACTION_POWER_DISCONNECTED] = 101
        eventMap[DownloadManager.ACTION_DOWNLOAD_COMPLETE] = 110
    }

    // write messages to a named pipe.
    private fun post(code: Int?) {
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
                Logger.e(tag, "Cannot write to file $fifoPath: \n$e")
            }
        }

    }

    // event filter
    fun filter(): IntentFilter {
        val info = StringBuilder()
        val filter = IntentFilter()
        for ((key, _) in eventMap) {
            info.append("$key\n")
            filter.addAction(key)
        }

        Logger.v(tag, "Filtering ${eventMap.size} events: \n$info")
        return filter
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { event ->
            if (eventMap.containsKey(event.action)) {
                Logger.v(tag, "Received event ${event.action}")
                post(eventMap[event.action])
            }
        }
    }
}
