package org.koreader.launcher.interfaces

import android.app.Activity

interface LightInterface {
    fun hasWarmth(): Boolean
    fun getBrightness(activity: Activity): Int
    fun getWarmth(activity: Activity): Int
    fun setBrightness(activity: Activity, brightness: Int)
    fun setWarmth(activity: Activity, warmth: Int)
    fun getMinWarmth(): Int
    fun getMaxWarmth(): Int
    fun getMinBrightness(): Int
    fun getMaxBrightness(): Int
}
