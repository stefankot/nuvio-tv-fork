package com.nuvio.tv.fork.voice

/** Czas z polecenia „przewiń o pięć minut” (B07): liczebnik słowny lub cyfry + jednostka; brak liczby = 1; brak jednostki = minuta. */
object DurationParser {
    private val words = mapOf(
        "jeden" to 1, "jedna" to 1, "dwie" to 2, "dwa" to 2, "trzy" to 3, "cztery" to 4, "piec" to 5, "szesc" to 6, "siedem" to 7,
        "osiem" to 8, "dziewiec" to 9, "dziesiec" to 10, "pietnascie" to 15, "dwadziescia" to 20, "trzydziesci" to 30,
    )

    fun millis(text: String): Long? {
        val n = Text.norm(text); if (n.isEmpty()) return null
        val tokens = n.split(' ')
        var count: Int? = null
        for (t in tokens) { t.toIntOrNull()?.let { count = it; break }; words[t]?.let { count = it; break } }
        val half = tokens.any { it == "pol" || it == "polowe" }
        val unit = when {
            tokens.any { it.startsWith("godz") } -> 3_600_000L
            tokens.any { it.startsWith("sek") } -> 1_000L
            else -> 60_000L
        }
        if (half && tokens.any { it.startsWith("godz") }) return 1_800_000L
        if (half && count == null) return 30_000L
        return (count ?: 1).coerceAtLeast(1) * unit
    }
}
