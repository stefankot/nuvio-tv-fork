package com.nuvio.tv.fork.tv.plus

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvGuideRequest
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.liveTvGuideId
import com.nuvio.tv.reshaped.livetv.liveTvGuideMatchingKey
import com.nuvio.tv.reshaped.livetv.liveTvNameKey
import com.nuvio.tv.reshaped.livetv.liveTvNameTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Przewodnik telewizji w bazie SQLite (silnik NuvioPlusIPTV: [EpgDatabase] + [XmltvParser], kopie 1:1) zamiast całego
 * XMLTV w pamięci (właściciel 23.11: „Skopiuj zatem optymalizacje, ale nie usuń funkcji związanych z catch-up”).
 *
 * JMGO 23.11 (Reshaped): 5 przewodników czytanych do jednej mapy w RAM — program „teraz” po ~65 s, całość po ~3 min,
 * sterta do 470 MB, a zapis na dysk dopiero po pełnym odczycie. Tutaj:
 * 1. Wejście: ulubione i kanały z próbki startowej (≤ [PRIORITY_MAX]) z bazy od −1 h do +6 h, od razu (z opisami).
 * 2. Pobranie przewodników jak w Reshaped (co [downloadEveryMs] albo „Odśwież”); plik, który się nie zmienił
 *    (rozmiar+czas), NIE jest czytany ponownie. Zmieniony — strumieniowo do bazy, kanał po kanale (mało pamięci).
 * 3. Kanały listy → kanały przewodnika tymi samymi regułami co Reshaped ([LiveTvGuideRequest]: id, potem nazwa,
 *    nazwa z tym samym krajem wygrywa, ranking przewodników [rank]). Duplikaty dzielą jedną kopię programu.
 * 4. Okno dla wszystkich kanałów: najpierw −3 h…+6 h, potem −[PAST_MS]…+[AHEAD_MS] („light”: bez opisów).
 * Catch-up: minione dni kanału z archiwum ([history], wołane przez `LiveTvRepository.loadHistory` zamiast pliku
 * `LiveTvArchive`), linki archiwum i `pastProgrammes` Xtream bez zmian. Wyłączone ([KEY]) — dokładnie jak Reshaped.
 */
object GuideDbBridge {
    const val KEY = "tv_guide_db"
    private const val TAG = "ForkTvPlus"
    private const val HOUR = 60L * 60 * 1000
    private const val PAST_MS = 12 * HOUR
    private const val AHEAD_MS = 36 * HOUR
    /** Okno zapisu do bazy: minione dni dla catch-up (jak `LiveTvGuideWindow.Regular`), 8 dni naprzód. */
    private const val STORE_PAST_MS = 7 * 24 * HOUR
    private const val STORE_AHEAD_MS = 8 * 24 * HOUR
    private const val PRIORITY_MAX = 600
    private const val NAME_MATCH_RANK = 1_000_000 // jak LiveTvRepository.NAME_MATCH_RANK
    /** Zmiana sposobu zapisu/linków = wszystko od nowa. */
    private const val FORMAT = "v1"

    @Volatile private var database: EpgDatabase? = null
    @Volatile private var enabledCache: Boolean? = null
    @Volatile private var appContext: Context? = null

