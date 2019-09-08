package org.koreader.launcher

import android.content.Context
import java.util.Locale

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import org.koreader.launcher.device.EPDFactory
import org.koreader.launcher.helper.ScreenHelper

/* MainActivity.java
 *
 * Takes care of activity callbacks.
 * Overrides einkUpdate methods with working implementations.
 */

class MainActivity : BaseActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val epd = EPDFactory.epdController
    private var view: NativeSurfaceView? = null
    private var takesWindowOwnership: Boolean = false

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_WRITE_STORAGE = 1
    }

    /* dumb surface used on Tolinos and other ntx boards */
    private class NativeSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
        private val tag: String = this.javaClass.simpleName

        init {
            holder.addCallback(this)
            Logger.d(tag, "Starting")
        }

        /* log surface callbacks */
        override fun surfaceCreated(holder: SurfaceHolder) {
            Logger.v(tag, "surface created")
            // override ondraw method in surfaceview.
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

        /* The NativeActivity framework takes care of the surface/view.
           It seems to work just-fine(TM) in all devices.

           But, apparently, Tolinos (and other ntx boards) need to take control
           of the underlying surface to be able to refresh their e-ink screen.

           This is just a guess based on user feedback, needs to be tested on
           real hardware */


        if ("freescale" == einkPlatform) {
            Logger.d(TAG, "onNativeSurfaceViewImpl()")
            window.takeSurface(null)
            view = NativeSurfaceView(this)
            view!!.holder.addCallback(this)
            setContentView(view)
            takesWindowOwnership = true
        } else {
            Logger.d(TAG, "onNativeWindowImpl()")
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
        setTimeout(true)
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val handler = Handler()
            handler.postDelayed({ setFullscreenLayout() }, 500)
        }
    }

    /* Called when another activity is taking focus. */
    override fun onPause() {
        Logger.d(TAG, "onPause()")
        setTimeout(false)
        super.onPause()
    }

    /* Called just before the activity is resumed by an intent */
    override fun onNewIntent(intent: Intent) {
        Logger.d(TAG, "onNewIntent()")
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /* Called on permission result */
    override fun onRequestPermissionsResult(requestCode: Int,
            permissions: Array<String>, grantResults: IntArray) {
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
        var mode_name = "invalid mode"
        if (mode == 1) {
            mode_name = "EPD_FULL"
        } else if (mode == 2) {
            mode_name = "EPD_PART"
        } else if (mode == 3) {
            mode_name = "EPD_A2"
        } else if (mode == 4) {
            mode_name = "EPD_AUTO"
        } else {
            Logger.e(String.format(Locale.US, "%s: %d", mode_name, mode))
            return
        }
        Logger.v(TAG, String.format(Locale.US,
                "requesting epd update, type: %s", mode_name))

        if (takesWindowOwnership) {
            epd.setEpdMode(view, 0, 0, 0, 0, 0, 0, mode_name)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            epd.setEpdMode(rootView, 0, 0, 0, 0, 0, 0, mode_name)
        }
    }

    // update a region or the entire screen (freescale)

    override fun einkUpdate(mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int) {

        Logger.v(TAG, String.format(Locale.US,
                "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
                mode, delay, x, y, width, height))

        if (takesWindowOwnership) {
            epd.setEpdMode(view, mode, delay, x, y, width, height, null)
        } else {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            epd.setEpdMode(rootView, mode, delay, x, y, width, height, null)
        }
    }

    override fun hasExternalStoragePermission(): Int {
        return if (ContextCompat.checkSelfPermission(this@MainActivity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) 1 else 0
    }

    override fun hasWriteSettingsPermission(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this@MainActivity)) {
                1
            } else {
                0
            }
        } else {
            // on older apis permissions are granted at install time
            1
        }
    }

    override var screenOffTimeout: Int
        get() = super.screenOffTimeout
        set(timeout) {
            if (timeout == ScreenHelper.TIMEOUT_WAKELOCK) {
                setWakelockState(true)
            } else {
                setWakelockState(false)
            }

            if (timeout == ScreenHelper.TIMEOUT_SYSTEM ||
                timeout == ScreenHelper.TIMEOUT_WAKELOCK ||
                hasWriteSettingsPermission() == 1) {
                screen!!.setTimeout(timeout)
            } else {
                requestWriteSettingsPermission()
            }
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

    /* request WRITE_SETTINGS permission.
     * It needs to be granted through a management screen.
     * See https://developer.android.com/reference/android/Manifest.permission.html#WRITE_SETTINGS
     */
    @SuppressWarnings("InlinedApi")
    private fun requestWriteSettingsPermission() {
        if (hasWriteSettingsPermission() == 0) {
            Logger.i(TAG, "Requesting WRITE_SETTINGS permission")
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            startActivity(intent)
        } else {
            Logger.v(TAG, "write settings permission is already granted")
        }
    }

    /* set a fullscreen layout */
    private fun setFullscreenLayout() {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE
        } else {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        }
    }

    /* set screen timeout based on activity state */
    private fun setTimeout(resumed: Boolean) {
        val sb = StringBuilder("timeout: ")
        if (resumed)
            sb.append("onResume callback -> ")
        else
            sb.append("onPause callback -> ")

        if (screen!!.appTimeout == ScreenHelper.TIMEOUT_WAKELOCK) {
            sb.append("using wakelocks: ")
            sb.append(resumed)
            Logger.d(TAG, sb.toString())
            setWakeLock(resumed)
        } else if (screen!!.appTimeout > ScreenHelper.TIMEOUT_SYSTEM) {
            sb.append("custom settings: ")
            sb.append(resumed)
            Logger.d(TAG, sb.toString())
            screen!!.setTimeout(resumed)
        }
    }
}
