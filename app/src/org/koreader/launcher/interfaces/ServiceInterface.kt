package org.koreader.launcher.interfaces

/* Declares methods that are described in aidl and implemented elsewhere */

interface ServiceInterface {
    /* local, to bind/unbind */
    fun bind()
    fun unbind()

    /* from aidl */
    fun enabled(): Int
    fun status(): String
    fun setDim(level: Int)
    fun setDimColor(color: Int)
    fun setWarmth(level: Int)
    fun setWarmthAlpha(alpha: Float)
}
