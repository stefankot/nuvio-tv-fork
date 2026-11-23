package com.nuvio.tv.fork.ai.subtitles

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.fork.sources.PlayerBridge
import com.nuvio.tv.ui.screens.player.autosync.AutoSyncState
import com.nuvio.tv.ui.screens.player.autosync.AutoSyncSyncedSubtitle
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fork 20.11 (canvas `PlayerSubs`, `Settings`): „Napisy AI, gdy brak dobrych polskich”.
 * 23.11 (właściciel): „najpierw auto-wyszukiwanie najlepszych napisów z Reshaped próbuje wszystkiego i jeśli nie ma napisów
 * polskich, to wtedy automatycznie włącza się tłumaczenie AI, nie na środku ekranu, tylko dyskretnie jako komunikat, i informacja,
 * że naciśnięcie przycisku anuluje tłumaczenie”. Dlatego: domyślnie WŁĄCZONE (nowy klucz [K_AUTO]; stary `ai_subs_auto_pl` zostaje
 * nieużywany), czeka, aż dodatki się wczytają i AutoSync skończy szukać ([AutoSyncState.IN_PROGRESS]) + [SETTLE_MS] spokoju,
 * zamiast okna [AiOffer] dymek w rogu ([AiAutoBubble]); Wstecz w czasie dymka ([onKey]) anuluje i przywraca napisy sprzed AI.
 * Ręczny wybór innej ścieżki ([manualPick]) wyłącza automat dla tego filmu. Wyłączony przełącznik = dawne okno z pytaniem.
 */
object AiAuto {
    private const val TAG = "ForkAiSubtitles"
    const val K_AUTO = "ai_subs_auto_pl2"
    /** Po zakończeniu szukania napisów: chwila na wybory Reshaped/PolishFirst (tick co 2 s). */
    const val SETTLE_MS = 4_000L
    /** Tyle widać dymek i tyle działa Wstecz = anuluj. */
    const val BUBBLE_MS = 12_000L

    fun enabled(c: Context) = ForkFeatures.enabled(c, K_AUTO, true)

    fun set(c: Context, on: Boolean) {
        ForkFeatures.set(c, K_AUTO, on)
        Log.i(TAG, "ustawienia: napisy AI, gdy brak dobrych polskich = ${if (on) "tak" else "nie"}")
    }

    private var film: String? = null
    private var reportedUnavailable: String? = null
    private var readySince = 0L
    @Volatile private var cancellableUntil = 0L
    @Volatile private var backDownTaken = false

    /** Napisy AI włączone automatycznie dla bieżącego filmu (panel pokazuje znacznik AUTO). */
    @Volatile var autoOn: Boolean = false
        private set

    /** Użytkownik wybrał ścieżkę ręcznie — automat nie wraca do końca tego filmu. */
    @Volatile var manual: Boolean = false
        private set

    /** Tekst dymka w rogu (null = brak). */
    val bubble = MutableStateFlow<String?>(null)

    fun isPl(lang: String?): Boolean = lang?.let { AuxiliaryLanguagePolicy.primaryLanguage(it) == "pl" } == true

    /** Napisy AI działają albo są gotowe dla bieżącego filmu. */
    fun active(): Boolean = when (AiSubtitles.state.value?.stage) {
        AiStage.SOURCES, AiStage.TRANSLATING, AiStage.WAITING, AiStage.DONE -> true
        else -> false
    }

    /** Dlaczego polskie napisy nie nadają się (null = są dobre). [autoSyncNoFit] = AutoSync nie dopasował wybranego polskiego pliku. */
    fun polishProblem(s: PlayerUiState, autoSyncNoFit: Boolean = false): String? {
        val anyPl = s.addonSubtitles.any { isPl(it.lang) } || s.subtitleTracks.any { isPl(it.language) }
        if (!anyPl) return "brak polskich napisów"
        val sel = s.selectedAddonSubtitle
        if (sel != null && isPl(sel.lang) && (autoSyncNoFit || !s.subtitleAutoSyncError.isNullOrBlank())) return "AutoSync nie dopasował polskiego pliku"
        return null
    }

    /** Dodatki jeszcze się wczytują albo AutoSync (Reshaped) jeszcze szuka pasującego pliku. */
    internal fun stillSearching(s: PlayerUiState, autoSync: AutoSyncState?): Boolean = s.isLoadingAddonSubtitles || autoSync == AutoSyncState.IN_PROGRESS

    private fun filmKey(s: PlayerUiState): String? = s.currentStreamUrl ?: s.title.takeIf { it.isNotBlank() }

