package org.koreader.launcher.device

import android.app.Activity

interface LightsInterface {
    fun getPlatform(): String

    fun hasFallback(): Boolean
    fun hasWarmth(): Boolean
    fun needsPermission(): Boolean
    fun getBrightness(activity: Activity): Int
    fun getWarmth(activity: Activity): Int
    fun setBrightness(activity: Activity, brightness: Int)
    fun setWarmth(activity: Activity, warmth: Int)
    fun getMinWarmth(): Int
    fun getMaxWarmth(): Int
    fun getMinBrightness(): Int
    fun getMaxBrightness(): Int
    fun enableFrontlightSwitch(activity: Activity): Int
    fun hasStandaloneWarmth(): Boolean
}
