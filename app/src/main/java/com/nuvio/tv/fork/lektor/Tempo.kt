package com.nuvio.tv.fork.lektor

/** Dopasowanie tempa kwestii do dostępnego czasu (B09-2). */
data class Fit(val rate: Float, val text: String, val borrowedGapMs: Long, val truncated: Boolean)

object Tempo {
    const val MAX_RATE = 1.3f
    const val MAX_GAP_MS = 1_500L

    /**
     * [naturalMs] — czas mowy w tempie 1,0; [availableMs] — czas do następnej kwestii; [gapMs] — przerwa, którą wolno zająć.
     * 1) mieści się: tempo 1,0; 2) przyspiesz do 1,3×; 3) zajmij przerwę; 4) utnij na granicy zdania.
     */
    fun fit(text: String, naturalMs: Long, availableMs: Long, gapMs: Long = MAX_GAP_MS): Fit {
        if (naturalMs <= availableMs) return Fit(1f, text, 0, false)
        val needed = naturalMs.toFloat() / availableMs
        if (needed <= MAX_RATE) return Fit(needed.coerceAtLeast(1f), text, 0, false)
        val fastMs = (naturalMs / MAX_RATE).toLong()
        val extra = fastMs - availableMs
        if (extra <= gapMs) return Fit(MAX_RATE, text, extra.coerceAtLeast(0), false)
        // Utnij na granicy zdania tak, aby zmieścić się z przerwą.
        val budget = ((availableMs + gapMs) * MAX_RATE).toLong()
        val sentences = text.split(Regex("(?<=[.!?…])\\s+"))
        var acc = 0L; val kept = ArrayList<String>()
        for (s in sentences) {
            val ms = naturalMs * s.length / text.length.coerceAtLeast(1)
            if (acc + ms > budget && kept.isNotEmpty()) break
            kept += s; acc += ms
        }
        return Fit(MAX_RATE, kept.joinToString(" "), gapMs, kept.size < sentences.size)
    }

    /** Fork 20.11: pierwsze zdania, które w zwykłym tempie zmieszczą się w [budgetMs] (zawsze co najmniej jedno). */
    fun sentences(text: String, naturalMs: Long, budgetMs: Long): String {
        val ss = text.split(Regex("(?<=[.!?…])\\s+"))
        var acc = 0L; val kept = ArrayList<String>()
        for (x in ss) { val ms = naturalMs * x.length / text.length.coerceAtLeast(1); if (acc + ms > budgetMs && kept.isNotEmpty()) break; kept += x; acc += ms }
        return kept.joinToString(" ")
    }
}
