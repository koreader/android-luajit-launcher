package org.koreader.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StrictMode

@Suppress("DEPRECATION")
class MainApp : android.app.Application() {
    companion object {
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
        val packageInfo = pm.getPackageInfo(packageName, 0)
        val applicationInfo = pm.getApplicationInfo(packageName, 0)
        val hasLargeHeap = (applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0)
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1)
        val runtimeVersion = android.os.Build.VERSION.SDK_INT

        val assetsPath = filesDir.absolutePath
        val libraryPath = applicationInfo.nativeLibraryDir
        val storagePath = Environment.getExternalStorageDirectory().absolutePath

        val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val managedHeap = if (hasLargeHeap) am.largeMemoryClass else am.memoryClass

        val stringBuilder = StringBuilder(400)
        stringBuilder.append("{\n  VM heap: ")
            .append(managedHeap)
            .append("MB ")
            .append(if (hasLargeHeap) "(large)" else "(normal)")
            .append("\n  Flags: ")
            .append(if (isSystemApp) "system" else "user")
            .append(if (isDebuggable) ", debuggable" else ".")
            .append("\n  Paths {")
            .append("\n    Assets: ").append(assetsPath)
            .append("\n    Library: ").append(libraryPath)
            .append("\n    Storage: ").append(storagePath)
            .append("\n  }\n}")

        name = getString(packageInfo.applicationInfo.labelRes)
        info = stringBuilder.toString()
        storage_path = storagePath
        platform_type = if (pm.hasSystemFeature("org.chromium.arc.device_management")) {
            "chrome"
        } else if ((runtimeVersion >= 21) && pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            "android_tv"
        } else {
            "android"
        }

        if (isDebuggable) {
            val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            val vmPolicy = StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            StrictMode.setThreadPolicy(threadPolicy)
            StrictMode.setVmPolicy(vmPolicy)
        }
    }
}
