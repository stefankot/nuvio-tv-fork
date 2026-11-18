package com.nuvio.tv.fork.voice

import java.text.Normalizer

object Text {
    private val COMBINING = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val NON_WORD = Regex("[^a-z0-9: ]+")
    private val SPACES = Regex("\\s+")

    /** Małe litery, bez znaków diakrytycznych i interpunkcji (zostają cyfry i dwukropek w godzinach), pojedyncze spacje. */
    fun norm(raw: String): String {
        val lower = raw.lowercase().replace('ł', 'l').replace('ø', 'o')
        val plain = COMBINING.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "")
        return plain.replace(NON_WORD, " ").replace(SPACES, " ").trim()
    }

    private val numberWords = mapOf(
        "zero" to "0", "jeden" to "1", "jedna" to "1", "jedynka" to "1", "pierwszy" to "1", "dwa" to "2", "dwie" to "2", "dwojka" to "2", "dwojce" to "2", "dwojke" to "2", "drugi" to "2",
        "trzy" to "3", "trojka" to "3", "trzeci" to "3", "cztery" to "4", "czworka" to "4", "czwarty" to "4", "piec" to "5", "piatka" to "5", "piaty" to "5",
        "szesc" to "6", "siedem" to "7", "osiem" to "8", "dziewiec" to "9", "dziesiec" to "10",
    )

    /** Zamienia liczebniki słowne na cyfry ("tvp jeden" → "tvp 1"). */
    fun digits(norm: String): String = norm.replace("dwadziescia cztery", "24").split(' ').joinToString(" ") { numberWords[it] ?: it }

    /** Końcówka fleksyjna odcięta od słów ≥ 5 liter ("polsacie" → "polsac" ~ "polsat"). */
    fun stem(token: String): String {
        if (token.length < 5) return token
        for (suf in listOf("owi", "ego", "iem", "ach", "ami", "emu", "ie", "em", "ow", "ej", "om", "a", "e", "u", "y", "i", "o")) {
            if (token.endsWith(suf) && token.length - suf.length >= 4) return token.dropLast(suf.length)
        }
        return token
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]; dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return dp[b.length]
    }
}

data class Entity(val id: String, val names: List<String>)
data class EntityMatch(val id: String, val score: Float, val name: String)

/** Słownik encji z przybliżonym dopasowaniem (bez diakrytyków, z odmianą, błędami rozpoznawania mowy). */
class EntityIndex(entities: List<Entity>) {
    private class Row(val id: String, val name: String, val key: String, val stemKey: String)
    private val rows: List<Row> = entities.flatMap { e ->
        e.names.flatMap { Variants.expand(it) }.distinct().map { n ->
            val d = Text.digits(Text.norm(n))
            Row(e.id, n, d, d.split(' ').joinToString(" ") { Text.stem(it) })
        }
    }

    fun match(query: String, limit: Int = 3): List<EntityMatch> {
        val q = Text.digits(Text.norm(query)); if (q.isEmpty()) return emptyList()
        val qs = q.split(' ').joinToString(" ") { Text.stem(it) }
        val best = HashMap<String, EntityMatch>()
        for (r in rows) {
            val sc = score(qs, r.stemKey, q, r.key)
            val prev = best[r.id]
            if (sc > 0.55f && (prev == null || sc > prev.score)) best[r.id] = EntityMatch(r.id, sc, r.name)
        }
        return best.values.sortedByDescending { it.score }.take(limit)
    }

    private fun score(qs: String, cs: String, q: String, c: String): Float {
        if (qs == cs) return 1f
        val ct = cs.split(' '); val qt = qs.split(' ')
        if (ct.all { it in qt } && ct.size >= 1) return 0.9f + 0.05f * (ct.size.toFloat() / qt.size)
        val d = Text.levenshtein(qs.replace(" ", ""), cs.replace(" ", ""))
        val len = maxOf(qs.replace(" ", "").length, cs.replace(" ", "").length)
        val s = 1f - d.toFloat() / len
        return if (len >= 4) s else if (d == 0) 1f else 0f
    }
}

/** Warianty nazw (jak mówi je człowiek lub słyszy rozpoznawanie mowy). */
object Variants {
    private val phonetic = mapOf("tvp" to "tefałpe", "tvn" to "tefałen", "hbo" to "habeo", "cnn" to "cenen", "bbc" to "bibisi", "tv" to "tefał")

    fun expand(name: String): List<String> {
        val out = linkedSetOf(name)
        val n = Text.norm(name)
        val tokens = n.split(' ')
        // "tvp 1" → "jedynka", "tvp 2" → "dwójka" (TVP i podobne)
        if (tokens.size == 2 && tokens[1].toIntOrNull() in 1..3 && tokens[0] == "tvp") out += listOf("jedynka", "dwojka", "trojka")[tokens[1].toInt() - 1]
        phonetic.forEach { (k, v) -> if (tokens.contains(k)) out += tokens.joinToString(" ") { if (it == k) v else it } }
        if (tokens.any { it.toIntOrNull() != null }) out += tokens.joinToString(" ") { t -> t.toIntOrNull()?.let { numberWord(it) } ?: t }
        return out.toList()
    }

    private fun numberWord(n: Int) = listOf("zero", "jeden", "dwa", "trzy", "cztery", "piec", "szesc", "siedem", "osiem", "dziewiec", "dziesiec").getOrNull(n) ?: n.toString()
}
