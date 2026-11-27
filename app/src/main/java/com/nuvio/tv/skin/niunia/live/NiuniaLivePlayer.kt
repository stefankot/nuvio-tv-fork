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
import androidx.compose.material.icons.outlined.History
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

internal enum class NiuniaLiveSlot { CHANNEL, BAR, PREV, RESTART, PAUSE, LIVE, FAV, INFO, MORE }

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

    /** UX JMGO 25.11 (#63): ostatni klawisz (z ForkHooks.onGlobalKey) — którym klawiszem otwarto nakładkę. */
    @Volatile var lastKeyCode = 0
    @Volatile var lastKeyAt = 0L
    fun noteKey(e: KeyEvent) { if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) { lastKeyCode = e.keyCode; lastKeyAt = android.os.SystemClock.elapsedRealtime() } }
    /** Kanał i tryb, dla których stan fokusu był ostatnio zerowany (nakładka znika i wraca — bez zerowania). */
    var resetFor: String? = null
    /** #65: rząd to inne odcinki; start oglądanego programu (▼ stawia fokus na najbliższym odcinku). */
    var rowEpisodes = false
    var shownStart = 0L

    /** Z-4: program z archiwum — bez przełącznika kanałów (▲▼ tylko przenosi fokus, ◀ z pierwszego przycisku zostaje). */
    var archiveMode = false

    /** Przegląd 27.11, ekran 22: w archiwum klawisze zmiany kanału nic nie robią (CH±, cyfry). */
    private val ARCHIVE_NO_ZAP_KEYS = setOf(KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_LAST_CHANNEL) +
        (KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9).toSet() + (KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9).toSet()

    /**
     * Przegląd 27.11, ekran 22 (wołane z `ForkHooks.onGlobalKey`, przed Live TV): archiwum przy schowanej nakładce —
     * ▲▼, CH± i cyfry nie zmieniają kanału (Reshaped przełączał); ▲▼ pokazuje nakładkę z fokusem na pasku przewijania.
     */
    fun archiveGlobalKey(e: KeyEvent): Boolean {
        if (!archiveMode || !NiuniaLive.active()) return false
        val st = NiuniaLive.state ?: return false
        if (!st.isCatchup()) return false
        val upDown = e.keyCode == KeyEvent.KEYCODE_DPAD_UP || e.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
        if (!(upDown && !st.infoOpen) && e.keyCode !in ARCHIVE_NO_ZAP_KEYS) return false
        if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) {
            Log.i(TAG, "iptv: archiwum — ${if (upDown) "▲▼ pokazuje nakładkę" else "klawisz kanału pominięty"} (bez zmiany kanału)")
            if (upDown) { level = 0; slot = NiuniaLiveSlot.BAR; st.showInfo() }
        }
        return true
    }

    /** Z-21 (P-31): poprzedni kanał (adres na liście) — przycisk „Poprzedni: TVP1” zamiast długiego Wstecz; OK przełącza tam i z powrotem. */
    var lastUrl: String? = null
    var prevUrl by mutableStateOf<String?>(null)
    // UX JMGO 26.11 (#67): poprzedni kanał trzymany jako obiekt — szukanie go na liście 59 tys. kanałów (state.zapList())
    // przy każdym rysowaniu nakładki zabierało 331 ms wątku UI przy wejściu w kanał.
    private var lastChannel: LiveTvChannel? = null
    private var prevCh: LiveTvChannel? = null
    fun noteChannel(ch: LiveTvChannel) {
        val url = ch.streamUrl
        if (lastUrl != null && lastUrl != url) { prevUrl = lastUrl; prevCh = lastChannel; Log.i(TAG, "iptv: poprzedni kanał zapamiętany") }
        lastUrl = url; lastChannel = ch
    }
    fun prevChannel(state: LiveTvPlayerState): LiveTvChannel? {
        val u = prevUrl ?: return null
        prevCh?.takeIf { it.streamUrl == u }?.let { return it }
        Log.i(TAG, "iptv: poprzedni kanał — szukam na liście")
        return (state.zapList().firstOrNull { it.streamUrl == u } ?: LiveTvRepository.uiState.value.channels.firstOrNull { it.streamUrl == u })?.also { prevCh = it }
    }

    fun reset(archive: Boolean) {
        // Przegląd 27.11, ekran 22 pkt 3: w archiwum fokus startowy na pasku — ◀▶ od razu przewija nagranie (jak w filmie).
        level = 0; slot = if (archive) NiuniaLiveSlot.BAR else NiuniaLiveSlot.CHANNEL; rowIndex = 0; section = 0; index = 0
        archiveMode = archive
        if (archive) Log.i(TAG, "archive: channel switch, channel logo and up/down arrows hidden")
    }

    private fun go(to: Int, why: String) {
        Log.i(TAG, "iptv: poziom $level → $to ($why, rząd=${row.size})")
        // UX JMGO 25.11 (#65): ▼ do rzędu odcinków — fokus na ostatnim odcinku przed oglądanym (najbliższym), nie na najstarszym.
        if (to == 1 && level == 0) rowIndex = 0 // przegląd 27.11 (ekran 18): poprzednie emisje od najnowszej
        level = to
        if (to == 2) { section = sections.indexOfFirst { it.count > 0 }.coerceAtLeast(0); index = 0 }
    }

    fun onKey(state: LiveTvPlayerState, event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        val code = event.keyCode
        val ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
        val back = code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE
        // UX JMGO 26.11 (#74): każdy klawisz przeszukiwał listę 59 tys. kanałów (dwa razy) — najpierw kanał rysowanej nakładki.
        val channel = lastChannel?.takeIf { it.streamUrl == state.currentListUrl }
            ?: state.zapList().firstOrNull { it.streamUrl == state.currentListUrl }
            ?: LiveTvRepository.uiState.value.channels.firstOrNull { it.streamUrl == state.currentListUrl }
            ?: return false
        if (!down) {
            if (back) {
                when (level) {
                    2 -> { go(if (row.isNotEmpty()) 1 else 0, "Wstecz"); state.showInfo() }
                    // UX JMGO 26.11 (#75): ze szczegółów do ukrycia nakładki trzeba było 3 × Wstecz — z rzędu Wstecz chowa ją od razu.
                    1 -> { go(0, "Wstecz z rzędu → schowana"); if (state.isPausedNow()) state.togglePause(); state.hideInfo() }
                    else -> {
                        // UX JMGO 25.11 (#55): Wstecz przy pauzie chował nakładkę, a obraz stał bez żadnego znaku pauzy („zawiesiło się?”).
                        // Teraz Wstecz przy pauzie wznawia (od miejsca pauzy) i chowa nakładkę; „Na żywo” zostaje przyciskiem.
                        if (state.isPausedNow()) { Log.i(TAG, "iptv: Wstecz przy pauzie → wznowienie i schowana nakładka"); state.togglePause() }
                        state.hideInfo()
                    }
                }
            }
            return true
        }
        if (back) return true
        if (event.repeatCount > 0 && ok) return true
        // Przegląd 27.11, ekran 22: w archiwum ⏪ ⏩ przewijają z każdego miejsca nakładki; CH± i cyfry nic nie zmieniają.
        if (archiveMode && (code == KeyEvent.KEYCODE_MEDIA_REWIND || code == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)) {
            if (event.repeatCount == 0) { Log.i(TAG, "iptv: archiwum — przewijanie ${if (code == KeyEvent.KEYCODE_MEDIA_REWIND) "⏪" else "⏩"}"); state.openScrub(channel, if (code == KeyEvent.KEYCODE_MEDIA_REWIND) -1 else 1, keepInfo = true) }
            return true
        }
        if (archiveMode && code in ARCHIVE_NO_ZAP_KEYS) { Log.i(TAG, "iptv: archiwum — klawisz kanału pominięty"); return true }
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
                    // UX JMGO 25.11 (#58): ▲ z najwyższej części szczegółów nic nie robiło (wyjście tylko Wstecz) — teraz wraca tam, skąd ▼ weszło.
                    code == KeyEvent.KEYCODE_DPAD_UP -> focusable.lastOrNull { it < section }?.let { section = it; index = index.coerceAtMost(sections[it].count - 1) }
                        ?: go(if (row.isNotEmpty()) 1 else 0, "▲ ze szczytu szczegółów")
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
                NiuniaLiveSlot.CHANNEL -> if (archiveMode) Log.i(TAG, "iptv: archiwum — ▲ nie zmienia kanału") else { ForgeMiniGuideNav.paused = false; state.zap(-1); Log.i(TAG, "iptv: kanał ▲") }
                NiuniaLiveSlot.BAR -> Unit
                else -> if (scrubbable) slot = NiuniaLiveSlot.BAR
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> when (slot) {
                NiuniaLiveSlot.CHANNEL -> if (archiveMode) Log.i(TAG, "iptv: archiwum — ▼ nie zmienia kanału") else { ForgeMiniGuideNav.paused = false; state.zap(1); Log.i(TAG, "iptv: kanał ▼") }
                NiuniaLiveSlot.BAR -> slot = buttons.firstOrNull() ?: NiuniaLiveSlot.CHANNEL
                else -> go(if (row.isNotEmpty()) 1 else 2, "▼ z przycisków")
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> when (slot) {
                NiuniaLiveSlot.BAR -> if (repeat == 0) state.openScrub(channel, -1, keepInfo = true)
                NiuniaLiveSlot.CHANNEL -> Unit
                else -> { val i = buttons.indexOf(slot); slot = if (i <= 0) (if (archiveMode) slot else NiuniaLiveSlot.CHANNEL) else buttons[i - 1] }
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
            NiuniaLiveSlot.PREV -> prevChannel(state)?.let { p -> ForgeMiniGuideNav.paused = false; Log.i(TAG, "iptv: Poprzedni → ${p.name}"); state.switchTo(p) }
                ?: Log.w(TAG, "iptv: Poprzedni — kanału nie ma już na liście")
            NiuniaLiveSlot.PAUSE -> state.togglePause()
            NiuniaLiveSlot.LIVE -> { ForgeMiniGuideNav.paused = false; Log.i(TAG, "iptv: Na żywo → ${channel.name}"); state.switchTo(channel, force = true) }
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

private val SEASON_TAIL = Regex("""\s\d{1,2}$""")
/**
 * UX JMGO 26.11 (#70, zdjęcia 209–210): przewodnik Nowej TV podaje „Świat według Bundych 3: Can't dance…” (serial, sezon:
 * odcinek) — rząd szukał tylko tego samego pełnego tytułu i miał 1 kafelek (powtórka tego samego odcinka). Klucz serialu:
 * tekst przed „:”, bez numeru sezonu na końcu; null, gdy tytuł nie ma takiej części.
 */
// #70b (26.11, zdjęcie 215): „360° Świat bez granic (3)” — numer w nawiasie („odc. 5”, S01E02) też rozbijał serial; klucz
// zawsze ze znormalizowanego tytułu (bez numerów odcinka i sezonu), tekst przed „:” gdy jest.
private fun seriesKey(title: String): String? =
    com.nuvio.tv.fork.tv.ShowIdentity.normalizeTitle(title.substringBefore(':')).replace(SEASON_TAIL, "").trim().takeIf { it.length >= 3 }

/**
 * Rząd pod odtwarzaczem. Przegląd ekranów 27.11, ekran 18 (właściciel): tylko to, co da się od razu obejrzeć z archiwum —
 *  (a) poprzednie emisje programu, który leci (najnowsze pierwsze; pamięć przewodnika + baza programu, dni archiwum kanału),
 *  (b) gdy ich nie ma — programy z archiwum z tej samej kategorii, z kanałów tego samego kraju co oglądany,
 *  (c) nic z archiwum — brak rzędu (bez kart „Wkrótce”, których nie da się włączyć).
 * Zwraca (rząd, czy to poprzednie emisje, nagłówek kategorii).
 */
private fun buildRow(ctx: android.content.Context, channel: LiveTvChannel, shown: LiveTvProgramme, nowMs: Long): Triple<List<NiuniaLiveTile>, Boolean, String?> {
    val series = seriesKey(shown.title)
    fun sameShow(p: LiveTvProgramme) = sameTitle(p, shown) || series != null && seriesKey(p.title) == series
    fun playable(ch: LiveTvChannel, p: LiveTvProgramme) =
        p.stopEpochMs <= nowMs && com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks.isPlayable(ch.catchup, p, nowMs)
    if (channel.catchup != null) {
        val eps = pastOf(ctx, channel, nowMs, maxDays = 7).filter { sameShow(it) && it.startEpochMs != shown.startEpochMs && playable(channel, it) }
            .distinctBy { it.startEpochMs }.sortedByDescending { it.startEpochMs }.take(12)
        Log.i(TAG, "iptv: rząd — poprzednie emisje w archiwum: ${eps.size}")
        if (eps.isNotEmpty()) return Triple(eps.map { NiuniaLiveTile(channel, it, false, true, false) }, true, null)
    } else Log.i(TAG, "iptv: rząd — kanał bez archiwum")
    val names = ForgeEpgCategories.programmePlaceNames(channel.guideKey, shown)
    val place = ForgeEpgCategories.programmePlace(channel.guideKey, shown)
    if (names == null || place == null) { Log.i(TAG, "iptv: rząd — program bez kategorii, brak rzędu"); return Triple(emptyList(), false, null) }
    val live = LiveTvRepository.uiState.value
    val providers = live.sources.map { it.name }.filter { it.isNotBlank() }
    val lists = com.nuvio.tv.fork.tv.guide.ExtraGuides.countryBySource
    fun country(ch: LiveTvChannel) = com.nuvio.tv.fork.tv.country.TvCountry.countryOf(ch, providers) { lists[it] }
    val mine = country(channel)
    fun inPlace(ch: LiveTvChannel, p: LiveTvProgramme?, leaf: Boolean) = p != null &&
        ForgeEpgCategories.programmePlace(ch.guideKey, p)?.let { it.branch == place.branch && (!leaf || it.leaf == place.leaf) } == true
    // Kanały z archiwum, z kraju oglądanego, na których teraz leci coś z tej kategorii (kanał informacyjny itp.); oglądany pierwszy.
    val pool = (sequenceOf(channel) + live.channels.asSequence().filter { it.catchup != null && it.streamUrl != channel.streamUrl })
        .filter { it.catchup != null && (mine == null || it === channel || country(it) == mine) }.take(3_000).toList()
    val leafFirst = place.leaf != null
    var useLeaf = leafFirst
    var chans = pool.filter { it === channel || inPlace(it, live.currentProgrammes[it.guideKey], useLeaf) }.take(12)
    if (chans.size <= 1 && leafFirst) { useLeaf = false; chans = pool.filter { it === channel || inPlace(it, live.currentProgrammes[it.guideKey], false) }.take(12) }
    val found = chans.flatMap { ch ->
        pastOf(ctx, ch, nowMs, maxDays = 2).filter { p ->
            playable(ch, p) && !(ch === channel && p.startEpochMs == shown.startEpochMs) &&
                (ForgeEpgCategories.programmePlace(ch.guideKey, p)?.let { it.branch == place.branch && (!useLeaf || it.leaf == place.leaf) }
                    ?: (ch !== channel)) // bez kategorii w przewodniku: kanał tej kategorii (np. informacyjny) — jego programy też
        }.sortedByDescending { it.startEpochMs }.take(3).map { ch to it }
    }.sortedByDescending { it.second.startEpochMs }.take(10)
    val title = if (useLeaf) names.second ?: names.first else names.first
    Log.i(TAG, "iptv: rząd — z archiwum, kategoria „$title”, kraj=${mine ?: "?"}: kanały=${chans.size}, programy=${found.size}")
    return Triple(found.map { (ch, p) -> NiuniaLiveTile(ch, p, false, true, false) }, false, title.takeIf { found.isNotEmpty() })
}

/** Minione programy kanału: przewodnik w pamięci (−12 h) + baza programu (dni archiwum kanału, najwyżej [maxDays]). */
private fun pastOf(ctx: android.content.Context, ch: LiveTvChannel, nowMs: Long, maxDays: Int): List<LiveTvProgramme> {
    val mem = LiveTvRepository.schedule(ch.guideKey).filter { it.stopEpochMs <= nowMs }
    val days = (ch.catchup?.days ?: 0).coerceIn(0, maxDays)
    if (days == 0) return mem
    val db = com.nuvio.tv.fork.tv.plus.GuideDbBridge.history(ctx, ch.guideKey, nowMs - days * 86_400_000L).orEmpty().filter { it.stopEpochMs <= nowMs }
    return (mem + db).distinctBy { it.startEpochMs }
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
    LaunchedEffect(channel.guideKey) { com.nuvio.tv.fork.tv.plus.GuideDbBridge.need(context, channel, 0L); com.nuvio.tv.fork.tv.plus.GuideDbBridge.want(context, listOf(channel.guideKey)) }
    val dbRows by com.nuvio.tv.fork.tv.plus.GuideDbBridge.version.collectAsState()
    // Odczyt [dbRows] w kompozycji = przeliczenie po paczce wierszy z bazy (jak dotąd: schedule() przy każdej kompozycji).
    val schedule = if (dbRows >= 0) LiveTvRepository.schedule(channel.guideKey) else emptyList()
    val scrubProgramme = scrubMs?.let { t -> schedule.firstOrNull { t >= it.startEpochMs && t < it.stopEpochMs } }
    val shown = scrubProgramme ?: replay ?: now
    val archive = replay != null
    val shiftMs = if (!archive) ForgeMiniGuideNav.shiftMs() else 0L
    val playState = when { archive -> NiuniaPlayState.ARCHIVE; shiftMs > 0L -> NiuniaPlayState.DELAY; else -> NiuniaPlayState.LIVE }
    // UX JMGO 25.11 (#63): nakładka znika i wraca (komponent od nowa) — zerowanie fokusu tylko przy zmianie kanału/archiwum.
    // Wcześniej ► przy schowanej nakładce pokazywał ją z fokusem na przełączniku kanałów, a następne ▼ zmieniało kanał
    // zamiast zejść do rzędu odcinków. Teraz ► (pokazanie nakładki) od razu stawia fokus na pierwszym przycisku.
    LaunchedEffect(channel.streamUrl, archive) {
        val key = channel.streamUrl + "|" + archive
        // IPTV #85 (JMGO uxr17, 26.11): kanał włączony OK z listy — to samo OK (≤ 1,5 s) i obraz jeszcze bez odtwarzania
        // (bufor = „nie gra”) dawały „nakładka otwarta OK (pauza) — fokus na ▶” zamiast przełącznika kanałów (▲▼ nie zmieniały
        // kanału). Fokus na ▶ tylko dla OK na tym samym kanale (pauza na gołym obrazie, #80).
        val fresh = nav.resetFor != key
        if (fresh) { nav.reset(archive); nav.resetFor = key }
        val byRight = nav.lastKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT && android.os.SystemClock.elapsedRealtime() - nav.lastKeyAt < 1_500
        if (!archive && byRight && nav.level == 0 && nav.slot == NiuniaLiveSlot.CHANNEL) {
            kotlinx.coroutines.delay(50) // przyciski publikuje SideEffect
            nav.buttons.firstOrNull()?.let { nav.slot = it; Log.i(TAG, "iptv: nakładka otwarta ► — fokus na $it") }
        }
        val byOk = (nav.lastKeyCode == KeyEvent.KEYCODE_DPAD_CENTER || nav.lastKeyCode == KeyEvent.KEYCODE_ENTER) &&
            android.os.SystemClock.elapsedRealtime() - nav.lastKeyAt < 1_500
        if (byOk && !fresh && nav.level == 0 && nav.slot != NiuniaLiveSlot.PAUSE) {
            kotlinx.coroutines.delay(50) // przyciski publikuje SideEffect
            if (state?.isPausedNow() == true && NiuniaLiveSlot.PAUSE in nav.buttons) { nav.slot = NiuniaLiveSlot.PAUSE; Log.i(TAG, "iptv: nakładka otwarta OK (pauza) — fokus na ▶") }
        }
        nav.noteChannel(channel)
        Log.i(TAG, "iptv: nakładka ${channel.name} (stan=$playState, program=${shown != null})")
    }
    // Z-21 (P-28): kanał gra kilka sekund → znacznik „Niedostępny” zdjęty.
    LaunchedEffect(channel.streamUrl) {
        kotlinx.coroutines.delay(8_000)
        if (com.nuvio.tv.skin.kicia.livetv.ForgeChannelFailed.playingBehind()) com.nuvio.tv.skin.niunia.livetv.NiuniaLiveStatus.ok(context, channel.streamUrl, channel.name)
    }
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
            try { buildRow(context, channel, shown, LiveTvClock.nowEpochMs()) } catch (e: Exception) {
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
    val prev = if (!archive && state != null) nav.prevUrl?.takeIf { it != channel.streamUrl }?.let { nav.prevChannel(state) } else null
    val buttons = buildList {
        if (prev != null) add(NiuniaLiveSlot.PREV) // Z-21 (P-31): pierwszy nad paskiem, dopiero po zmianie kanału, nie w archiwum
        if (startOver || archive) add(NiuniaLiveSlot.RESTART)
        if (archive || group || paused) add(NiuniaLiveSlot.PAUSE)
        // UX JMGO 26.11 (#72) dodało „Na żywo” w archiwum; przegląd 27.11 (właściciel, ekran 22) odwraca: w archiwum bez „Na żywo”,
        // wyjście tylko Wstecz. „Na żywo” zostaje przy opóźnieniu transmisji.
        if (playState == NiuniaPlayState.DELAY && !archive) add(NiuniaLiveSlot.LIVE)
        add(NiuniaLiveSlot.FAV)
        if (group) { add(NiuniaLiveSlot.INFO); add(NiuniaLiveSlot.MORE) }
    }
    SideEffect { nav.buttons = buttons; nav.row = row; nav.rowEpisodes = episodesRow; nav.shownStart = shown?.startEpochMs ?: 0L }
    val level = nav.level
    val focusSlot = if (level == 0) nav.slot else null

    Box(Modifier.fillMaxSize()) {
        if (level < 2) {
            NiuniaPlayerScrim(from = 0.34f)
            // ▼: przyciski w górę (canvas Group: pasek 368 → 244), rząd 502 → 408 z tytułem nad nim.
            val lift by animateDpAsState(if (level >= 1) (-124).dp else 0.dp, label = "niuniaLiveLift")
            val rowLift by animateDpAsState(if (level >= 1) (-94).dp else 0.dp, label = "niuniaLiveRowLift")
            // Z-3 (NPlayerControls kind live/delay/archive): rząd 1 = przełącznik kanałów + informacje + przyciski (do dołu,
            // odstęp 24), rząd 2 = pasek postępu (odstęp 16) — jak w odtwarzaczu filmu. Rząd programów wystaje pod paskiem.
            Box(Modifier.fillMaxSize().offset(y = lift)) {
                Column(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, end = 40.dp, bottom = if (row.isNotEmpty()) 76.dp else 46.dp).fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Z-4: w archiwum bez przełącznika kanałów.
                        if (!archive) ChannelSwitch(channel, logo, focused = focusSlot == NiuniaLiveSlot.CHANNEL, modifier = Modifier)
                        Column(Modifier.weight(1f)) {
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
                                com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name),
                                if (playState == NiuniaPlayState.DELAY) stringResource(R.string.niunia_live_still_live) else null,
                            ).joinToString(" · ")
                            // UX JMGO 26.11 (zdjęcie 213): przy 4 przyciskach linia była ucięta w pół słowa („· na”) — wielokropek.
                            Text(line, style = NiuniaText.Button.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp), color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        // UX JMGO 25.11 (#57, zdjęcie 196): długi „Poprzedni: Strong8K · AR: …” zabierał całą szerokość — tytuł, godziny
                        // i stan znikały. Przyciski najwyżej 520 dp, „Poprzedni” najwyżej 180 dp z krótką nazwą kanału (520/180: 5 przycisków mieszczą się, zdjęcie 204).
                        Row(Modifier.widthIn(max = 520.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val left = buttons.filter { it == NiuniaLiveSlot.PREV || it == NiuniaLiveSlot.RESTART || it == NiuniaLiveSlot.PAUSE || it == NiuniaLiveSlot.LIVE }
                            val right = buttons.filter { it == NiuniaLiveSlot.FAV || it == NiuniaLiveSlot.INFO || it == NiuniaLiveSlot.MORE }
                            left.forEach { s ->
                                when (s) {
                                    NiuniaLiveSlot.PREV -> NiuniaPill(stringResource(R.string.niunia_ke2_prev_channel, com.nuvio.tv.fork.tv.names.ChannelNames.shown(prev?.name.orEmpty())), focusSlot == s,
                                        icon = Icons.Outlined.History, height = 36.dp, modifier = Modifier.widthIn(max = 180.dp))
                                    NiuniaLiveSlot.RESTART -> NiuniaIconCircle(Icons.Outlined.Replay, stringResource(R.string.niunia_live_restart), focusSlot == s)
                                    NiuniaLiveSlot.PAUSE -> NiuniaIconCircle(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                        stringResource(if (paused) R.string.niunia_live_play else R.string.niunia_live_pause), focusSlot == s)
                                    else -> NiuniaPill(stringResource(R.string.niunia_live_back_live), focusSlot == s, height = 36.dp, dot = NiuniaStatus.Live)
                                }
                            }
                            // Pionowy separator (`divider`) między odtwarzaniem a programem.
                            if (left.isNotEmpty() && right.isNotEmpty()) Box(Modifier.width(1.dp).height(24.dp).background(NiuniaColors.Line))
                            right.forEach { s ->
                                val (icon, label) = when (s) {
                                    NiuniaLiveSlot.FAV -> (if (favorite) Icons.Outlined.Check else Icons.Outlined.Add) to stringResource(R.string.niunia_live_fav)
                                    NiuniaLiveSlot.INFO -> Icons.Outlined.Info to stringResource(R.string.niunia_live_info)
                                    else -> Icons.Outlined.MoreHoriz to stringResource(R.string.niunia_live_more)
                                }
                                NiuniaIconCircle(icon, label, focusSlot == s)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LiveBar(shown, playState, clockMs, playheadMs, shiftMs, scrubMs, focused = focusSlot == NiuniaLiveSlot.BAR, modifier = Modifier)
                }
            }
            // Rząd (.rail y 502; ▼ → 408, w grupie z tytułem kategorii nad nim).
            if (row.isNotEmpty()) {
                Column(Modifier.offset(y = 472.dp + rowLift)) {
                    val titleText = when {
                        episodesRow -> stringResource(R.string.niunia_review_live_prev_episodes)
                        groupTitle != null -> stringResource(R.string.niunia_review_live_from_archive, groupTitle)
                        else -> null
                    }
                    Row(Modifier.padding(start = NiuniaDims.Gutter).height(30.dp), verticalAlignment = Alignment.Top) {
                        if (titleText != null && level >= 1) NiuniaShelfTitle(titleText, on = true)
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
                genre = (tvdb?.genres?.takeIf { it.isNotEmpty() } ?: tm?.genres?.takeIf { it.isNotEmpty() })?.take(2)?.mapNotNull { com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(it) }?.distinct()?.joinToString(" · ")?.ifBlank { null }
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.niunia_live_prev), tint = tint, modifier = Modifier.size(26.dp))
        // Przegląd 27.11 (ekran 19, właściciel): logo na całą szerokość kafla (było 32 wysokości, z lewej), wyśrodkowane;
        // bez logo — nazwa kanału w 2 liniach.
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            com.nuvio.tv.skin.niunia.livetv.NiuniaChannelLogo(channel, Modifier.fillMaxWidth().height(56.dp), NiuniaText.Logo.copy(fontSize = 19.sp, lineHeight = 22.sp), Alignment.Center, nameLines = 2) // Paczka „oczywiste” pkt 5

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
                // UX JMGO 25.11 (#54): „Nowa TV · 21:00” dwa razy (wczoraj i dziś) — przy innym dniu niż dziś dopisany dzień.
                fallbackTitle = t.programme.title, fallbackSub = com.nuvio.tv.fork.tv.names.ChannelNames.shown(t.channel.name) + " · " +
                    (if (dayStart(t.programme.startEpochMs) == dayStart(clockMs)) "" else dayLabel(ctx, t.programme.startEpochMs, clockMs).lowercase() + " ") + hm(t.programme.startEpochMs),
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
