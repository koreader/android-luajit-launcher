package org.koreader.launcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import androidx.multidex.MultiDexApplication
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainApp : MultiDexApplication() {
    companion object {
        const val name = BuildConfig.APP_NAME
        const val flavor = BuildConfig.FLAVOR_CHANNEL
        const val has_ota_updates = BuildConfig.IN_APP_UPDATES
        const val supports_runtime_changes = BuildConfig.SUPPORTS_RUNTIME_CHANGES

        val is_debug = BuildConfig.DEBUG
        val provider = "${BuildConfig.APPLICATION_ID}.provider"

        // internal path for app files
        lateinit var assets_path: String
            private set

        // external path
        lateinit var storage_path: String
            private set

        // app dir in external path
        lateinit var app_storage_path: String
            private set

        private var targetSdk = 0

        // logcat to crash.log
        fun dumpLogcat() {
            writeLog()
        }

        // crash report
        fun crashReport(context: Context, reason: String? = null) {
            val reportIntent = Intent(context, CrashReportActivity::class.java)
            reportIntent.putExtra("title", "$name crashed")
            reportIntent.putExtra("reason", reason ?: "")
            reportIntent.putExtra("logs", getLog(true).toString())
            reportIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_TASK_ON_HOME or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                Intent.FLAG_ACTIVITY_NO_HISTORY
            context.startActivity(reportIntent)
        }

        // logcat -> stringBuilder
        private fun getLog(clearAfterDump: Boolean = false): StringBuilder {
            val log = StringBuilder()
            val proc = Runtime.getRuntime().exec("logcat -d -v time")
            val buffer = BufferedReader(InputStreamReader(proc.inputStream))
            while (true) {
                buffer.readLine()?.let { line ->
                    log.append("$line\n")
                } ?: break
            }
            if (clearAfterDump) {
                Runtime.getRuntime().exec("logcat -c")
            }
            return log
        }

        // stringBuilder -> file
        private fun writeLog(sb: StringBuilder? = getLog(),
                             path: String = app_storage_path,
                             name: String = "crash.log")
        {
            File(String.format("%s/%s", path, name)).let {
                try {
                    it.printWriter().use { log ->
                        log.print(sb.toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun isAtLeastApi(version: Int, runtimeOnly: Boolean = false): Boolean {
            return if (runtimeOnly) {
                (Build.VERSION.SDK_INT >= version)
            } else {
                ((Build.VERSION.SDK_INT >= version) and (targetSdk >= version))
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()
        assets_path = filesDir.absolutePath
        storage_path = Environment.getExternalStorageDirectory().absolutePath
        app_storage_path = String.format("%s/%s", storage_path, name.lowercase())
        targetSdk = applicationContext.applicationInfo.targetSdkVersion

        Thread.setDefaultUncaughtExceptionHandler { thread, _ ->
            val msg = "Uncaught exception in thread #${thread.id} (${thread.name})"
            crashReport(applicationContext, msg)
            kotlin.system.exitProcess(1)
        }

        if (is_debug) {
            // detect and log any potential issue within the vm.
            val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            val vmPolicy = StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            StrictMode.setThreadPolicy(threadPolicy)
            StrictMode.setVmPolicy(vmPolicy)
        }
    }
}
