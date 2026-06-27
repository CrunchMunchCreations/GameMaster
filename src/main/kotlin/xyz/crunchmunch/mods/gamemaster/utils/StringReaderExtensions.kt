package xyz.crunchmunch.mods.gamemaster.utils

import com.mojang.brigadier.StringReader

fun StringReader.readStringUntilOrEnd(terminator: Char): String {
    val result = StringBuilder()
    var escaped = false
    while (canRead()) {
        val c = read()
        if (escaped) {
            result.append(c)
            escaped = false
        } else if (c == '\\') {
            escaped = true
        } else if (c == terminator) {
            return result.toString()
        } else {
            result.append(c)
        }
    }

    return result.toString()
}

fun StringReader.readBooleanOr(default: Boolean): Boolean {
    return try {
        this.readBoolean()
    } catch (_: Throwable) {
        default
    }
}
