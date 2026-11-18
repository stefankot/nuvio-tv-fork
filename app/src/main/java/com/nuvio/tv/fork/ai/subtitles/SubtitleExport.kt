package com.nuvio.tv.fork.ai.subtitles

/** Text-only exports: not a lossless ASS/SSA, karaoke or placement-preserving conversion. */
object SubtitleExport {
    // Formatting millions of timestamps through String.format allocates a Formatter each time.
    // Appending directly to the output builder keeps the exact SRT/VTT timestamp contract.
    private fun StringBuilder.append2(value: Long) {
        if (value < 10) append('0')
        append(value)
    }

    private fun StringBuilder.append3(value: Long) {
        if (value < 100) append('0')
        if (value < 10) append('0')
        append(value)
    }

    private fun StringBuilder.appendStamp(ms: Long, decimal: Char) {
        require(ms >= 0)
        append2(ms / 3_600_000)
        append(':')
        append2(ms / 60_000 % 60)
        append(':')
        append2(ms / 1_000 % 60)
        append(decimal)
        append3(ms % 1_000)
    }

    fun toSrt(cues: List<ValidatedCue>): String = buildString {
        cues.sortedBy { it.source.startMs }.forEachIndexed { index, cue ->
            append(index + 1).append('\n')
            appendStamp(cue.source.startMs, ',')
            append(" --> ")
            appendStamp(cue.source.endMs, ',')
            append('\n').append(cue.translation).append("\n\n")
        }
    }

    fun toWebVtt(cues: List<ValidatedCue>): String = buildString {
        append("WEBVTT\n\n")
        cues.sortedBy { it.source.startMs }.forEach { cue ->
            appendStamp(cue.source.startMs, '.')
            append(" --> ")
            appendStamp(cue.source.endMs, '.')
            append('\n').append(cue.translation).append("\n\n")
        }
    }
}
