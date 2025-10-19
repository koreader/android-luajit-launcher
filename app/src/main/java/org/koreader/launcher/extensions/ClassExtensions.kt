package org.koreader.launcher.extensions

import android.util.Log
import java.lang.reflect.Method

/**
 * Safe function for [Class.getMethod].
 */
fun Class<*>.getMethodOrNull(
    methodName: String,
    vararg parameterTypes: Class<*>,
): Method? {
    try {
        return this
            .getMethod(methodName, *parameterTypes)
    } catch (e: Exception) {
        Log.e("ClassExtensions", "Class.getMethod failed", e)
        return null
    }
}

/**
 * Safe function for [Class.forName].
 */
fun forNameOrNull(className: String): Class<*>? {
    return try {
        Class.forName(className)
    } catch (e: Exception) {
        Log.e("ClassExtensions", "Class.forName failed", e)
        null
    }
}
