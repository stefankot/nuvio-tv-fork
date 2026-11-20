package com.nuvio.tv.skin.niunia.player

/**
 * Test JMGO build151: liczba wyświetleń i data z YouTube przychodzą po angielsku („2.6M views”, „Published on Nov 13, 2026”,
 * „7 days ago”). Teksty dla użytkownika po polsku → zamiana na „2,6 mln wyśw.”, „13 lis 2026”, „7 dni temu”.
 * Nierozpoznany tekst wraca bez zmian.
 */
object NiuniaYtText {
    private val VIEWS = Regex("""^\s*([\d.,]+)\s*([KMB])?\s*views?\s*$""", RegexOption.IGNORE_CASE)
    private val DATE = Regex("""^(?:(?:Published|Premiered|Streamed live)(?: on)?\s+)?([A-Za-z]{3})[a-z]*\.?\s+(\d{1,2}),\s*(\d{4})$""")
    private val AGO = Regex("""^(?:Streamed\s+)?(\d+)\s+(second|minute|hour|day|week|month|year)s?\s+ago$""", RegexOption.IGNORE_CASE)
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val PL_MONTHS = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")

    fun views(raw: String?): String? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val m = VIEWS.matchEntire(s) ?: return s
        val num = m.groupValues[1]
        val unit = when (m.groupValues[2].uppercase()) { "K" -> " tys."; "M" -> " mln"; "B" -> " mld"; else -> "" }
        // „2.6M” → „2,6 mln”; „1,234” (separator tysięcy) → „1 234”.
        val n = if (unit.isEmpty()) num.replace(",", " ").replace(".", " ") else num.replace(".", ",")
        return "$n$unit wyśw."
    }

    fun date(raw: String?): String? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        DATE.matchEntire(s)?.let { m ->
            val i = MONTHS.indexOf(m.groupValues[1].lowercase())
            if (i >= 0) return "${m.groupValues[2]} ${PL_MONTHS[i]} ${m.groupValues[3]}"
        }
        AGO.matchEntire(s)?.let { m ->
            val n = m.groupValues[1].toInt()
            val word = when (m.groupValues[2].lowercase()) {
                "second" -> "s"
                "minute" -> "min"
                "hour" -> "godz."
                "day" -> if (n == 1) "dzień" else "dni"
                "week" -> plural(n, "tydzień", "tygodnie", "tygodni")
                "month" -> plural(n, "miesiąc", "miesiące", "miesięcy")
                else -> plural(n, "rok", "lata", "lat")
            }
            return "$n $word temu"
        }
        return s
    }

    /** 1 rok, 2–4 lata (bez 12–14), 5+ lat. */
    internal fun plural(n: Int, one: String, few: String, many: String): String = when {
        n == 1 -> one
        n % 10 in 2..4 && n % 100 !in 12..14 -> few
        else -> many
    }
}
