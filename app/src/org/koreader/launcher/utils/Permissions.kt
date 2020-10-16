package org.koreader.launcher.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Permissions {
    private const val STORAGE_READ_ID = 1000
    private const val STORAGE_WRITE_ID = 1001

    fun hasStoragePermission(activity: Activity): Boolean {
        return hasPermissionGranted(activity, getApiPermission())
    }

    fun requestStoragePermission(activity: Activity) {
        val perm = getApiPermission()
        Logger.i("Requesting $perm permission")
        requestPermissions(activity, perm, getPermissionCode(perm))
    }

    fun hasPermissionGranted(activity: Activity, perm: String): Boolean {
        val state = ContextCompat.checkSelfPermission(activity, perm)
        return (state == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions(activity: Activity, perm: String, code: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(perm), code)
    }

    @SuppressLint("InlinedApi")
    fun hasWriteSettingsPermission(context: Context): Boolean {
        return if (isApi23()) Settings.System.canWrite(context) else true
    }
    @SuppressLint("InlinedApi")
    fun requestWriteSettingsPermission(activity: Activity) {
        if (isApi23()) activity.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))

    }

    @SuppressLint("InlinedApi")
    private fun getApiPermission(): String {
        return if (isApi30()) {
            Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
    }

    private fun getPermissionCode(perm: String): Int {
        return when (perm) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> STORAGE_WRITE_ID
            Manifest.permission.READ_EXTERNAL_STORAGE -> STORAGE_READ_ID
            else -> 0
        }
    }

    private fun isApi30(): Boolean {
        return (Build.VERSION.SDK_INT >= 30)
    }

    private fun isApi23(): Boolean {
        return (Build.VERSION.SDK_INT >= 23)
    }
}
