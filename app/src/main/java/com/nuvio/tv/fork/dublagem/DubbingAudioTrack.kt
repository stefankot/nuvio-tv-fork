package com.nuvio.tv.fork.dublagem

import kotlin.math.abs

data class DubbingAudioTrack(
    val ordinal: Int,
    val language: String,
    val mime: String?,
    val channels: Int = 0,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val label: String? = null,
    val auxiliary: Boolean = false,
) {
    val isPolish: Boolean
        get() = languageCode(language) in setOf("pl", "pol") ||
            (languageCode(language) in setOf("", "und") && label?.let(polish::containsMatchIn) == true)

    val isMainProgram: Boolean
        get() = !auxiliary && label?.let(alternate::containsMatchIn) != true

    companion object {
        private val polish = Regex("""(?i)\b(?:polish|polski|polska|lektor|pol|pl)\b""")
        private val alternate = Regex("""(?i)\b(?:commentary|komentarz|audio[ -]?description|audiodeskrypcja)\b""")
        private fun languageCode(value: String) = value.trim().lowercase().substringBefore('-').substringBefore('_')

        internal val quality = compareBy<DubbingAudioTrack>(
            { it.channels }, { codecQuality(it.mime) }, { it.bitrate }, { it.sampleRate }
        )

        private fun codecQuality(mime: String?): Int = when (mime) {
            "audio/true-hd", "audio/vnd.dts.hd", "audio/flac", "audio/raw", "audio/alac" -> 3
            "audio/eac3", "audio/eac3-joc", "audio/vnd.dts", "audio/opus" -> 2
            "audio/ac3", "audio/mp4a-latm", "audio/vorbis", "audio/mpeg" -> 1
            else -> 0
        }

        internal fun matroskaMime(codec: String?): String? = when (codec) {
            "A_AAC" -> "audio/mp4a-latm"
            "A_AC3", "A_AC3/BSID9", "A_AC3/BSID10" -> "audio/ac3"
            "A_EAC3" -> "audio/eac3"
            "A_TRUEHD" -> "audio/true-hd"
            "A_DTS", "A_DTS/EXPRESS" -> "audio/vnd.dts"
            "A_DTS/LOSSLESS" -> "audio/vnd.dts.hd"
            "A_FLAC" -> "audio/flac"
            "A_OPUS" -> "audio/opus"
            "A_VORBIS" -> "audio/vorbis"
            "A_MPEG/L3" -> "audio/mpeg"
            "A_ALAC" -> "audio/alac"
            else -> when {
                codec?.startsWith("A_AAC/") == true -> "audio/mp4a-latm"
                codec?.startsWith("A_PCM/") == true -> "audio/raw"
                else -> null
            }
        }
    }
}

internal data class DubbingEdition(val cut: String?, val durationMs: Long?, val frameRate: Float?) {
    fun matches(other: DubbingEdition): Boolean {
        val duration = durationMs ?: return false
        val otherDuration = other.durationMs ?: return false
        if (cut != other.cut || abs(duration - otherDuration) > 4_000L) return false
        return frameRate == null || other.frameRate == null || abs(frameRate - other.frameRate) < 0.015f
    }
}

internal fun matchingDubbingOffset(firstMs: Long, confirmationMs: Long): Long? =
    firstMs.takeIf { abs(it) <= 3_000L && abs(confirmationMs) <= 3_000L && abs(it - confirmationMs) <= 120L }
