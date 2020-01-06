package org.koreader.launcher

import java.io.File

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import android.util.Log

@Suppress("ConstantConditionIf")
class MainApp : android.app.Application() {

    companion object {
        const val LEGACY_STORAGE = BuildConfig.legacy_storage

        lateinit var name: String
            private set
        lateinit var storage_path: String
            private set

        private const val UNKNOWN_STRING = "Unknown"
        private lateinit var assets_path: String
        private lateinit var library_path: String
        private var debuggable: Boolean = false
        private var is_system_app: Boolean = false
        private var large_heap: Boolean = false
        private var managed_heap: Int = 0
    }

    override fun onCreate() {
        super.onCreate()
        getAppInfo()
        Log.i(name, "Application started\n" + formatAppInfo())
    }

    /* app info into a String */
    private fun formatAppInfo(): String {
        val sb = StringBuilder(400)
        sb.append("Application info {")
        sb.append("\n  VM heap: ")
            .append(managed_heap)
            .append("MB ")
            .append(if (large_heap) "(large)" else "(normal)")
        sb.append("\n  Flags: ")
        sb.append(if (is_system_app) "system" else "user")
        sb.append(if (LEGACY_STORAGE) ", legacy" else ", scoped").append(" storage")
        sb.append(if (debuggable) ", debuggable" else ".")
        sb.append("\n  Paths {")
            .append("\n    Assets: ").append(assets_path)
            .append("\n    Library: ").append(library_path)
            .append("\n    Storage: ").append(storage_path)
            .append("\n  }\n}")
        return sb.toString()
    }

    /* get read-only app info */
    private fun getAppInfo() {
        try {
            val pm = packageManager
            val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pi = pm.getPackageInfo(packageName, 0)
            val ai = pm.getApplicationInfo(packageName, 0)

            name = getString(pi.applicationInfo.labelRes)
            library_path = ai.nativeLibraryDir
            assets_path = filesDir.absolutePath

            @Suppress("DEPRECATION")
            storage_path = if (LEGACY_STORAGE) {
                // deprecated in API 29
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                val writableFolder: File? = applicationContext.getExternalFilesDir(null)
                writableFolder?.absolutePath ?: UNKNOWN_STRING
            }

            if (ai.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) debuggable = true
            if (ai.flags and ApplicationInfo.FLAG_SYSTEM == 1) is_system_app = true
            if (ai.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0) large_heap = true

            managed_heap = getManagedMemory(am)

        } catch (e: Exception) {
            /* early exception, never reached.
               Use "unknown" to let the user face the crash via logcat */
            name = UNKNOWN_STRING
            assets_path = UNKNOWN_STRING
            library_path = UNKNOWN_STRING
            storage_path = UNKNOWN_STRING
        }
    }

    /* get managed heap size */
    private fun getManagedMemory(am: ActivityManager): Int {
        return if (large_heap) {
            am.largeMemoryClass
        } else {
            am.memoryClass
        }
    }
}
