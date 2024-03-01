package org.koreader.launcher.device.lights

import android.app.Activity
import android.util.Log
import org.koreader.launcher.device.LightsInterface

class OnyxSdk2LightsController : LightsInterface {
    companion object {
        private const val TAG = "Lights"
        private const val MIN_LIGHT_VALUE = 0
    }

    override fun getPlatform(): String {
        return "onyx-sdk-2-lights"
    }

    override fun hasFallback(): Boolean {
        return false
    }

    override fun hasWarmth(): Boolean {
        return true
    }

    override fun needsPermission(): Boolean {
        return false
    }

    override fun getBrightness(activity: Activity): Int {
        return OnyxSdkDeviceController.getLightValue(OnyxSdkDeviceController.Light.COLD)
    }

    override fun getWarmth(activity: Activity): Int {
        return OnyxSdkDeviceController.getLightValue(OnyxSdkDeviceController.Light.WARM)
    }

    override fun setBrightness(activity: Activity, brightness: Int) {
        if (brightness < getMinBrightness() || brightness > getMaxBrightness()) {
            Log.w(TAG, "brightness value of of range: $brightness")
            return
        }
        Log.v(TAG, "Setting brightness to $brightness")
        OnyxSdkDeviceController.setLightValue(OnyxSdkDeviceController.Light.COLD, brightness)
    }

    override fun setWarmth(activity: Activity, warmth: Int) {
        if (warmth < getMinWarmth() || warmth > getMaxWarmth()) {
            Log.w(TAG, "warmth value of of range: $warmth")
            return
        }
        Log.v(TAG, "Setting warmth to $warmth")
        OnyxSdkDeviceController.setLightValue(OnyxSdkDeviceController.Light.WARM, warmth)
    }

    override fun getMinWarmth(): Int {
        return MIN_LIGHT_VALUE
    }

    override fun getMaxWarmth(): Int {
        return OnyxSdkDeviceController.getMaxLightValue(OnyxSdkDeviceController.Light.WARM)
    }

    override fun getMinBrightness(): Int {
        return MIN_LIGHT_VALUE
    }

    override fun getMaxBrightness(): Int {
        return OnyxSdkDeviceController.getMaxLightValue(OnyxSdkDeviceController.Light.COLD)
    }

    override fun enableFrontlightSwitch(activity: Activity): Int {
        return 1
    }

    override fun hasStandaloneWarmth(): Boolean {
        return false
    }
}
