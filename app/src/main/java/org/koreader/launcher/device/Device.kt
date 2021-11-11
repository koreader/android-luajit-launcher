package org.koreader.launcher.device

import android.app.Activity
import android.os.Build
import org.koreader.launcher.extensions.platform


class Device(activity: Activity) {
    val epd = EPDFactory.epdController
    val lights = LightsFactory.lightsController

    val product = DeviceInfo.PRODUCT
    val needsWakelocks = DeviceInfo.QUIRK_NEEDS_WAKELOCKS
    val bugRotation = DeviceInfo.QUIRK_NO_HW_ROTATION
    val bugLifecycle = DeviceInfo.QUIRK_BROKEN_LIFECYCLE

    val hasEinkSupport = epd.getPlatform() != "none"
    val hasFullEinkSupport = epd.getMode() == "all"

    val hasLights = when (activity.platform) {
        "android" -> !DeviceInfo.QUIRK_NO_LIGHTS
        else -> false
    }

    val needsView = when (activity.platform) {
        "android_tv" -> true
        "chrome" -> true
        else -> epd.needsView()
    }

    val einkPlatform = when (val platform = epd.getPlatform()) {
        "freescale" -> {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                || (DeviceInfo.EINK == DeviceInfo.EinkDevice.CREMA)) {
                platform
            } else {
                "$platform-legacy"
            }
        }
        else -> {
            platform
        }
    }


}
