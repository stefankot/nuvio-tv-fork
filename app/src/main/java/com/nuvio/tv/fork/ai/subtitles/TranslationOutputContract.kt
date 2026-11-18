package com.nuvio.tv.fork.ai.subtitles

/** Timing and styles are owned by the application, not by the translation model. */
data class SourceCue(val id: String, val startMs: Long, val endMs: Long, val sourceText: String) {
    init {
        require(id.isNotBlank() && startMs >= 0 && endMs > startMs)
        require(sourceText.isNotBlank())
    }
}

data class TranslatedCue(val cueId: String, val translation: String, val flags: Set<String> = emptySet())
data class TranslationBlockReply(
    val schemaVersion: Int,
    val blockId: String,
    val items: List<TranslatedCue>,
    val issues: List<String> = emptyList(),
)

data class ValidatedCue(val source: SourceCue, val translation: String)
data class TranslationValidation(
    val accepted: List<ValidatedCue>?,
    val blockingErrors: List<String>,
    val readabilityWarnings: List<String>,
) {
    val valid: Boolean get() = accepted != null && blockingErrors.isEmpty()
}

/** Validate the *complete parsed response*, not a partially streamed JSON fragment. */
object TranslationOutputContract {
    private val allowedFlags = setOf("uncertain_meaning", "speaker_ambiguous", "source_mismatch")
    private val assOverride = Regex("""\{\\[^}]*}""")
    private val markup = Regex("""</?[A-Za-z][^>]*>""")
    private val blankLine = Regex("""\n\s*\n""")

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n").trimEnd().trimStart('\n')

    /** C0/C1 controls, lone surrogates and bidi/line-separator overrides can spoof or break rendering. */
    private fun hasUnsafeCharacter(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\n' -> Unit
                c.code < 0x20 || c.code in 0x7f..0x9f -> return true
                c in '\u202a'..'\u202e' || c in '\u2066'..'\u2069' || c == '\u2028' || c == '\u2029' -> return true
                Character.isHighSurrogate(c) ->
                    if (i + 1 < text.length && Character.isLowSurrogate(text[i + 1])) i++ else return true
                Character.isLowSurrogate(c) -> return true
            }
            i++
        }
        return false
    }

    fun validate(
        expectedBlockId: String,
        sourceCues: List<SourceCue>,
        answer: TranslationBlockReply,
        maxCharsPerLine: Int = 42,
        maxLines: Int = 2,
        maxCharsPerSecond: Double = 17.0,
    ): TranslationValidation {
        require(sourceCues.isNotEmpty())
        require(maxCharsPerLine > 0 && maxLines > 0 && maxCharsPerSecond.isFinite() && maxCharsPerSecond > 0)
        val sourceIds = HashSet<String>(sourceCues.size)
        require(sourceCues.all { sourceIds.add(it.id) }) { "Source cue IDs must be unique" }

        val errors = mutableListOf<String>()
        if (answer.schemaVersion != 1) errors += "Unsupported schema_version"
        if (answer.blockId != expectedBlockId) errors += "Unexpected block_id"
        val replyIds = HashSet<String>(answer.items.size)
        var orderMatches = answer.items.size == sourceCues.size
        // Models often answer with CRLF or a trailing newline; normalising here avoids a paid retry
        // and keeps exported SRT/VTT blocks free of stray empty lines.
        val texts = answer.items.map { normalize(it.translation) }
        answer.items.forEachIndexed { index, item ->
            val text = texts[index]
            if (!replyIds.add(item.cueId) && "Duplicate cue_id" !in errors) errors += "Duplicate cue_id"
            if (index >= sourceCues.size || item.cueId != sourceCues[index].id) orderMatches = false
            if (text.isBlank()) errors += "Blank translation: ${item.cueId}"
            if (text.length > 4000) errors += "Excessive response size: ${item.cueId}"
            if (hasUnsafeCharacter(text)) errors += "Unsafe control character: ${item.cueId}"
            if (assOverride.containsMatchIn(text)) errors += "ASS override injection: ${item.cueId}"
            if (markup.containsMatchIn(text)) errors += "Markup injection: ${item.cueId}"
            if ("-->" in text || blankLine.containsMatchIn(text)) {
                errors += "Subtitle structure injection: ${item.cueId}"
            }
            if (!allowedFlags.containsAll(item.flags)) errors += "Unknown output flag: ${item.cueId}"
        }
        if (!orderMatches) errors += "Missing, extra or reordered cue_id"
        if (errors.isNotEmpty()) return TranslationValidation(null, errors.distinct(), emptyList())

        val warnings = mutableListOf<String>()
        val validated = ArrayList<ValidatedCue>(sourceCues.size)
        for (index in sourceCues.indices) {
            val source = sourceCues[index]
            val translation = texts[index]
            // Scan UTF-16 only once; count surrogate pairs as one visible code point.
            var totalChars = 0
            var lineChars = 0
            var lineCount = 1
            var tooLongLine = false
            var offset = 0
            while (offset < translation.length) {
                val char = translation[offset]
                if (char == '\n') {
                    if (lineChars > maxCharsPerLine) tooLongLine = true
                    lineChars = 0
                    lineCount++
                } else {
                    lineChars++
                    totalChars++
                    if (Character.isHighSurrogate(char) && offset + 1 < translation.length &&
                        Character.isLowSurrogate(translation[offset + 1])
                    ) offset++
                }
                offset++
            }
            if (lineCount > maxLines || tooLongLine || lineChars > maxCharsPerLine) {
                warnings += "Readability / lines: ${source.id}"
            }
            val seconds = (source.endMs - source.startMs) / 1000.0
            if (totalChars / seconds > maxCharsPerSecond) warnings += "Readability / speed: ${source.id}"
            if (translation == source.sourceText.trim()) warnings += "Untranslated: ${source.id}"
            // Three identical outputs in a row for different source lines is a typical model loop.
            if (index >= 2 && translation == texts[index - 1] && translation == texts[index - 2] &&
                source.sourceText != sourceCues[index - 1].sourceText
            ) warnings += "Repeated translation: ${source.id}"
            validated += ValidatedCue(source, translation)
        }
        return TranslationValidation(validated, emptyList(), warnings)
    }
}
