package org.koreader.launcher

import java.util.Locale

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/* MainActivity.java
 *
 * Implements e-ink updates
 * Implements WRITE_EXTERNAL_STORAGE/WRITE_SETTINGS permissions
 *
 * Takes care of most activity callbacks that are view-aware.
 * It handles custom timeout based on activity state too.
 */

class MainActivity : BaseActivity() {

    // EPD driver for this device
    private val epd = EPDFactory.epdController

    // Some e-ink devices need to take control of the native window from the java side
    private var takesWindowOwnership: Boolean = false

    // Use this setting to avoid screen dimming
    private var isScreenAlwaysOn: Boolean = false

    // Use this setting to override screen off timeout system setting
    private var isCustomTimeout: Boolean = false

    // Use this setting to ignore system settings and always rumble when requested
    private var isHapticOverride: Boolean = false

    // When custom timeouts are used these are the current timeouts for activity and system
    private var systemTimeout: Int = 0
    private var appTimeout: Int = 0

    companion object {
        private const val HAPTIC_OVERRIDE = 2
        private const val TAG_MAIN = "MainActivity"
        private const val TAG_WINDOW_MANAGER = "WindowManager"
        private const val TIMEOUT_MIN = 2 * 60 * 1000
        private const val TIMEOUT_MAX = 45 * 60 * 1000
        private const val SCREEN_ON_ENABLED = -1
        private const val SCREEN_ON_DISABLED = 0
        private const val WRITE_STORAGE = 1
    }

