// A broadcast receiver that writes event codes to a Unix domain socket, so they can be consumed from lua

package org.koreader.launcher

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlin.collections.HashMap

class EventReceiver : BroadcastReceiver() {
    private val tag = this::class.java.simpleName
    private val eventMap = HashMap<String, Int>()

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

    public fun write(code: Int?) {
        if (code == null) {
            Log.e(tag, "Invalid code: must be a 32-bit integer")
            return
        }

        try {
            val rc = nativeSendEvent(code)
            if (rc != 0) {
                Log.e(tag, "nativeSendEvent failed with code $rc")
            }
        } catch (e: Throwable) {
            Log.e(tag, "Cannot send event to native socket: $e")
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { event ->
            if (eventMap.containsKey(event.action)) {
                Log.v(tag, "Received event ${event.action}")
                write(eventMap[event.action])
            }
        }
    }

    companion object {
        @JvmStatic
        private external fun nativeSendEvent(code: Int): Int
    }
}
