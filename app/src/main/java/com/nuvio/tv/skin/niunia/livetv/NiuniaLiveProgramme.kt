package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.fork.tv.ShowIdentity
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.fork.tv.index.TvTextIndex
import com.nuvio.tv.fork.tv.names.ChannelNames
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.niuniaPanelTrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "ForkNiunia"

internal enum class NiuniaAiringKind { LIVE, ARCHIVE, SOON }

/** Emisja programu na kanale listy: na żywo, z archiwum albo jeszcze nie nadana. */
internal class NiuniaAiring(val channel: LiveTvChannel, val programme: LiveTvProgramme, val kind: NiuniaAiringKind)

/**
 * IPTV #82 (właściciel 26.11): wynik szukania prowadził do emisji, której jeszcze nie nadano („Ten program zacznie się…”),
 * a nie do strony programu, z której da się go obejrzeć. Emisje tytułu: pamięć przewodnika (−12 h…+36 h) i baza
 * ([com.nuvio.tv.fork.tv.plus.GuideDbBridge.findTitle]: 7 dni archiwum, 8 dni naprzód). Liczone w tle.
 */
internal object NiuniaProgrammeAirings {
    const val MAX_ARCHIVE = 30
    const val MAX_SOON = 15

    /** „Gry małżeńskie (196)” → „Gry małżeńskie”: tytuł strony bez numeru odcinka w nawiasie. */
    fun showName(title: String): String = BRACKET_NO.replace(title, "").trim().ifEmpty { title.trim() }
    /** Numer odcinka z tytułu („(196)”) albo pole odcinka przewodnika. */
    fun episodeOf(p: LiveTvProgramme): String? = BRACKET_NO.find(p.title)?.groupValues?.get(1) ?: p.episode?.takeIf { it.isNotBlank() }
    private val BRACKET_NO = Regex("""\s*\((\d{1,5})\)\s*$""")

    fun load(c: Context, programme: LiveTvProgramme, from: LiveTvChannel, channels: List<LiveTvChannel>, now: Long): List<NiuniaAiring> {
        // JMGO uxr17 (26.11): „Gry małżeńskie (196)” pasowało tylko do siebie — 1 emisja z 17 w bazie. Serial = klucz tytułu bez
        // numeru odcinka (jak ulubione programy, [NiuniaFavShowsCache.key]), więc strona ma wszystkie odcinki, także z archiwum.
        val show = NiuniaFavShowsCache.key(programme.title)
        fun same(p: LiveTvProgramme) = NiuniaFavShowsCache.key(p.title) == show
        val found = HashMap<String, MutableMap<Long, LiveTvProgramme>>()
        fun put(key: String, p: LiveTvProgramme) { found.getOrPut(key) { HashMap() }.putIfAbsent(p.startEpochMs, p) }
        put(from.guideKey, programme)
        // Najpierw pamięć (z obrazkami i odcinkami): kanał wyniku i kanały z indeksu tytułów.
        val keys = LinkedHashSet<String>().apply { add(from.guideKey); TvTextIndex.titles(TvTextIndex.norm(showName(programme.title))).forEach { addAll(it.second) } }
        for (k in keys) for (p in LiveTvRepository.schedule(k)) if (same(p)) put(k, p)
        val inMemory = found.values.sumOf { it.size }
        val db = com.nuvio.tv.fork.tv.plus.GuideDbBridge.findTitle(c, showName(programme.title), now)
        db?.forEach { (k, list) -> for (p in list) if (same(p)) put(k, p) }
        Log.i(TAG, "strona programu: emisje z pamięci $inMemory, razem ${found.values.sumOf { it.size }} (baza ${if (db == null) "niedostępna" else "${db.size} kanałów"})")
        return pick(found.mapValues { it.value.values.toList() }, channels, now,
            sameChannel = { TvTextIndex.sameChannelKey(it.name) },
            playable = { ch, p -> LiveTvCatchupLinks.isPlayable(ch.catchup, p, now) })
    }

