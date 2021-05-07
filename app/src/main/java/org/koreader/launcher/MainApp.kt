package org.koreader.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StrictMode

class MainApp : android.app.Application() {
    companion object {

        // build time
        const val name = BuildConfig.APP_NAME
        const val flavor = BuildConfig.FLAVOR_CHANNEL
        const val has_ota_updates = BuildConfig.IN_APP_UPDATES
        const val supports_runtime_changes = BuildConfig.SUPPORTS_RUNTIME_CHANGES
        const val provider = "${BuildConfig.APPLICATION_ID}.provider"

        val is_debug = BuildConfig.DEBUG


        // runtime dependant
        private val runtime = android.os.Build.VERSION.SDK_INT

        lateinit var info: String
            private set
        lateinit var assets_path: String
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
        assets_path = filesDir.absolutePath
        val isSystemApp = (pm.getPackageInfo(packageName, 0).applicationInfo.flags
            and ApplicationInfo.FLAG_SYSTEM == 1)

        val flags: String = if (is_debug) {
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
            .append("Paths: {\n\tAssets: $assets_path\n\tStorage: $storage_path\n  }\n}")
            .toString()
    }
}
