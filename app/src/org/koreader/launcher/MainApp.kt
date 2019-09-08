package org.koreader.launcher

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.PowerManager
import android.util.Log


class MainApp : android.app.Application() {

    companion object {

        var name: String? = null
            private set

        private var assets_path: String? = null
        private var library_path: String? = null
        private var storage_path: String? = null
        private var system_app: Boolean = false
        private var debuggable: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        getAppInfo()
        Log.i(name, "Application started")
        Log.v(name, formatAppInfo())
    }

    /* app information into a String */
    private fun formatAppInfo(): String {
        val sb = StringBuilder(400)
        sb.append("Application info {\n  Flags: ")
        if (system_app) sb.append("system") else sb.append("user")
        if (debuggable) sb.append(", debuggable")

        sb.append("\n  Paths {")
            .append("\n    Assets: ").append(assets_path)
            .append("\n    Library: ").append(library_path)
            .append("\n    Storage: ").append(storage_path).append("\n  }\n}")
        return sb.toString()
    }

    /* get app information that doesn't change during the lifetime of the application. */
    private fun getAppInfo() {
        try {
            val pm = packageManager
            val pi = pm.getPackageInfo(packageName, 0)
            val ai = pm.getApplicationInfo(packageName, 0)
            name = getString(pi.applicationInfo.labelRes)
            library_path = ai.nativeLibraryDir
            assets_path = filesDir.absolutePath
            storage_path = Environment.getExternalStorageDirectory().absolutePath
            if (ai.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) debuggable = true
            if (ai.flags and ApplicationInfo.FLAG_SYSTEM == 1) system_app = true
        } catch (e: Exception) {
            name = "MainApp"
            assets_path = "?"
            library_path = "?"
            storage_path = "?"
        }
    }
}
