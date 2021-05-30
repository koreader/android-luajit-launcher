package org.koreader.launcher

import android.os.StrictMode
import androidx.multidex.MultiDexApplication

class MainApp : MultiDexApplication() {
    companion object {
        const val name = BuildConfig.APP_NAME
        const val flavor = BuildConfig.FLAVOR_CHANNEL
        const val has_ota_updates = BuildConfig.IN_APP_UPDATES
        const val supports_runtime_changes = BuildConfig.SUPPORTS_RUNTIME_CHANGES
        const val provider = "${BuildConfig.APPLICATION_ID}.provider"
        val is_debug = BuildConfig.DEBUG
        lateinit var assets_path: String
            private set
    }

    override fun onCreate() {
        super.onCreate()
        if (is_debug) {
            val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            val vmPolicy = StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            StrictMode.setThreadPolicy(threadPolicy)
            StrictMode.setVmPolicy(vmPolicy)
        }
        assets_path = filesDir.absolutePath
    }
}
