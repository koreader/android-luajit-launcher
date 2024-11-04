package org.koreader.launcher.device

import android.app.Activity
import org.koreader.launcher.extensions.platform

class Device(activity: Activity) {
    val epd = EPDFactory.epdController
    val lights = LightsFactory.lightsController

    @Suppress("unused")
    val product = DeviceInfo.PRODUCT
    val needsWakelocks = DeviceInfo.QUIRK_NEEDS_WAKELOCKS
    val bugRotation = DeviceInfo.QUIRK_NO_HW_ROTATION
    val bugLifecycle = DeviceInfo.QUIRK_BROKEN_LIFECYCLE
    val hasColorScreen = DeviceInfo.HAS_COLOR_SCREEN

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

    val einkPlatform = epd.getPlatform()

    val properties: String
      get() = String.format("%s;%s;%s;%s;%s;%s;%s",
          DeviceInfo.MANUFACTURER,
          DeviceInfo.BRAND,
          DeviceInfo.MODEL,
          DeviceInfo.DEVICE,
          DeviceInfo.PRODUCT,
          DeviceInfo.HARDWARE,
          when (DeviceInfo.ID) {
              DeviceInfo.Id.TOLINO,
              DeviceInfo.Id.TOLINO_EPOS2,
              DeviceInfo.Id.TOLINO_EPOS3,
              DeviceInfo.Id.TOLINO_PAGE2,
              DeviceInfo.Id.TOLINO_SHINE3,
              DeviceInfo.Id.TOLINO_VISION4,
              DeviceInfo.Id.TOLINO_VISION5,
              DeviceInfo.Id.TOLINO_VISION6,
              -> true else -> false
          }
      )
}