    // ------------------------------------------------------------------ leniwe wiersze (etap 1b)
    //
    // Właściciel 23.11 ~19:30 (iptvdb2: „teraz” 11 s, pełne okno 72 s): „czy ekran nie może czytać najpierw małej części, a potem
    // dociągać resztę?”. W pamięci Reshaped (kept) jest tylko bieżący i następny program wszystkich kanałów ([NOW_AHEAD_MS]);
    // pełne okno −[PAST_MS]…+[AHEAD_MS] mają tylko kanały, o które pyta ekran ([want]) — paczkami z bazy, w pamięci [ROWS_MAX].
    private const val NOW_AHEAD_MS = 2 * HOUR
    private const val ROWS_MAX = 800
    private const val ROWS_BATCH = 200
    private val rows = object : LinkedHashMap<String, List<LiveTvProgramme>>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<LiveTvProgramme>>?): Boolean = size > ROWS_MAX
    }
    private val wanted = LinkedHashSet<String>()
    @Volatile private var rowsJob: kotlinx.coroutines.Job? = null
    private val io = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    /** Jak `BackgroundWork` NuvioPlusIPTV: długie przeliczenia na jednym wątku o niskim priorytecie, żeby nie zabierały ekranu. */
    private val background = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() +
        java.util.concurrent.Executors.newSingleThreadExecutor { r ->
            Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); r.run() }, "ForkTvPlusBg")
        }.asCoroutineDispatcher())
    /** Rośnie po każdej paczce wierszy z bazy: ekrany Niuni przeliczają wiersze (Reshaped ma swoje `guideVersion`). */
    val version = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** [publish] z [load] — tylko „teraz” i priorytet: most TV (indeks szukania, odcinki) czeka na pełny przegląd w tle ([fullPass]). */
    @Volatile var partialPublish = false
        private set
    @Volatile private var passJob: kotlinx.coroutines.Job? = null
    /** Plik przewodnika → ten sam przewodnik w bazie ("" = żaden); klucz z rozmiarem i datą pliku. */
    private val twinMemo = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Pełny program kanału z pamięci wierszy (łatka `dostawca/LiveTvRepository/16` w `schedule()`): gdy go nie ma — [kept]
     * (bieżący i następny); ekran prosi o niego przez [want].
     */
    fun rowFor(guideKey: String, kept: List<LiveTvProgramme>): List<LiveTvProgramme> {
        if (appContext == null) return kept
        synchronized(rows) { return rows[guideKey] ?: kept }
    }

    /** Ekran pokazuje te kanały: ich pełne okno z bazy w tle (paczkami), potem [version] + 1. Tanie, gdy już są. */
    fun want(c: Context, keys: Collection<String>) {
        if (keys.isEmpty() || !enabled(c)) return
        if (appContext == null) appContext = c.applicationContext
        synchronized(rows) { keys.forEach { if (!rows.containsKey(it)) wanted += it } ; if (wanted.isEmpty()) return }
        if (rowsJob?.isActive == true) return
        rowsJob = io.launch {
            kotlinx.coroutines.delay(20)
            while (true) {
                val batch = synchronized(rows) { wanted.take(ROWS_BATCH).also { wanted.removeAll(it.toSet()) } }
                if (batch.isEmpty()) break
                val t0 = System.currentTimeMillis()
                val db = db(c)
                if (db.fingerprint() == null) { synchronized(rows) { wanted.clear() }; break }
                val now = LiveTvClock.nowEpochMs()
                val got = convert(safeRange(db, now - PAST_MS, now + AHEAD_MS, light = true, channels = batch))
                synchronized(rows) { batch.forEach { k -> rows[k] = got[k].orEmpty() } }
                version.value = version.value + 1
                Log.i(TAG, "wiersze z bazy: ${got.size} z ${batch.size} kanałów w ${System.currentTimeMillis() - t0} ms")
            }
        }
    }

    /** Nowy przewodnik w bazie: stare wiersze do wymiany. */
    private fun dropRows() { synchronized(rows) { rows.clear(); wanted.clear() }; version.value = version.value + 1 }

    fun enabled(c: Context): Boolean =
        enabledCache ?: com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true).also {
            enabledCache = it
            Log.i(TAG, "przewodnik w bazie (szybki start): ${if (it) "tak" else "nie"}")
        }

    /** Ustawienia: zmiana przełącznika działa od następnego wejścia do telewizji. */
    fun reset() { enabledCache = null }

    private fun db(c: Context): EpgDatabase = database ?: synchronized(this) {
        database ?: EpgDatabase(c.applicationContext).also { database = it }
    }

    /**
     * Odczyt przewodnika przez bazę (łatka `dostawca/LiveTvRepository/15-baza-przewodnika` w `startEpg`).
     * [publish] — jak `publishGuide` Reshaped (kept, logo kanałów bez logo, nieudane linki). Zwraca (kept, następny odczyt).
     */
    suspend fun load(
        c: Context,
        epgUrls: List<String>,
        guideFiles: List<File>,
        downloads: List<suspend (File) -> Unit>,
        channels: List<LiveTvChannel>,
        favoriteUrls: Set<String>,
        sourceLinks: Map<String, List<String>>,
        addedGuides: Map<String, String>,
        force: Boolean,
        downloadEveryMs: Long,
        publish: (Map<String, List<LiveTvProgramme>>, Map<String, String>?, Set<String>?) -> Unit,
    ): Pair<Map<String, List<LiveTvProgramme>>, Long> = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        appContext = c.applicationContext
        val db = db(c)
        val now = LiveTvClock.nowEpochMs()
        val publishPart = { kept: Map<String, List<LiveTvProgramme>>, l: Map<String, String>?, f: Set<String>? ->
            partialPublish = true
            try { publish(kept, l, f) } finally { partialPublish = false }
        }
        val logosFile = File(File(c.filesDir, "fork_tv"), "guide_db_logos.tsv")
        var logos: Map<String, String>? = readLogos(logosFile)

        // 1. Od razu: ulubione i kanały z próbki (te, które widać na ekranie po wejściu).
        val priority = priorityKeys(channels, favoriteUrls)
        if (db.fingerprint() != null && priority.isNotEmpty()) {
            val first = convert(safeRange(db, now - HOUR, now + 6 * HOUR, light = false, channels = priority))
            Log.i(TAG, "start: ${first.size} z ${priority.size} kanałów priorytetowych z bazy w ${System.currentTimeMillis() - t0} ms")
            if (first.isNotEmpty()) publishPart(first, logos, null)
            want(c, priority)
        }

        // 1b. Wszystkie kanały: tylko bieżący i następny program (iptvdb2: okno −3…+6 h = 8 s, −12…+36 h = 72 s).
        val hadGuide = db.fingerprint() != null
        var kept: Map<String, List<LiveTvProgramme>> = emptyMap()
        if (hadGuide) {
            kept = withPriority(convert(safeRange(db, now, now + NOW_AHEAD_MS, light = true, channels = null)), priority, db, now)
            Log.i(TAG, "start: „teraz i następny” dla ${kept.size} kanałów z bazy po ${System.currentTimeMillis() - t0} ms")
            if (kept.isNotEmpty()) publishPart(kept, logos, null)
        }

        // 2. Pobranie przewodników, które są stare (jak readGuide Reshaped; nieudane pobranie zostawia poprzedni plik).
        val failed = HashSet<String>()
        epgUrls.forEachIndexed { i, url ->
            val f = guideFiles[i]
            val age = now - f.lastModified()
            if (force || !f.isFile || f.length() == 0L || age !in 0 until downloadEveryMs) {
                val t = System.currentTimeMillis()
                try {
                    downloads[i](f)
                    Log.i(TAG, "przewodnik ${i + 1}/${epgUrls.size}: pobrany (${f.length() / 1024} KB) w ${System.currentTimeMillis() - t} ms")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "przewodnik ${i + 1}/${epgUrls.size}: pobieranie: ${e.javaClass.simpleName}: ${e.message?.substringBefore('?')}")
                    failed += url
                }
            }
        }

        // 3. Do bazy tylko pliki zmienione albo z nowymi kanałami listy.
        val request = LiveTvGuideRequest.from(channels)
        var changed = false
        // JMGO iptvdb1/2: przewodnik FlixNest pod dwoma adresami (lista Xtream i M3U) = ten sam program, importowany 2× po
        // 95 s. Bajty plików się różnią (data wygenerowania), więc „ten sam” = ta sama lista kanałów na początku pliku.
        val alias = HashMap<String, String>()
        val done = ArrayList<Pair<String, Long>>() // src, rozmiar pliku
        epgUrls.forEachIndexed { i, _ ->
            val f = guideFiles[i]
            if (!f.isFile || f.length() == 0L) return@forEachIndexed
            // JMGO iptvdb3/4: jeden plik spakowany, drugi nie — rozmiar nic nie mówi; lista kanałów z początku pliku raz,
            // porównana z każdym przewodnikiem już w bazie.
            // Wynik pamiętany dla pliku bez zmian (odczyt co 30 min nie czyta początku pliku za każdym razem).
            val memoKey = "${f.name}:${f.length()}:${f.lastModified()}:${done.joinToString(",") { it.first }}"
            val twin = if (done.isEmpty()) null else twinMemo.getOrPut(memoKey) {
                headChannels(f)?.let { ids -> done.firstOrNull { (src, _) -> sameChannels(db, src, ids, f.name) }?.first }.orEmpty()
            }.ifEmpty { null }
            if (twin != null) { alias[srcOf(f)] = twin; Log.i(TAG, "${srcOf(f)}: ten sam przewodnik co $twin — bez drugiego importu"); return@forEachIndexed }
            changed = importFile(db, f, srcOf(f), request, now) || changed
            done += srcOf(f) to f.length()
        }

        // 4. Kanały listy → kanały przewodnika (gdy zmienił się plik, lista kanałów albo ranking).
        val sources = guideFiles.filter { it.isFile && it.length() > 0L }.map(::srcOf).filter { it !in alias }
        val fp = fingerprint(db, sources, channels, sourceLinks, addedGuides)
        if (changed || db.fingerprint() != fp) {
            logos = link(db, epgUrls, guideFiles, request, channels, sourceLinks, addedGuides, fp, sources, alias)
            writeLogos(logosFile, logos)
        }

        // 5. Nowy przewodnik w bazie: „teraz i następny” od nowa, wiersze ekranu od nowa.
        val dbChanged = changed || !hadGuide
        if (dbChanged) {
            dropRows()
            kept = withPriority(convert(safeRange(db, now, now + NOW_AHEAD_MS, light = true, channels = null)), priority, db, now)
            want(c, priority)
        }
        publishPart(kept, logos, failed)
        val programmes = kept.values.sumOf { it.size }
        Log.i(TAG, "przewodnik z bazy: ${kept.size} kanałów, $programmes programów (teraz i następny) w ${System.currentTimeMillis() - t0} ms" +
            (if (failed.isNotEmpty()) ", nieudane przewodniki: ${failed.size}" else ""))
        // 6. Indeks szukania, odcinki obserwowanych, dziennik catch-up, kategorie — w tle, raz na zmianę bazy (i raz po starcie aplikacji).
        fullPass(c, channels, dbChanged, publish)
        // „Teraz i następny” wystarcza na ~2 h; odczyt z bazy co 30 min (pliki i tak pobierane po downloadEveryMs).
        kept to (now + 30 * 60_000L)
    }

    /**
     * Catch-up: minione dni kanału z bazy (zamiast pliku `LiveTvArchive`); null — baza wyłączona albo pusta
     * (wtedy Reshaped czyta swoje archiwum). Łatka `dostawca/LiveTvRepository/15` w `loadHistory`.
     */
    fun history(c: Context, guideKey: String, fromMs: Long): List<LiveTvProgramme>? {
        if (!enabled(c)) return null
        return try {
            val db = db(c)
            if (db.fingerprint() == null) return null
            val list = db.channelRange(guideKey, fromMs, LiveTvClock.nowEpochMs() + HOUR)
            Log.i(TAG, "archiwum kanału z bazy: ${list.size} programów")
            list.map { p -> LiveTvProgramme(p.title, p.startMs, p.stopMs, p.description, p.icon, p.episode) }
        } catch (e: Exception) {
            Log.w(TAG, "archiwum kanału z bazy: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    /**
     * Ekrany Niuni (wiersz przewodnika cofnięty w czasie, odtwarzacz z rzędem odcinków): kanał z catch-up dostaje minione dni
     * z bazy (`LiveTvRepository.requestHistory` → [history]). W pamięci jest tylko −[PAST_MS]…+[AHEAD_MS], więc bez tego
     * archiwum starsze niż 12 h nie byłoby widać (Reshaped z oknem 7 dni miało je od razu).
     */
    fun need(c: Context, channel: LiveTvChannel, fromMs: Long) {
        if (channel.catchup == null || !enabled(c)) return
        if (fromMs > LiveTvClock.nowEpochMs() - PAST_MS + HOUR) return
        com.nuvio.tv.reshaped.livetv.LiveTvRepository.requestHistory(channel)
    }

    /**
     * Pełne okno wszystkich kanałów dla rzeczy liczonych z całego przewodnika ([com.nuvio.tv.fork.tv.most.TvBridge.onGuideRead]:
     * indeks szukania programów, nowe odcinki obserwowanych, dziennik catch-up) i kategorie Kici — w tle, 60 s po wejściu,
     * tylko gdy baza się zmieniła od ostatniego przeglądu albo pierwszy raz po starcie aplikacji. Mapa puszczana po przeglądzie.
     */
    private var passRevision = 0L
    private fun fullPass(c: Context, channels: List<LiveTvChannel>, dbChanged: Boolean, publish: (Map<String, List<LiveTvProgramme>>, Map<String, String>?, Set<String>?) -> Unit) {
        val db = db(c)
        val revision = try { db.revision() } catch (e: Exception) { Log.w(TAG, "przegląd: rewizja: ${e.javaClass.simpleName}: ${e.message}"); return }
        if (!dbChanged && revision == passRevision) return
        if (passJob?.isActive == true) return
        passJob = background.launch {
            kotlinx.coroutines.delay(60_000)
            // Nie w czasie oglądania (jak BackgroundWork NuvioPlusIPTV): czeka, aż nic nie gra.
            while (com.nuvio.tv.fork.sources.PlayerBridge.current()?.isPlaying == true) kotlinx.coroutines.delay(30_000)
            val t0 = System.currentTimeMillis()
            val now = LiveTvClock.nowEpochMs()
            if (dbChanged || com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories.needsGuideRead()) recordCategories(db, now)
            val all = convert(safeRange(db, now - PAST_MS, now + AHEAD_MS, light = true, channels = null))
            if (all.isEmpty()) return@launch
            com.nuvio.tv.fork.tv.most.TvBridge.onGuideRead(channels, all, now)
            passRevision = revision
            Log.i(TAG, "przegląd w tle: ${all.size} kanałów, ${all.values.sumOf { it.size }} programów (indeks, odcinki, archiwum) w ${System.currentTimeMillis() - t0} ms")
        }
    }

    // ------------------------------------------------------------------ import

    private fun srcOf(f: File): String = f.name

    /** Czy przewodnik [f] trafił teraz do bazy. Plik bez zmian i bez nowych kanałów listy — pominięty. */
    private fun importFile(db: EpgDatabase, f: File, src: String, request: LiveTvGuideRequest, now: Long): Boolean {
        val stamp = "$FORMAT:${f.length()}:${f.lastModified()}"
        val state = try { db.sourceState(src) } catch (e: Exception) { Log.w(TAG, "stan $src: ${e.javaClass.simpleName}: ${e.message}"); null }
        if (state != null && state.stamp == stamp) {
            // Nowe kanały u dostawcy, które ten przewodnik zna, a których programów nie zapisano → czytamy go jeszcze raz.
            val listed = try { db.sourceChannels(src) } catch (e: Exception) { Log.w(TAG, "kanały $src: ${e.javaClass.simpleName}: ${e.message}"); emptyList() }
            val missing = listed.count { (id, names, _) -> id !in state.meta && wanted(id, names, request) }
            if (missing == 0) { Log.i(TAG, "$src: bez zmian (${state.meta.size} kanałów w bazie)"); return false }
            Log.i(TAG, "$src: bez zmian, ale $missing nowych kanałów listy — czytam ponownie")
        }
        // Próg pamięci (NuvioPlusIPTV `LiveTvRepository` ~2500): sterta > 85 % → GC; nadal > 85 % → ten przewodnik później.
        val rt = Runtime.getRuntime()
        fun used() = rt.totalMemory() - rt.freeMemory()
        if (used() > rt.maxMemory() * 0.85) System.gc()
        if (used() > rt.maxMemory() * 0.85) {
            Log.w(TAG, "$src: pamięć prawie pełna (${used() / 1048576} z ${rt.maxMemory() / 1048576} MB) — przewodnik przy następnym odczycie")
            return false
        }
        val t0 = System.currentTimeMillis()
        val writer = db.startSource(src)
        var stored = 0L
        try {
            var seen: Map<String, XmltvChannel> = emptyMap()
            val result = open(f).use { input ->
                XmltvParser.parse(
                    input, now - STORE_PAST_MS, now + STORE_AHEAD_MS,
                    keepUnlisted = { id -> wanted(id, seen[id]?.displayNames.orEmpty(), request) },
                    onPrograms = { id, list -> writer.add(id, list, ::isPlaceholder); stored += list.size },
                ) { channels -> seen = channels; null }
            }
            val listed = result.channels.values.map { Triple(it.id, it.displayNames, it.icon) }
            // JMGO iptvdb1: tylko kanały z programami → wymagane kanały bez programów liczone przy każdym wejściu jako
            // „nowe” i plik czytany od nowa. Wszystkie wymagane (z programami i bez) — jak `kept` w NuvioPlusIPTV.
            val keptIds = (result.channels.keys + result.programChannelIds).filter { wanted(it, result.channels[it]?.displayNames.orEmpty(), request) }
            val extra = result.programChannelIds.filter { it !in result.channels }
            writer.finish(stamp, listed, keptIds, listed = result.channels.size, extraIds = extra)
            Log.i(TAG, "$src: do bazy ${keptIds.size} kanałów, $stored programów w ${System.currentTimeMillis() - t0} ms" +
                (result.error?.let { " (plik urwany: ${it.take(80)})" } ?: ""))
            return true
        } catch (e: CancellationException) {
            writer.abort(); throw e
        } catch (e: Exception) {
            writer.abort()
            Log.w(TAG, "$src: zapis do bazy: ${e.javaClass.simpleName}: ${e.message}")
            return false
        } catch (e: OutOfMemoryError) {
            writer.abort()
            Log.w(TAG, "$src: za mało pamięci przy zapisie do bazy")
            return false
        }
    }

    /** Czy lista kanałów [ids] (≥ 50) to lista przewodnika [src] w bazie. */
    private fun sameChannels(db: EpgDatabase, src: String, ids: Set<String>, name: String): Boolean = try {
        val stored = db.sourceChannels(src).mapTo(HashSet()) { it.first }
        (stored.size >= 50 && ids == stored).also { Log.i(TAG, "$name vs $src: ${ids.size} / ${stored.size} kanałów — ${if (it) "ten sam przewodnik" else "inny"}") }
    } catch (e: Exception) {
        Log.w(TAG, "$name: porównanie kanałów: ${e.javaClass.simpleName}: ${e.message}"); false
    }

    /** Kanały `<channel>` z początku pliku (do pierwszego programu); null — za mało albo błąd. */
    private fun headChannels(f: File): Set<String>? = try {
        run {
            val ids = HashSet<String>()
            open(f).use { input ->
                val parser = android.util.Xml.newPullParser()
                parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                try { parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true) } catch (e: Exception) { Log.w(TAG, "${f.name}: tryb relaxed: ${e.javaClass.simpleName}") }
                parser.setInput(input, null)
                var event = parser.eventType
                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                        if (parser.name == "programme") break
                        if (parser.name == "channel") parser.getAttributeValue(null, "id")?.let { ids += it }
                    }
                    event = parser.next()
                }
            }
            ids.takeIf { it.size >= 50 }
        }
    } catch (e: Exception) {
        Log.w(TAG, "${f.name}: kanały z początku pliku: ${e.javaClass.simpleName}: ${e.message}"); null
    }

    /** Plik przewodnika Reshaped: gzip albo zwykły XML. */
    private fun open(f: File): InputStream {
        val raw = BufferedInputStream(f.inputStream(), 64 * 1024)
        raw.mark(2)
        val gz = raw.read() == 0x1f && raw.read() == 0x8b
        raw.reset()
        return if (gz) BufferedInputStream(GZIPInputStream(raw, 64 * 1024), 64 * 1024) else raw
    }

    private fun isPlaceholder(title: String): Boolean =
        title.equals("No information", true) || title.equals("Programming", true) || title.equals("Brak programu", true)

    /** Czy kanał przewodnika może zasilić jakiś kanał listy (id albo nazwa — jak `LiveTvEpg.channel/directKeys`). */
    private fun wanted(id: String, names: List<String>, request: LiveTvGuideRequest): Boolean {
        val gid = liveTvGuideId(id)
        if (request.keysById.containsKey(gid) || gid in request.keys) return true
        return names.any { request.keysByName.containsKey(liveTvNameKey(it)) }
    }

    // ------------------------------------------------------------------ linki

    private class Feed(val src: String, val xid: String, val byName: Boolean, val sameTag: Boolean, val rank: Int)

    /**
     * Kanał listy → kanał przewodnika jak w Reshaped: w jednym pliku id wygrywa z nazwą, nazwa z tym samym krajem
     * z inną nazwą; między plikami ranking `publish` z `startEpg` (GuidePriority, przewodnik dodany przez użytkownika,
     * kolejność przewodników źródła, potem inne; dopasowanie po nazwie dalej). Zwraca logo z przewodnika dla kanałów bez logo.
     */
    private fun link(
        db: EpgDatabase, epgUrls: List<String>, guideFiles: List<File>, request: LiveTvGuideRequest, channels: List<LiveTvChannel>,
        sourceLinks: Map<String, List<String>>, addedGuides: Map<String, String>, fp: String, sources: List<String>,
        alias: Map<String, String>,
    ): Map<String, String> {
        val t0 = System.currentTimeMillis()
        val sourceForKey = HashMap<String, String>(channels.size * 2)
        channels.forEach { sourceForKey.putIfAbsent(it.guideKey, it.sourceId) }
        val best = HashMap<String, Feed>(request.keys.size * 2)
        val icons = HashMap<String, String>()
        epgUrls.forEachIndexed { index, url ->
            val f = guideFiles[index]
            if (!f.isFile || f.length() == 0L) return@forEachIndexed
            val src = alias[srcOf(f)] ?: srcOf(f)
            val listed = try { db.sourceChannels(src) } catch (e: Exception) { Log.w(TAG, "linki $src: ${e.javaClass.simpleName}: ${e.message}"); return@forEachIndexed }
            fun rank(key: String, byName: Boolean): Int {
                com.nuvio.tv.fork.tv.guide.GuidePriority.repoRank(url, byName, NAME_MATCH_RANK)?.let { return it }
                val added = addedGuides[sourceForKey[key]]?.trim().orEmpty()
                if (added.isNotEmpty() && added == url) return -1
                val order = sourceLinks[sourceForKey[key]]?.indexOf(url)?.takeIf { it >= 0 } ?: (epgUrls.size + index)
                return if (byName) NAME_MATCH_RANK + order else order
            }
            // W pliku: najpierw wszystkie dopasowania po id, potem po nazwie.
            val inFile = HashMap<String, Feed>()
            for ((id, names, icon) in listed) {
                val gid = liveTvGuideId(id)
                val direct = request.keysById[gid] ?: if (gid in request.keys) listOf(gid) else null
                direct?.forEach { key ->
                    if (inFile[key]?.byName != false) inFile[key] = Feed(src, id, false, false, rank(key, false))
                    if (icon != null && key in request.keysWithoutLogo) icons.putIfAbsent(key, icon)
                }
            }
            for ((id, names, icon) in listed) {
                val gid = liveTvGuideId(id)
                if (request.keysById.containsKey(gid) || gid in request.keys) continue
                for (name in names) {
                    val keys = request.keysByName[liveTvNameKey(name)] ?: continue
                    val tag = liveTvNameTag(name)
                    keys.forEach { key ->
                        val sameTag = tag != null && tag == request.tagsByKey[key]
                        val cur = inFile[key]
                        if (cur == null || (cur.byName && sameTag && !cur.sameTag)) inFile[key] = Feed(src, id, true, sameTag, rank(key, true))
                        if (icon != null && key in request.keysWithoutLogo) icons.putIfAbsent(key, icon)
                    }
                    break
                }
            }
            inFile.forEach { (key, feed) -> if (feed.rank < (best[key]?.rank ?: Int.MAX_VALUE)) best[key] = feed }
        }
        val links = HashMap<String, EpgDatabase.GuideLink>(best.size * 2)
        best.forEach { (key, feed) -> links[key] = EpgDatabase.GuideLink(feed.src, feed.xid, request.shiftMsByKey[key] ?: 0L) }
        try {
            db.applyGuide(fp, links, emptyMap(), null, sources)
            Log.i(TAG, "linki: ${links.size} z ${request.keys.size} kanałów listy ma przewodnik (${links.values.count { it.shiftMs != 0L }} z przesunięciem) w ${System.currentTimeMillis() - t0} ms")
        } catch (e: Exception) {
            Log.w(TAG, "linki: ${e.javaClass.simpleName}: ${e.message}")
        }
        return icons
    }

    private fun fingerprint(db: EpgDatabase, sources: List<String>, channels: List<LiveTvChannel>, sourceLinks: Map<String, List<String>>, addedGuides: Map<String, String>): String {
        val stamps = sources.joinToString("|") { src -> src + "=" + (try { db.sourceState(src)?.stamp } catch (e: Exception) { Log.w(TAG, "odcisk $src: ${e.javaClass.simpleName}: ${e.message}"); null }).orEmpty() }
        val h = 31L * (31L * liveTvGuideMatchingKey(channels) + sourceLinks.hashCode()) + addedGuides.hashCode()
        return "$FORMAT|$stamps|$h"
    }

    private fun recordCategories(db: EpgDatabase, now: Long) {
        val t0 = System.currentTimeMillis()
        var n = 0
        try {
            db.scanListingsGrouped(now - PAST_MS, now + AHEAD_MS, now - PAST_MS) { chans, start, _, _, _, cat ->
                if (cat != null) { com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories.record(chans, start, cat); n++ }
            }
            com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories.markRead()
            Log.i(TAG, "kategorie programów z bazy: $n w ${System.currentTimeMillis() - t0} ms")
        } catch (e: Exception) {
            Log.w(TAG, "kategorie programów z bazy: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    // ------------------------------------------------------------------ odczyt

    private fun safeRange(db: EpgDatabase, from: Long, to: Long, light: Boolean, channels: Collection<String>?): Map<String, List<EpgProgram>> =
        try {
            db.range(from, to, light = light, channels = channels)
        } catch (e: Exception) {
            Log.w(TAG, "odczyt okna: ${e.javaClass.simpleName}: ${e.message}")
            emptyMap()
        }

    /** EpgProgram → LiveTvProgramme; te same tytuły i adresy obrazków jako jeden obiekt (setki tysięcy wierszy). */
    private fun convert(m: Map<String, List<EpgProgram>>): Map<String, List<LiveTvProgramme>> {
        val pool = HashMap<String, String>()
        fun shared(s: String?): String? = s?.let { pool.getOrPut(it) { it } }
        val sameList = java.util.IdentityHashMap<List<EpgProgram>, List<LiveTvProgramme>>()
        val out = HashMap<String, List<LiveTvProgramme>>(m.size * 2)
        m.forEach { (key, list) ->
            // Kanały z jednym kanałem przewodnika (duplikaty) dostają tę samą listę.
            out[key] = sameList.getOrPut(list) {
                list.map { p -> LiveTvProgramme(shared(p.title)!!, p.startMs, p.stopMs, p.description, shared(p.icon), shared(p.episode)) }
            }
        }
        return out
    }

    /** Ulubione i próbka: pełne programy (z opisami) z bliskiego okna zamiast „light”. */
    private fun withPriority(all: Map<String, List<LiveTvProgramme>>, priority: List<String>, db: EpgDatabase, now: Long): Map<String, List<LiveTvProgramme>> {
        if (priority.isEmpty()) return all
        val full = convert(safeRange(db, now - HOUR, now + 6 * HOUR, light = false, channels = priority))
        if (full.isEmpty()) return all
        val out = HashMap(all)
        full.forEach { (key, near) ->
            val wide = all[key].orEmpty()
            val first = near.firstOrNull()?.startEpochMs ?: return@forEach
            val last = near.last().stopEpochMs
            out[key] = wide.filter { it.stopEpochMs <= first } + near + wide.filter { it.startEpochMs >= last }
        }
        return out
    }

    private fun priorityKeys(channels: List<LiveTvChannel>, favoriteUrls: Set<String>): List<String> {
        val out = LinkedHashSet<String>()
        channels.forEach { if (it.streamUrl in favoriteUrls) out += it.guideKey }
        com.nuvio.tv.fork.tv.ram.TvSnapshot.data?.channels?.forEach { if (out.size < PRIORITY_MAX) out += it.guideKey }
        if (out.size < 60) channels.asSequence().take(200).forEach { if (out.size < PRIORITY_MAX) out += it.guideKey }
        return out.take(PRIORITY_MAX)
    }

    private fun readLogos(f: File): Map<String, String>? = try {
        if (!f.isFile) null else f.readLines().mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    } catch (e: Exception) {
        Log.w(TAG, "logo z przewodnika: ${e.javaClass.simpleName}: ${e.message}"); null
    }

    private fun writeLogos(f: File, logos: Map<String, String>) {
        try {
            f.parentFile?.mkdirs()
            val tmp = File(f.path + ".tmp")
            tmp.bufferedWriter().use { w -> logos.forEach { (k, v) -> if ('\t' !in k && '\n' !in k && '\t' !in v && '\n' !in v) { w.write(k); w.write("\t"); w.write(v); w.write("\n") } } }
            if (!tmp.renameTo(f)) Log.w(TAG, "logo z przewodnika: nie udało się zapisać ${f.name}")
        } catch (e: Exception) {
            Log.w(TAG, "logo z przewodnika: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
