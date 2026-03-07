package org.koreader.launcher.extensions

import java.io.*

fun File.read(): Int {
    return try {
        this.readText().replace("\n", "").toInt()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

fun File.write(value: Int) {
    try {
        writeText(value.toString())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
