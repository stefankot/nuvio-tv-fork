package com.nuvio.tv.skin.niunia.awards

import android.util.Log

/*
 * Z-13 „Nagrody i nominacje” (DS 2.2.0, NAwards): model, rejestr gal, kolejność, odmiana i podsumowania.
 * Czysty Kotlin (bez Compose), testy: NiuniaAwardsTest. Wzór: design-system/bundle/nuvio.js (AWARD_BODIES, parseAwards, NAwards).
 */

internal const val AWARDS_TAG = "ForkNiunia"

enum class NiuniaAwardResult { Won, Nominated, Pending }

/** Jedna nagroda albo nominacja tytułu. [name]/[short] tylko dla gal spoza rejestru; [date] tylko dla [NiuniaAwardResult.Pending]. */
data class NiuniaAward(
    val body: String,
    val year: Int?,
    val category: String,
    val result: NiuniaAwardResult,
    val nominee: String? = null,
    val date: String? = null,
    val name: String? = null,
    val short: String? = null,
)

/** Gala: [icon] = sylwetka (NIcon award-*), inaczej znak słowny [short] w ramce. [tier]: 1 międzynarodowe, 2 akademie krajowe, 3 gildie/krytycy/gatunkowe, 4 festiwale i reszta. */
data class NiuniaAwardBody(val key: String, val name: String, val short: String?, val icon: String?, val tier: Int) {
    /** Krótka nazwa do logów i testów („BAFTA”, „VES”). */
    val label: String get() = if (icon != null) name else short ?: name
}

/** Rejestr gal w kolejności AWARD_BODIES (= rozpoznawalność; ta sama kolejność sortowania co w design systemie). */
object NiuniaAwardBodies {
    val registry: List<NiuniaAwardBody> = listOf(
        NiuniaAwardBody("oscar", "Oscar", null, "award-oscar", 1),
        NiuniaAwardBody("cannes", "Festiwal w Cannes", null, "award-palm", 1),
        NiuniaAwardBody("berlinale", "Berlinale", null, "award-bear", 1),
        NiuniaAwardBody("venice", "Festiwal w Wenecji", "Venezia", null, 1),
        NiuniaAwardBody("sundance", "Sundance", "Sundance", null, 1),
        NiuniaAwardBody("emmy", "Emmy", null, "award-emmy", 1),
        NiuniaAwardBody("globe", "Złoty Glob", null, "award-globe", 1),
        NiuniaAwardBody("bafta", "BAFTA", null, "award-bafta", 1),
        NiuniaAwardBody("saturn", "Saturn", null, "award-saturn", 3),
        NiuniaAwardBody("magritte", "Magritte", "Magritte", null, 2),
        NiuniaAwardBody("jafp", "Japan Academy Film Prize", "Japan Academy", null, 2),
        NiuniaAwardBody("baeksang", "Baeksang Arts Awards", "Baeksang", null, 2),
        NiuniaAwardBody("bifa", "British Independent Film Awards", "BIFA", null, 2),
        NiuniaAwardBody("ves", "Visual Effects Society", "VES", null, 3),
        NiuniaAwardBody("adg", "Art Directors Guild", "ADG", null, 3),
        NiuniaAwardBody("sag", "Screen Actors Guild", "SAG", null, 3),
        NiuniaAwardBody("wga", "Writers Guild of America", "WGA", null, 3),
        NiuniaAwardBody("cca", "Critics' Choice", "CCA", null, 3),
        NiuniaAwardBody("crunchyroll", "Crunchyroll Anime Awards", "Crunchyroll", null, 3),
        NiuniaAwardBody("grammy", "Grammy", "Grammy", null, 2),
    )
    private val byKey = registry.associateBy { it.key }

    /** Pozycja w rejestrze; gala spoza rejestru = 999 (za wszystkimi). */
    fun rank(key: String): Int = registry.indexOfFirst { it.key == key.lowercase() }.let { if (it < 0) 999 else it }

