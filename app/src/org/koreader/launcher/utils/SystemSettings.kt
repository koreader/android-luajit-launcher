package org.koreader.launcher.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object SystemSettings {
    private const val TAG = "SystemSettings"

    fun canWrite(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.System.canWrite(context) else true
    }

    @SuppressWarnings("InlinedApi")
    fun getWriteSettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
    }

    fun getSystemScreenOffTimeout(context: Context): Int {
        return try {
            Settings.System.getInt(context.applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            Logger.w(TAG, e.toString())
            0
        }
    }

    fun setSystemScreenOffTimeout(context: Context, timeout: Int) {
        if (timeout <= 0) return
        try {
            Settings.System.putInt(context.applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
        }
    }
}
