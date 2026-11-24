package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.fork.tv.dupes.ChannelDupes
import com.nuvio.tv.fork.tv.names.ChannelNames
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows
import com.nuvio.tv.skin.niunia.components.niuniaPanelTrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "ForkNiunia"

/**
 * Z-21 (P-26/P-27, makieta `LiveTVGuide`): ręczny wybór przewodnika kanału. Zapis na tym TV: kanał (źródło + id) →
 * klucz przewodnika innego kanału albo „bez przewodnika”. Bez wpisu = przewodnik dobrany automatycznie ([LiveTvChannel.guideKey]).
 * Działa w przewodniku Niuni (siatka, opis programu, ramówka); kafelki i odtwarzacz zostają przy przewodniku automatycznym.
 */
internal object NiuniaGuideChoice {
    private const val PREFS = "niunia_guide_choice"
    private const val NONE = "-"
    /** Zmienia się przy każdym wyborze — wiersze przewodnika czytają ją, żeby odświeżyć program. */
    val version = mutableIntStateOf(0)
    @Volatile private var map: Map<String, String> = emptyMap()
    @Volatile private var loaded = false

    private fun id(ch: LiveTvChannel) = "${ch.sourceId}|${ch.id}"

    fun ensure(c: Context) {
        if (loaded) return
        val all = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        map = all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
        loaded = true
        if (map.isNotEmpty()) Log.i(TAG, "live tv: ręczne przewodniki z zapisu: ${map.size}")
    }

    /** Klucz przewodnika do pokazania ("" = bez przewodnika). */
    fun keyFor(ch: LiveTvChannel): String = when (val v = map[id(ch)]) {
        null -> ch.guideKey
        NONE -> ""
        else -> v
    }

    fun isManual(ch: LiveTvChannel) = map.containsKey(id(ch))

    /** [guideKey] null = automatycznie (wpis usunięty), "" = bez przewodnika. */
    fun set(c: Context, ch: LiveTvChannel, guideKey: String?) {
        val next = map.toMutableMap()
        if (guideKey == null || guideKey == ch.guideKey) next.remove(id(ch)) else next[id(ch)] = guideKey.ifEmpty { NONE }
        map = next
        val ed = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (next.containsKey(id(ch))) ed.putString(id(ch), next[id(ch)]) else ed.remove(id(ch))
        ed.apply()
        version.intValue++
        Log.i(TAG, "live tv: przewodnik dla ${ch.name} → " + when (guideKey) { null, ch.guideKey -> "automatyczny"; "" -> "bez przewodnika"; else -> guideKey })
    }

    data class Candidate(val guideKey: String, val name: String, val sameName: Boolean, val auto: Boolean)

    /**
     * Przewodniki do wyboru: najpierw automatyczny, potem kanały o tej samej nazwie (bez kraju i jakości), potem o
     * podobnej (ten sam początek nazwy). Najwyżej [limit]. Liczone w tle (lista do 60 tys. kanałów).
     */
    fun candidates(ch: LiveTvChannel, all: List<LiveTvChannel>, limit: Int = 8): List<Candidate> {
        val mine = ChannelDupes.plain(ChannelNames.shown(ch.name)).trim()
        val head = mine.substringBefore(' ').takeIf { it.length >= 2 } ?: mine
        val out = LinkedHashMap<String, Candidate>()
        if (ch.guideKey.isNotEmpty()) out[ch.guideKey] = Candidate(ch.guideKey, ch.name.trim().take(60), sameName = true, auto = true)
        if (head.isEmpty()) return out.values.toList()
        val same = ArrayList<Candidate>()
        val similar = ArrayList<Candidate>()
        val seen = HashSet<String>(out.keys)
        for (o in all) {
            if (o.guideKey.isEmpty() || o.guideKey in seen) continue
            // Test JMGO tv2c: shown() dla każdego z 58 tys. kanałów trwało kilka sekund — najpierw tani test surowej nazwy.
            if (!o.name.contains(head, ignoreCase = true)) continue
            val n = ChannelDupes.plain(ChannelNames.shown(o.name)).trim()
            if (!n.startsWith(head)) continue
            seen += o.guideKey
            // Test JMGO tv2c: krótka nazwa („CNBC”) była taka sama dla wszystkich kopii — pełna nazwa z listy.
            (if (n == mine) same else similar) += Candidate(o.guideKey, o.name.trim().take(60), sameName = n == mine, auto = false)
            if (same.size >= limit) break
        }
        return (out.values + same + similar.sortedBy { it.name.length }).take(limit)
    }
}

private enum class OptionsPage { MAIN, GUIDE, SCHEDULE }

/**
 * Opcje kanału (Z-14 `LiveTVOptions`, długie OK na kanale w przewodniku i na kafelku): Dodaj do / Usuń z ulubionych,
 * Wybierz przewodnik (panel „Przewodnik dla: …”), Ramówka kanału (dziś i jutro, OK = odtwórz), Ukryj kanał, a gdy są
 * ukryte — Pokaż ukryte kanały (w Niuni nie ma innego miejsca, gdzie wracają). Wstecz: z podstrony do opcji, z opcji = zamknięcie.
 * Bez wiersza „Strumień 1 z N” — fork nie zbiera dziś historii działania kopii kanału.
 */
