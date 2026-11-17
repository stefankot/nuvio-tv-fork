package com.nuvio.tv.fork.sources

/** Język dźwięku źródła z jego nazwy (te same wzorce co reguły rankingu z `source_presets.json`). */
object SourceLanguage {
    private val lektor = Regex("""\blektor\b""", RegexOption.IGNORE_CASE)
    private val dubbing = Regex("""\b(?:dubbing|dub[ ._-]?pl|pl[ ._-]?dub)\b""", RegexOption.IGNORE_CASE)
    private val sub = Regex("""sub|napisy""", RegexOption.IGNORE_CASE)
    private val pl = Regex("""\b(?:pl|pol|polish|polski)\b""", RegexOption.IGNORE_CASE)
    private val multiPl = Regex("""\bmulti\b.*\bpl\b(?![ ._-]?sub)""", RegexOption.IGNORE_CASE)

    /** „lektor PL”, „dubbing PL”, „PL” albo null (brak polskiego dźwięku; „PL subs” to nie dźwięk). */
    fun detect(text: String): String? = when {
        lektor.containsMatchIn(text) -> "lektor PL"
        dubbing.containsMatchIn(text) -> "dubbing PL"
        !sub.containsMatchIn(text) && pl.containsMatchIn(text) -> "PL"
        multiPl.containsMatchIn(text) -> "PL"
        else -> null
    }

    /** Linia „Gotowe · 1080p · lektor PL”. */
    fun readyLine(info: ReadyInfo, sourceText: String): String =
        listOfNotNull("Gotowe".takeIf { info.ready } ?: "Prawie gotowe", info.quality, detect(sourceText)).joinToString(" · ")
}
