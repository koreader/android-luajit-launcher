package org.koreader.launcher.device

import android.util.Log
import kotlin.math.abs

open class Ioctl {

    private val tag = this::class.java.simpleName

    init {
        Log.i(tag, "loading libioctl")
        System.loadLibrary("ioctl")
    }

    private external fun ioctl(device: String, command: Int, args: Int): Int

    companion object {
        private const val ENOENT = 2
        private const val EBADF = 9
        private const val EACCES = 13
        private const val EFAULT = 14
        private const val EBUSY = 16
        private const val ENODEV = 19
        private const val EINVAL = 22
        private const val ENOTTY = 25
    }

    fun io(device: String, command: Int, args: Int): Boolean {
        val status = ioctl(device, command, args)
        return if (status >= 0) {
            Log.v(tag, "$device: ioctl ok, code $status")
            true
        } else {
            val err = when (abs(status)) {
                ENOENT -> "no such file or directory"
                EBADF -> "bad file number"
                EACCES -> "permission denied"
                EFAULT -> "bad address"
                EBUSY -> "device or resource busy"
                ENODEV -> "no such device"
                EINVAL -> "bad argument"
                ENOTTY -> "not a typewriter"
                else -> "unknown error ${abs(status)}"
            }

            Log.w(tag, "$device: ioctl failed: $err")
            false
        }
    }
}
