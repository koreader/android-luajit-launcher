package org.koreader.launcher

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StrictMode
import java.io.*

class MainApp : android.app.Application() {
    companion object {
        private val runtime = android.os.Build.VERSION.SDK_INT

        lateinit var info: String
            private set
        lateinit var storage_path: String
            private set
        lateinit var platform_type: String
            private set

        private lateinit var app: Application
        private lateinit var context: Context
        private lateinit var fifo_name: String
        private lateinit var alooper_fifo: FileWriter

        // feeds an 32 bit value to the fifo
        // low byte first
        fun feed_alooper_fifo(state: Int) {
            if (!this::app.isInitialized) {
                -1
            }

            if (!this::context.isInitialized) {
                try {
                    context = app.getApplicationContext()
                } catch (e: Exception) {
                    Logger.e("ERROR: could not get application context")
                    Logger.e("$e")
                }
            }
            if (!this::alooper_fifo.isInitialized) {
                try {
                    fifo_name = File(context.getFilesDir(),"alooper.fifo").getPath()
                    alooper_fifo = FileWriter(fifo_name, true)
                } catch (e: Exception) {
                    Logger.e("BroadcastReceiver: ERROR opening ALooper fifo: \"$fifo_name\"")
                    Logger.e("$e")
                }
            }

            try {
                val fifo_bytes = CharArray(4)
                fifo_bytes[0] = (state and 0xFF).toChar()
                fifo_bytes[1] = ((state ushr 8) and 0xFF).toChar()
                fifo_bytes[2] = ((state ushr 16) and 0xFF).toChar()
                fifo_bytes[3] = ((state ushr 24) and 0xFF).toChar()
                alooper_fifo.write(fifo_bytes, 0, 4)
                alooper_fifo.flush()
            } catch (e: Exception) {
                    Logger.e("BroadcastReceiver: ERROR writing to  ALooper fifo: \"$fifo_name\"")
                    Logger.e("$e")
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        val pm = packageManager
        val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val assetsDir = filesDir.absolutePath
        val isSystemApp = (pm.getPackageInfo(packageName, 0).applicationInfo.flags
            and ApplicationInfo.FLAG_SYSTEM == 1)

        app = this

        @Suppress("ConstantConditionIf")
        val flags: String = if (BuildConfig.DEBUG) {
            val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            val vmPolicy = StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            StrictMode.setThreadPolicy(threadPolicy)
            StrictMode.setVmPolicy(vmPolicy)
            if (isSystemApp) "system, debuggable" else "user,debuggable"
        } else {
            if (isSystemApp) "system" else "user"
        }

        @Suppress("DEPRECATION")
        storage_path = Environment.getExternalStorageDirectory().absolutePath
        platform_type = if (pm.hasSystemFeature("org.chromium.arc.device_management")) {
            "chrome"
        } else if ((runtime >= 21) && pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            "android_tv"
        } else {
            "android"
        }

        info = StringBuilder(400)
            .append("{\n  VM heap: ${am.memoryClass}MB\n  Flags: $flags\n  ")
            .append("Paths: {\n\tAssets: $assetsDir\n\tStorage: $storage_path\n  }\n}")
            .toString()
    }
}