    /**
     * Czysty wybór (testy JVM): emisje [found] (klucz przewodnika → programy już tego tytułu) na kanałach listy [channels].
     * Ta sama godzina na kopiach jednego kanału („TVN 7 HD”, „TVN 7 FHD”) raz — minione na pierwszej kopii z archiwum,
     * pozostałe na pierwszej z listy. Minione bez archiwum odpadają (nie da się ich obejrzeć). Kolejność: na żywo,
     * archiwum od najnowszej, potem najbliższe przyszłe.
     */
    fun pick(
        found: Map<String, List<LiveTvProgramme>>,
        channels: List<LiveTvChannel>,
        now: Long,
        sameChannel: (LiveTvChannel) -> String,
        playable: (LiveTvChannel, LiveTvProgramme) -> Boolean,
    ): List<NiuniaAiring> {
        if (found.isEmpty()) return emptyList()
        val byKey = HashMap<String, MutableList<LiveTvChannel>>()
        val pos = HashMap<LiveTvChannel, Int>()
        channels.forEachIndexed { i, ch -> if (ch.guideKey in found) { byKey.getOrPut(ch.guideKey) { ArrayList(2) } += ch; pos.putIfAbsent(ch, i) } }
        val groups = LinkedHashMap<String, MutableList<Pair<LiveTvChannel, LiveTvProgramme>>>()
        for ((key, list) in found) {
            val chs = byKey[key] ?: continue
            for (p in list) for (ch in chs) groups.getOrPut(sameChannel(ch) + "|" + p.startEpochMs) { ArrayList(2) } += ch to p
        }
        val out = ArrayList<NiuniaAiring>()
        for (g in groups.values) {
            val sorted = g.sortedBy { pos[it.first] ?: Int.MAX_VALUE }
            val p = sorted.first().second
            when {
                p.startEpochMs > now -> out += NiuniaAiring(sorted.first().first, p, NiuniaAiringKind.SOON)
                p.stopEpochMs > now -> out += NiuniaAiring(sorted.first().first, p, NiuniaAiringKind.LIVE)
                else -> sorted.firstOrNull { playable(it.first, it.second) }?.let { out += NiuniaAiring(it.first, it.second, NiuniaAiringKind.ARCHIVE) }
            }
        }
        val live = out.filter { it.kind == NiuniaAiringKind.LIVE }.sortedBy { pos[it.channel] ?: Int.MAX_VALUE }
        val archive = out.filter { it.kind == NiuniaAiringKind.ARCHIVE }.sortedByDescending { it.programme.startEpochMs }.take(MAX_ARCHIVE)
        val soon = out.filter { it.kind == NiuniaAiringKind.SOON }.sortedBy { it.programme.startEpochMs }.take(MAX_SOON)
        return live + archive + soon
    }
}

/** Długie OK na kafelku programu: dodaje tytuł do ulubionych programów (albo usuwa) i mówi, co naprawdę dodano. */
internal fun niuniaToggleFollow(context: Context, title: String) {
    if (ShowIdentity.normalizeTitle(title).isEmpty()) {
        com.nuvio.tv.fork.ForkLog.say(context, TAG, context.getString(R.string.niunia_tvprog_follow_failed, title))
        return
    }
    val on = TvStore.toggleFavoriteShow(title)
    Log.i(TAG, "live tv: długie OK → ulubione programy ${if (on) "dodany" else "usunięty"}: „$title”")
    Toast.makeText(context, context.getString(if (on) R.string.niunia_tvprog_followed else R.string.niunia_tvprog_unfollowed, NiuniaProgrammeAirings.showName(title)), Toast.LENGTH_SHORT).show()
}

/**
 * Strona programu (panel jak „Opcje kanału”, IPTV #82): tytuł, opis, „Dodaj do ulubionych programów” i wszystkie emisje —
 * na żywo, z archiwum (OK = obejrzyj) i przyszłe (OK = kiedy się zacznie). Wstecz zamyka panel.
 */
