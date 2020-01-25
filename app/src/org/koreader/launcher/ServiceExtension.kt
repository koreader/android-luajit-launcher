package org.koreader.launcher

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException

import org.koreader.IService
import org.koreader.launcher.interfaces.ServiceInterface
import org.koreader.launcher.utils.Logger

private const val ID = "org.koreader.service"
private const val TAG = "IPC Service"

class ServiceExtension: ServiceInterface {
    private val intent = Intent().apply {
        action = ID
        setPackage(ID)
    }

    private var remoteService: IService? = null
    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Logger.d(TAG, "onServiceConnected()")
            remoteService = IService.Stub.asInterface(service)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Logger.d(TAG, "onServiceDisconnected()")
            remoteService = null
        }
    }

    override fun bind(context: Context) {
        try {
            context.bindService(intent, conn, Service.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    override fun unbind(context: Context) {
        Logger.d(TAG, "onUnbind()")
        context.unbindService(conn)
    }

    override fun enabled(): Int {
        return if(isReady()) 1 else 0
    }

    override fun status(): String {
        return remoteService?.status() ?: "not available"
    }

    override fun setDim(level: Int) {
        if (!isReady()) return
        remoteService?.setDim(level)
    }

    override fun setDimColor(color: Int) {
        if (!isReady()) return
        remoteService?.setDimColor(color)
    }

    override fun setWarmth(level: Int) {
        if (!isReady()) return
        remoteService?.setWarmth(level)
    }

    override fun setWarmthAlpha(alpha: Float) {
        if (!isReady()) return
        remoteService?.let {
            try {
                it.setWarmthAlpha(alpha)
            } catch (re: RemoteException) {
                re.printStackTrace()
            }
        }
    }

    private fun isReady(): Boolean {
        return remoteService?.let {
            try {
                it.enabled()
            } catch (re: RemoteException) {
                re.printStackTrace()
                false
            }
        } ?: false
    }
}
