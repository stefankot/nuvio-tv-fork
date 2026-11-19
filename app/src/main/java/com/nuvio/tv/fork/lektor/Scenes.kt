package com.nuvio.tv.fork.lektor

import kotlin.math.sqrt

/** Silnik mowy, który czyta całą scenę jednym zapytaniem. null = nagrania nie dało się pociąć (wtedy kwestia po kwestii). */
interface SceneLektor {
    suspend fun synthesizeScene(texts: List<String>): List<Pcm>?
    /** Fork 19.11: największa scena w bajtach UTF-8 (0 = małe sceny [Scenes.MAX_LINES]/[Scenes.MAX_CHARS]). Chirp: limit 5000 B/zapytanie. */
    val sceneMaxBytes: Int get() = 0
}

/**
 * Właściciel 19.11: „tekst do TTS w partiach z podziałem na sceny i z dużym wyprzedzeniem”. Scena = kolejne kwestie
 * z przerwą poniżej [MAX_GAP_MS], najwyżej [MAX_LINES] kwestii i [MAX_CHARS] znaków. Mniej zapytań (mniej 429),
 * jedna barwa i dynamika w scenie, intonacja z kontekstem sąsiednich kwestii.
 */
object Scenes {
    const val MAX_GAP_MS = 2_500L
    const val MAX_LINES = 8
    const val MAX_CHARS = 600

    /** Fork 19.11 (właściciel: „najdłuższe fragmenty, model rozumie kontekst”): duże sceny — przerwa do 30 s, bez limitu kwestii. */
    const val BIG_MAX_GAP_MS = 30_000L
    /** Na separator `[pause long]` między kwestiami w markup. */
    private const val SEPARATOR_BYTES = 14

    fun group(lines: List<Line>): List<List<Line>> = group(lines, MAX_GAP_MS, MAX_LINES, MAX_CHARS, 0)

    /** [maxBytes] > 0: sceny do tylu bajtów UTF-8 (z separatorami), inaczej limity znaków/kwestii. */
    fun group(lines: List<Line>, maxGapMs: Long, maxLines: Int, maxChars: Int, maxBytes: Int): List<List<Line>> {
        val out = ArrayList<List<Line>>()
        var cur = ArrayList<Line>(); var chars = 0; var bytes = 0
        for (l in lines.sortedBy { it.startMs }) {
            val prev = cur.lastOrNull()
            val b = l.text.toByteArray(Charsets.UTF_8).size + SEPARATOR_BYTES
            val full = if (maxBytes > 0) bytes + b > maxBytes else cur.size >= maxLines || chars + l.text.length > maxChars
            if (prev != null && (l.startMs - prev.endMs > maxGapMs || full)) {
                out += cur; cur = ArrayList(); chars = 0; bytes = 0
            }
            cur += l; chars += l.text.length; bytes += b
        }
        if (cur.isNotEmpty()) out += cur
        return out
    }
}

/**
 * Cięcie nagrania sceny na kwestie w ciszach (okna 20 ms, RMS).
 * Fork 19.11 (JMGO: „ucina 1–3 słowa, czasem powtarza”): wybór najdłuższych pauz mylił pauzę w środku zdania z granicą
 * kwestii — koniec kwestii trafiał na początek następnej. Teraz [texts] (gdy podane): pauzy dobierane do miejsc
 * oczekiwanych z długości tekstu (programowanie dynamiczne), a części o długości niepasującej do tekstu = null.
 */
object SilenceSplit {
    private const val WIN_MS = 20
    private const val MIN_PAUSE_MS = 200

