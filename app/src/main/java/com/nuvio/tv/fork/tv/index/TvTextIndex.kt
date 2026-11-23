package com.nuvio.tv.fork.tv.index

import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Indeks tekstów Live TV do szukania — liczony w tle i przyrostowo (właściciel 22.11: „mechanizmy, które liczą wszystko
 * w tle, nie przy każdym zapytaniu, i nie od nowa, tylko inkrementalnie”). JMGO build171: szukanie „tvn” przy
 * 252 827 kanałach i ~1 mln emisji trwało ~110 s, bo każde zapytanie normalizowało od nowa każdą nazwę i każdy tytuł.
 *
 * - nazwa kanału → tekst do szukania ([norm]) liczony raz na nazwę; przeładowanie listy liczy tylko nowe nazwy;
 * - tytuł programu → tekst do szukania liczony raz na tytuł (tytuły powtarzają się tysiące razy: „News”, „5 minutes”);
 * - spis „tytuł → kanały (klucz przewodnika)” aktualizowany tylko dla kanałów, których lista programów się zmieniła
 *   (porównanie z poprzednim odczytem po tożsamości listy — niezmienione kanały nic nie kosztują).
 * Zasilany z [com.nuvio.tv.fork.tv.most.TvBridge.onGuideRead] (bez nowych wpięć w Reshaped). Jeden wątek tła o niskim
 * priorytecie; dane, które przyjdą w czasie liczenia, czekają w jednym miejscu (starsze są pomijane — liczy się najnowsze).
 * Zapytanie ([titles], [nameOf]) tylko czyta gotowe struktury.
 */
object TvTextIndex {
    private const val TAG = "ForkTvIndex"

