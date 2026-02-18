package org.koreader.launcher.util

import android.util.Log
import org.koreader.launcher.extensions.forNameOrNull
import java.lang.reflect.Method

const val ANDROID_OS_SYS_PROP_CLASS_NAME = "android.os.SystemProperties"

private val systemPropertiesClass: Class<*>? by lazy {
    forNameOrNull(ANDROID_OS_SYS_PROP_CLASS_NAME)
}

private val systemPropertiesMethodSet: Method? by lazy {
    systemPropertiesClass
        ?.getMethod("set", String::class.java, String::class.java)
}

private val systemPropertiesMethodGet: Method? by lazy {
    systemPropertiesClass
        ?.getMethod("get", String::class.java, String::class.java)
}

/**
 * Sets the [value] for the [key] as a system property via reflection.
 */
fun setSystemProperty(key: String, value: String) {
    try {
        systemPropertiesMethodSet?.invoke(systemPropertiesClass, key, value)
    } catch (e: Exception) {
        Log.e("SystemPropertiesUtil", "setSystemProperty has failed!", e)
    }
}

/**
 * @return the system property value for the [key].
 */
fun getSystemProperty(key: String, default: String): String? {
    try {
        return systemPropertiesMethodGet?.invoke(systemPropertiesClass, key, default) as String
    } catch (e: Exception) {
        Log.e("SystemPropertiesUtil", "getSystemProperty has failed!", e)
        return null
    }
}
