package org.koreader.launcher

import android.os.BatteryManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
//import org.koreader.launcher.interfaces.JNILuaInterface


class PowerConnection : BroadcastReceiver() {
    var isPowerConnected = -1
    var oldPowerConnection = -1

	override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
		when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
				this.isPowerConnected = 1
				// replace this by a better method to send a keyevent to self (so ALooper_pollAll wakes up)
				Runtime.getRuntime().exec("su -c input keyevent KEYCODE_BUTTON_A");
			}
			Intent.ACTION_POWER_DISCONNECTED -> {
				this.isPowerConnected = 0
				// wake up ALooper_pollAll
				Runtime.getRuntime().exec("su -c input keyevent KEYCODE_BUTTON_A");
			}
		}
	}

	fun connectionEvent(): Boolean {
        if (this.isPowerConnected != this.oldPowerConnection) {
                            Logger.e("xxxxxxxxxx connectionEvent true")

            this.oldPowerConnection = this.isPowerConnected
            return true
        } else {
                            Logger.e("xxxxxxxxxx connectionEvent false")

            return false
        }
    }
}
