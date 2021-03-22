package org.koreader.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StrictMode
import java.io.File

class MainApp : android.app.Application() {
    companion object {
        private val runtime = android.os.Build.VERSION.SDK_INT

        lateinit var info: String
            private set
        lateinit var fifo_path: String
            private set
        lateinit var storage_path: String
            private set
        lateinit var platform_type: String
            private set

    }

    override fun onCreate() {
        super.onCreate()
        val pm = packageManager
        val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val assetsDir = filesDir.absolutePath
        val isSystemApp = (pm.getPackageInfo(packageName, 0).applicationInfo.flags
            and ApplicationInfo.FLAG_SYSTEM == 1)

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

        // Path to the fifo for sending messages to ALooper (native glue and Lua)
        fifo_path = File(filesDir, "alooper.fifo").path

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
