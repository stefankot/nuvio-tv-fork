package com.nuvio.tv.skin.niunia.awards

/*
 * Z-13 (statystyki zamiast listy) i Z-13b (nagrody tekstem w hero): kategorie, nagłówki gal i linie tekstu.
 * Czysty Kotlin, wzór: design-system/bundle/nuvio.js (AWARD_CATEGORIES, bodyHeadline, AwardsSummary, AwardsText) — ta sama kolejność reguł.
 * Słowa gal (trofea, „w Cannes”, „do Oscara”) i nazwy kategorii są danymi rejestru jak nazwy gal w [NiuniaAwardBodies].
 */

/** Dziedzina nagrody: [id], polska nazwa, ikona `cat-*`, reguła po słowach kluczowych (pierwsza pasująca wygrywa). */
data class NiuniaAwardCategory(val id: String, val label: String, val icon: String, val re: Regex)

val NIUNIA_AWARD_OTHER = NiuniaAwardCategory("other", "Inne wyróżnienia", "award-laurel", Regex("(?!)"))

val NIUNIA_AWARD_CATEGORIES: List<NiuniaAwardCategory> = listOf(
    NiuniaAwardCategory("stunt", "Kaskaderzy", "cat-stunt", Regex("stunt|kaskader")),
    NiuniaAwardCategory("sound", "Dźwięk", "cat-sound", Regex("sound(?!track)|dźwięk")),
    NiuniaAwardCategory("music", "Muzyka", "cat-music", Regex("music|score|song|composer|soundtrack|muzyk|piosenk")),
    NiuniaAwardCategory("vfx", "Efekty wizualne", "cat-vfx", Regex("visual|effects|compositing|environment|vfx|efekt")),
    NiuniaAwardCategory("title", "Czołówka", "cat-title", Regex("main title|title design|czołówk")),
    NiuniaAwardCategory("design", "Scenografia", "cat-design", Regex("production design|art direction|scenograf")),
    NiuniaAwardCategory("costume", "Kostiumy", "cat-costume", Regex("costume|kostium")),
    NiuniaAwardCategory("makeup", "Charakteryzacja", "cat-makeup", Regex("make-?up|prosthetic|hairstyl|charakteryz")),
    NiuniaAwardCategory("camera", "Zdjęcia", "cat-camera", Regex("cinematograph|photography|zdjęci")),
    NiuniaAwardCategory("edit", "Montaż", "cat-edit", Regex("editing|montaż")),
    NiuniaAwardCategory("casting", "Casting", "cat-casting", Regex("casting")),
    NiuniaAwardCategory("writing", "Scenariusz", "cat-writing", Regex("screenplay|written|writing|writer|script|scenariusz")),
    NiuniaAwardCategory("directing", "Reżyseria", "cat-directing", Regex("best director|directing|director[’']s award|reżyser")),
    NiuniaAwardCategory("acting", "Aktorstwo", "cat-acting", Regex("actor|actress|performance|ensemble|voice|villain|talent|promising|aktor|rola")),
    NiuniaAwardCategory("film", "Najlepszy film", "cat-film", Regex("picture|film|feature|movie|cinema")),
    NiuniaAwardCategory("series", "Najlepszy serial", "cat-series", Regex("series|television|\\btv\\b|show|drama\\b|serial")),
    NiuniaAwardCategory("film", "Najlepszy film", "cat-film", Regex("anime|bear|prix|palm|teddy|top ten|of the year|award|jury|audience|kino")),
)

fun niuniaAwardCategory(category: String): NiuniaAwardCategory {
    val t = category.lowercase()
    return NIUNIA_AWARD_CATEGORIES.firstOrNull { it.re.containsMatchIn(t) } ?: NIUNIA_AWARD_OTHER
}

