package org.koreader.launcher.extensions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File


fun Activity.getSdcardPath(): String? {
    val context = this.applicationContext
    val packageName = context.packageName
    val volumes: Array<out File> = ContextCompat.getExternalFilesDirs(context, null)
    return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        try {
            volumes[1].absolutePath.replace("/Android/data/$packageName/files", "")
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
}

@SuppressLint("NewApi")
fun Activity.hasStoragePermissionCompat(): Boolean {
    return if (newStoragePermissions(this)) {
        Environment.isExternalStorageManager()
    } else {
        return (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }
}

@SuppressLint("NewApi")
fun Activity.requestStoragePermissionCompat(code: Int, rationale: String) {
    return if (newStoragePermissions(this)) {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        requestSpecialPermission(this, intent, rationale, null, null)
    } else {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), code)

    }
}

@SuppressLint("NewApi")
fun Activity.isIgnoringBatteryOptimizationCompat(): Boolean {
    return if (newSettingsPermission()) {
        val pm = this.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(this.packageName)
    } else false
}

@SuppressLint("NewApi")
fun Activity.requestIgnoreBatteryOptimizationCompat(rationale: String,
                                                    okButton: String?, cancelButton: String?) {
    if (newSettingsPermission()) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        requestSpecialPermission(this, intent, rationale, okButton, cancelButton)
    }
}

@SuppressLint("NewApi")
fun Activity.hasWriteSettingsPermissionCompat(): Boolean {
    return if (newSettingsPermission())  {
        Settings.System.canWrite(this)
    } else true
}

@SuppressLint("NewApi")
fun Activity.requestWriteSettingsPermissionCompat(rationale: String,
                                                  okButton: String?, cancelButton: String?) {
    if (newSettingsPermission()) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        requestSpecialPermission(this, intent, rationale, okButton, cancelButton)
    }
}

private fun requestSpecialPermission(activity: Activity, intent: Intent, rationale: String,
                                     okButton: String?, cancelButton: String?) {
    activity.runOnUiThread {
        val ok = okButton ?: "OK"
        val builder = AlertDialog.Builder(activity)
            .setMessage(rationale)
            .setCancelable(false)
            .setPositiveButton(ok) { _, _ ->
                activity.startActivity(intent)
                activity.finish()
            }

        if (cancelButton != null) {
            builder.setNegativeButton(cancelButton) { _, _ -> }
        }
        builder.create().show()
    }
}

private fun newStoragePermissions(activity: Activity): Boolean {
    val targetSdk = activity.applicationContext.applicationInfo.targetSdkVersion
    val runtimeSdk = Build.VERSION.SDK_INT
    val minApi = Build.VERSION_CODES.R
    return ((runtimeSdk >= minApi) and (targetSdk >= minApi))
}

private fun newSettingsPermission(): Boolean {
    val runtimeSdk = Build.VERSION.SDK_INT
    val minApi = Build.VERSION_CODES.M
    return (runtimeSdk >= minApi)
}
