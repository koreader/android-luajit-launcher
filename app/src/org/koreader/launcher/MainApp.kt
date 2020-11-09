package org.koreader.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StrictMode

@Suppress("ConstantConditionIf")
class MainApp : android.app.Application() {
    companion object {
        const val debuggable = BuildConfig.DEBUG
        const val flavor = BuildConfig.FLAVOR_flavor
        private val runtime = android.os.Build.VERSION.SDK_INT

        lateinit var name: String
            private set
        lateinit var info: String
            private set
        lateinit var storage_path: String
            private set
        lateinit var platform_type: String
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val pm = packageManager
        val pi = pm.getPackageInfo(packageName, 0)
        val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isSystemApp = (pi.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1)

        name = getString(pi.applicationInfo.labelRes)
        storage_path = Environment.getExternalStorageDirectory().absolutePath

        platform_type = if (pm.hasSystemFeature("org.chromium.arc.device_management")) {
            "chrome"
        } else if ((runtime >= 21) && pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            "android_tv"
        } else {
            "android"
        }

        info = StringBuilder(400)
            .append("{\n  VM heap: ")
            .append(am.memoryClass)
            .append("MB")
            .append("\n  Flags: ")
            .append(if (isSystemApp) "system" else "user")
            .append(if (debuggable) ", debuggable" else ".")
            .append("\n  Paths {")
            .append("\n    Assets: ").append(filesDir.absolutePath)
            .append("\n    Storage: ").append(storage_path)
            .append("\n  }\n}")
            .toString()

        if (debuggable) {
            val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            val vmPolicy = StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            StrictMode.setThreadPolicy(threadPolicy)
            StrictMode.setVmPolicy(vmPolicy)
        }
    }
}
