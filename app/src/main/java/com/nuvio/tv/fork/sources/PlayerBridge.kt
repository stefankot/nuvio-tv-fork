package com.nuvio.tv.fork.sources

import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

/** Most do odtwarzacza (B04-8, B07): ostatni stan i wysyłanie zdarzeń; działa tylko, gdy ekran odtwarzacza jest otwarty. */
object PlayerBridge {
    @Volatile private var state: PlayerUiState? = null
    @Volatile private var send: ((PlayerEvent) -> Unit)? = null
    private var queue: SubtitleQueue? = null
    private var queueKey: List<String> = emptyList()

    private var autoTried = false
    // Pomiar opóźnienia startu (znacznik logcat „ForkPerf”): od ustawienia adresu strumienia i od ostatniego klawisza do pierwszego odtwarzania.
    private var perfUrl: String? = null; private var perfStart = 0L; private var perfDone = true
    @Volatile var lastKeyMs = 0L
    private fun perf(s: PlayerUiState) {
        val u = s.currentStreamUrl
        if (u != perfUrl) { perfUrl = u; perfStart = android.os.SystemClock.elapsedRealtime(); perfDone = u == null }
        if (!perfDone && s.isPlaying && !s.showLoadingOverlay && !s.isBuffering) {
            perfDone = true
            val now = android.os.SystemClock.elapsedRealtime()
            val host = runCatching { android.net.Uri.parse(u).host }.getOrNull()
            android.util.Log.i("ForkPerf", "first_play start_to_play_ms=${now - perfStart} key_to_play_ms=${if (lastKeyMs > 0) now - lastKeyMs else -1} host=$host")
        }
    }
    fun set(s: PlayerUiState, e: (PlayerEvent) -> Unit) { state = s; send = e; perf(s) }

    /** B09-6: lektor sam, gdy profil chce polskiego, brak polskiej ścieżki dźwięku, są polskie napisy i włączono „lektor_auto”. Raz na odtwarzanie. */
    fun maybeAutoLektor(c: android.content.Context) {
        val s = state ?: return
        if (autoTried || s.audioTracks.isEmpty() || !s.isPlaying) return
        if (!com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "lektor_auto", false)) return
        autoTried = true
        val hasPlAudio = AudioCheck.hasPolish(s.audioTracks.map { it.language })
        val hasPlSubs = s.addonSubtitles.any { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }
        val wantsPl = SourceChoice.answer(c).let { it == "pl_always" || it == "pl_if_good" }
        if (com.nuvio.tv.fork.lektor.LektorRules.step(wantsPl, hasPlAudio, hasPlSubs, true) == com.nuvio.tv.fork.lektor.LektorRules.Step.LEKTOR) com.nuvio.tv.fork.lektor.LektorController.start(c)
    }
    fun position(posMs: Long, playing: Boolean) = com.nuvio.tv.fork.lektor.LektorEngine.updatePosition(posMs, playing)
    fun clear() { state = null; send = null; queue = null; autoTried = false }
    val active: Boolean get() = send != null
    fun send(e: PlayerEvent) { send?.invoke(e) }
    /** Lektor czyta napisy z synchronizacji: wybierz polskie napisy z dodatku, jeśli żadne nie jest wybrane. */
    fun ensurePolishSubtitle() {
        val s = state ?: return
        if (s.selectedAddonSubtitle != null) return
        s.addonSubtitles.firstOrNull { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }?.let { send?.invoke(PlayerEvent.OnSelectAddonSubtitle(it)) }
    }
    fun streamUrl(): String? = state?.currentStreamUrl
    fun positionMs(): Long = com.nuvio.tv.fork.lektor.LektorEngine.positionMs()
    fun cues() = state?.subtitleAutoSyncCues.orEmpty()

    /** „Napisy nie pasują”: następny plik napisów w tym samym języku i ponowna synchronizacja. Zwraca komunikat dla użytkownika albo null (brak odtwarzacza). */
    fun subtitleMismatch(): String? {
        val s = state ?: return null
        val emit = send ?: return null
        val current = s.selectedAddonSubtitle
        val lang = current?.lang ?: s.addonSubtitles.firstOrNull { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }?.lang ?: s.addonSubtitles.firstOrNull()?.lang
        val same = s.addonSubtitles.filter { it.lang == lang }
        if (same.isEmpty()) return "Brak innych napisów"
        val ids = same.map { it.id }
        if (queue == null || queueKey != ids) { queue = SubtitleQueue(ids); queueKey = ids }
        val q = queue!!
        val nextId = q.next() ?: return "Brak innych napisów"
        val next = same.firstOrNull { it.id == nextId } ?: return "Brak innych napisów"
        emit(PlayerEvent.OnSelectAddonSubtitle(next))
        emit(PlayerEvent.OnReloadSubtitleAutoSyncCues)
        return when (q.suggestion(lektorReady = false)) {
            SubtitleQueue.Suggestion.SUBTITLES_OFF -> "Trzecia próba: może wyłączyć napisy?"
            else -> "Inne napisy: ${next.addonName}"
        }
    }
}