    /** Gala z rejestru; spoza rejestru: nazwa z danych (poziom 4); bez nazwy: znak słowny z klucza + błąd w logu. */
    fun of(key: String, name: String? = null, short: String? = null): NiuniaAwardBody {
        byKey[key.lowercase()]?.let { return it }
        if (!name.isNullOrBlank()) return NiuniaAwardBody(key.lowercase(), name, short?.takeIf { it.isNotBlank() } ?: name, null, 4)
        Log.e(AWARDS_TAG, "NAwards: UnknownAwardBody: brak gali $key i brak nazwy w danych (znak słowny z klucza)")
        return NiuniaAwardBody(key.lowercase(), key, key.uppercase().take(4), null, 4)
    }
}

/** Gala tytułu: wygrane i nominacje (z galą przed rozdaniem). */
data class NiuniaAwardGroup(val body: NiuniaAwardBody, val wins: List<NiuniaAward>, val noms: List<NiuniaAward>) {
    val total: Int get() = wins.size + noms.size
}

/** Nagrody tytułu pogrupowane i ułożone: poziom → liczba wygranych → kolejność w rejestrze → liczba wszystkich. */
data class NiuniaAwardsData(val all: List<NiuniaAward>, val groups: List<NiuniaAwardGroup>, val wins: Int, val noms: Int) {
    val isEmpty: Boolean get() = all.isEmpty()
}

fun niuniaAwardsData(awards: List<NiuniaAward>): NiuniaAwardsData {
    val order = LinkedHashMap<String, Pair<MutableList<NiuniaAward>, MutableList<NiuniaAward>>>()
    val first = HashMap<String, NiuniaAward>()
    awards.forEach { a ->
        val k = a.body.lowercase()
        val g = order.getOrPut(k) { first[k] = a; mutableListOf<NiuniaAward>() to mutableListOf() }
        if (a.result == NiuniaAwardResult.Won) g.first.add(a) else g.second.add(a)
    }
    val groups = order.map { (k, g) -> NiuniaAwardGroup(NiuniaAwardBodies.of(k, first[k]?.name, first[k]?.short), g.first, g.second) }
        .sortedWith(
            compareBy<NiuniaAwardGroup> { it.body.tier }
                .thenByDescending { it.wins.size }
                .thenBy { NiuniaAwardBodies.rank(it.body.key) }
                .thenByDescending { it.total }
        )
    val wins = awards.count { it.result == NiuniaAwardResult.Won }
    return NiuniaAwardsData(awards, groups, wins, awards.size - wins)
}

/** Polskie formy liczebnika (z resources skórki; w testach podane wprost). */
data class NiuniaAwardWords(
    val awardOne: String, val awardFew: String, val awardMany: String,
    val nomOne: String, val nomFew: String, val nomMany: String,
    val galaOne: String, val galaFew: String, val galaMany: String,
)

/** „1 nagroda”, „2 nagrody”, „5 nagród”, „12 nagród”, „22 nagrody”. */
fun niuniaPlural(n: Int, one: String, few: String, many: String): String {
    val t = n % 10
    val h = n % 100
    val w = when {
        n == 1 -> one
        t in 2..4 && (h < 12 || h > 14) -> few
        else -> many
    }
    return "$n $w"
}

fun NiuniaAwardWords.awards(n: Int) = niuniaPlural(n, awardOne, awardFew, awardMany)
fun NiuniaAwardWords.noms(n: Int) = niuniaPlural(n, nomOne, nomFew, nomMany)
fun NiuniaAwardWords.galas(n: Int) = niuniaPlural(n, galaOne, galaFew, galaMany)

/** „6 nagród · 16 nominacji”; bez wygranych tylko nominacje, bez nominacji tylko nagrody. */
fun niuniaAwardsSummary(wins: Int, noms: Int, w: NiuniaAwardWords): String =
    listOfNotNull(if (wins > 0) w.awards(wins) else null, if (noms > 0) w.noms(noms) else null).joinToString(" · ")

/** Licznik jednej gali: wygrane, a gdy ich nie ma — nominacje. */
fun niuniaGroupCount(g: NiuniaAwardGroup, w: NiuniaAwardWords): String = if (g.wins.isNotEmpty()) w.awards(g.wins.size) else w.noms(g.noms.size)

/** Pasek w hero: znaki (najwyżej [max], tylko poziomy 1–3) i reszta jako jedna liczba („26 nagród” albo „5 nominacji”). */
data class NiuniaAwardsStripPlan(val shown: List<NiuniaAwardGroup>, val rest: String?)

