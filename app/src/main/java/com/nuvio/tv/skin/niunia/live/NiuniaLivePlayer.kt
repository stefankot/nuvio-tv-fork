package com.nuvio.tv.skin.niunia.live

import android.util.Log
import android.view.KeyEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.fork.tv.TvMeta
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.fork.tv.TvStoreMetaViewModel
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.api.SkinPreferences
import com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories
import com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav
import com.nuvio.tv.skin.kicia.livetv.forgeAirings
import com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo
import com.nuvio.tv.skin.kicia.player.formatScrubTime
import com.nuvio.tv.skin.niunia.player.NiuniaChip
import com.nuvio.tv.skin.niunia.player.NiuniaDeepBackground
import com.nuvio.tv.skin.niunia.player.NiuniaIconCircle
import com.nuvio.tv.skin.niunia.player.NiuniaInfoFields
import com.nuvio.tv.skin.niunia.player.NiuniaPersonInitials
import com.nuvio.tv.skin.niunia.player.NiuniaPill
import com.nuvio.tv.skin.niunia.player.NiuniaPlayState
import com.nuvio.tv.skin.niunia.player.NiuniaPlayerScrim
import com.nuvio.tv.skin.niunia.player.NiuniaShelfTitle
import com.nuvio.tv.skin.niunia.player.NiuniaStateChip
import com.nuvio.tv.skin.niunia.player.NiuniaTextTile
import com.nuvio.tv.skin.niunia.player.NiuniaTrack
import com.nuvio.tv.skin.niunia.player.NiuniaTrackLabel
import com.nuvio.tv.skin.niunia.player.NiuniaWideTile
import com.nuvio.tv.skin.niunia.player.color
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpace
import com.nuvio.tv.skin.niunia.tokens.NiuniaStatus
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPlayerState
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.onEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "ForkNiunia"

/** Skórka Niunia w odtwarzaczu IPTV: zamiast karty MiniGuide Kicii rysuje [NiuniaLivePlayer], klawisze bierze [NiuniaLiveNav]. */
internal object NiuniaLive {
    fun active(): Boolean = SkinPreferences.skin.value == SkinId.NIUNIA
    /** Stan Live TV odtwarzacza (ustawiany przy każdym rysowaniu nakładki Live TV). */
    var state: LiveTvPlayerState? = null
}

internal enum class NiuniaLiveSlot { CHANNEL, BAR, RESTART, PAUSE, LIVE, FAV, INFO, MORE }

/** Kafel rzędu pod odtwarzaczem IPTV: inny odcinek programu albo program z tej samej kategorii EPG na innym kanale. */
internal data class NiuniaLiveTile(val channel: LiveTvChannel, val programme: LiveTvProgramme, val live: Boolean, val archive: Boolean, val future: Boolean)

/** Sekcja strony ▼▼ dla nawigacji klawiszami: liczba elementów i akcja OK. */
internal class NiuniaLiveSection(val count: Int, val onOk: (Int) -> Unit)

/**
 * Nawigacja klawiszami (jak MiniGuide Kicii — Reshaped oddaje klawisze karcie, gdy jest otwarta):
 * poziom 0 = przyciski (przełącznik kanałów ▲▼ zmienia kanał od razu, ▶ do przycisków, ▲ z przycisków na pasek, gdy kanał ma archiwum),
 * ▼ z przycisków = poziom 1 (rząd), ▼ z rzędu = poziom 2 (szczegóły `PlayerLiveInfo`). Wstecz cofa krok po kroku.
 */
internal object NiuniaLiveNav {
    var level by mutableIntStateOf(0)
    var slot by mutableStateOf(NiuniaLiveSlot.CHANNEL)
    var rowIndex by mutableIntStateOf(0)
    var section by mutableIntStateOf(0)
    var index by mutableIntStateOf(0)
    /** Publikowane przez rysowanie: przyciski w kolejności ◀▶, rząd, sekcje szczegółów. */
    var buttons: List<NiuniaLiveSlot> = emptyList()
    var row: List<NiuniaLiveTile> = emptyList()
    var sections: List<NiuniaLiveSection> = emptyList()
    var shownTitle: String? = null
    var appContext: android.content.Context? = null

    fun reset(archive: Boolean) {
        level = 0; slot = if (archive) NiuniaLiveSlot.PAUSE else NiuniaLiveSlot.CHANNEL; rowIndex = 0; section = 0; index = 0
    }

    private fun go(to: Int, why: String) {
        Log.i(TAG, "iptv: poziom $level → $to ($why, rząd=${row.size})")
        level = to
        if (to == 2) { section = sections.indexOfFirst { it.count > 0 }.coerceAtLeast(0); index = 0 }
    }

