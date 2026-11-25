package com.nuvio.tv.fork.tv.most

import android.content.Context
import com.nuvio.tv.fork.tv.EpgExtras
import com.nuvio.tv.fork.tv.GuideProgramme
import com.nuvio.tv.fork.tv.TvData
import com.nuvio.tv.fork.tv.WatchRules
import java.io.File
import java.util.concurrent.Executors
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Most do Reshaped: jedyne miejsce, przez które Live TV woła moduł TV. Wyłączony moduł (`module_tv`)
 * nic nie zapisuje. Wołane z łatek `Fork patch: dostawca` / `Fork hook: tv` w Reshaped.
 */
object TvBridge {
    @Volatile private var enabled = true
    @Volatile var data: TvData? = null
        private set
    @Volatile private var lastKept: Map<String, List<LiveTvProgramme>> = emptyMap()
    private val io = Executors.newSingleThreadExecutor { Thread(it, "fork-tv-io").apply { isDaemon = true } }

    private fun LiveTvProgramme.toGuide() = GuideProgramme(title, startEpochMs, stopEpochMs, description, episode, image)

    fun attach(context: Context, profileId: Int) {
        // 22.11: trwałe indeksy TV (teksty, kategorie, kraj) — wczytanie z dysku w tle, bez liczenia od nowa po starcie.
        com.nuvio.tv.fork.tv.index.TvIndexStore.attach(context)
        // 23.11 (TvRam): kategorie wracają z pliku po zwolnieniu telewizji z pamięci (po starcie: bez zmian, raz).
        com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories.ensure(context)
        enabled = ForkFeatures.enabled(context, "module_tv", true)
        if (!enabled) { data = null; return }
        ForkTvHooks.sink = { title, start, extra -> EpgExtras.put(title, start, extra) }
        TvStore.attach(context, profileId)
        val app = context.applicationContext
        data = TvData(
            File(app.filesDir, "fork_tv"), profileId, enabled = true,
            rules = { WatchRules(watchedFraction = (if (ForkFeatures.enabled(app, "tv_watched_strict", false)) 80 else 50) / 100.0) },
            autoFollow = { ForkFeatures.enabled(app, "tv_auto_follow", true) },
            save = { job -> io.execute(job) },
        )
    }

    /** 23.11 (TvRam): po wyjściu z telewizji — bez kopii całego przewodnika i mapy kanałów (wracają z następnym odczytem). */
    fun releaseMemory() {
        lastKept = emptyMap()
        TvStore.releaseMemory()
    }

    fun onGuideRead(channels: List<LiveTvChannel>, kept: Map<String, List<LiveTvProgramme>>, nowMs: Long) {
        // 23.11 (przewodnik w bazie, etap 1b): ekran dostaje tylko „teraz i następny” — indeks szukania i odcinki liczy pełny
        // przegląd w tle (GuideDbBridge.fullPass), inaczej każdy częściowy odczyt przebudowywałby indeks.
        if (com.nuvio.tv.fork.tv.plus.GuideDbBridge.partialPublish) return
        // 22.11: indeks szukania TV (w tle, przyrostowo) — także przy wyłączonym module TV, bo służy szukaniu w Live TV.
        com.nuvio.tv.fork.tv.index.TvTextIndex.submit(channels, kept)
        if (!enabled) return
        TvStore.updateSchedule(channels, kept, nowMs)
        lastKept = kept
        val days = channels.mapNotNull { c -> c.catchup?.days?.takeIf { it > 0 }?.let { c.guideKey to it } }.toMap()
        if (days.isEmpty()) return
        data?.onGuide(days.keys.associateWith { k -> kept[k].orEmpty().map { it.toGuide() } }, days, nowMs)
    }

    /**
     * Audyt UI p. 4: cały przewodnik (wszystkie kanały, także bez archiwum) dla Szukaj w telewizji, w kształcie wpisów
     * archiwum (ten sam klucz programu: ShowIdentity.normalizeTitle), żeby strona programu działała tak samo.
     */
    fun guideAirings(): List<com.nuvio.tv.fork.tv.LogEntry> = lastKept.flatMap { (guideKey, list) ->
        list.mapNotNull { p ->
            val key = programKey(p.title).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            com.nuvio.tv.fork.tv.LogEntry(key, guideKey, p.startEpochMs, p.stopEpochMs - p.startEpochMs, p.episode.orEmpty(), p.title.take(80))
        }
    }

    /**
     * Klucz programu raz na tytuł (JMGO build171: indeks Odkrywaj/Szukaj liczył `normalizeTitle` — kilka wyrażeń
     * regularnych — dla każdej z ~1 mln emisji przy każdej przebudowie; 62% CPU w czasie szukania). Pamięć tytułów
     * przeżywa przebudowy (liczone są tylko nowe tytuły); czyszczona powyżej [TITLE_MEMO_MAX] wpisów.
     */
    private val titleMemo = java.util.concurrent.ConcurrentHashMap<String, String>()
    private const val TITLE_MEMO_MAX = 400_000

    internal fun programKey(title: String): String = titleMemo[title] ?: com.nuvio.tv.fork.tv.ShowIdentity.normalizeTitle(title).also {
        if (titleMemo.size > TITLE_MEMO_MAX) titleMemo.clear()
        titleMemo[title] = it
    }

    /** Kiedy ostatnio włączono kanał IPTV (uptime); intro Echo pomija start kanału (właściciel 19.11). */
    @Volatile var lastChannelPlayAtMs = 0L

    fun onPlay(channel: LiveTvChannel, programme: LiveTvProgramme?) {
        lastChannelPlayAtMs = android.os.SystemClock.elapsedRealtime()
        if (!enabled) return
        // UX JMGO 25.11 (#59, am profile): TvStore.onWatched → EpisodeAlerts.detect trwało 1,8 s na wątku UI przy włączaniu
        // kanału (obraz czarny, klawisze stały). Teraz w wątku „fork-tv-io” (ta sama kolejka co zapis pozycji).
        val now = System.currentTimeMillis()
        io.execute {
            try {
                TvStore.onWatched(channel, programme, now)
                data?.onPlay(channel.guideKey, programme?.toGuide(), now)
            } catch (e: Exception) {
                android.util.Log.w("ForkTv", "zapis włączenia kanału: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    /** Co ~10 s z odtwarzacza: kanał (adres listy), czy z archiwum, i miejsce w czasie kanału. */
    fun onPosition(listUrl: String?, catchup: Boolean, playheadEpochMs: Long) {
        if (!enabled || listUrl == null) return
        // UX JMGO 25.11 (#59, próbkowanie am profile): zapis historii (TvData.afterChange: zapis list + klasyfikacja serii)
        // trwał 2,3 s na wątku UI odtwarzacza co minutę → obraz i klawisze stały. Teraz w wątku „fork-tv-io”.
        val channels = com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.value.channels
        val now = System.currentTimeMillis()
        io.execute {
            try {
                val guideKey = channels.firstOrNull { it.streamUrl == listUrl }?.guideKey ?: return@execute
                data?.onPosition(guideKey, playheadEpochMs, now) { key, ms ->
                    lastKept[key]?.firstOrNull { it.startEpochMs <= ms && ms < it.stopEpochMs }?.toGuide()
                }
            } catch (e: Exception) {
                android.util.Log.w("ForkTv", "pozycja w historii: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }
}
