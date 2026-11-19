package com.nuvio.tv.fork.lektor
import com.nuvio.tv.fork.logged

/** Napis: start, koniec (ms), tekst. */
data class SubtitleCue(val startMs: Long, val endMs: Long, val text: String)

/** Kwestia do przeczytania z czasem dostępnym do następnej. */
data class Line(val startMs: Long, val endMs: Long, val text: String, val availableMs: Long)

object CuePlanner {
    private val tags = Regex("<[^>]*>|\\{\\\\[^}]*\\}")
    private val brackets = Regex("\\([^)]*\\)|\\[[^\\]]*]|♪[^♪]*♪|♪")
    private val speaker = Regex("^\\s*[A-ZŻŹĆĄŚĘŁÓŃ][A-ZŻŹĆĄŚĘŁÓŃ0-9 .'-]{1,20}:\\s*")
    private val dash = Regex("^\\s*[-–—]\\s*", RegexOption.MULTILINE)

    /** Usuwa znaczniki formatowania, opisy dźwięków w nawiasach, myślniki dialogowe i imiona mówiących. */
    fun clean(raw: String): String {
        var t = raw.replace("\\N", "\n").replace("\n", " \n")
        t = tags.replace(t, "")
        t = brackets.replace(t, " ")
        t = speakerBreaks(t)
        t = dash.replace(t, "")
        t = t.lines().joinToString(" ") { speaker.replace(it, "").trim() }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    private val sentenceEnd = Regex("[.!?…]\\W*$")
    private val dashStart = Regex("^\\s*[-–—]")
    private val endPunct = Regex("[.!?…:;,]$")

    /**
     * Fork 19.11 (JMGO: „bez przerw między wypowiedziami osób — wszystko jak jedno długie zdanie”): linia dialogowa
     * zaczynająca się myślnikiem = inna osoba. Poprzednia linia bez znaku końca dostaje kropkę, żeby głos zrobił pauzę.
     */
    private fun speakerBreaks(t: String): String {
        val ls = t.lines()
        if (ls.size < 2) return t
        return ls.mapIndexed { i, l ->
            val next = ls.getOrNull(i + 1)
            val cur = l.trimEnd()
            if (next != null && dashStart.containsMatchIn(next) && cur.isNotBlank() && !endPunct.containsMatchIn(cur)) "$cur." else l
        }.joinToString("\n")
    }

    /** Lista kwestii: bez pustych, kwestie rozdzielone o mniej niż [mergeGapMs] sklejone, z czasem dostępnym do następnej. */
    fun plan(cues: List<SubtitleCue>, mergeGapMs: Long = 300L): List<Line> {
        val cleaned = cues.sortedBy { it.startMs }.mapNotNull { c -> clean(c.text).takeIf { it.isNotEmpty() }?.let { SubtitleCue(c.startMs, c.endMs, it) } }
        val merged = ArrayList<SubtitleCue>()
        for (c in cleaned) {
            val last = merged.lastOrNull()
            // Fork 19.11 (JMGO build122: 843 napisy → 298 długich kwestii, potem ucinane do „kilku słów” i cisza): sklejamy tylko,
            // gdy zdanie przechodzi na następny napis (poprzedni bez znaku końca zdania) i razem nie wychodzi za długo.
            val continues = !sentenceEnd.containsMatchIn(last?.text.orEmpty())
            if (last != null && c.startMs - last.endMs < mergeGapMs && continues && last.text.length + c.text.length <= 160) merged[merged.size - 1] = SubtitleCue(last.startMs, maxOf(last.endMs, c.endMs), last.text + " " + c.text)
            else merged += c
        }
        return merged.mapIndexed { i, c -> Line(c.startMs, c.endMs, c.text, ((merged.getOrNull(i + 1)?.startMs ?: (c.endMs + 4_000L)) - c.startMs).coerceAtLeast(1)) }
    }

    /** SRT: bloki „numer / czas --> czas / tekst”. */
    fun parseSrt(text: String): List<SubtitleCue> {
        val out = ArrayList<SubtitleCue>()
        val time = Regex("(\\d+):(\\d+):(\\d+)[,.](\\d+)\\s*-->\\s*(\\d+):(\\d+):(\\d+)[,.](\\d+)")
        fun ms(h: String, m: String, s: String, f: String) = h.toLong() * 3_600_000 + m.toLong() * 60_000 + s.toLong() * 1000 + f.padEnd(3, '0').take(3).toLong()
        for (block in text.replace("\r", "").split(Regex("\n\\s*\n"))) {
            val lines = block.lines().filter { it.isNotBlank() }
            val idx = lines.indexOfFirst { time.containsMatchIn(it) }; if (idx < 0) continue
            val m = time.find(lines[idx])!!.groupValues
            out += SubtitleCue(ms(m[1], m[2], m[3], m[4]), ms(m[5], m[6], m[7], m[8]), lines.drop(idx + 1).joinToString("\n"))
        }
        return out
    }

    /** ASS: linie `Dialogue:` (format standardowy: 9 pól przed tekstem). */
    fun parseAss(text: String): List<SubtitleCue> {
        fun t(s: String): Long { val p = s.trim().split(':', '.'); return p[0].toLong() * 3_600_000 + p[1].toLong() * 60_000 + p[2].toLong() * 1000 + p[3].padEnd(2, '0').take(2).toLong() * 10 }
        return text.lineSequence().filter { it.startsWith("Dialogue:") }.mapNotNull { l ->
            val f = l.removePrefix("Dialogue:").split(',', limit = 10)
            if (f.size < 10) null else runCatching { SubtitleCue(t(f[1]), t(f[2]), f[9]) }.logged("ForkLektor", "Cues.kt:56").getOrNull()
        }.toList()
    }
}