/** Słowa gali: trofeum z własną nazwą (1 / 2–4 / 5+), fraza po „nagroda” ([of]) i po „nominacje” ([nom]); [label] krótka nazwa do tekstu w hero. */
private data class Phrases(val trophy: Triple<String, String, String>? = null, val of: String? = null, val nom: String? = null, val label: String? = null)
private val PHRASES = mapOf(
    "oscar" to Phrases(Triple("Oscar", "Oscary", "Oscarów"), nom = "do Oscara"),
    "cannes" to Phrases(of = "w Cannes", nom = "w Cannes", label = "Cannes"),
    "berlinale" to Phrases(of = "na Berlinale", nom = "na Berlinale"),
    "venice" to Phrases(of = "w Wenecji", nom = "w Wenecji", label = "Wenecja"),
    "sundance" to Phrases(of = "na Sundance", nom = "na Sundance"),
    "emmy" to Phrases(of = "Emmy", nom = "do Emmy"),
    "globe" to Phrases(Triple("Złoty Glob", "Złote Globy", "Złotych Globów"), nom = "do Złotego Globu"),
    "bafta" to Phrases(of = "BAFTA", nom = "do BAFTA"),
    "saturn" to Phrases(Triple("Saturn", "Saturny", "Saturnów"), nom = "do Saturna"),
    "grammy" to Phrases(of = "Grammy", nom = "do Grammy"),
)

/** Krótka nazwa gali w tekście hero („Emmy”, „BAFTA”, „VES”, „Cannes”). */
fun niuniaAwardTextLabel(b: NiuniaAwardBody): String = PHRASES[b.key]?.label ?: if (b.icon != null) b.name else b.short ?: b.name

/** „2023–2025” albo „2024”; brak lat = pusty tekst. */
fun niuniaYearSpan(xs: List<NiuniaAward>): String {
    val ys = xs.mapNotNull { it.year }.filter { it > 0 }.sorted()
    return when { ys.isEmpty() -> ""; ys.first() == ys.last() -> ys.first().toString(); else -> "${ys.first()}–${ys.last()}" }
}

/** „1 Oscar”, „3 Złote Globy”, „2 nagrody VES”, „7 nominacji do Emmy”. */
fun niuniaBodyHeadline(b: NiuniaAwardBody, wins: Int, noms: Int, w: NiuniaAwardWords): String {
    val ph = PHRASES[b.key]
    if (wins > 0) return ph?.trophy?.let { (o, f, m) -> niuniaPlural(wins, o, f, m) } ?: "${w.awards(wins)} ${ph?.of ?: b.short ?: b.name}"
    return "${w.noms(noms)} ${ph?.nom ?: ph?.of ?: b.short ?: b.name}"
}

/** Kafelek statystyki: [body] = znak gali (null przy kategorii i „innych”), [icon] = ikona kategorii albo laur. */
data class NiuniaStatTile(val key: String, val label: String, val supporting: String, val body: NiuniaAwardBody?, val icon: String?, val description: String)

/** Dziedzina z sumą wygranych i nominacji. */
data class NiuniaCategoryCount(val category: NiuniaAwardCategory, val wins: Int, val noms: Int)

fun niuniaCategoryCounts(d: NiuniaAwardsData): List<NiuniaCategoryCount> {
    val map = LinkedHashMap<String, Pair<NiuniaAwardCategory, IntArray>>()
    d.all.forEach { a ->
        val c = niuniaAwardCategory(a.category)
        val e = map.getOrPut(c.id) { c to IntArray(2) }
        if (a.result == NiuniaAwardResult.Won) e.second[0]++ else e.second[1]++
    }
    return map.values.map { (c, n) -> NiuniaCategoryCount(c, n[0], n[1]) }
        .sortedWith(compareByDescending<NiuniaCategoryCount> { it.wins }.thenByDescending { it.wins + it.noms })
}

/**
 * Z-13: kafelki gal (poziomy 1–3, każda osobno; poziom 4 i nadmiar ponad [maxBodies] w jednym kafelku „innych”) i kafelki dziedzin
 * (najwięcej wygranych; ponad [maxCategories] reszta jako „Inne wyróżnienia”).
 */
