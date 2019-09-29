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

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/* MainActivity.java
 *
 * Takes care of activity callbacks.
 * Implements e-ink updates
 * Implements WRITE_EXTERNAL_STORAGE permission
 */

class MainActivity : BaseActivity() {

    private val epd = EPDFactory.epdController
    private var view: NativeSurfaceView? = null
    private var takesWindowOwnership: Boolean = false

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_WRITE_STORAGE = 1
    }

    /* dumb surface used on Tolinos and other ntx boards to refresh the e-ink screen */
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
        Logger.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        if ("freescale" == getEinkPlatform()) {
            /* take control of the native window from the java framework
               as it seems the only option to forward screen refreshes */
            Logger.v(TAG, "onNativeSurfaceViewImpl()")
            view = NativeSurfaceView(this)
            window.takeSurface(null)
            view?.holder?.addCallback(this)
            setContentView(view)
            takesWindowOwnership = true
        } else {
            /* native content without further processing */
            Logger.v(TAG, "onNativeWindowImpl()")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setFullscreenLayout()
            val decorView = window.decorView
            decorView.setOnSystemUiVisibilityChangeListener { setFullscreenLayout() }
        }
        requestExternalStoragePermission()
    }

    /* Called when the activity has become visible. */
    override fun onResume() {
        Logger.d(TAG, "onResume()")
        setAppCustomSettings(true)
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val handler = Handler()
            handler.postDelayed({ setFullscreenLayout() }, 500)
        }
    }

    /* Called when another activity is taking focus. */
    override fun onPause() {
        Logger.d(TAG, "onPause()")
        setAppCustomSettings(false)
        super.onPause()
    }

    /* Called just before the activity is resumed by an intent */
    override fun onNewIntent(intent: Intent) {
        Logger.d(TAG, "onNewIntent()")
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /* Called on permission result */
    override fun onRequestPermissionsResult(requestCode: Int, permissions:
        Array<String>, grantResults: IntArray) {
        Logger.d(TAG, "onRequestPermissionResult()")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(this,
                        permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Logger.v(TAG, String.format(Locale.US,
                    "Permission granted for request code: %d", requestCode))
        } else {
            val msg = String.format(Locale.US,
                    "Permission rejected for request code %d", requestCode)
            if (requestCode == REQUEST_WRITE_STORAGE) {
                Logger.e(TAG, msg)
            } else {
                Logger.w(TAG, msg)
            }
        }
    }

    /* Called when the activity is going to be destroyed */
    public override fun onDestroy() {
        Logger.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    /*---------------------------------------------------------------
     *             override methods used by lua/JNI                *
     *--------------------------------------------------------------*/

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
            Logger.v(TAG, String.format(Locale.US,
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

        Logger.v(TAG, String.format(Locale.US,
                "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
                mode, delay, x, y, width, height))

        if (takesWindowOwnership) {
            epd.setEpdMode(view as View, mode, delay, x, y, width, height, null)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            epd.setEpdMode(rootView, mode, delay, x, y, width, height, null)
        }
    }

    override fun hasExternalStoragePermission(): Int {
        return if (ContextCompat.checkSelfPermission(this@MainActivity,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            1 else 0
    }

    /*---------------------------------------------------------------
     *                       private methods                        *
     *--------------------------------------------------------------*/

    /* request WRITE_EXTERNAL_STORAGE permission.
     * see https://developer.android.com/guide/topics/permissions/overview.html#normal-dangerous
     */
    private fun requestExternalStoragePermission() {
        if (hasExternalStoragePermission() == 0) {
            Logger.i(TAG, "Requesting WRITE_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE)
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
}