    fun onKey(state: LiveTvPlayerState, event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        val code = event.keyCode
        val ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
        val back = code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE
        val channel = state.zapList().firstOrNull { it.streamUrl == state.currentListUrl }
            ?: LiveTvRepository.uiState.value.channels.firstOrNull { it.streamUrl == state.currentListUrl }
            ?: return false
        if (!down) {
            if (back) {
                when (level) {
                    2 -> { go(if (row.isNotEmpty()) 1 else 0, "Wstecz"); state.showInfo() }
                    1 -> { go(0, "Wstecz"); state.showInfo() }
                    else -> state.hideInfo()
                }
            }
            return true
        }
        if (back) return true
        if (event.repeatCount > 0 && ok) return true
        state.showInfo()
        when (level) {
            0 -> controlsKey(state, channel, code, ok, event.repeatCount)
            1 -> when {
                code == KeyEvent.KEYCODE_DPAD_LEFT -> rowIndex = (rowIndex - 1).coerceAtLeast(0)
                code == KeyEvent.KEYCODE_DPAD_RIGHT -> rowIndex = (rowIndex + 1).coerceAtMost((row.size - 1).coerceAtLeast(0))
                code == KeyEvent.KEYCODE_DPAD_UP -> go(0, "▲")
                code == KeyEvent.KEYCODE_DPAD_DOWN -> go(2, "▼")
                ok -> row.getOrNull(rowIndex)?.let { play(state, it) }
                else -> return false
            }
            else -> {
                val focusable = sections.indices.filter { sections[it].count > 0 }
                when {
                    code == KeyEvent.KEYCODE_DPAD_UP -> focusable.lastOrNull { it < section }?.let { section = it; index = index.coerceAtMost(sections[it].count - 1) }
                    code == KeyEvent.KEYCODE_DPAD_DOWN -> focusable.firstOrNull { it > section }?.let { section = it; index = index.coerceAtMost(sections[it].count - 1) }
                    code == KeyEvent.KEYCODE_DPAD_LEFT -> index = (index - 1).coerceAtLeast(0)
                    code == KeyEvent.KEYCODE_DPAD_RIGHT -> index = (index + 1).coerceAtMost(((sections.getOrNull(section)?.count ?: 1) - 1).coerceAtLeast(0))
                    ok -> sections.getOrNull(section)?.takeIf { index < it.count }?.onOk?.invoke(index)
                    else -> return false
                }
            }
        }
        return true
    }