    /** Wywoływane co 2 s w czasie odtwarzania. true = automat zadziałał albo czeka (okno [AiOffer] niepotrzebne). */
    fun tick(c: Context): Boolean {
        val s = PlayerBridge.current() ?: return false
        val key = filmKey(s) ?: return false
        if (key != film) { film = key; autoOn = false; manual = false; reportedUnavailable = null; readySince = 0L }
        if (!enabled(c)) return false
        if (manual || autoOn || active() || AiSubtitles.running) return true
        if (com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry.isLiveTv(s.currentStreamUrl)) return true // IPTV bez napisów AI
        val sync = AutoSyncSyncedSubtitle.status.value
        if (!s.isPlaying || stillSearching(s, sync?.state)) { readySince = 0L; return true }
        if (s.addonSubtitles.isEmpty() && s.subtitleTracks.isEmpty()) return true // jeszcze nic nie wiadomo albo brak źródła tekstu
        val noFit = s.selectedAddonSubtitle?.let { sel -> sync != null && sync.url == sel.url && sync.state == AutoSyncState.NO_FIT } == true
        val why = polishProblem(s, noFit) ?: run { readySince = 0L; return true }
        val now = android.os.SystemClock.elapsedRealtime()
        if (readySince == 0L) { readySince = now; return true }
        if (now - readySince < SETTLE_MS) return true
        if (!ForkFeatures.enabled(c, "module_ai_subtitles", true)) return true
        AiSubtitles.unavailableReason(c)?.let { r ->
            if (reportedUnavailable != r) { reportedUnavailable = r; Log.w(TAG, "napisy AI auto: nie można włączyć ($why): $r") }
            return true
        }
        val msg = AiSubtitles.start(c, AiMode.WATCH)
        autoOn = true
        cancellableUntil = now + BUBBLE_MS
        Log.i(TAG, "napisy AI auto: włączone same ($why; dodatki=${s.addonSubtitles.size}, wbudowane=${s.subtitleTracks.size}, AutoSync=${sync?.state ?: "-"}) — $msg")
        bubble.value = (if (why.startsWith("brak")) "Brak polskich napisów" else "Polskie napisy nie pasują do filmu") +
            " — tłumaczę je na polski (AI). Naciśnij Wstecz, aby anulować"
        return true
    }

    /**
     * Fork hook (ForkHooks.onGlobalKey): Wstecz, póki widać dymek automatu, anuluje tłumaczenie i przywraca napisy sprzed AI.
     * true = klawisz zużyty (nie zamyka odtwarzacza).
     */
    fun onKey(event: android.view.KeyEvent): Boolean {
        if (event.keyCode != android.view.KeyEvent.KEYCODE_BACK) return false
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            if (!autoOn || android.os.SystemClock.elapsedRealtime() > cancellableUntil) return false
            backDownTaken = true
            return true
        }
        if (event.action == android.view.KeyEvent.ACTION_UP && backDownTaken) {
            backDownTaken = false
            cancelAuto()
            return true
        }
        return false
    }

    private fun cancelAuto() {
        cancellableUntil = 0L
        manual = true
        autoOn = false
        val msg = AiSubtitles.cancelAndRestore("Wstecz po automatycznym włączeniu")
        bubble.value = "$msg — wróciły poprzednie napisy"
        Log.i(TAG, "napisy AI auto: anulowane klawiszem Wstecz — automat wyłączony dla tego filmu")
    }

    /** Ręczny wybór ścieżki napisów: koniec automatu dla tego filmu (i wyłączenie Napisów AI, jeśli działały). */
    fun manualPick(what: String) {
        manual = true
        if (autoOn || active() || AiSubtitles.running) AiSubtitles.stop("Wybrano inne napisy (postęp zapisany)")
        autoOn = false
        Log.i(TAG, "napisy AI auto: ręczny wybór ($what) — automat wyłączony dla tego filmu")
    }

    /** Ręczne włączenie Napisów AI z panelu. */
    fun manualStart(c: Context): String {
        manual = true
        val msg = AiSubtitles.start(c, AiMode.WATCH)
        Log.i(TAG, "napisy AI: włączone ręcznie — $msg")
        return msg
    }

    /** Ile jeszcze ma być widać dymek (ms): czas na Wstecz albo krótkie potwierdzenie. */
    internal fun bubbleMs(): Long = (cancellableUntil - android.os.SystemClock.elapsedRealtime()).takeIf { it > 0 } ?: 4_000L
}

/** Dymek w lewym górnym rogu (canvas `PlayerSubs`: .frost, 40/28, max 420 szer.): znika po 12 s (czas na Wstecz) albo 4 s. */
@Composable
fun AiAutoBubble() {
    val text by AiAuto.bubble.collectAsState()
    LaunchedEffect(text) { if (text != null) { delay(AiAuto.bubbleMs()); AiAuto.bubble.value = null } }
    val t = text ?: return
    Box(Modifier.fillMaxSize()) {
        val shape = RoundedCornerShape(16.dp)
        Row(
            Modifier
                .padding(start = 40.dp, top = 28.dp)
                .widthIn(max = 420.dp)
                .background(Color(0xD93A3A3C), shape)
                .border(1.dp, Color(0x0FFFFFFF), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val chip = RoundedCornerShape(7.dp)
            Box(Modifier.height(20.dp).background(Color(0x59000000), chip).border(1.dp, Color(0x59FFFFFF), chip).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Text("AI", fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            }
            Text(t, fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
        }
    }
}
