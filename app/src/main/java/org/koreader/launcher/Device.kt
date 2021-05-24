package org.koreader.launcher

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.Keep
import org.koreader.launcher.device.DeviceInfo
import org.koreader.launcher.device.EPDFactory
import org.koreader.launcher.device.LightsFactory
import java.util.*

@Keep
class Device(activity: Activity) {
    private val tag = this::class.java.simpleName

    val product = DeviceInfo.PRODUCT
    val hasEinkSupport = DeviceInfo.EINK_SUPPORT
    val hasFullEinkSupport = DeviceInfo.EINK_FULL_SUPPORT
    val needsWakelocks = DeviceInfo.BUG_WAKELOCKS
    val bugRotation = DeviceInfo.BUG_SCREEN_ROTATION
    val externalStorage = MainApp.storage_path
    val platform = MainApp.platform_type

    val einkPlatform: String = if (DeviceInfo.EINK_FREESCALE) {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            || (DeviceInfo.EINK == DeviceInfo.EinkDevice.CREMA)) {
            "freescale"
        } else {
            "freescale-legacy"
        }
    } else if (DeviceInfo.EINK_ROCKCHIP) {
        "rockchip"
    } else if (DeviceInfo.EINK_QCOM) {
        "qualcomm"
    } else {
        "none"
    }
    val isTv = (platform == "android_tv")
    val isChromeOS = (platform == "chrome")

    val needsView: Boolean = if (DeviceInfo.NEEDS_VIEW) {
        true
    } else {
        (isTv || isChromeOS)
    }

    var isResumed = false

    // EPD driver for this device
    private val epd = EPDFactory.epdController

    // Light controller for this device
    private val lights = LightsFactory.lightsController
    private var lightDialogState = LIGHT_DIALOG_CLOSED

    // Hardware orientation for this device (matches android logo)
    private var screenIsLandscape: Boolean = false

    companion object {
        private const val LIGHT_DIALOG_CLOSED = -1
        private const val LIGHT_DIALOG_OPENED = 0
        private const val LIGHT_DIALOG_CANCEL = 1
        private const val LIGHT_DIALOG_OK = 2

        // constants from https://developer.android.com/reference/android/content/res/Configuration
        private const val ANDROID_LANDSCAPE = 0
        private const val ANDROID_PORTRAIT = 1
        private const val ANDROID_REVERSE_LANDSCAPE = 8
        private const val ANDROID_REVERSE_PORTRAIT = 9

        // constants from https://github.com/koreader/koreader-base/blob/master/ffi/framebuffer.lua
        private const val LINUX_PORTRAIT = 0
        private const val LINUX_LANDSCAPE = 1
        private const val LINUX_REVERSE_PORTRAIT = 2
        private const val LINUX_REVERSE_LANDSCAPE = 3
    }

    init {
        this.screenIsLandscape = isHwLandscape(activity)

        Log.v(tag, String.format(Locale.US,
            "native orientation: %s", if (this.screenIsLandscape) "landscape" else "portrait"))
    }

    fun onResume() {
        isResumed = true
        epd.resume()
    }

    fun onPause() {
        isResumed = false
        epd.pause()
    }

    /* Eink */
    fun einkUpdate(view: View, mode: Int) {
        val modeName = when (mode) {
            1 -> "EPD_FULL"
            2 -> "EPD_PART"
            3 -> "EPD_A2"
            4 -> "EPD_AUTO"
            else -> "invalid"
        }

        if (modeName != "invalid") {
            Log.v(tag, String.format(Locale.US,
                "requesting epd update, type: %s", modeName))

            epd.setEpdMode(view, 0, 0, 0, 0, 0, 0, modeName)
        }
    }

    fun einkUpdate(view: View, mode: Int, delay: Long, x: Int, y: Int, width: Int, height: Int) {
        Log.v(tag, String.format(Locale.US,
            "requesting epd update, mode:%d, delay:%d, [x:%d, y:%d, w:%d, h:%d]",
            mode, delay, x, y, width, height))

        epd.setEpdMode(view, mode, delay, x, y, width, height, null)
    }

    /* Lights */
    fun enableFrontlightSwitch(activity: Activity): Boolean {
        return lights.enableFrontlightSwitch(activity) == 1
    }

    fun getLightDialogState(): Int {
        return lightDialogState
    }

    fun getScreenBrightness(activity: Activity): Int {
        return lights.getBrightness(activity)
    }

    fun getScreenMinBrightness(): Int {
        return lights.getMinBrightness()
    }

    fun getScreenMaxBrightness(): Int {
        return lights.getMaxBrightness()
    }

    fun getScreenWarmth(activity: Activity): Int {
        return lights.getWarmth(activity)
    }

    fun getScreenMinWarmth(): Int {
        return lights.getMinWarmth()
    }

    fun getScreenMaxWarmth(): Int {
        return lights.getMaxWarmth()
    }

    fun isWarmthDevice(): Boolean {
        return lights.hasWarmth()
    }

    fun setScreenWarmth(activity: Activity, warmth: Int) {
        lights.setWarmth(activity, warmth)
    }
    fun setScreenBrightness(activity: Activity, brightness: Int) {
        lights.setBrightness(activity, brightness)
    }
    fun showDialog(
        activity: Activity,
        title: String,
        dim: String,
        warmth: String,
        okButton: String,
        cancelButton: String
    ) {
        val hasWarmth = lights.hasWarmth()
        setDialogState(LIGHT_DIALOG_OPENED)
        activity.runOnUiThread {
            val dimText = TextView(activity)
            val dimSeekBar = SeekBar(activity)
            dimText.text = dim
            dimText.gravity = Gravity.CENTER_HORIZONTAL
            dimText.textSize = 18f
            dimSeekBar.max = lights.getMaxBrightness()
            dimSeekBar.progress = lights.getBrightness(activity)
            dimSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    lights.setBrightness(activity, p1)
                }
            })
            val linearLayout = LinearLayout(activity)
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.addView(dimText)
            linearLayout.addView(dimSeekBar)
            if (hasWarmth) {
                val warmthText = TextView(activity)
                val warmthSeekBar = SeekBar(activity)
                warmthText.text = warmth
                warmthText.gravity = Gravity.CENTER_HORIZONTAL
                warmthText.textSize = 18f
                warmthSeekBar.max = lights.getMaxWarmth()
                warmthSeekBar.progress = lights.getWarmth(activity)
                warmthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        lights.setWarmth(activity, p1)
                    }
                })
                linearLayout.addView(warmthText)
                linearLayout.addView(warmthSeekBar)
            }

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
                .setCancelable(false)
                .setPositiveButton(okButton) {
                    _, _ -> setDialogState(LIGHT_DIALOG_OK)
                }
                .setNegativeButton(cancelButton) {
                    _, _ -> setDialogState(LIGHT_DIALOG_CANCEL)
                }

            val dialog: AlertDialog = builder.create()
            dialog.setView(linearLayout)
            dialog.show()
        }
    }

    /* Haptic feedback */
    fun hapticFeedback(activity: Activity, constant: Int, force: Boolean, view: View) {
        activity.runOnUiThread {
            if (force) {
                view.performHapticFeedback(constant, 2)
            } else {
                view.performHapticFeedback(constant)
            }
        }
    }

    /* Orientation */
    @Suppress("DEPRECATION")
    fun getScreenOrientation(activity: Activity): Int {
        return when (activity.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> if (screenIsLandscape) LINUX_PORTRAIT else LINUX_REVERSE_LANDSCAPE
            Surface.ROTATION_180 -> if (screenIsLandscape) LINUX_REVERSE_LANDSCAPE else LINUX_REVERSE_PORTRAIT
            Surface.ROTATION_270 -> if (screenIsLandscape) LINUX_REVERSE_PORTRAIT else LINUX_LANDSCAPE
            else -> if (screenIsLandscape) LINUX_LANDSCAPE else LINUX_PORTRAIT
        }
    }

    fun setScreenOrientation(activity: Activity, orientation: Int) {
        val newOrientation = if (screenIsLandscape) {
            when (orientation) {
                ANDROID_LANDSCAPE -> ANDROID_PORTRAIT
                ANDROID_PORTRAIT -> ANDROID_LANDSCAPE
                ANDROID_REVERSE_LANDSCAPE -> ANDROID_REVERSE_PORTRAIT
                ANDROID_REVERSE_PORTRAIT -> ANDROID_REVERSE_LANDSCAPE
                else -> orientation
            }
        } else {
            when (orientation) {
                ANDROID_LANDSCAPE -> ANDROID_REVERSE_LANDSCAPE
                ANDROID_REVERSE_LANDSCAPE -> ANDROID_LANDSCAPE
                else -> orientation
            }
        }
        activity.requestedOrientation = newOrientation
    }

    @Suppress("DEPRECATION")
    private fun isHwLandscape(activity: Activity): Boolean {
        val display = activity.windowManager.defaultDisplay
        return (display.width > display.height)
    }

    private fun setDialogState(state: Int) {
        lightDialogState = state
    }
}