@Composable
internal fun NiuniaLiveOptionsPanel(
    channel: LiveTvChannel,
    allChannels: List<LiveTvChannel>,
    now: Long,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
    onClose: () -> Unit,
) {
    val ctx = LocalContext.current
    var page by remember { mutableStateOf(OptionsPage.MAIN) }
    val repo by LiveTvRepository.uiState.collectAsState()
    val dbRows by com.nuvio.tv.fork.tv.plus.GuideDbBridge.version.collectAsState()
    val choiceVersion = NiuniaGuideChoice.version.intValue
    val name = ChannelNames.shown(channel.name)
    val first = remember(page) { FocusRequester() }
    BackHandler {
        if (page == OptionsPage.MAIN) { Log.i(TAG, "live tv: opcje kanału $name zamknięte (Wstecz)"); onClose() }
        else { Log.i(TAG, "live tv: opcje kanału $name ← ${page.name.lowercase()}"); page = OptionsPage.MAIN }
    }
    LaunchedEffect(page) { Log.i(TAG, "live tv: opcje kanału $name, strona ${page.name.lowercase()}") }
    val listState = remember(page) { androidx.compose.foundation.lazy.LazyListState() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        val title = when (page) {
            OptionsPage.MAIN -> stringResource(R.string.niunia_tv1_options_title, name)
            OptionsPage.GUIDE -> stringResource(R.string.niunia_tv1_guide_title, name)
            OptionsPage.SCHEDULE -> stringResource(R.string.niunia_tv1_schedule_title, name)
        }
        NiuniaPanelOverlay(title,
            Modifier.padding(top = 40.dp).niuniaPanelTrap("live tv: opcje kanału")) {
            when (page) {
                OptionsPage.MAIN -> {
                    val fav = channel.streamUrl in repo.favoriteUrls
                    NiuniaListRow(stringResource(if (fav) R.string.niunia_tv1_fav_remove else R.string.niunia_live_fav), icon = "heart", focusRequester = first,
                        onClick = { niuniaToggleFavourite(ctx, channel); onClose() })
                    val guideNow = remember(choiceVersion) {
                        when {
                            !NiuniaGuideChoice.isManual(channel) -> ctx.getString(R.string.niunia_tv1_guide_auto)
                            NiuniaGuideChoice.keyFor(channel).isEmpty() -> ctx.getString(R.string.niunia_tv1_guide_none)
                            else -> allChannels.firstOrNull { it.guideKey == NiuniaGuideChoice.keyFor(channel) }?.name?.trim()?.take(60) ?: NiuniaGuideChoice.keyFor(channel)
                        }
                    }
                    NiuniaListRow(stringResource(R.string.niunia_tv1_guide_pick), supporting = stringResource(R.string.niunia_tv1_guide_now, guideNow), trailing = NiuniaTrailing.Chevron,
                        onClick = { page = OptionsPage.GUIDE })
                    NiuniaListRow(stringResource(R.string.niunia_tv1_schedule), supporting = stringResource(R.string.niunia_tv1_schedule_sub), icon = "calendar", trailing = NiuniaTrailing.Chevron,
                        onClick = { page = OptionsPage.SCHEDULE })
                    NiuniaListRow(stringResource(R.string.niunia_tv1_hide), supporting = stringResource(R.string.niunia_tv1_hide_sub),
                        onClick = {
                            LiveTvRepository.setChannelsHidden(listOf(channel), hidden = true)
                            Log.i(TAG, "live tv: ukryty kanał $name")
                            Toast.makeText(ctx, ctx.getString(R.string.niunia_tv1_hidden_toast, name), Toast.LENGTH_SHORT).show()
                            onClose()
                        })
                    val hidden = repo.hiddenChannelKeys.size
                    if (hidden > 0) NiuniaListRow(stringResource(R.string.niunia_tv1_unhide, hidden),
                        onClick = {
                            val back = repo.channels.filter { it.hideKey in repo.hiddenChannelKeys }
                            LiveTvRepository.setChannelsHidden(back, hidden = false)
                            Log.i(TAG, "live tv: pokazane ukryte kanały: ${back.size}")
                            Toast.makeText(ctx, ctx.getString(R.string.niunia_tv1_unhidden_toast, back.size), Toast.LENGTH_SHORT).show()
                            onClose()
                        })
                    NiuniaListRow(stringResource(R.string.niunia_tv1_options_note), onClick = null, enabled = false)
                }
                OptionsPage.GUIDE -> {
                    var list by remember { mutableStateOf<List<NiuniaGuideChoice.Candidate>?>(null) }
                    LaunchedEffect(channel) {
                        list = try {
                            withContext(Dispatchers.Default) { NiuniaGuideChoice.candidates(channel, allChannels) }
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                            Log.w(TAG, "live tv: przewodniki dla $name: ${e.javaClass.simpleName}: ${e.message}"); emptyList()
                        }
                        Log.i(TAG, "live tv: przewodniki dla $name: ${list?.size}")
                        list?.let { l -> com.nuvio.tv.fork.tv.plus.GuideDbBridge.want(ctx, l.map { it.guideKey }) }
                        delay(100)
                        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus listy przewodników: ${e.javaClass.simpleName}: ${e.message}") }
                    }
                    val cands = list
                    // Test JMGO tv2c: wiersz bez fokusu w czasie szukania wypuszczał fokus spod panelu na kafelki.
                    if (cands == null) NiuniaListRow(stringResource(R.string.niunia_tv1_guide_searching), onClick = { Log.i(TAG, "live tv: przewodniki jeszcze się szukają") }, focusRequester = first)
                    else {
                        val current = remember(choiceVersion) { NiuniaGuideChoice.keyFor(channel) }
                        LazyColumn(Modifier.heightIn(max = 380.dp), state = listState) {
                            itemsIndexed(cands, key = { _, c -> c.guideKey }) { i, c ->
                                val onAir = remember(c.guideKey, repo.guideVersion, dbRows, now / MIN_MS) { LiveTvRepository.schedule(c.guideKey).firstOrNull { isLive(it, now) } }
                                val kind = when { c.auto -> ctx.getString(R.string.niunia_tv1_guide_auto_row); c.sameName -> ctx.getString(R.string.niunia_tv1_guide_same); else -> ctx.getString(R.string.niunia_tv1_guide_similar) }
                                NiuniaListRow(c.name, kind = NiuniaRowKind.Track,
                                    supporting = listOfNotNull(kind, onAir?.let { ctx.getString(R.string.niunia_tv1_guide_on_air, it.title) } ?: ctx.getString(R.string.niunia_tv1_guide_no_now)).joinToString(" · "),
                                    trailing = if (c.guideKey == current) NiuniaTrailing.Check else NiuniaTrailing.None,
                                    focusRequester = if (c.guideKey == current || (i == 0 && cands.none { it.guideKey == current })) first else null,
                                    onClick = { NiuniaGuideChoice.set(ctx, channel, if (c.auto) null else c.guideKey); page = OptionsPage.MAIN })
                            }
                            item(key = "none") {
                                NiuniaListRow(stringResource(R.string.niunia_tv1_guide_none), kind = NiuniaRowKind.Track, supporting = stringResource(R.string.niunia_tv1_guide_none_sub),
                                    trailing = if (current.isEmpty()) NiuniaTrailing.Check else NiuniaTrailing.None,
                                    focusRequester = if (current.isEmpty()) first else null,
                                    onClick = { NiuniaGuideChoice.set(ctx, channel, ""); page = OptionsPage.MAIN })
                            }
                        }
                    }
                }
                OptionsPage.SCHEDULE -> {
                    val key = remember(choiceVersion) { NiuniaGuideChoice.keyFor(channel) }
                    LaunchedEffect(key) { if (key.isNotEmpty()) com.nuvio.tv.fork.tv.plus.GuideDbBridge.want(ctx, listOf(key)) }
                    val items = remember(key, repo.guideVersion, dbRows, now / MIN_MS) {
                        LiveTvRepository.schedule(key).filter { it.stopEpochMs > now - 3 * 60 * MIN_MS && it.startEpochMs < now + 2 * DAY_MS }
                    }
                    if (items.isEmpty()) NiuniaListRow(stringResource(R.string.niunia_live_no_guide), onClick = null, enabled = false)
                    else {
                        val liveIndex = items.indexOfFirst { isLive(it, now) }.coerceAtLeast(0)
                        LaunchedEffect(items.size) {
                            try { listState.scrollToItem(liveIndex) } catch (e: IllegalStateException) { Log.w(TAG, "live tv: ramówka, przewiń: ${e.javaClass.simpleName}: ${e.message}") }
                        }
                        LazyColumn(Modifier.heightIn(max = 380.dp), state = listState) {
                            itemsIndexed(items, key = { _, p -> p.startEpochMs }) { i, p ->
                                val time = "${whenLabel(p.startEpochMs, now)} – ${hhmm(p.stopEpochMs)}"
                                NiuniaListRow(p.title, kind = NiuniaRowKind.Track,
                                    supporting = if (isLive(p, now)) stringResource(R.string.niunia_tv1_schedule_now, time) else time,
                                    focusRequester = if (i == liveIndex) first else null,
                                    onClick = { Log.i(TAG, "live tv: ramówka $name → „${p.title}”"); onClose(); onPlay(channel, p) })
                            }
                        }
                    }
                }
            }
        }
    }
    // Fokus startowy każdej strony: pierwsza pozycja (przewodnik: zaznaczony; ramówka: trwający program).
    LaunchedEffect(page) {
        for (wait in longArrayOf(80, 250, 600)) {
            delay(wait)
            try { first.requestFocus(); Log.i(TAG, "live tv: opcje kanału, fokus startowy (${page.name.lowercase()})"); break }
            catch (e: IllegalStateException) { Log.w(TAG, "live tv: opcje kanału, fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
}
