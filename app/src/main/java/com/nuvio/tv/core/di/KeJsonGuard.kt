package com.nuvio.tv.core.di

internal const val UNTRUSTED_JSON_MAX_DEPTH = 64

/**
 * Byte-wise JSON nesting counter that ignores brackets inside strings. It sees what the
 * decoder sees only for UTF-8 or a single-byte ASCII superset (quote and backslash bytes
 * never occur inside a character): the guard rejects any other declared charset, and this
 * counter rejects 0x00/0xFE/0xFF, which never occur in UTF-8 JSON but begin every
 * UTF-16/32 byte-order mark ResponseBody.string() would otherwise decode by.
 */
internal class JsonNestingDepthCounter(
    private val maxDepth: Int,
    val maxValues: Long = Long.MAX_VALUE
) {
    private var depth = 0
    private var inString = false
    private var escaped = false
    private var values = 0L

    /** True once [accept] failed because the body holds more than [maxValues] values. */
    var tooManyValues = false
        private set

    /**
     * Feeds one byte; false once the nesting exceeds [maxDepth], the value count exceeds
     * [maxValues], or the byte is not UTF-8 JSON.
     */
    fun accept(byte: Byte): Boolean {
        if (byte == NUL || byte == UTF_INVALID_FE || byte == UTF_INVALID_FF) return false
        val c = byte.toInt().toChar()
        when {
            inString -> when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
            c == '"' -> inString = true
            c == '[' || c == '{' -> if (++depth > maxDepth || !countValue()) return false
            c == ']' || c == '}' -> depth = (depth - 1).coerceAtLeast(0)
            c == ',' -> if (!countValue()) return false
        }
        return true
    }

    private fun countValue(): Boolean {
        if (++values <= maxValues) return true
        tooManyValues = true
        return false
    }

    private companion object {
        const val NUL: Byte = 0x00
        const val UTF_INVALID_FE: Byte = 0xFE.toByte()
        const val UTF_INVALID_FF: Byte = 0xFF.toByte()
    }
}
