package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import com.nuvio.tv.fork.tv.AlertKind
import com.nuvio.tv.fork.tv.Badge
import com.nuvio.tv.fork.tv.ForYouQueue
import com.nuvio.tv.fork.tv.QueueInput
import com.nuvio.tv.fork.tv.QueueItem
import com.nuvio.tv.fork.tv.ShowIdentity
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories
import com.nuvio.tv.skin.kicia.livetv.ForgeProgrammeSort
import com.nuvio.tv.skin.kicia.livetv.buildEntries

private const val TAG = "ForkNiunia"
internal const val MIN_MS = 60_000L
internal const val HOUR_MS = 3_600_000L
internal const val DAY_MS = 86_400_000L

/** Znacznik kafelka Live TV Niuni (jednym słowem albo kropką — decyzje właściciela 20.11). */
internal enum class NiuniaTileMark { LIVE, ARCHIVE, NEW, RESUME, SOON, NONE }

/**
 * Jeden kafelek Live TV Niuni (rząd u góry przewodnika, Odkrywaj, wyniki Szukaj).
 * [programme] = null → kafelek kanału (Ostatnie kanały). [progress] = pasek pod podpisem (0..1) albo null.
 */
internal data class NiuniaLiveTile(
    val channel: LiveTvChannel,
    val programme: LiveTvProgramme?,
    val mark: NiuniaTileMark,
    val progress: Float? = null,
    val series: Boolean = false,
    /** Dopisek po tytule („Dziś 21:30”, „E5”); pusty = sam tytuł. */
    val extra: String? = null,
    /** Pokazać nazwę kanału w podpisie („program · kanał” — przewodnik; Odkrywaj = sam tytuł). */
    val withChannel: Boolean = true,
    /** 22.11: ile godzin program jest jeszcze w archiwum, gdy mniej niż [EXPIRY_SHOW_H] (żółta etykieta). */
    val expiresHours: Int? = null,
) {
    val key: String get() = channel.streamUrl + "|" + (programme?.startEpochMs ?: 0L)
}

/** Filtr linii pod rzędem kafelków: Wszystkie, Ulubione, Oglądaj dalej albo kategoria EPG (klucz drzewa kanałów). */
internal sealed class NiuniaLiveFilter(val id: String) {
    object All : NiuniaLiveFilter("all")
    object Favourites : NiuniaLiveFilter("fav")
    object Continue : NiuniaLiveFilter("continue")
    class Category(val key: String, val name: String) : NiuniaLiveFilter(key)
}

/** Serial (do stosu pod kafelkiem): znacznik odcinka w tytule/opisie albo pole odcinka z przewodnika. */
internal fun isSeries(p: LiveTvProgramme?): Boolean {
    if (p == null) return false
    if (!p.episode.isNullOrBlank()) return true
    val id = ShowIdentity.episodeId(p.title, p.description)
    return id.startsWith("s") || id.startsWith("e")
}

internal fun isLive(p: LiveTvProgramme, now: Long) = now >= p.startEpochMs && now < p.stopEpochMs

internal fun liveFraction(p: LiveTvProgramme, now: Long): Float =
    ((now - p.startEpochMs).toFloat() / (p.stopEpochMs - p.startEpochMs).coerceAtLeast(1L)).coerceIn(0f, 1f)

/** Kafelek programu wg czasu: leci → kropka czerwona z paskiem, minął i jest w archiwum → zielona, przyszły → godzina. */
internal fun tileFor(ch: LiveTvChannel, p: LiveTvProgramme, now: Long, withChannel: Boolean = true, soonMark: Boolean = false): NiuniaLiveTile = when {
    isLive(p, now) -> NiuniaLiveTile(ch, p, NiuniaTileMark.LIVE, liveFraction(p, now), isSeries(p), withChannel = withChannel)
    p.stopEpochMs <= now -> {
        val resume = TvStore.resumeFor(ch.streamUrl, p.startEpochMs)?.takeIf { it.started }
        val playable = LiveTvCatchupLinks.isPlayable(ch.catchup, p, now)
        NiuniaLiveTile(ch, p, if (playable) NiuniaTileMark.ARCHIVE else NiuniaTileMark.NONE,
            resume?.fraction, isSeries(p), withChannel = withChannel,
            expiresHours = if (playable) com.nuvio.tv.fork.tv.CatchupDepth.hoursLeft(ch, p, now)?.takeIf { it < EXPIRY_SHOW_H } else null)
    }
    else -> NiuniaLiveTile(ch, p, if (soonMark) NiuniaTileMark.SOON else NiuniaTileMark.NONE, null, isSeries(p), extra = whenLabel(p.startEpochMs, now), withChannel = withChannel)
}

