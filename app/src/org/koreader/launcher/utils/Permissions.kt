package org.koreader.launcher.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings

import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import org.koreader.launcher.R

object Permissions {
    private const val STORAGE_WRITE_ID = 1001

    fun hasStoragePermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            hasPermissionGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun hasWriteSettingsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            Settings.System.canWrite(context)
        } else true
    }

    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 30) {
            val perm = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            val rationale = activity.resources.getString(R.string.warning_manage_storage)
            requestSpecialPermission(activity, perm, rationale)
        } else {
            val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
            requestPermission(activity, perm, STORAGE_WRITE_ID)
        }
    }

    fun requestWriteSettingsPermission(activity: Activity, rationale: String, okButton: String, cancelButton: String) {
        if (Build.VERSION.SDK_INT >= 23) {
            requestSpecialPermission(activity, Settings.ACTION_MANAGE_WRITE_SETTINGS, rationale, okButton, cancelButton)
        }
    }

    private fun hasPermissionGranted(activity: Activity, perm: String): Boolean {
        val state = ContextCompat.checkSelfPermission(activity, perm)
        return (state == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission(activity: Activity, perm: String, code: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(perm), code)
    }

    private fun requestSpecialPermission(activity: Activity, perm: String, rationale: String, okButton: String?, cancelButton: String?) {
        activity.runOnUiThread {
            val ok = okButton ?: "OK"
            val builder = AlertDialog.Builder(activity)
                .setMessage(rationale)
                .setCancelable(false)
                .setPositiveButton(ok) { _, _ ->
                    activity.startActivity(Intent(perm))
                    activity.finish()
                }

            if (cancelButton != null) {
                builder.setNegativeButton(cancelButton) { _, _ -> }
            }
            builder.create().show()
        }
    }
}