    /** Małe litery, ł → l, bez znaków diakrytycznych (jak `norm()` makiety i ForgeEpgCategories). */
    fun norm(s: String): String {
        val d = Normalizer.normalize(s.lowercase().replace('ł', 'l'), Normalizer.Form.NFD)
        var plain = true
        for (c in d) if (Character.getType(c) == Character.NON_SPACING_MARK.toInt() || Character.isSurrogate(c)) { plain = false; break }
        if (plain) return d
        val out = StringBuilder(d.length)
        var i = 0
        while (i < d.length) {
            val cp = d.codePointAt(i)
            if (Character.getType(cp) != Character.NON_SPACING_MARK.toInt()) out.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return out.toString()
    }

    // 22.11 (właściciel: „liczyć tylko przy większej zmianie”): teksty w trwałej pamięci na dysku (TvIndexStore) —
    // po restarcie aplikacji wczytywane, liczone tylko nowe nazwy i tytuły.
    private val names get() = TvIndexStore.names
    private val keys get() = TvIndexStore.keys
    private val titleNorm get() = TvIndexStore.titles

    /** Znormalizowany tytuł → klucze przewodnika kanałów, które go nadają (zbiór niezmienny, podmieniany przy zmianie). */
    private val keysByTitle = ConcurrentHashMap<String, Set<String>>()

    /** Tylko wątek indeksu: poprzedni odczyt przewodnika (listy po kluczu) — do porównania po tożsamości. */
    private var lastSchedule: Map<String, List<LiveTvProgramme>> = emptyMap()

    @Volatile var version = 0
        private set

    private class Input(val channels: List<LiveTvChannel>, val schedule: Map<String, List<LiveTvProgramme>>)
    private val pending = AtomicReference<Input?>(null)
    private val running = AtomicBoolean(false)
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); r.run() }, "fork-tv-index").apply { isDaemon = true }
    }

    /** Nowy stan kanałów i przewodnika (wołane z odczytu przewodnika — wraca od razu, liczenie w tle). */
    fun submit(channels: List<LiveTvChannel>, schedule: Map<String, List<LiveTvProgramme>>) {
        pending.set(Input(channels, schedule))
        if (running.compareAndSet(false, true)) worker.execute(::drain)
    }

    private fun drain() {
        try {
            while (true) {
                val input = pending.getAndSet(null) ?: break
                try {
                    TvIndexStore.awaitLoaded()
                    restoreTitles()
                    update(input.channels, input.schedule)
                } catch (e: Exception) {
                    Log.w(TAG, "indeks: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        } finally {
            running.set(false)
            if (pending.get() != null && running.compareAndSet(false, true)) worker.execute(::drain)
        }
    }

    /** Jeden przebieg (w tle; testy wołają wprost). */
    @Synchronized
    internal fun update(channels: List<LiveTvChannel>, schedule: Map<String, List<LiveTvProgramme>>) {
        val t0 = System.nanoTime()
        var newNames = 0
        for (ch in channels) if (names[ch.name] == null) { val n = norm(ch.name); names.put(ch.name, n); keys.put(ch.name, channelKey(n)); newNames++ }
        // 23.11: nazwy pozycji, których nie ma już na listach (np. odsiane filmy), znikają z pamięci i z dysku.
        TvIndexStore.pruneNames(channels)
        var changed = 0
        var newTitles = 0
        fun titleOf(raw: String): String = titleNorm[raw] ?: norm(raw.trim()).also { titleNorm.put(raw, it); newTitles++ }
        var sameAsSaved = 0
        val previous = lastSchedule
        // Kanały, które zniknęły z przewodnika.
        for ((key, old) in previous) if (!schedule.containsKey(key)) { remove(key, old.mapTo(HashSet()) { titleOf(it.title) }); TvIndexStore.fingerprints.remove(key); changed++ }
        for ((key, list) in schedule) {
            val old = previous[key]
            if (old === list) continue
            // Po restarcie (brak poprzedniego odczytu w pamięci): lista taka sama jak przy ostatnim zapisie → nic do liczenia.
            val fp = fingerprint(list)
            if (old == null && TvIndexStore.fingerprints[key] == fp) { sameAsSaved++; continue }
            TvIndexStore.fingerprints.put(key, fp)
            changed++
            val now = list.mapTo(HashSet()) { titleOf(it.title) }
            val before = old?.mapTo(HashSet()) { titleOf(it.title) }.orEmpty()
            remove(key, before - now)
            for (t in now) if (t.isNotEmpty() && t !in before) keysByTitle.merge(t, setOf(key)) { a, b -> a + b }?.let { saveTitle(t, it) }
        }
        lastSchedule = schedule
        version++
        Log.i(TAG, "indeks: ${channels.size} kanałów (+$newNames nazw), ${schedule.size} przewodników ($changed zmienionych, $sameAsSaved jak w zapisie, +$newTitles tytułów), " +
            "tytułów ${keysByTitle.size}, ${(System.nanoTime() - t0) / 1_000_000} ms")
    }

    private fun remove(key: String, titles: Set<String>) {
        for (t in titles) {
            val left = keysByTitle.computeIfPresent(t) { _, ks -> (ks - key).ifEmpty { null } }
            if (left == null) TvIndexStore.titleKeys.remove(t) else saveTitle(t, left)
        }
    }

    private fun saveTitle(t: String, keys: Set<String>) = TvIndexStore.titleKeys.put(t, keys.joinToString("\u0001"))

    /** Po restarcie: spis „tytuł → kanały” z dysku (raz, zanim przyjdzie pierwszy odczyt przewodnika). */
    @Volatile private var restored = false
    internal fun restoreTitles() {
        if (restored) return
        restored = true
        if (keysByTitle.isNotEmpty()) return
        for ((t, v) in TvIndexStore.titleKeys.map) keysByTitle[t] = v.split('\u0001').toHashSet()
        if (keysByTitle.isNotEmpty()) { version++; Log.i(TAG, "indeks: spis ${keysByTitle.size} tytułów wczytany z dysku") }
    }

    /** Skrót listy programów kanału (liczba + tytuły + początki) — porównanie z zapisem po restarcie. */
    internal fun fingerprint(list: List<LiveTvProgramme>): String {
        var h = 1125899906842597L
        for (p in list) { h = 31 * h + p.title.hashCode(); h = 31 * h + p.startEpochMs }
        return list.size.toString(36) + ":" + java.lang.Long.toString(h, 36)
    }

    /** Tekst nazwy kanału do szukania (z indeksu; brakująca nazwa liczona i zapamiętana od razu). */
    fun nameOf(name: String): String = names[name] ?: norm(name).also { names.put(name, it) }

    /** Nazwa kanału bez kraju, nawiasów i jakości („PL: TVN HD”, „TVN (PL) FHD” → „tvn”): ocena i łączenie wyników. */
    fun keyOf(name: String): String = keys[name] ?: channelKey(nameOf(name)).also { keys.put(name, it) }

    private val QUALITY = setOf("fhd", "uhd", "hd", "sd", "4k", "8k", "hevc", "h264", "h265", "hdr", "raw", "backup", "alt", "vip", "50fps", "+1", "pl")

    /** Jak ForgeEpgCategories.cleanChannelName, ale bez wyrażeń regularnych (liczone dla setek tysięcy nazw). [n] po [norm]. */
    internal fun channelKey(n: String, keepBrackets: Boolean = false): String {
        var t = n.replace('_', ' ').trim()
        // „pl:”, „[pl] |”, „uk |” na początku
        run {
            var i = 0
            if (i < t.length && t[i] == '[') i++
            val start = i
            while (i < t.length && t[i] in 'a'..'z') i++
            val len = i - start
            if (len in 2..3) {
                if (i < t.length && t[i] == ']') i++
                while (i < t.length && t[i] == ' ') i++
                if (i < t.length && (t[i] == ':' || t[i] == '|')) { t = t.substring(i + 1).trim() }
            }
        }
        // nawiasy (…) i […]
        if (!keepBrackets && ('(' in t || '[' in t)) {
            val b = StringBuilder(t.length)
            var depth = 0
            for (c in t) when {
                c == '(' || c == '[' -> { depth++; b.append(' ') }
                (c == ')' || c == ']') && depth > 0 -> depth--
                depth == 0 -> b.append(c)
            }
            t = b.toString()
        }
        for (d in listOf(".pl", ".tv", ".com", ".uk", ".de", ".cz", ".ua")) if (t.endsWith(d)) { t = t.dropLast(d.length); break }
        val words = t.split(' ').filter { it.isNotEmpty() }.toMutableList()
        while (words.size > 1 && (words.last() in QUALITY || (words.size > 2 && words[words.size - 2] == "50" && words.last() == "fps"))) {
            if (words.last() == "fps") words.removeAt(words.size - 1)
            words.removeAt(words.size - 1)
        }
        return words.joinToString(" ")
    }

    /** Klucz łączenia wyników: jak [keyOf], ale z nawiasami („TVN (PA)” ≠ „TVN”, „PL: TVN HD” = „PL: TVN FHD”). */
    fun sameChannelKey(name: String): String = channelKey(nameOf(name), keepBrackets = true)

    /** Tekst tytułu do szukania (z indeksu albo policzony i zapamiętany). */
    fun titleOf(raw: String): String = titleNorm[raw] ?: norm(raw.trim()).also { titleNorm.put(raw, it) }

    /** Znormalizowane tytuły zawierające [q] (bez normalizacji przy zapytaniu) z kluczami kanałów. */
    fun titles(q: String): List<Pair<String, Set<String>>> {
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<Pair<String, Set<String>>>()
        for ((t, keys) in keysByTitle) if (t.contains(q)) out += t to keys
        return out
    }

    /** Czy indeks programów ma już dane (przed pierwszym przebiegiem szukanie idzie starą drogą). */
    fun ready(): Boolean = version > 0 && keysByTitle.isNotEmpty()

    /** 23.11 (TvRam): po wyjściu z telewizji spis tytułów i poprzedni odczyt przewodnika wychodzą z RAM (spis jest na dysku). */
    fun releaseMemory() = synchronized(this) {
        keysByTitle.clear(); lastSchedule = emptyMap(); restored = false; version++
    }

    /** Testy: „restart aplikacji” — pamięć w RAM znika, trwała pamięć (TvIndexStore) zostaje. */
    internal fun forgetMemoryForTests() = synchronized(this) {
        keysByTitle.clear(); lastSchedule = emptyMap(); version = 0; restored = false
    }

    internal fun clearForTests() = synchronized(this) {
        names.map.clear(); keys.map.clear(); titleNorm.map.clear(); TvIndexStore.titleKeys.map.clear(); TvIndexStore.fingerprints.map.clear()
        keysByTitle.clear(); lastSchedule = emptyMap(); version = 0; restored = false
    }
}
