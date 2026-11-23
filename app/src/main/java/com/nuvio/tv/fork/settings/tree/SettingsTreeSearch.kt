package com.nuvio.tv.fork.settings.tree

/**
 * Szukanie w drzewie ustawień (23.11). Dawne szukanie (`ui/screens/settings/SettingsSearch.kt`) znało tylko angielskie nazwy
 * kategorii Nuvio: „napisy”, „pilot”, „lektor” nic nie znajdowały, pozycji forka i Reshaped w ogóle nie było.
 * Tu: każda pozycja drzewa (także ukryta w trybie prostym i pod „Więcej”), bez polskich znaków, po początkach słów,
 * plus nazwy wierszy w środku ekranów ([SettingsTreeWords]). Czysty Kotlin (test JVM).
 */
data class TreeHit(
    val category: TreeCategory,
    val group: TreeGroup,
    val item: TreeItem,
    /** Nazwa wiersza w środku ekranu, gdy pasuje tylko ona (pokazywana jako „zawiera: …”). */
    val inner: String? = null,
    val score: Int = 0,
)

object SettingsTreeSearch {
    const val LIMIT = 40

    private val PL = mapOf('ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n', 'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z')

    /** Małe litery, bez polskich znaków, tylko litery i cyfry (reszta = spacja). */
    fun norm(s: String): String = buildString(s.length) {
        s.lowercase().forEach { ch -> val c = PL[ch] ?: ch; append(if (c.isLetterOrDigit()) c else ' ') }
    }

    private fun words(s: String) = norm(s).split(' ').filter { it.isNotEmpty() }

    /** Czy każde słowo zapytania jest początkiem któregoś słowa tekstu (dwuznakowe i dłuższe także w środku słowa złożonego). */
    fun matches(queryWords: List<String>, text: String): Boolean {
        if (queryWords.isEmpty()) return false
        val w = words(text)
        val joined = " " + w.joinToString(" ")
        return queryWords.all { q -> w.any { it.startsWith(q) } || (q.length >= 4 && joined.contains(q)) }
    }

    class Entry(val category: TreeCategory, val group: TreeGroup, val item: TreeItem, val title: String, val main: String, val inner: List<String>)

    // Punkty: nazwa 80–100, opis i słowa kluczowe 60, nazwa grupy 50, wiersz w środku ekranu 40, nazwa kategorii 30.

    /** Indeks z pełnego drzewa (nie z widoku trybu prostego — szukanie znajduje też ukryte pozycje). */
    fun index(tree: List<TreeCategory>, words: Map<String, List<String>> = SettingsTreeWords.byFile): List<Entry> =
        tree.flatMap { c ->
            c.groups.flatMap { g ->
                g.items.map { i ->
                    Entry(c, g, i, i.title, listOf(i.title, i.description, i.keywords).joinToString(" "),
                        i.words.flatMap { words[it].orEmpty() }.distinct())
                }
            }
        }

    fun search(index: List<Entry>, query: String): List<TreeHit> {
        val q = words(query)
        if (q.isEmpty()) return emptyList()
        val nq = q.joinToString(" ")
        val hits = index.mapNotNull { e ->
            val t = norm(e.title).trim()
            val score = when {
                t == nq -> 100
                t.startsWith(nq) -> 90
                matches(q, e.title) -> 80
                matches(q, e.main) -> 60
                matches(q, e.group.title + " " + e.main) -> 50
                matches(q, e.category.title + " " + e.group.title + " " + e.main) -> 30
                else -> -1
            }
            val inner = if (score >= 50) null else e.inner.firstOrNull { matches(q, it) }
            when {
                inner != null -> TreeHit(e.category, e.group, e.item, inner, 40)
                score >= 0 -> TreeHit(e.category, e.group, e.item, null, score)
                else -> null
            }
        }
        return hits.sortedWith(compareByDescending<TreeHit> { it.score }.thenBy { it.item.level.ordinal }.thenBy { it.item.title.length })
            .distinctBy { it.item.id }.take(LIMIT)
    }

    /** Opis wyniku: „Kategoria › Grupa” (+ „zawiera: …” albo „Więcej”). */
    fun subtitle(h: TreeHit): String = buildString {
        append(h.category.title).append(" › ").append(h.group.title)
        if (h.item.level == TreeLevel.MORE) append(" › Więcej")
        if (h.inner != null) append(" — zawiera: ").append(h.inner)
    }
}