fun niuniaAwardsSummaryTiles(
    d: NiuniaAwardsData, w: NiuniaAwardWords, also: String, otherAwards: Triple<String, String, String>, otherNoms: Triple<String, String, String>,
    festivalsName: String, maxBodies: Int = 8, maxCategories: Int = 8,
): Pair<List<NiuniaStatTile>, List<NiuniaStatTile>> {
    var top = d.groups.filter { it.body.tier < 4 }
    var tail = d.groups.filter { it.body.tier >= 4 }
    if (top.size + (if (tail.isNotEmpty()) 1 else 0) > maxBodies) { tail = top.drop(maxBodies - 1) + tail; top = top.take(maxBodies - 1) }
    val bodies = top.map { g ->
        val head = niuniaBodyHeadline(g.body, g.wins.size, g.noms.size, w)
        val sub = listOfNotNull(if (g.wins.isNotEmpty() && g.noms.isNotEmpty()) "$also ${w.noms(g.noms.size)}" else null, niuniaYearSpan(g.wins + g.noms).ifBlank { null }).joinToString(" · ")
        NiuniaStatTile(g.body.key, head, sub, g.body, null, "${g.body.name}: $head" + if (sub.isNotBlank()) ", $sub" else "")
    }.toMutableList()
    if (tail.isNotEmpty()) {
        val tw = tail.sumOf { it.wins.size }; val tn = tail.sumOf { it.noms.size }
        val th = if (tw > 0) niuniaPlural(tw, otherAwards.first, otherAwards.second, otherAwards.third) else niuniaPlural(tn, otherNoms.first, otherNoms.second, otherNoms.third)
        val ts = listOfNotNull(if (tw > 0 && tn > 0) "$also ${w.noms(tn)}" else null, w.galas(tail.size)).joinToString(" · ")
        bodies += NiuniaStatTile("tail", th, ts, null, "award-laurel", "$festivalsName: $th, $ts")
    }
    var cats = niuniaCategoryCounts(d)
    if (cats.size > maxCategories) {
        val rest = cats.drop(maxCategories - 1)
        cats = cats.take(maxCategories - 1) + NiuniaCategoryCount(NIUNIA_AWARD_OTHER, rest.sumOf { it.wins }, rest.sumOf { it.noms })
    }
    val catTiles = cats.map { c ->
        val sum = niuniaAwardsSummary(c.wins, c.noms, w)
        NiuniaStatTile(c.category.id, c.category.label, sum, null, c.category.icon, "${c.category.label}: $sum")
    }
    return bodies to catTiles
}

/** Z-13b: pozycja linii tekstu „Emmy 0+7”. */
data class NiuniaTextItem(val label: String, val wins: Int, val noms: Int)

/** Z-13b: gale (najbardziej znane, potem najwięcej łącznie; poziom 4 jako „inne”) i dziedziny (najwięcej wygranych). */
fun niuniaAwardsTextItems(d: NiuniaAwardsData, otherLabel: String): Pair<List<NiuniaTextItem>, List<NiuniaTextItem>> {
    val gal = d.groups.filter { it.body.tier < 4 }
        .sortedWith(compareBy<NiuniaAwardGroup> { it.body.tier }.thenByDescending { it.total }.thenByDescending { it.wins.size })
        .map { NiuniaTextItem(niuniaAwardTextLabel(it.body), it.wins.size, it.noms.size) }.toMutableList()
    val tail = d.groups.filter { it.body.tier >= 4 }
    if (tail.isNotEmpty()) gal += NiuniaTextItem(otherLabel, tail.sumOf { it.wins.size }, tail.sumOf { it.noms.size })
    val cat = niuniaCategoryCounts(d).map { NiuniaTextItem(it.category.label, it.wins, it.noms) }
    return gal to cat
}

/** „Gale **Emmy 0+7 · BAFTA 1+0** i 4 inne” (pokazane [max] pozycji, reszta jako liczba). */
fun niuniaAwardsTextLine(lead: String, items: List<NiuniaTextItem>, max: Int, rest: Triple<String, String, String>): String {
    val shown = items.take(max)
    val left = items.size - shown.size
    return "$lead **" + shown.joinToString(" · ") { "${it.label} ${it.wins}+${it.noms}" } + "**" + if (left > 0) " i " + niuniaPlural(left, rest.first, rest.second, rest.third) else ""
}
