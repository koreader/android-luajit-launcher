package org.koreader.launcher.utils

import android.app.Activity
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.util.Locale
import java.util.concurrent.CountDownLatch

@Suppress("DEPRECATION")
object ScreenUtils {
    private const val TAG = "ScreenUtils"

    fun getScreenAvailableWidth(activity: Activity): Int {
        return getScreenSizeWithConstraints(activity).x
    }

    fun getScreenAvailableHeight(activity: Activity): Int {
        return getScreenSizeWithConstraints(activity).y
    }

    fun getScreenWidth(activity: Activity): Int {
        return getScreenSize(activity).x
    }

    fun getScreenHeight(activity: Activity): Int {
        return getScreenSize(activity).y
    }

    // DEPRECATED: returns 0 on API16+
    fun getStatusBarHeight(activity: Activity): Int {
        val rectangle = Rect()
        val window = activity.window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        return rectangle.top
    }

    fun isFullscreenDeprecated(activity: Activity): Boolean {
        return (activity.window.attributes.flags and
            WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
    }

    fun setFullscreenDeprecated(activity: Activity, fullscreen: Boolean) {
        val cd = CountDownLatch(1)
        activity.runOnUiThread {
            try {
                val window = activity.window
                if (fullscreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
            } catch (e: Exception) {
                Log.w(TAG, e.toString())
            }
            cd.countDown()
        }
        try {
            cd.await()
        } catch (ex: InterruptedException) {
            Log.e(TAG, ex.toString())
        }
    }

    fun pixelFormatName(format: Int): String {
        return when(format) {
            PixelFormat.OPAQUE -> "OPAQUE"
            PixelFormat.RGBA_1010102 -> "RGBA_1010102"
            PixelFormat.RGBA_8888 -> "RGBA_8888"
            PixelFormat.RGBA_F16 -> "RGBA_F16"
            PixelFormat.RGBX_8888 -> "RGBX_8888"
            PixelFormat.RGB_888 -> "RGB_888"
            PixelFormat.RGB_565 -> "RGB_565"
            PixelFormat.TRANSLUCENT -> "TRANSLUCENT"
            PixelFormat.TRANSPARENT -> "TRANSPARENT"
            else -> String.format(Locale.US, "Unknown: %d", format)
        }
    }

    private fun getScreenSize(activity: Activity): Point {
        val size = Point()
        val display = activity.windowManager.defaultDisplay

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)
            size.set(metrics.widthPixels, metrics.heightPixels)
        } else {
            display.getSize(size)
        }
        return size
    }

    private fun getScreenSizeWithConstraints(activity: Activity): Point {
        val size = Point()
        val display = activity.windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        size.set(metrics.widthPixels, metrics.heightPixels)
        return size
    }
}
