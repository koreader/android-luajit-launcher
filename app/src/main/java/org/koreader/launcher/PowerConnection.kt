package org.koreader.launcher

import android.os.BatteryManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koreader.launcher.interfaces.JNILuaInterface


class PowerConnection : BroadcastReceiver() {
    var isPowerConnected = 666

	override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
		when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
				this.isPowerConnected = 1
			}
			Intent.ACTION_POWER_DISCONNECTED -> {
				this.isPowerConnected = 0
			}
		}
	}
}
