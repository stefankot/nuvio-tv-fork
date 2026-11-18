package com.nuvio.tv.fork.subs

/**
 * Fork 19.11 (JMGO: polskie napisy z SubSense/OpenSubtitles pobierały się, ale były puste): polskie pliki często są w
 * formacie MPL2 (`[425][575]tekst|druga linia`, czas w 1/10 s) albo MicroDVD (`{100}{150}tekst`, czas w klatkach).
 * ExoPlayer ich nie zna (czytał je jako SRT i odrzucał każdą linię), więc zamieniamy je na SRT zaraz po pobraniu.
 * Inne formaty wracają bez zmian.
 */
object TextSubtitleFormats {
    private val MPL2 = Regex("""^\[(\d+)]\[(\d*)](.*)$""")
    private val MICRODVD = Regex("""^\{(\d+)\}\{(\d*)\}(.*)$""")
    private val MDVD_TAG = Regex("""\{[^}]*\}""")
    private const val DEFAULT_FPS = 23.976

    fun toSrt(raw: String): String {
        if (raw.contains("-->")) return raw // SRT / VTT
        val lines = raw.removePrefix("﻿").lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return raw
        val sample = lines.take(30)
        return when {
            sample.count { MPL2.matches(it) } >= minOf(3, sample.size) -> mpl2(lines)
            sample.count { MICRODVD.matches(it) } >= minOf(3, sample.size) -> microDvd(lines)
            else -> raw
        }
    }

    private class Cue(val startMs: Long, var endMs: Long, val text: String)

    private fun mpl2(lines: List<String>): String = srt(lines.mapNotNull { l ->
        val m = MPL2.matchEntire(l) ?: return@mapNotNull null
        val start = m.groupValues[1].toLong() * 100
        val end = m.groupValues[2].toLongOrNull()?.times(100) ?: -1
        Cue(start, end, text(m.groupValues[3]))
    })

    private fun microDvd(lines: List<String>): String {
        var fps = DEFAULT_FPS
        val cues = ArrayList<Cue>()
        for ((i, l) in lines.withIndex()) {
            val m = MICRODVD.matchEntire(l) ?: continue
            val body = m.groupValues[3]
            // {1}{1}25 = liczba klatek na sekundę zapisana w pierwszej linii
            val fpsHint = if (i == 0 && m.groupValues[1].toLong() <= 1) body.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 10.0..120.0 } else null
            if (fpsHint != null) { fps = fpsHint; continue }
            val start = (m.groupValues[1].toLong() * 1000 / fps).toLong()
            val end = m.groupValues[2].toLongOrNull()?.let { (it * 1000 / fps).toLong() } ?: -1
            cues += Cue(start, end, text(MDVD_TAG.replace(body) { t -> if (t.value.contains("y:i", true) || t.value.contains("Y:i")) "/" else "" }))
        }
        return srt(cues)
    }

    /** `|` = nowa linia; `/` na początku linii = kursywa. */
    private fun text(body: String): String = body.split('|').joinToString("\n") { part ->
        val p = part.trim()
        if (p.startsWith("/")) "<i>" + p.removePrefix("/").trim() + "</i>" else p
    }

    private fun srt(raw: List<Cue>): String {
        val cues = raw.filter { it.text.isNotBlank() }.sortedBy { it.startMs }
        cues.forEachIndexed { i, c ->
            if (c.endMs <= c.startMs) c.endMs = minOf(c.startMs + 4000, cues.getOrNull(i + 1)?.startMs ?: Long.MAX_VALUE).coerceAtLeast(c.startMs + 500)
        }
        val sb = StringBuilder()
        cues.forEachIndexed { i, c -> sb.append(i + 1).append('\n').append(t(c.startMs)).append(" --> ").append(t(c.endMs)).append('\n').append(c.text).append("\n\n") }
        return sb.toString()
    }

    private fun t(ms: Long) = "%02d:%02d:%02d,%03d".format(ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000)
}
