// A broadcast receiver that writes event codes to a Unix domain socket, so they can be consumed from lua

package org.koreader.launcher

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.io.FileOutputStream
import android.os.ParcelFileDescriptor
import kotlin.collections.HashMap

class EventReceiver : BroadcastReceiver() {
    private val tag = this::class.java.simpleName
    private val eventMap = HashMap<String, Int>()
    private var eventOut: FileOutputStream? = null
    private var eventPfd: ParcelFileDescriptor? = null
    private var eventFd: Int = -1
    private val eventLock = Any()

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
                val out = ensureSocketOpen() ?: return
                // 32-bit event code, low byte first
                val msg = ByteArray(4)
                msg[0] = (it and 0xFF).toByte()
                msg[1] = ((it ushr 8) and 0xFF).toByte()
                msg[2] = ((it ushr 16) and 0xFF).toByte()
                msg[3] = ((it ushr 24) and 0xFF).toByte()
                out.write(msg)
                out.flush()
            } catch (e: Exception) {
                Log.e(tag, "Cannot write to event socket: \n$e")
            }
        } ?: Log.e(tag, "Invalid code: must be a 32-bit integer")
    }

    private fun ensureSocketOpen(): FileOutputStream? {
        synchronized(eventLock) {
            if (eventOut != null) {
                return eventOut
            }
            if (eventFd < 0) {
                eventFd = nativeGetEventSocketFd()
                if (eventFd < 0) {
                    Log.e(tag, "Event socket fd is not available")
                    return null
                }
            }
            eventPfd = ParcelFileDescriptor.adoptFd(eventFd)
            eventOut = FileOutputStream(eventPfd!!.fileDescriptor)
            return eventOut
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
        private external fun nativeGetEventSocketFd(): Int
    }
}
