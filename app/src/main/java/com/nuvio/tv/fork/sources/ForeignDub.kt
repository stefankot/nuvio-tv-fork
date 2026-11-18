package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.Stream

/**
 * Fork 19.11 (JMGO: serial po angielsku włączył się z hiszpańskim dźwiękiem — „[AC3 5.1 Castellano]”, Torrentio):
 * wydania z dubbingiem w obcym języku (nie polskim i nie oryginalnym) idą na koniec rankingu. Nie dotyczy wydań,
 * które mają też angielski, MULTI/DUAL albo polski. KE (ReleaseLanguages) nie zna m.in. „Castellano”.
 */
object ForeignDub {
    private val FOREIGN = Regex(
        """(?i)(?:\b(?:castellano|espa[nñ]ol|spanish|latino|lat(?:am)?|temporada|cap\.\s?\d+|vff?|vfq|vf2|truefrench|french|fran[cç]ais|german|deutsch|ger|ita(?:lian(?:o)?)?|rus(?:sian)?|ukr|türkçe|turkish|hindi|tamil|telugu)\b)"""
    )
    private val KEEP = Regex("""(?i)\b(?:eng(?:lish)?|multi|dual|vo(?:stfr)?|original|pl|pol(?:ish|ski)?|lektor|dubbing\s?pl|napisy)\b""")

    /** Słowa tytułu oglądanej pozycji (KeRanking.begin): „The Italian Job”, „The French Dispatch” to nie dubbing. */
    @Volatile var titleWords: Set<String> = emptySet()

    fun setTitle(title: String?) {
        titleWords = title?.lowercase()?.split(Regex("[^\\p{L}]+"))?.filter { it.isNotEmpty() }?.toSet().orEmpty()
    }

    fun tier(s: Stream): Int {
        val text = listOfNotNull(s.name, s.title, s.description).joinToString(" ")
        if (KEEP.containsMatchIn(text)) return 0
        val words = titleWords
        return if (FOREIGN.findAll(text).any { it.value.lowercase().trimEnd('.').let { w -> w !in words } }) 1 else 0
    }
}