    private fun controlsKey(state: LiveTvPlayerState, channel: LiveTvChannel, code: Int, ok: Boolean, repeat: Int) {
        val scrubbable = channel.catchup != null
        if (slot != NiuniaLiveSlot.CHANNEL && slot != NiuniaLiveSlot.BAR && slot !in buttons) slot = buttons.firstOrNull() ?: NiuniaLiveSlot.CHANNEL
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> when (slot) {
                NiuniaLiveSlot.CHANNEL -> { ForgeMiniGuideNav.paused = false; state.zap(-1); Log.i(TAG, "iptv: kanał ▲") }
                NiuniaLiveSlot.BAR -> Unit
                else -> if (scrubbable) slot = NiuniaLiveSlot.BAR
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> when (slot) {
                NiuniaLiveSlot.CHANNEL -> { ForgeMiniGuideNav.paused = false; state.zap(1); Log.i(TAG, "iptv: kanał ▼") }
                NiuniaLiveSlot.BAR -> slot = buttons.firstOrNull() ?: NiuniaLiveSlot.CHANNEL
                else -> go(if (row.isNotEmpty()) 1 else 2, "▼ z przycisków")
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> when (slot) {
                NiuniaLiveSlot.BAR -> if (repeat == 0) state.openScrub(channel, -1, keepInfo = true)
                NiuniaLiveSlot.CHANNEL -> Unit
                else -> { val i = buttons.indexOf(slot); slot = if (i <= 0) NiuniaLiveSlot.CHANNEL else buttons[i - 1] }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> when (slot) {
                NiuniaLiveSlot.BAR -> if (repeat == 0) state.openScrub(channel, 1, keepInfo = true)
                NiuniaLiveSlot.CHANNEL -> buttons.firstOrNull()?.let { slot = it }
                else -> { val i = buttons.indexOf(slot); if (i in 0 until buttons.lastIndex) slot = buttons[i + 1] }
            }
            else -> if (ok) act(state, channel, slot)
        }
    }

    private fun act(state: LiveTvPlayerState, channel: LiveTvChannel, s: NiuniaLiveSlot) {
        Log.i(TAG, "iptv: OK na $s")
        when (s) {
            NiuniaLiveSlot.CHANNEL -> if (state.isPausedNow()) state.togglePause() else buttons.firstOrNull()?.let { slot = it }
            NiuniaLiveSlot.BAR -> state.openScrub(channel, 0, keepInfo = true)
            NiuniaLiveSlot.RESTART -> {
                val p = state.replayingProgramme ?: state.startOverProgramme(channel)
                if (p != null) state.playCatchup(channel, p) else appContext?.let { com.nuvio.tv.fork.ForkLog.say(it, TAG, it.getString(R.string.niunia_live_no_restart)) }
            }
            NiuniaLiveSlot.PAUSE -> state.togglePause()
            NiuniaLiveSlot.LIVE -> { ForgeMiniGuideNav.paused = false; state.switchTo(channel, force = true) }
            NiuniaLiveSlot.FAV -> shownTitle?.let { t -> val on = TvStore.toggleFavoriteShow(t); Log.i(TAG, "iptv: ulubione programy → $on") }
            NiuniaLiveSlot.INFO -> go(2, "Informacje")
            NiuniaLiveSlot.MORE -> { state.hideInfo(); state.player.onEvent(PlayerEvent.OnShowAudioOverlay) }
        }
    }

    fun play(state: LiveTvPlayerState, t: NiuniaLiveTile) {
        val now = LiveTvClock.nowEpochMs()
        Log.i(TAG, "iptv: rząd → ${t.channel.name} ${t.programme.startEpochMs} (na żywo=${t.live}, archiwum=${t.archive})")
        when {
            t.live && t.channel.streamUrl != state.currentListUrl -> { level = 0; state.switchTo(t.channel) }
            t.live -> { level = 0; state.switchTo(t.channel, force = true) }
            t.archive -> { level = 0; state.playCatchup(t.channel, t.programme) }
            else -> appContext?.let { com.nuvio.tv.fork.ForkLog.say(it, TAG, it.getString(R.string.niunia_live_starts_at, hm(t.programme.startEpochMs))) }
        }
        if (t.programme.stopEpochMs < now && !t.archive) Log.w(TAG, "iptv: program z rzędu nie jest w archiwum")
    }
}

internal fun hm(ms: Long): String = SimpleDateFormat("HH:mm", Locale("pl")).format(Date(ms))

private fun dayStart(ms: Long): Long = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

/** DZIŚ / JUTRO / WCZORAJ / PN…ND. */
private fun dayLabel(ctx: android.content.Context, ms: Long, now: Long): String {
    val d = ((dayStart(ms) - dayStart(now)) / 86_400_000L).toInt()
    return when (d) {
        0 -> ctx.getString(R.string.niunia_live_today)
        1 -> ctx.getString(R.string.niunia_live_tomorrow)
        -1 -> ctx.getString(R.string.niunia_live_yesterday).uppercase()
        else -> SimpleDateFormat("EEE", Locale("pl")).format(Date(ms)).trimEnd('.').uppercase()
    }
}

/** „S1 E3” z etykiety przewodnika (S1E3, 0.2, E12). */
private fun episodeText(raw: String?): String? {
    val (s, e) = com.nuvio.tv.fork.tvdb.Tvdb.parseLabel(raw) ?: return null
    return if (s != null) "S$s E$e" else "E$e"
}

private fun sameTitle(a: LiveTvProgramme, b: LiveTvProgramme) = a.title.trim().equals(b.title.trim(), ignoreCase = true)

/**
 * Rząd pod odtwarzaczem (SKIN.md „Player IPTV”): program powtarzalny → inne odcinki z przewodnika kanału (wyemitowane w archiwum
 * = zielona kropka, przyszłe = „Wkrótce”); jednorazowy → programy z innych kanałów z najwęższej kategorii (liść) drzewa EPG, potem gałąź;
 * bez kategorii — brak rzędu. Zwraca (rząd, czy to odcinki, nazwa kategorii).
 */
private fun buildRow(channel: LiveTvChannel, shown: LiveTvProgramme, nowMs: Long): Triple<List<NiuniaLiveTile>, Boolean, String?> {
    val schedule = LiveTvRepository.schedule(channel.guideKey)
    val window = 7L * 86_400_000
    val same = schedule.filter { sameTitle(it, shown) && it.stopEpochMs > nowMs - window && it.startEpochMs < nowMs + window }
    if (same.size >= 2) {
        val tiles = same.sortedBy { it.startEpochMs }.mapNotNull { p ->
            val live = p.startEpochMs <= nowMs && nowMs < p.stopEpochMs
            val arch = !live && p.stopEpochMs <= nowMs && com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks.isPlayable(channel.catchup, p, nowMs)
            val future = p.startEpochMs > nowMs
            if (!live && !arch && !future) null else NiuniaLiveTile(channel, p, live, arch, future)
        }.filter { it.programme.startEpochMs != shown.startEpochMs }
        if (tiles.isNotEmpty()) return Triple(tiles.take(12), true, null)
    }
    val names = ForgeEpgCategories.programmePlaceNames(channel.guideKey, shown) ?: return Triple(emptyList(), false, null)
    val place = ForgeEpgCategories.programmePlace(channel.guideKey, shown) ?: return Triple(emptyList(), false, null)
    val live = LiveTvRepository.uiState.value
    val candidates = live.channels.asSequence().filter { it.streamUrl != channel.streamUrl }.take(600).flatMap { ch ->
        sequenceOf(live.currentProgrammes[ch.guideKey], LiveTvRepository.nextProgramme(ch.guideKey, nowMs)).filterNotNull().map { ch to it }
    }.toList()
    fun pick(match: (ForgeEpgCategories.Place) -> Boolean) = candidates.filter { (ch, p) -> ForgeEpgCategories.programmePlace(ch.guideKey, p)?.let(match) == true }
    var found = if (place.leaf != null) pick { it.branch == place.branch && it.leaf == place.leaf } else emptyList()
    var title = names.second ?: names.first
    if (found.isEmpty()) { found = pick { it.branch == place.branch }; title = names.first }
    val tiles = found.sortedWith(compareBy({ it.second.startEpochMs > nowMs }, { it.second.startEpochMs })).take(10).map { (ch, p) ->
        val isLive = p.startEpochMs <= nowMs && nowMs < p.stopEpochMs
        NiuniaLiveTile(ch, p, isLive, false, !isLive)
    }
    return Triple(tiles, false, title)
}

/**
 * Odtwarzacz IPTV Niuni (canvas `PlayerLive` / `PlayerLiveDelay` / `PlayerLiveArchive` / `PlayerLiveGroup`, SKIN.md „IPTV player, 3 states”):
 * znacznik stanu (LIVE czerwony / −12:40 żółty / ARCHIWUM biały) + godziny · kanał, linia wiek · odcinek, tytuł programu
 * (logo — gdy któreś źródło je ma; tu żadne z TVDB/TMDB/EPG/playlisty nie podaje logo programu → tytuł tekstem),
 * pasek w kolorze stanu, przełącznik kanałów (▲ logo ▼) od lewej, przyciski od x=170, po prawej + (i ⋯ w grupie),
 * na dole wystaje rząd (inne odcinki albo ta sama kategoria na innych kanałach). ▼▼ = [NiuniaLiveDetails].
 * Dane: TVDB > TMDB > EPG > playlista; tekst zastępczy dopiero, gdy żadne źródło nie ma wartości.
 */
@Composable
internal fun NiuniaLivePlayer(
    channel: LiveTvChannel,
    logo: String?,
    now: LiveTvProgramme?,
    clockMs: Long,
    startOver: Boolean,
    number: Int,
    replay: LiveTvProgramme?,
    playheadMs: Long,
    scrubMs: Long?,
) {
    val context = LocalContext.current
    val nav = NiuniaLiveNav
    nav.appContext = context.applicationContext
    val state = NiuniaLive.state
    // 23.11 (przewodnik w bazie): odcinki z archiwum w rzędzie pod odtwarzaczem — minione dni kanału z bazy.
    LaunchedEffect(channel.guideKey) { com.nuvio.tv.fork.tv.plus.GuideDbBridge.need(context, channel, 0L) }
    val schedule = LiveTvRepository.schedule(channel.guideKey)
    val scrubProgramme = scrubMs?.let { t -> schedule.firstOrNull { t >= it.startEpochMs && t < it.stopEpochMs } }
    val shown = scrubProgramme ?: replay ?: now
    val archive = replay != null
    val shiftMs = if (!archive) ForgeMiniGuideNav.shiftMs() else 0L
    val playState = when { archive -> NiuniaPlayState.ARCHIVE; shiftMs > 0L -> NiuniaPlayState.DELAY; else -> NiuniaPlayState.LIVE }
    LaunchedEffect(channel.streamUrl) { nav.reset(archive); Log.i(TAG, "iptv: nakładka ${channel.name} (stan=$playState, program=${shown != null})") }
    LaunchedEffect(nav.level) { while (nav.level > 0) { state?.showInfo(); kotlinx.coroutines.delay(2_500) } }

    // Dane: TVDB > TMDB (ten sam view model co karta programu Kicii) > EPG > playlista.
    val metaVm: TvStoreMetaViewModel = hiltViewModel()
    LaunchedEffect(shown?.title, shown?.episode) { shown?.let { metaVm.load(it.title, null, it.episode) } }
    val tvdb by metaVm.tvdb.collectAsState()
    val tmdb by metaVm.meta.collectAsState()
    val data = NiuniaLiveData.of(shown, channel, logo, tvdb, tmdb)
    nav.shownTitle = shown?.title

    var rowData by remember { mutableStateOf(Triple(emptyList<NiuniaLiveTile>(), false, null as String?)) }
    LaunchedEffect(channel.guideKey, shown?.startEpochMs) {
        rowData = if (shown == null) Triple(emptyList(), false, null) else withContext(Dispatchers.Default) {
            try { buildRow(channel, shown, LiveTvClock.nowEpochMs()) } catch (e: Exception) {
                Log.w(TAG, "iptv: rząd: ${e.javaClass.simpleName}: ${e.message}"); Triple(emptyList(), false, null)
            }
        }
        Log.i(TAG, "iptv: rząd=${rowData.first.size} (${if (rowData.second) "odcinki" else "kategoria ${rowData.third}"})")
    }
    val (row, episodesRow, groupTitle) = rowData
    val group = !episodesRow && row.isNotEmpty()
    val paused = state?.isPausedNow() == true
    val favShows by TvStore.favoriteShows.collectAsState()
    val favorite = shown != null && com.nuvio.tv.fork.tv.ShowIdentity.normalizeTitle(shown.title) in favShows
    val buttons = buildList {
        if (startOver || archive) add(NiuniaLiveSlot.RESTART)
        if (archive || group || paused) add(NiuniaLiveSlot.PAUSE)
        if (playState == NiuniaPlayState.DELAY) add(NiuniaLiveSlot.LIVE)
        add(NiuniaLiveSlot.FAV)
        if (group) { add(NiuniaLiveSlot.INFO); add(NiuniaLiveSlot.MORE) }
    }
    SideEffect { nav.buttons = buttons; nav.row = row }
    val level = nav.level
    val focusSlot = if (level == 0) nav.slot else null

    Box(Modifier.fillMaxSize()) {
        if (level < 2) {
            NiuniaPlayerScrim(from = 0.34f)
            // ▼: przyciski w górę (canvas Group: pasek 368 → 244), rząd 502 → 408 z tytułem nad nim.
            val lift by animateDpAsState(if (level >= 1) (-124).dp else 0.dp, label = "niuniaLiveLift")
            val rowLift by animateDpAsState(if (level >= 1) (-94).dp else 0.dp, label = "niuniaLiveRowLift")
            Box(Modifier.fillMaxSize().offset(y = lift)) {
                Column(Modifier.offset(x = 40.dp, y = 206.dp).width(600.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val chip = when (playState) {
                            NiuniaPlayState.ARCHIVE -> stringResource(R.string.niunia_live_archive)
                            NiuniaPlayState.DELAY -> "−" + formatScrubTime(shiftMs)
                            else -> stringResource(R.string.niunia_live_live)
                        }
                        NiuniaStateChip(chip, playState)
                        val day = if (archive && shown != null && dayStart(shown.startEpochMs) != dayStart(clockMs)) dayLabel(context, shown.startEpochMs, clockMs).lowercase().replaceFirstChar { it.uppercase() } + " " else ""
                        val line = listOfNotNull(
                            shown?.let { day + hm(it.startEpochMs) + " – " + hm(it.stopEpochMs) },
                            channel.name,
                            if (playState == NiuniaPlayState.DELAY) stringResource(R.string.niunia_live_still_live) else null,
                        ).joinToString(" · ")
                        Text(line, style = NiuniaText.Button.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp), color = NiuniaColors.Text, maxLines = 1)
                    }
                    val meta = listOfNotNull(episodeText(shown?.episode), data.genre).joinToString(" · ")
                    if (meta.isNotEmpty()) Text(meta, style = NiuniaText.Body.copy(fontSize = 15.sp, lineHeight = 20.sp), color = NiuniaColors.Text70, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
                    if (data.programmeLogo != null) {
                        AsyncImage(model = data.programmeLogo, contentDescription = data.title, contentScale = ContentScale.Fit, alignment = Alignment.BottomStart,
                            modifier = Modifier.padding(top = 12.dp).width(300.dp).heightIn(max = 84.dp))
                    } else {
                        Text(data.title, style = NiuniaText.Logo.copy(fontSize = 32.sp, lineHeight = 38.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                // Pasek (.pline y 368).
                LiveBar(shown, playState, clockMs, playheadMs, shiftMs, scrubMs, focused = focusSlot == NiuniaLiveSlot.BAR, modifier = Modifier.offset(x = 40.dp, y = 362.dp))
                // Przełącznik kanałów (.chsw: 40, 398, szer. 110).
                ChannelSwitch(channel, logo, focused = focusSlot == NiuniaLiveSlot.CHANNEL, modifier = Modifier.offset(x = 40.dp, y = 398.dp))
                // Przyciski od x=170 (y 414) i po prawej (right 28).
                Row(Modifier.offset(x = 170.dp, y = 414.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    buttons.filter { it == NiuniaLiveSlot.RESTART || it == NiuniaLiveSlot.PAUSE || it == NiuniaLiveSlot.LIVE }.forEach { s ->
                        when (s) {
                            NiuniaLiveSlot.RESTART -> NiuniaIconCircle(Icons.Outlined.Replay, stringResource(R.string.niunia_live_restart), focusSlot == s,
                                label = if (focusSlot == s) stringResource(R.string.niunia_live_restart) else null, modifier = Modifier.width(64.dp))
                            NiuniaLiveSlot.PAUSE -> NiuniaIconCircle(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                stringResource(if (paused) R.string.niunia_live_play else R.string.niunia_live_pause), focusSlot == s,
                                label = if (focusSlot == s || archive) stringResource(if (paused) R.string.niunia_live_play else R.string.niunia_live_pause) else null, modifier = Modifier.width(64.dp))
                            else -> NiuniaPill(stringResource(R.string.niunia_live_back_live), focusSlot == s, height = 40.dp, dot = NiuniaStatus.Live)
                        }
                    }
                }
                Row(Modifier.align(Alignment.TopEnd).offset(x = (-28).dp, y = 414.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    buttons.filter { it == NiuniaLiveSlot.FAV || it == NiuniaLiveSlot.INFO || it == NiuniaLiveSlot.MORE }.forEach { s ->
                        val (icon, label) = when (s) {
                            NiuniaLiveSlot.FAV -> (if (favorite) Icons.Outlined.Check else Icons.Outlined.Add) to stringResource(R.string.niunia_live_fav)
                            NiuniaLiveSlot.INFO -> Icons.Outlined.Info to stringResource(R.string.niunia_live_info)
                            else -> Icons.Outlined.MoreHoriz to stringResource(R.string.niunia_live_more)
                        }
                        NiuniaIconCircle(icon, label, focusSlot == s, label = if (focusSlot == s) label else null, modifier = Modifier.width(64.dp))
                    }
                }
            }
            // Rząd (.rail y 502; ▼ → 408, w grupie z tytułem kategorii nad nim).
            if (row.isNotEmpty()) {
                Column(Modifier.offset(y = 472.dp + rowLift)) {
                    val titleText = if (group && groupTitle != null) groupTitle else null
                    Row(Modifier.padding(start = NiuniaDims.Gutter).height(30.dp), verticalAlignment = Alignment.Top) {
                        if (titleText != null && level >= 1) {
                            NiuniaShelfTitle(titleText, on = true)
                            Text(" · " + stringResource(R.string.niunia_live_other_channels), style = NiuniaText.Body.copy(fontSize = 13.sp, lineHeight = 22.sp), color = NiuniaColors.Text50)
                        }
                    }
                    LiveRow(row, focused = if (level == 1) nav.rowIndex else -1, clockMs = clockMs)
                }
            }
        } else {
            NiuniaLiveDetails(channel, shown, number, data, row.takeIf { episodesRow }.orEmpty(), schedule, clockMs, startOver || archive, favorite, playState, state)
        }
    }
}

/** Wynik łączenia źródeł danych programu: TVDB > TMDB > EPG > playlista. */
internal data class NiuniaLiveData(
    val title: String,
    val description: String?,
    val image: Any?,
    val programmeLogo: String?,
    val genre: String?,
    val cast: List<com.nuvio.tv.fork.tv.TvPerson>,
    val isTv: Boolean?,
    val source: String,
) {
    companion object {
        fun of(p: LiveTvProgramme?, channel: LiveTvChannel, logo: String?, tvdb: TvMeta?, tmdb: TvMeta?): NiuniaLiveData {
            val tm = tmdb?.takeIf { it !== tvdb }
            val sources = buildList {
                if (tvdb != null) add("TVDB")
                if (tm != null) add("TMDB")
                if (p?.description != null || p?.image != null) add("EPG")
                if (logo != null || channel.logoUrl != null) add("Playlista")
            }
            return NiuniaLiveData(
                title = p?.title ?: tvdb?.title ?: tm?.title ?: com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name),
                description = tvdb?.overview ?: tm?.overview ?: p?.description,
                image = tvdb?.backdropUrl ?: tm?.backdropUrl ?: p?.image?.takeIf { it.isNotBlank() } ?: logo ?: forgeChannelLogo(channel),
                programmeLogo = null,
                genre = (tvdb?.genres?.takeIf { it.isNotEmpty() } ?: tm?.genres?.takeIf { it.isNotEmpty() })?.take(2)?.joinToString(", ")
                    ?: p?.let { ForgeEpgCategories.programmePlaceNames(channel.guideKey, it)?.let { n -> n.second ?: n.first } },
                cast = (tvdb?.cast?.takeIf { it.isNotEmpty() } ?: tm?.cast.orEmpty()) + (tvdb?.directors?.takeIf { it.isNotEmpty() } ?: tm?.directors.orEmpty()),
                isTv = tvdb?.isTv ?: tm?.isTv,
                source = sources.joinToString(" + ").ifEmpty { "EPG" },
            )
        }
    }
}

@Composable
private fun LiveBar(p: LiveTvProgramme?, st: NiuniaPlayState, nowMs: Long, playheadMs: Long, shiftMs: Long, scrubMs: Long?, focused: Boolean, modifier: Modifier) {
    val start = p?.startEpochMs ?: nowMs
    val stop = p?.stopEpochMs ?: (nowMs + 1)
    val span = (stop - start).coerceAtLeast(1L).toFloat()
    val liveFrac = ((nowMs - start) / span).coerceIn(0f, 1f)
    val color = st.color()
    Row(modifier.width(880.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val (left, right) = when (st) {
            NiuniaPlayState.ARCHIVE -> formatScrubTime((playheadMs - start).coerceAtLeast(0)) to "−" + formatScrubTime((stop - playheadMs).coerceAtLeast(0))
            else -> hm(start) to hm(stop)
        }
        Text(left, style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 20.sp), color = NiuniaColors.Text)
        BoxWithConstraints(Modifier.weight(1f).height(40.dp)) {
            val w = maxWidth
            when (st) {
                NiuniaPlayState.DELAY -> {
                    val played = ((nowMs - shiftMs - start) / span).coerceIn(0f, 1f)
                    val knob = scrubMs?.let { ((it - start) / span) } ?: played
                    NiuniaTrack(played, color, focused, buffer = liveFrac, edge = liveFrac, knob = knob, modifier = Modifier.fillMaxWidth())
                    NiuniaTrackLabel("−" + formatScrubTime(shiftMs), knob, w, color)
                    // Test JMGO build159: przy małym opóźnieniu („−0:08”) obie etykiety nakładały się („−0:0”) — „na żywo” tylko z odstępem.
                    if (w * (liveFrac - knob) > 64.dp) NiuniaTrackLabel(stringResource(R.string.niunia_live_live), liveFrac, w, NiuniaStatus.Live, small = true)
                }
                NiuniaPlayState.ARCHIVE -> {
                    val played = ((playheadMs - start) / span).coerceIn(0f, 1f)
                    NiuniaTrack(played, color, focused, knob = scrubMs?.let { (it - start) / span } ?: played, modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    val knob = scrubMs?.let { (it - start) / span } ?: liveFrac
                    NiuniaTrack(liveFrac, color, focused, knob = knob, modifier = Modifier.fillMaxWidth())
                    NiuniaTrackLabel(if (scrubMs != null) hm(scrubMs) else stringResource(R.string.niunia_live_live), knob, w, color)
                }
            }
        }
        Text(right, style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 20.sp), color = NiuniaColors.Text70)
    }
}

@Composable
private fun ChannelSwitch(channel: LiveTvChannel, logo: String?, focused: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val tint = if (focused) NiuniaColors.Text else NiuniaColors.Text70
    Column(
        modifier.width(110.dp).then(if (focused) Modifier.background(NiuniaColors.Fill24, shape).border(1.dp, NiuniaColors.Line, shape) else Modifier).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.niunia_live_prev), tint = tint, modifier = Modifier.size(26.dp))
        val url = forgeChannelLogo(channel) ?: logo
        Box(Modifier.height(36.dp).widthIn(max = 102.dp), contentAlignment = Alignment.CenterStart) {
            com.nuvio.tv.skin.niunia.livetv.NiuniaChannelLogo(channel, Modifier.height(32.dp).widthIn(max = 102.dp), NiuniaText.Logo.copy(fontSize = 22.sp, lineHeight = 26.sp), Alignment.CenterStart) // Paczka „oczywiste” pkt 5

        }
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = stringResource(R.string.niunia_live_next), tint = tint, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun LiveRow(row: List<NiuniaLiveTile>, focused: Int, clockMs: Long, state: LazyListState = rememberLazyListState()) {
    val ctx = LocalContext.current
    LaunchedEffect(focused) { if (focused >= 0) state.animateScrollToItem((focused - 2).coerceAtLeast(0)) }
    LazyRow(state = state, contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpace.TileGap)) {
        itemsIndexed(row, key = { _, t -> t.channel.streamUrl + t.programme.startEpochMs }) { i, t ->
            val badge = when {
                !t.future -> null
                t.programme.startEpochMs - clockMs < 60 * 60_000L -> ctx.getString(R.string.niunia_live_in_min, ((t.programme.startEpochMs - clockMs) / 60_000L).toInt().coerceAtLeast(1))
                else -> ctx.getString(R.string.niunia_live_soon)
            }
            val cap = listOfNotNull(episodeText(t.programme.episode), if (t.future) dayLabel(ctx, t.programme.startEpochMs, clockMs).lowercase() + " " + hm(t.programme.startEpochMs) else null).joinToString(" · ").ifEmpty { null }
            NiuniaWideTile(
                image = t.programme.image?.takeIf { it.isNotBlank() }, caption = if (t.programme.image.isNullOrBlank()) null else cap ?: t.programme.title,
                focused = i == focused, dot = if (t.live) true else if (t.archive) false else null, badge = badge,
                fallbackTitle = t.programme.title, fallbackSub = com.nuvio.tv.fork.tv.names.ChannelNames.shown(t.channel.name) + " · " + hm(t.programme.startEpochMs),
            )
        }
    }
}

/**
 * Szczegóły programu IPTV (▼▼, canvas `PlayerLiveInfo`, WPIECIE pkt 4): rozmyte tło, logo/tytuł, od lewej [LIVE] godziny · kanał,
 * linia typ · odcinek · kategoria EPG, opis, [🔔 Obserwuj] [↺ Od początku] + „● Archiwum N dni”. Sekcje: Odcinki · Sezon N →
 * Najbliższe emisje (kafle) → Obsada i twórcy (inicjały) → Informacje. Fokus prowadzą klawisze ([NiuniaLiveNav]).
 */
@Composable
private fun NiuniaLiveDetails(
    channel: LiveTvChannel,
    shown: LiveTvProgramme?,
    number: Int,
    data: NiuniaLiveData,
    episodes: List<NiuniaLiveTile>,
    schedule: List<LiveTvProgramme>,
    nowMs: Long,
    canRestart: Boolean,
    favorite: Boolean,
    st: NiuniaPlayState,
    state: LiveTvPlayerState?,
) {
    val ctx = LocalContext.current
    val nav = NiuniaLiveNav
    val upcoming = remember(shown?.startEpochMs, schedule.size) {
        if (shown == null) emptyList() else {
            val a = forgeAirings(schedule, shown, nowMs)
            val past = a.others.filter { it.stopEpochMs <= nowMs }.mapNotNull { it.episode }.toSet()
            a.others.filter { it.startEpochMs > nowMs }.take(5).map { p ->
                p to when {
                    p.episode != null && p.episode in past -> ctx.getString(R.string.niunia_live_repeat)
                    p.episode != null -> ctx.getString(R.string.niunia_live_new_ep)
                    p.startEpochMs - nowMs < 2 * 3_600_000L -> ctx.getString(R.string.niunia_live_soon)
                    else -> ""
                }
            }
        }
    }
    val headerButtons = if (canRestart) 2 else 1
    val people = data.cast.take(20)
    // Sekcje: 0 przyciski, 1 odcinki, 2 najbliższe emisje, 3 obsada, 4 informacje (bez fokusu).
    val sections = listOf(
        NiuniaLiveSection(headerButtons) { i ->
            if (i == 0) shown?.let { val on = TvStore.toggleFavoriteShow(it.title); Log.i(TAG, "iptv: szczegóły → obserwuj=$on") }
            else state?.let { s -> (s.replayingProgramme ?: s.startOverProgramme(channel))?.let { p -> nav.level = 0; s.playCatchup(channel, p) } }
        },
        NiuniaLiveSection(episodes.size) { i -> state?.let { nav.play(it, episodes[i]) } },
        NiuniaLiveSection(upcoming.size) { i -> com.nuvio.tv.fork.ForkLog.say(ctx, TAG, ctx.getString(R.string.niunia_live_starts_at, hm(upcoming[i].first.startEpochMs))) },
        NiuniaLiveSection(people.size) { _ -> },
        NiuniaLiveSection(0) { _ -> },
    )
    SideEffect { nav.sections = sections }
    LaunchedEffect(Unit) { Log.i(TAG, "iptv: szczegóły (odcinki=${episodes.size}, emisje=${upcoming.size}, obsada=${people.size}, źródło=${data.source})") }
    val sec = nav.section
    val idx = nav.index
    val scroll = rememberScrollState()
    val tops = remember { mutableStateMapOf<Int, Float>() }
    val density = LocalDensity.current
    LaunchedEffect(sec) { val y = tops[sec] ?: 0f; scroll.animateScrollTo((y - with(density) { 120.dp.toPx() }).toInt().coerceAtLeast(0)) }
    fun Modifier.top(i: Int) = onGloballyPositioned { tops[i] = it.positionInParent().y }

    Box(Modifier.fillMaxSize()) {
        NiuniaDeepBackground(data.image)
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = NiuniaSpace.AfterRow)) {
            if (data.programmeLogo != null) {
                Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.TopCenter) {
                    AsyncImage(model = data.programmeLogo, contentDescription = data.title, contentScale = ContentScale.Fit, modifier = Modifier.widthIn(max = 320.dp).heightIn(max = 64.dp))
                }
            } else {
                Text(data.title, style = NiuniaText.Logo.copy(fontSize = 32.sp, lineHeight = 38.sp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 28.dp).width(680.dp))
            }
            Column(Modifier.padding(start = NiuniaDims.Gutter, top = 20.dp).width(680.dp).top(0), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (st != NiuniaPlayState.ARCHIVE) NiuniaStateChip(stringResource(R.string.niunia_live_live), NiuniaPlayState.LIVE)
                    else NiuniaStateChip(stringResource(R.string.niunia_live_archive), NiuniaPlayState.ARCHIVE)
                    Text(listOfNotNull(shown?.let { hm(it.startEpochMs) + " – " + hm(it.stopEpochMs) }, com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name)).joinToString(" · "),
                        style = NiuniaText.Button.copy(fontSize = 14.sp, lineHeight = 20.sp), color = NiuniaColors.Text, maxLines = 1)
                }
                val type = stringResource(when { episodes.isNotEmpty() || shown?.episode != null || data.isTv == true -> R.string.niunia_live_type_series; data.isTv == false -> R.string.niunia_live_type_film; else -> R.string.niunia_live_type_show })
                val place = shown?.let { ForgeEpgCategories.programmePlaceNames(channel.guideKey, it) }?.let { (b, l) -> listOfNotNull(b, l).joinToString(" › ") }
                Text(listOfNotNull(type, episodeText(shown?.episode), place).joinToString(" · "), style = NiuniaText.Meta, color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                data.description?.let { Text(it, style = NiuniaText.Body, color = NiuniaColors.Text70, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    NiuniaPill(stringResource(if (favorite) R.string.niunia_live_following else R.string.niunia_live_follow), sec == 0 && idx == 0, icon = Icons.Outlined.NotificationsNone)
                    if (canRestart) NiuniaPill(stringResource(R.string.niunia_live_restart), sec == 0 && idx == 1, icon = Icons.Outlined.Replay)
                    channel.catchup?.days?.takeIf { it > 0 }?.let { d ->
                        Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(10.dp).background(NiuniaStatus.Archive, androidx.compose.foundation.shape.CircleShape))
                            Text(stringResource(R.string.niunia_live_archive_days, d), style = NiuniaText.Body.copy(fontSize = 13.sp), color = NiuniaStatus.Archive)
                        }
                    }
                }
            }
            Spacer(Modifier.height(NiuniaSpace.AfterRow))
            if (episodes.isNotEmpty()) {
                val season = com.nuvio.tv.fork.tvdb.Tvdb.parseLabel(shown?.episode)?.first
                NiuniaShelfTitle(if (season != null) stringResource(R.string.niunia_live_episodes_season, season) else stringResource(R.string.niunia_live_episodes),
                    on = sec == 1, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(22.dp).top(1))
                Box(Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp)) { LiveRow(episodes, if (sec == 1) idx else -1, nowMs) }
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            if (upcoming.isNotEmpty()) {
                NiuniaShelfTitle(stringResource(R.string.niunia_live_upcoming), on = sec == 2, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(22.dp).top(2))
                val rs = rememberLazyListState()
                LaunchedEffect(sec, idx) { if (sec == 2) rs.animateScrollToItem((idx - 2).coerceAtLeast(0)) }
                LazyRow(Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp), state = rs, contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpace.TileGap)) {
                    itemsIndexed(upcoming) { i, (p, label) ->
                        NiuniaTextTile(dayLabel(ctx, p.startEpochMs, nowMs), hm(p.startEpochMs), com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name), label, focused = sec == 2 && idx == i, big = true)
                    }
                }
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            if (people.isNotEmpty()) {
                NiuniaShelfTitle(stringResource(R.string.niunia_pinfo_cast), on = sec == 3, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(22.dp).top(3))
                val rs = rememberLazyListState()
                LaunchedEffect(sec, idx) { if (sec == 3) rs.animateScrollToItem((idx - 3).coerceAtLeast(0)) }
                LazyRow(Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp), state = rs, contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    itemsIndexed(people) { i, person -> NiuniaPersonInitials(person.name, person.role, focused = sec == 3 && idx == i) }
                }
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            NiuniaShelfTitle(stringResource(R.string.niunia_pinfo_info), on = false, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(22.dp).top(4))
            val placeNames = shown?.let { ForgeEpgCategories.programmePlaceNames(channel.guideKey, it) }
            val fields = listOfNotNull(
                stringResource(R.string.niunia_live_l_channel) to (if (number > 0) "$number · " else "") + com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name),
                placeNames?.let { (b, l) -> stringResource(R.string.niunia_live_l_category) to listOfNotNull(b, l).joinToString(" › ") },
                shown?.let { stringResource(R.string.niunia_live_l_airing) to dayLabel(ctx, it.startEpochMs, nowMs).lowercase() + " " + hm(it.startEpochMs) + "–" + hm(it.stopEpochMs) },
                channel.catchup?.days?.takeIf { it > 0 }?.let { stringResource(R.string.niunia_live_l_archive) to stringResource(R.string.niunia_live_archive_back, it) },
                stringResource(R.string.niunia_live_l_source) to data.source,
            )
            NiuniaInfoFields(fields, Modifier.padding(start = NiuniaDims.Gutter, top = NiuniaSpace.TitleToRow - 22.dp))
        }
    }
}