@Composable
internal fun NiuniaLiveProgrammePanel(
    channel: LiveTvChannel,
    programme: LiveTvProgramme,
    channels: List<LiveTvChannel>,
    now: Long,
    onPlay: (LiveTvChannel, LiveTvProgramme) -> Unit,
    onClose: () -> Unit,
) {
    val ctx = LocalContext.current
    val title = programme.title
    var airings by remember { mutableStateOf<List<NiuniaAiring>?>(null) }
    val favShows by TvStore.favoriteShows.collectAsState()
    val followed = ShowIdentity.normalizeTitle(title) in favShows
    val followReq = remember { FocusRequester() }
    val firstReq = remember { FocusRequester() }
    BackHandler { Log.i(TAG, "strona programu „$title” zamknięta (Wstecz)"); onClose() }
    LaunchedEffect(title, programme.startEpochMs) {
        val t0 = android.os.SystemClock.elapsedRealtime()
        airings = try {
            withContext(Dispatchers.IO) { NiuniaProgrammeAirings.load(ctx, programme, channel, channels, now) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "strona programu „$title”: ${e.javaClass.simpleName}: ${e.message}"); emptyList()
        }
        val a = airings.orEmpty()
        Log.i(TAG, "strona programu „$title”: na żywo ${a.count { it.kind == NiuniaAiringKind.LIVE }}, archiwum ${a.count { it.kind == NiuniaAiringKind.ARCHIVE }}, " +
            "wkrótce ${a.count { it.kind == NiuniaAiringKind.SOON }} (${android.os.SystemClock.elapsedRealtime() - t0} ms)")
    }
    // Fokus: od razu na „Dodaj do ulubionych”, po wczytaniu na pierwszej emisji (najlepszej do obejrzenia).
    LaunchedEffect(airings == null) {
        val target = if (airings.isNullOrEmpty()) followReq else firstReq
        for (wait in longArrayOf(80, 250, 600)) {
            delay(wait)
            try { target.requestFocus(); Log.i(TAG, "strona programu: fokus → ${if (target === firstReq) "1. emisja" else "ulubione"}"); break }
            catch (e: IllegalStateException) { Log.w(TAG, "strona programu: fokus: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
    val description = programme.description?.takeIf { it.isNotBlank() } ?: airings?.firstNotNullOfOrNull { a -> a.programme.description?.takeIf { it.isNotBlank() } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        NiuniaPanelOverlay(NiuniaProgrammeAirings.showName(title), Modifier.padding(top = 40.dp).niuniaPanelTrap("strona programu")) {
            if (description != null) NiuniaText(description, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2)
            NiuniaListRow(stringResource(if (followed) R.string.niunia_tvprog_unfollow else R.string.niunia_tvprog_follow), icon = "heart",
                focusRequester = followReq, onClick = { niuniaToggleFollow(ctx, title) })
            val list = airings
            when {
                list == null -> NiuniaListRow(stringResource(R.string.niunia_tvprog_searching), onClick = null, enabled = false)
                list.isEmpty() -> NiuniaListRow(stringResource(R.string.niunia_tvprog_none), onClick = null, enabled = false)
                else -> LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    itemsIndexed(list, key = { _, a -> a.channel.streamUrl + "|" + a.programme.startEpochMs }) { i, a ->
                        val p = a.programme
                        val name = ChannelNames.shown(a.channel.name)
                        NiuniaListRow("${whenLabel(p.startEpochMs, now)} – ${hhmm(p.stopEpochMs)}", kind = NiuniaRowKind.Track,
                            supporting = stringResource(when (a.kind) {
                                NiuniaAiringKind.LIVE -> R.string.niunia_tvprog_live
                                NiuniaAiringKind.ARCHIVE -> R.string.niunia_tvprog_archive
                                NiuniaAiringKind.SOON -> R.string.niunia_tvprog_soon
                            }, name) + (NiuniaProgrammeAirings.episodeOf(p)?.let { " · " + stringResource(R.string.niunia_tvprog_episode, it) } ?: ""),
                            icon = when (a.kind) { NiuniaAiringKind.LIVE -> "live"; NiuniaAiringKind.ARCHIVE -> "history"; NiuniaAiringKind.SOON -> "calendar" },
                            focusRequester = if (i == 0) firstReq else null,
                            onClick = {
                                if (a.kind == NiuniaAiringKind.SOON) {
                                    Log.i(TAG, "strona programu: „$title” ${whenLabel(p.startEpochMs, now)} jeszcze nie leci")
                                    Toast.makeText(ctx, ctx.getString(R.string.niunia_tv_not_yet, whenLabel(p.startEpochMs, System.currentTimeMillis())), Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.i(TAG, "strona programu: „$title” → ${if (a.kind == NiuniaAiringKind.LIVE) "na żywo" else "archiwum ${whenLabel(p.startEpochMs, now)}"}")
                                    onClose(); onPlay(a.channel, p)
                                }
                            })
                    }
                }
            }
        }
    }
}
