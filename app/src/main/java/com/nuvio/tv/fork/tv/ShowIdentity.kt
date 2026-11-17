package com.nuvio.tv.fork.tv

import java.text.Normalizer

/**
 * What makes two guide entries "the same show", and which episode an entry is, from nothing but the
 * guide's title and description. Plain XMLTV from many IPTV providers has no episode numbers, so
 * this is deliberately conservative: an episode is only told from another when the guide gives a
 * marker (S01E02, odc. 5, (12)) or a description that is specific to it.
 */
object ShowIdentity {
    private val COMBINING = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val SEASON_EPISODE = Regex("""\bs\s?(\d{1,2})\s?[ex]\s?(\d{1,3})\b""")
    private val EPISODE_WORD = Regex("""\b(?:odc|odcinek|ep|episode|folge|cz|czesc)\.?\s*(\d{1,4})\b""")
    private val SEASON_WORD = Regex("""\b(?:sezon|season|seria|staffel)\s*(\d{1,2})\b""")
    private val BRACKET_NUMBER = Regex("""[(\[]\s*(\d{1,3})\s*[)\]]""")
    private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")
    private val SPACES = Regex("""\s+""")

    /** A show's key: lower case, no accents, no episode or season markers, no punctuation. Empty when nothing is left. */
    fun normalizeTitle(raw: String): String {
        var text = fold(raw)
        text = SEASON_EPISODE.replace(text, " ")
        text = EPISODE_WORD.replace(text, " ")
        text = SEASON_WORD.replace(text, " ")
        text = BRACKET_NUMBER.replace(text, " ")
        text = NON_WORD.replace(text, " ")
        return SPACES.replace(text, " ").trim()
    }

    /**
     * The episode marker the guide gives, like `s02e05` or `e12`, or null. The title is read first,
     * then the description; a bare number is never a marker (it is often part of the title).
     */
    fun episodeMarker(title: String, description: String?): String? {
        val foldedTitle = fold(title)
        val foldedDescription = description?.let(::fold).orEmpty()
        for (text in listOf(foldedTitle, foldedDescription)) {
            SEASON_EPISODE.find(text)?.let { return "s%02de%02d".format(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        }
        val season = SEASON_WORD.find(foldedTitle)?.groupValues?.get(1)?.toIntOrNull()
        val episode = EPISODE_WORD.find(foldedTitle)?.groupValues?.get(1)?.toIntOrNull()
            ?: BRACKET_NUMBER.find(foldedTitle)?.groupValues?.get(1)?.toIntOrNull()
            ?: EPISODE_WORD.find(foldedDescription)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        return if (season != null) "s%02de%02d".format(season, episode) else "e%02d".format(episode)
    }

    /** A short stable fingerprint of a description, or "" when there is none worth comparing. */
    fun fingerprint(description: String?): String {
        val text = description?.let(::fold)?.let { NON_WORD.replace(it, "") }.orEmpty()
        if (text.length < 12) return ""
        var hash = -0x340d631b7bdddcdbL // FNV-1a 64-bit offset
        for (ch in text.take(200)) {
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        return java.lang.Long.toHexString(hash)
    }

    /** The marker if there is one, else `d:<fingerprint>`, else "" (the guide does not tell the episode). */
    fun episodeId(title: String, description: String?): String {
        episodeMarker(title, description)?.let { return it }
        val print = fingerprint(description)
        return if (print.isEmpty()) "" else "d:$print"
    }

    private fun fold(text: String): String {
        val lower = text.lowercase().replace('ł', 'l').replace('ø', 'o').replace('đ', 'd').replace('ß', 's')
        return COMBINING.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "")
    }
}
