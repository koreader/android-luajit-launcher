package org.koreader.launcher.utils

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import org.koreader.launcher.Logger
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
                Logger.w(TAG, e.toString())
            }
            cd.countDown()
        }
        try {
            cd.await()
        } catch (ex: InterruptedException) {
            Logger.e(TAG, ex.toString())
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