    fun split(pcm: Pcm, n: Int, texts: List<String>? = null): List<Pcm>? {
        if (n <= 1) return listOf(trim(pcm))
        val s = pcm.samples
        val win = (pcm.sampleRate * WIN_MS / 1000).coerceAtLeast(1)
        val frames = s.size / win
        if (frames < n * 10) return null
        val rms = FloatArray(frames) { f -> rmsAt(s, f * win, win) }
        val peak = rms.maxOrNull() ?: return null
        val thr = maxOf(peak * 0.05f, 0.003f)
        val runs = ArrayList<IntArray>() // [początek, koniec) w oknach
        var i = 0
        while (i < frames) {
            if (rms[i] < thr) {
                val st = i
                while (i < frames && rms[i] < thr) i++
                if (st > 0 && i < frames && (i - st) * WIN_MS >= MIN_PAUSE_MS) runs += intArrayOf(st, i)
            } else i++
        }
        if (runs.size < n - 1) return null
        val weights = texts?.takeIf { it.size == n }?.map { it.length.coerceAtLeast(1).toDouble() }
        val chosen = if (weights == null) runs.sortedByDescending { it[1] - it[0] }.take(n - 1).sortedBy { it[0] }
            else alignRuns(runs, weights, frames) ?: return null
        val cuts = chosen.map { (it[0] + it[1]) / 2 * win }
        val bounds = listOf(0) + cuts + listOf(s.size)
        val parts = (0 until n).map { k -> trim(Pcm(s.copyOfRange(bounds[k], bounds[k + 1]), pcm.sampleRate), thr) }
        if (parts.any { it.durationMs < 250 }) return null
        if (weights != null) {
            val total = weights.sum(); val speech = parts.sumOf { it.durationMs }.toDouble()
            for (k in 0 until n) {
                val exp = speech * weights[k] / total; val got = parts[k].durationMs.toDouble()
                val ok = if (weights[k] < 15) kotlin.math.abs(got - exp) < 1_500 else got in exp * 0.4..exp * 2.5
                if (!ok) { android.util.Log.i("ForkLektor", "cięcie sceny: część ${k + 1}/$n ${got.toLong()} ms, oczekiwano ~${exp.toLong()} ms — odrzucone"); return null }
            }
        }
        return parts
    }

    /** n−1 pauz (po kolei) najbliższych oczekiwanym granicom z długości tekstów; dłuższa pauza = pewniejsza granica. */
    private fun alignRuns(runs: List<IntArray>, weights: List<Double>, frames: Int): List<IntArray>? {
        val n = weights.size; val m = runs.size; val need = n - 1
        val total = weights.sum()
        val exp = DoubleArray(need) { k -> frames * weights.take(k + 1).sum() / total }
        val neg = Double.NEGATIVE_INFINITY
        // dp[k][j]: najlepszy wynik, gdy granica k leży w pauzie j
        val dp = Array(need) { DoubleArray(m) { neg } }; val from = Array(need) { IntArray(m) { -1 } }
        fun score(k: Int, j: Int): Double {
            val len = (runs[j][1] - runs[j][0]).coerceAtMost(75).toDouble() // do 1,5 s
            val mid = (runs[j][0] + runs[j][1]) / 2.0
            return len - 0.1 * kotlin.math.abs(mid - exp[k])
        }
        for (j in 0 until m) dp[0][j] = score(0, j)
        for (k in 1 until need) {
            var best = neg; var bestJ = -1
            for (j in 0 until m) {
                if (j > 0 && dp[k - 1][j - 1] > best) { best = dp[k - 1][j - 1]; bestJ = j - 1 }
                if (bestJ >= 0) { dp[k][j] = best + score(k, j); from[k][j] = bestJ }
            }
        }
        var j = (0 until m).maxByOrNull { dp[need - 1][it] } ?: return null
        if (dp[need - 1][j] == neg) return null
        val out = ArrayList<IntArray>()
        for (k in need - 1 downTo 0) { out += runs[j]; if (k > 0) j = from[k][j] }
        return out.reversed()
    }

    /**
     * Bez ciszy na początku i końcu: głos zaczyna się w chwili kwestii.
     * Fork 19.11 (właściciel: „ucina słowa przy samym końcu”): koniec szukany z niższym progiem (ciche końcówki
     * „-ść”, „-ch”, wybrzmienie) i 160 ms zapasu; początek 40 ms zapasu.
     */
    fun trim(p: Pcm, thr: Float = 0.003f): Pcm {
        val s = p.samples
        val win = (p.sampleRate * WIN_MS / 1000).coerceAtLeast(1)
        val frames = s.size / win
        var a = 0
        while (a < frames && rmsAt(s, a * win, win) < thr) a++
        if (a >= frames) return p
        val tailThr = maxOf(thr * 0.25f, 0.0015f)
        var z = frames - 1
        while (z > a && rmsAt(s, z * win, win) < tailThr) z--
        val from = ((a - 2) * win).coerceAtLeast(0)
        val to = minOf(s.size, (z + 1 + 8) * win)
        return Pcm(s.copyOfRange(from, to), p.sampleRate)
    }

    private fun rmsAt(s: ShortArray, from: Int, len: Int): Float {
        val end = minOf(s.size, from + len)
        if (end <= from) return 0f
        var sum = 0.0
        for (i in from until end) { val v = s[i] / 32768.0; sum += v * v }
        return sqrt(sum / (end - from)).toFloat()
    }
}