fun niuniaAwardsStrip(d: NiuniaAwardsData, w: NiuniaAwardWords, max: Int = 3): NiuniaAwardsStripPlan {
    val shown = d.groups.filter { it.body.tier < 4 }.take(max)
    val rest = d.groups.filter { it !in shown }
    val rw = rest.sumOf { it.wins.size }
    val rn = rest.sumOf { it.noms.size }
    val restText = when {
        rest.isEmpty() -> null
        rw > 0 -> w.awards(rw)
        rn > 0 -> w.noms(rn)
        else -> null
    }
    return NiuniaAwardsStripPlan(shown, restText)
}

/** Opis dostępności paska: „Nagrody: 6 nagród · 16 nominacji (BAFTA 1 nagroda, …)” — bez słowa „Nagrody:” (z resources). */
fun niuniaAwardsStripDescription(d: NiuniaAwardsData, w: NiuniaAwardWords): String =
    niuniaAwardsSummary(d.wins, d.noms, w) + " (" + d.groups.joinToString(", ") { it.body.name + " " + niuniaGroupCount(it, w) } + ")"

/** Wiersz listy: [via] = nazwa festiwalu w zbiorczej grupie „Festiwale i inne gale”. */
data class NiuniaAwardRowData(val award: NiuniaAward, val via: String?)

/** Grupa listy: [body] == null = zbiorcza grupa festiwali (poziom 4, znak award-laurel, [galas] gal). Wygrane pierwsze. */
data class NiuniaAwardListGroup(val body: NiuniaAwardBody?, val galas: Int, val rows: List<NiuniaAwardRowData>, val wins: Int, val noms: Int)

fun niuniaAwardsListGroups(d: NiuniaAwardsData): List<NiuniaAwardListGroup> {
    val main = d.groups.filter { it.body.tier < 4 }
    val tail = d.groups.filter { it.body.tier >= 4 }
    val out = main.map { g -> NiuniaAwardListGroup(g.body, 1, (g.wins + g.noms).map { NiuniaAwardRowData(it, null) }, g.wins.size, g.noms.size) }.toMutableList()
    when {
        tail.size == 1 -> tail[0].let { g -> out += NiuniaAwardListGroup(g.body, 1, (g.wins + g.noms).map { NiuniaAwardRowData(it, null) }, g.wins.size, g.noms.size) }
        tail.size > 1 -> {
            val rows = tail.flatMap { g -> g.wins.map { NiuniaAwardRowData(it, g.body.name) } } + tail.flatMap { g -> g.noms.map { NiuniaAwardRowData(it, g.body.name) } }
            out += NiuniaAwardListGroup(null, tail.size, rows, tail.sumOf { it.wins.size }, tail.sumOf { it.noms.size })
        }
    }
    return out
}

/**
 * Dwie zrównoważone kolumny (jak CSS column-count 2): grupy w kolejności, nie łamią się między kolumnami.
 * Zwraca liczbę grup w lewej kolumnie (waga grupy = wiersze + 2 za nagłówek i odstęp).
 */
fun niuniaAwardsSplit(groups: List<NiuniaAwardListGroup>): Int {
    if (groups.size < 2) return groups.size
    val weights = groups.map { it.rows.size + 2 }
    val total = weights.sum()
    var best = 1
    var bestDiff = Int.MAX_VALUE
    var left = 0
    for (k in 1 until groups.size) {
        left += weights[k - 1]
        val diff = maxOf(left, total - left)
        if (diff < bestDiff) { bestDiff = diff; best = k }
    }
    return best
}

/** Plakietka na plakacie: znak gali + tekst („28 nagród”, „4 nominacje”, „Cannes: najlepsza reżyseria”); [nomination] = przygaszona. */
data class NiuniaAwardBadge(val text: String, val body: NiuniaAwardBody?, val nomination: Boolean)

/** Plakietka z nagród tytułu: najważniejsza gala jako znak, liczba wygranych (albo nominacji, gdy wygranych brak). */
fun niuniaAwardBadge(d: NiuniaAwardsData, w: NiuniaAwardWords): NiuniaAwardBadge? {
    val top = d.groups.firstOrNull() ?: return null
    return if (d.wins > 0) NiuniaAwardBadge(w.awards(d.wins), top.body, false)
    else NiuniaAwardBadge(w.noms(d.noms), top.body, true)
}