/** 22.11: żółta etykieta „Dostępny jeszcze …” dla programów, które znikną z archiwum w ciągu doby. */
internal const val EXPIRY_SHOW_H = 25

/** „Dziś 21:30”, „Jutro 20:15”, „Pt 20:15”. */
internal fun whenLabel(ms: Long, now: Long): String {
    val zone = java.util.TimeZone.getDefault()
    fun day(t: Long) = Math.floorDiv(t + zone.getOffset(t), DAY_MS)
    val d = when (day(ms) - day(now)) {
        0L -> "Dziś"
        1L -> "Jutro"
        -1L -> "Wczoraj"
        else -> listOf("Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "Sob")[java.util.Calendar.getInstance().apply { timeInMillis = ms }.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    }
    return "$d ${hhmm(ms)}"
}

internal fun hhmm(ms: Long): String = java.util.Calendar.getInstance().run { timeInMillis = ms; "%02d:%02d".format(get(java.util.Calendar.HOUR_OF_DAY), get(java.util.Calendar.MINUTE)) }

/** Kanały po kluczu przewodnika (pierwszy kanał danego przewodnika). */
internal fun byGuide(channels: List<LiveTvChannel>): Map<String, LiveTvChannel> = NiuniaListMaps.byGuide(channels)


/**
 * Mapy kanałów dla ostatniej listy (ten sam obiekt). Build171 na JMGO (252 827 kanałów): kafelki liczyły obie mapy od nowa
 * na wątku UI przy każdym odświeżeniu przewodnika (przycięcia ~1,4 s). [NiuniaLiveOrder.prepare] liczy je z góry w tle.
 */
internal object NiuniaListMaps {
    private class Memo(val list: List<LiveTvChannel>, val map: Map<String, LiveTvChannel>)
    @Volatile private var guide: Memo? = null
    @Volatile private var url: Memo? = null

    fun byGuide(list: List<LiveTvChannel>): Map<String, LiveTvChannel> = guide?.takeIf { it.list === list }?.map
        ?: HashMap<String, LiveTvChannel>(list.size * 2).also { m -> list.forEach { m.putIfAbsent(it.guideKey, it) } }.also { guide = Memo(list, it) }

    /** Kanał po adresie (jak `associateBy { streamUrl }`: przy powtórce wygrywa ostatni). */
    fun byUrl(list: List<LiveTvChannel>): Map<String, LiveTvChannel> = url?.takeIf { it.list === list }?.map
        ?: list.associateBy { it.streamUrl }.also { url = Memo(list, it) }
}

/** Najlepsza emisja tytułu na kanale: trwająca, potem ostatnia z archiwum, potem najbliższa. */
internal fun bestAiring(ch: LiveTvChannel, titleKey: String, now: Long): LiveTvProgramme? {
    val all = LiveTvRepository.schedule(ch.guideKey).filter { ShowIdentity.normalizeTitle(it.title) == titleKey }
    return all.firstOrNull { isLive(it, now) }
        ?: all.filter { it.stopEpochMs <= now && LiveTvCatchupLinks.isPlayable(ch.catchup, it, now) }.maxByOrNull { it.startEpochMs }
        ?: all.filter { it.startEpochMs > now }.minByOrNull { it.startEpochMs }
}

/** „Wszystkie”: ostatnio oglądane z postępem (kropka czerwona, gdy dalej leci; zielona, gdy już w archiwum). */
internal fun recentTiles(channels: List<LiveTvChannel>, current: Map<String, LiveTvProgramme>, now: Long): List<NiuniaLiveTile> {
    val byUrl = NiuniaListMaps.byUrl(channels)
    val guides = byGuide(channels)
    val out = ArrayList<NiuniaLiveTile>()
    TvStore.recent.value.sortedByDescending { it.lastViewedMs }.forEach { s ->
        val ch = byUrl[s.channelUrl] ?: guides[s.guideKey] ?: return@forEach
        val onNow = current[ch.guideKey]?.takeIf { isLive(it, now) && ShowIdentity.normalizeTitle(it.title) == s.key }
        val tile = if (onNow != null) tileFor(ch, onNow, now) else {
            val resume = TvStore.resume.value.filter { ShowIdentity.normalizeTitle(it.title) == s.key && it.channelUrl == ch.streamUrl }.maxByOrNull { it.updatedMs }
            val p = resume?.let { r -> LiveTvRepository.schedule(ch.guideKey).firstOrNull { it.startEpochMs == r.startMs } } ?: bestAiring(ch, s.key, now)
            p?.let { tileFor(ch, it, now) }
        }
        if (tile != null) out += tile
    }
    // Bez historii: kanały oglądane ostatnio (to, co na nich leci).
    if (out.isEmpty()) TvStore.recentChannels.value.forEach { r ->
        val ch = byUrl[r.streamUrl] ?: return@forEach
        current[ch.guideKey]?.takeIf { isLive(it, now) }?.let { out += tileFor(ch, it, now) }
    }
    return out.distinctBy { it.key }.take(20)
}

/** „Ulubione”: programy obserwowane (+), nowy odcinek → znacznik „Nowy odcinek”; potem ulubione kanały (co leci). */
internal fun favouriteTiles(channels: List<LiveTvChannel>, favUrls: Set<String>, current: Map<String, LiveTvProgramme>, now: Long): List<NiuniaLiveTile> {
    val guides = byGuide(channels)
    val favShows = TvStore.favoriteShows.value
    val out = ArrayList<NiuniaLiveTile>()
    TvStore.alerts.value.filter { it.kind == AlertKind.NEW_EPISODE && it.showKey in favShows }.sortedBy { it.airing.startMs }.forEach { a ->
        val ch = guides[a.airing.guideKey] ?: return@forEach
        val p = LiveTvRepository.schedule(ch.guideKey).firstOrNull { it.startEpochMs == a.airing.startMs } ?: return@forEach
        out += tileFor(ch, p, now).copy(mark = NiuniaTileMark.NEW, progress = null, extra = p.episode)
    }
    if (favShows.isNotEmpty()) channels.distinctBy { it.guideKey }.forEach { ch ->
        val p = current[ch.guideKey]?.takeIf { isLive(it, now) } ?: return@forEach
        if (ShowIdentity.normalizeTitle(p.title) in favShows) out += tileFor(ch, p, now)
    }
    channels.filter { it.streamUrl in favUrls }.forEach { ch -> current[ch.guideKey]?.takeIf { isLive(it, now) }?.let { out += tileFor(ch, it, now) } }
    return out.distinctBy { it.key }.take(20)
}

/** Kanały przewodnika dla „Ulubione”: ulubione kanały i kanały, na których są obserwowane programy. */
internal fun favouriteChannels(channels: List<LiveTvChannel>, favUrls: Set<String>, tiles: List<NiuniaLiveTile>): List<LiveTvChannel> {
    val withShows = tiles.mapTo(HashSet()) { it.channel.streamUrl }
    return channels.filter { it.streamUrl in favUrls || it.streamUrl in withShows }
}

/** „Oglądaj dalej”: niedokończone programy z archiwum. */
internal fun continueTiles(channels: List<LiveTvChannel>, now: Long): List<NiuniaLiveTile> {
    val byUrl = NiuniaListMaps.byUrl(channels)
    return TvStore.resume.value.filter { it.started }.sortedByDescending { it.updatedMs }.mapNotNull { r ->
        val ch = byUrl[r.channelUrl] ?: return@mapNotNull null
        val p = LiveTvRepository.schedule(ch.guideKey).firstOrNull { it.startEpochMs == r.startMs }
            ?: LiveTvProgramme(title = r.title, startEpochMs = r.startMs, stopEpochMs = r.stopMs, image = r.image, episode = r.episode)
        NiuniaLiveTile(ch, p, NiuniaTileMark.RESUME, r.fraction, isSeries(p))
    }.take(20)
}

/** Kategoria: programy tej kategorii teraz, potem wkrótce (po jednym na kanał). */
internal fun categoryTiles(channels: List<LiveTvChannel>, current: Map<String, LiveTvProgramme>, now: Long): List<NiuniaLiveTile> {
    val live = channels.distinctBy { it.guideKey }.mapNotNull { ch -> current[ch.guideKey]?.takeIf { isLive(it, now) }?.let { tileFor(ch, it, now) } }
        .sortedByDescending { (if (it.programme?.image.isNullOrBlank()) 0 else 1) }
    val soon = channels.distinctBy { it.guideKey }.mapNotNull { ch ->
        LiveTvRepository.schedule(ch.guideKey).firstOrNull { it.startEpochMs > now && it.startEpochMs < now + 3 * HOUR_MS }?.let { tileFor(ch, it, now) }
    }.sortedBy { it.programme?.startEpochMs }
    return (live + soon).take(20)
}

/**
 * Linia filtrów: kategorie wyłącznie z EPG (drzewo kanałów [ForgeEpgCategories.channelTree], puste ukryte), kolejność
 * wg częstości wyboru (licznik w ustawieniach Niuni), bez historii — kolejność właściciela.
 */
internal object NiuniaLiveFilters {
    private const val PREFS = "niunia_livetv"
    private val DEFAULT = listOf("informacje", "filmy", "sport", "dokumentalne", "dzieci", "muzyka", "styl", "regionalne", "zagraniczne", "ogolne", "inne", "dorosli")

    fun categories(context: Context, channels: List<LiveTvChannel>, hiddenGroups: Set<String>): List<NiuniaLiveFilter.Category> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val nodes = ForgeEpgCategories.channelTree(channels, hiddenGroups).filter { it.count > 0 }
        return nodes.sortedWith(compareByDescending<ForgeEpgCategories.Node> { prefs.getInt("use_" + it.key, 0) }
            .thenBy { DEFAULT.indexOf(it.key.removePrefix(ForgeEpgCategories.PREFIX)).let { i -> if (i < 0) 99 else i } })
            .map { NiuniaLiveFilter.Category(it.key, it.name) }
    }

    fun used(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val n = prefs.getInt("use_$key", 0) + 1
        prefs.edit().putInt("use_$key", n).apply()
        Log.i(TAG, "live tv: kategoria ${ForgeEpgCategories.nameOf(key)} wybrana $n×")
    }

    fun searchGrid(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("search_grid", true)
    fun setSearchGrid(context: Context, grid: Boolean) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("search_grid", grid).apply()
}

/** Rząd „Odkrywaj”: tytuł i kafelki (kanały — [NiuniaLiveTile.programme] = null). */
internal class NiuniaDiscoverRow(val title: String, val tiles: List<NiuniaLiveTile>)

/**
 * „Odkrywaj”: wszystkie zbiory programów, które aplikacja już liczy (bez nowych źródeł danych). Liczone poza wątkiem UI —
 * sortowania przewodnika przechodzą po całej ramówce.
 */
internal fun discoverRows(channels: List<LiveTvChannel>, favUrls: Set<String>, current: Map<String, LiveTvProgramme>, now: Long, titles: Map<String, String>): List<NiuniaDiscoverRow> {
    val guides = byGuide(channels)
    val byUrl = NiuniaListMaps.byUrl(channels)
    fun t(id: String) = titles[id] ?: id
    fun plain(list: List<NiuniaLiveTile>) = list.map { it.copy(withChannel = false) }
    val rows = ArrayList<NiuniaDiscoverRow>()

    rows += NiuniaDiscoverRow(t("fav"), plain(favouriteTiles(channels, favUrls, current, now)).map { if (it.mark == NiuniaTileMark.NEW) it.copy(extra = null) else it })

    // Dla ciebie: kolejka ForYouQueue (w trakcie, nowe odcinki obserwowanych, na żywo na ulubionych, na żywo z oglądanych kanałów).
    val favShows = TvStore.favoriteShows.value
    fun item(ch: LiveTvChannel, p: LiveTvProgramme, b: Badge) = QueueItem(ShowIdentity.normalizeTitle(p.title), p.title, ch.guideKey, p.startEpochMs, b)
    val inProgress = TvStore.resume.value.filter { it.started }.sortedByDescending { it.updatedMs }.mapNotNull { r ->
        byUrl[r.channelUrl]?.let { QueueItem(ShowIdentity.normalizeTitle(r.title), r.title, it.guideKey, r.startMs, Badge.RESUME) } }
    val newEp = TvStore.alerts.value.filter { it.kind == AlertKind.NEW_EPISODE }.map { QueueItem(it.showKey, it.showTitle, it.airing.guideKey, it.airing.startMs, Badge.NEW_EPISODE) }
    val liveFav = channels.mapNotNull { ch -> current[ch.guideKey]?.takeIf { isLive(it, now) && (ch.streamUrl in favUrls || ShowIdentity.normalizeTitle(it.title) in favShows) }?.let { item(ch, it, Badge.LIVE) } }
    val liveUsual = TvStore.recentChannels.value.mapNotNull { r -> byUrl[r.streamUrl]?.let { ch -> current[ch.guideKey]?.takeIf { isLive(it, now) }?.let { item(ch, it, Badge.LIVE) } } }
    val anyLive = channels.take(40).mapNotNull { ch -> current[ch.guideKey]?.takeIf { isLive(it, now) }?.let { item(ch, it, Badge.LIVE) } }
    val forYou = ForYouQueue.build(QueueInput(inProgress, newEp, liveFav, liveUsual, anyLive)).mapNotNull { q ->
        val ch = guides[q.guideKey] ?: return@mapNotNull null
        val p = LiveTvRepository.schedule(ch.guideKey).firstOrNull { it.startEpochMs == q.startMs } ?: return@mapNotNull null
        val base = tileFor(ch, p, now, withChannel = false)
        when (q.badge) {
            Badge.RESUME -> base.copy(mark = NiuniaTileMark.RESUME)
            Badge.NEW_EPISODE -> base.copy(mark = NiuniaTileMark.NEW, extra = null)
            Badge.LIVE -> base
        }
    }
    rows += NiuniaDiscoverRow(t("foryou"), forYou)

    val onAir = channels.distinctBy { it.guideKey }.mapNotNull { ch -> current[ch.guideKey]?.takeIf { isLive(it, now) && it.title.isNotBlank() }?.let { tileFor(ch, it, now, withChannel = false) } }
        .sortedByDescending { (if (it.programme?.image.isNullOrBlank()) 0 else 2) + (if (it.programme?.description.isNullOrBlank()) 0 else 1) }
    rows += NiuniaDiscoverRow(t("live"), onAir.take(20))

    rows += NiuniaDiscoverRow(t("new"), TvStore.alerts.value.sortedWith(compareBy({ it.kind != AlertKind.NEW_EPISODE }, { it.airing.startMs })).mapNotNull { a ->
        val ch = guides[a.airing.guideKey] ?: return@mapNotNull null
        val p = LiveTvRepository.schedule(ch.guideKey).firstOrNull { it.startEpochMs == a.airing.startMs } ?: return@mapNotNull null
        val base = tileFor(ch, p, now, withChannel = false)
        when {
            p.startEpochMs > now -> base.copy(mark = if (a.kind == AlertKind.NEW_EPISODE) NiuniaTileMark.NEW else NiuniaTileMark.SOON)
            else -> base
        }
    }.take(20))

    rows += NiuniaDiscoverRow(t("recent"), plain(recentTiles(channels, current, now)))
    rows += NiuniaDiscoverRow(t("channels"), TvStore.recentChannels.value.mapNotNull { r -> byUrl[r.streamUrl]?.let { NiuniaLiveTile(it, null, NiuniaTileMark.LIVE, withChannel = false) } })

    // 3 najliczniejsze kategorie EPG teraz.
    val branchCount = HashMap<String, MutableList<NiuniaLiveTile>>()
    onAir.forEach { tile -> tile.programme?.let { ForgeEpgCategories.programmeBranchName(tile.channel.guideKey, it) }?.let { branchCount.getOrPut(it) { ArrayList() } += tile } }
    branchCount.entries.sortedByDescending { it.value.size }.take(3).forEach { (name, tiles) -> rows += NiuniaDiscoverRow(name, tiles) }

    fun sorted(sort: ForgeProgrammeSort, mark: Boolean = false) = buildEntries(channels, ForgeEpgCategories.PROG_ALL, now, sort).shown.take(20)
        .map { tileFor(it.channel, it.programme, now, withChannel = false, soonMark = mark) }
    rows += NiuniaDiscoverRow(t("recommended"), sorted(ForgeProgrammeSort.RECOMMENDED))
    rows += NiuniaDiscoverRow(t("popular"), sorted(ForgeProgrammeSort.POPULAR))
    rows += NiuniaDiscoverRow(t("soon"), sorted(ForgeProgrammeSort.SOON, mark = true))
    rows += NiuniaDiscoverRow(t("archive"), sorted(ForgeProgrammeSort.NEWEST).filter { it.mark == NiuniaTileMark.ARCHIVE })
    return rows.filter { it.tiles.isNotEmpty() }
}

/**
 * Kolejność kanałów przewodnika (polskie przed zagranicznymi), kategorie z EPG i numery kanałów.
 *
 * JMGO 22.11 (ANR „Input dispatching timed out”): przy 59 349 widocznych kanałach (trzy listy, razem 252 827 pozycji)
 * liczone na wątku UI trwały ponad 5 s — dla każdej nazwy kilkanaście wyrażeń regularnych. Teraz mała lista
 * (do [SYNC_LIMIT] kanałów) liczy się od razu jak dawniej, duża w tle; do tego czasu przewodnik ma kolejność listy
 * i nie ma kategorii. Ostatni wynik zostaje w pamięci: powrót z odtwarzacza (ta sama lista) nie czeka, więc fokus
 * wraca na kanał jak dawniej.
 */
internal object NiuniaLiveOrder {
    const val SYNC_LIMIT = 3_000

    class Prepared(
        val input: List<LiveTvChannel>,
        val hidden: Set<String>,
        val shown: List<LiveTvChannel>,
        val categories: List<NiuniaLiveFilter.Category>,
        val numbers: Map<String, Int>,
        val sorted: Boolean,
    ) {
        fun isFor(list: List<LiveTvChannel>, hiddenGroups: Set<String>): Boolean = input === list && hidden == hiddenGroups
    }

    @Volatile private var last: Prepared? = null

    /** Gotowy wynik dla dokładnie tej listy (ten sam obiekt z repozytorium) i tych ukrytych grup. */
    fun cached(input: List<LiveTvChannel>, hidden: Set<String>): Prepared? = last?.takeIf { it.isFor(input, hidden) }

    /** Pełne liczenie (w tle dla dużej listy); wynik zapamiętany. */
    fun prepare(input: List<LiveTvChannel>, hidden: Set<String>, categories: (List<LiveTvChannel>) -> List<NiuniaLiveFilter.Category>): Prepared {
        val shown = ForgeEpgCategories.polishFirst(input)
        // Mapy kafelków z góry (w tle), żeby wątek UI ich nie liczył.
        NiuniaListMaps.byGuide(shown); NiuniaListMaps.byUrl(shown)
        return Prepared(input, hidden, shown, categories(shown), numbersOf(shown), sorted = true).also { last = it }
    }

    /** Zanim duża lista się policzy: kolejność listy, bez kategorii i — dla dużej listy — bez numerów (niezapamiętane). */
    fun unsorted(input: List<LiveTvChannel>, hidden: Set<String>): Prepared =
        Prepared(input, hidden, input, emptyList(), if (input.size <= SYNC_LIMIT) numbersOf(input) else emptyMap(), sorted = false)

    private fun numbersOf(list: List<LiveTvChannel>): Map<String, Int> =
        HashMap<String, Int>(list.size * 2).also { m -> list.forEachIndexed { i, ch -> m.putIfAbsent(ch.streamUrl, i + 1) } }
}