    // Dumb surface used on Tolinos and other ntx boards to refresh the e-ink screen
    private var view: NativeSurfaceView? = null
    private class NativeSurfaceView(context: Context): SurfaceView(context),
        SurfaceHolder.Callback {
        val tag = "NativeSurfaceView"
        init { holder.addCallback(this) }
        override fun surfaceCreated(holder: SurfaceHolder) {
            Logger.v(tag, "surface created")
            setWillNotDraw(false)
        }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Logger.v(tag, String.format(Locale.US,
                "surface changed {\n  width:  %d\n  height: %d\n}", width, height))
        }
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Logger.v(tag, "surface destroyed")
        }
    }

    /*---------------------------------------------------------------
     *                        activity callbacks                    *
     *--------------------------------------------------------------*/

    /* Called when the activity is first created. */
    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.d(TAG_MAIN, "onCreate()")
        super.onCreate(savedInstanceState)
        if ("freescale" == getEinkPlatform()) {
            Logger.v(TAG_MAIN, "onNativeSurfaceViewImpl()")
            view = NativeSurfaceView(this)
            window.takeSurface(null)
            view?.holder?.addCallback(this)
            setContentView(view)
            takesWindowOwnership = true
        } else {
            /* native content without further processing */
            Logger.v(TAG_MAIN, "onNativeWindowImpl()")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setFullscreenLayout()
            val decorView = window.decorView
            decorView.setOnSystemUiVisibilityChangeListener { setFullscreenLayout() }
        }
        requestExternalStoragePermission()
        systemTimeout = SystemSettings.getSystemScreenOffTimeout(this)
    }

    /* Called when the activity has become visible. */
    override fun onResume() {
        Logger.d(TAG_MAIN, "onResume()")
        super.onResume()
        applyCustomTimeout(true)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val handler = Handler()
            handler.postDelayed({ setFullscreenLayout() }, 500)
        }
    }

    /* Called when another activity is taking focus. */
    override fun onPause() {
        Logger.d(TAG_MAIN, "onPause()")
        super.onPause()
        applyCustomTimeout(false)
    }

    /* Called just before the activity is resumed by an intent */
    override fun onNewIntent(intent: Intent) {
        Logger.d(TAG_MAIN, "onNewIntent()")
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /* Called on permission result */
    override fun onRequestPermissionsResult(requestCode: Int, permissions:
        Array<String>, grantResults: IntArray) {
        Logger.d(TAG_MAIN, "onRequestPermissionResult()")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(this,
                        permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Logger.v(TAG_MAIN, String.format(Locale.US,
                    "Permission granted for request code: %d", requestCode))
        } else {
            val msg = String.format(Locale.US,
                    "Permission rejected for request code %d", requestCode)
            if (requestCode == WRITE_STORAGE) {
                Logger.e(TAG_MAIN, msg)
            } else {
                Logger.w(TAG_MAIN, msg)
            }
        }
    }

    /* Called when the activity is going to be destroyed */
    public override fun onDestroy() {
        Logger.d(TAG_MAIN, "onDestroy()")
        super.onDestroy()
    }

    /*---------------------------------------------------------------
     *             override methods used by lua/JNI                *
     *--------------------------------------------------------------*/

    override fun canWriteSystemSettings(): Int {
        return if (SystemSettings.canWrite(this)) 1 else 0
    }

    // update the entire screen (rockchip)
    override fun einkUpdate(mode: Int) {
        val modeName = when (mode) {
            1 -> "EPD_FULL"
            2 -> "EPD_PART"
            3 -> "EPD_A2"
            4 -> "EPD_AUTO"
            else -> "invalid"
        }

        if (modeName != "invalid") {
            Logger.v(TAG_MAIN, String.format(Locale.US,
                "requesting epd update, type: %s", modeName))

            if (takesWindowOwnership and (view != null)) {
                epd.setEpdMode(view as View, 0, 0, 0, 0, 0, 0, modeName)
            } else {
                val rootView = window.decorView.findViewById<View>(android.R.id.content)
                epd.setEpdMode(rootView, 0, 0, 0, 0, 0, 0, modeName)
            }
        }
    }

    // update a region or the entire screen (freescale)
    override fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int) {

        Logger.v(TAG_MAIN, String.format(Locale.US,
                "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
                mode, delay, x, y, width, height))

        if (takesWindowOwnership) {
            epd.setEpdMode(view as View, mode, delay, x, y, width, height, null)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            epd.setEpdMode(rootView, mode, delay, x, y, width, height, null)
        }
    }

    override fun getScreenOffTimeout(): Int {
        // return current setting
        return SystemSettings.getSystemScreenOffTimeout(this)
    }

    override fun getSystemTimeout(): Int {
        // return last known value
        return systemTimeout
    }

    override fun hasExternalStoragePermission(): Int {
        return if (ContextCompat.checkSelfPermission(this@MainActivity,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            1 else 0
    }

    override fun performHapticFeedback(constant: Int) {
        if (!takesWindowOwnership) {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            hapticFeedback(constant, rootView)
        }
    }

    override fun requestWriteSystemSettings() {
        val intent = SystemSettings.getWriteSettingsIntent()
        startActivity(intent)
    }

    override fun setHapticOverride(enabled: Boolean) {
        isHapticOverride = enabled
    }

    override fun setScreenOffTimeout(timeout: Int) {
        when {
            timeout > SCREEN_ON_DISABLED -> {
                // custom timeout
                isCustomTimeout = true
                appTimeout = safeTimeout(timeout)
                val mins = toMin(appTimeout)
                setScreenOn(false)
                Logger.d("SystemSettings",
                    "applying activity custom timeout: $mins minutes")
                SystemSettings.setSystemScreenOffTimeout(this, appTimeout)
            }
            timeout == SCREEN_ON_ENABLED -> {
                // always on
                isCustomTimeout = false
                appTimeout = 0
                setScreenOn(true)
            }
            else -> {
                // default
                appTimeout = 0
                isCustomTimeout = false
                setScreenOn(false)
            }
        }
    }

    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    private fun hapticFeedback(constant: Int, view: View) {
        runOnUiThread {
            if (isHapticOverride) {
                view.performHapticFeedback(constant, HAPTIC_OVERRIDE)
            } else {
                view.performHapticFeedback(constant)
            }
        }
    }

    /* request WRITE_EXTERNAL_STORAGE permission.
     * see https://developer.android.com/guide/topics/permissions/overview.html#normal-dangerous
     */
    private fun requestExternalStoragePermission() {
        if (hasExternalStoragePermission() == 0) {
            Logger.i(TAG_MAIN, "Requesting WRITE_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_STORAGE)
        }
    }

    /* set a fullscreen layout */
    private fun setFullscreenLayout() {
        val decorView = window.decorView
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ->
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
            else -> decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
    }

    /* keep screen awake toggle */
    private fun setScreenOn(enable: Boolean) {
        if (enable != isScreenAlwaysOn) {
            Logger.d(TAG_WINDOW_MANAGER, "screen on: switching to $enable")
            isScreenAlwaysOn = enable
            runOnUiThread {
                if (enable) {
                    Logger.d(TAG_WINDOW_MANAGER, "add FLAG_KEEP_SCREEN_ON")
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    Logger.d(TAG_WINDOW_MANAGER, "clear FLAG_KEEP_SCREEN_ON")
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    /* toggle system/activity timeouts based on activity state */
    private fun applyCustomTimeout(enable: Boolean) {
        if (enable)
            systemTimeout = SystemSettings.getSystemScreenOffTimeout(this)

        var message = ""
        val resumed = ((enable && isCustomTimeout) && (appTimeout > 0))
        val paused = ((!enable && isCustomTimeout) && (systemTimeout > 0))

        val newTimeout: Int? = when {
            resumed -> {
                setScreenOn(false)
                message = "applying activity custom timeout"
                SystemSettings.setSystemScreenOffTimeout(this, safeTimeout(appTimeout))
                appTimeout
            }

            paused -> {
                message = "restoring system timeout"
                SystemSettings.setSystemScreenOffTimeout(this, systemTimeout)
                systemTimeout
            }

            else -> null
        }
        if (newTimeout != null) {
            val mins = toMin(newTimeout)
            Logger.d("SystemSettings", "$message: $mins minutes")
        }
    }

    private fun safeTimeout(timeout: Int): Int {
        return when {
            timeout < TIMEOUT_MIN -> TIMEOUT_MIN
            timeout > TIMEOUT_MAX -> TIMEOUT_MAX
            else -> timeout
        }
    }

    private fun toMin(milliseconds: Int): Int {
        return if (milliseconds > 0) milliseconds / (1000 * 60) else 0
    }
}
