package org.koreader.launcher.interfaces

import android.content.Context

/* Declares methods that are described in aidl and implemented elsewhere */

interface ServiceInterface {
    /* local, to (un)bind */
    fun bind(context: Context)
    fun unbind(context: Context)

    /* from aidl */
    fun enabled(): Int
    fun status(): String
    fun setDim(level: Int)
    fun setDimColor(color: Int)
    fun setWarmth(level: Int)
    fun setWarmthAlpha(alpha: Float)
}
