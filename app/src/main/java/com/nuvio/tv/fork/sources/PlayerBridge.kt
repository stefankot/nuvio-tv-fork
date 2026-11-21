package com.nuvio.tv.fork.sources

import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.fork.logged

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
            val host = runCatching { android.net.Uri.parse(u).host }.logged("ForkSources", "PlayerBridge.kt:23").getOrNull()
            android.util.Log.i("ForkPerf", "first_play start_to_play_ms=${now - perfStart} key_to_play_ms=${if (lastKeyMs > 0) now - lastKeyMs else -1} host=$host")
            com.nuvio.tv.fork.family.FamilyStats.play(now - perfStart, s.currentStreamName)
        }
    }
    private var lastError: String? = null
    fun set(s: PlayerUiState, e: (PlayerEvent) -> Unit) {
        state = s; send = e; perf(s); TryAnotherSource.onState(s)
        com.nuvio.tv.fork.subs.PolishFirst.onState(appContext, s) { ev -> send(ev) } // pkt 7: polskie napisy, wbudowane przed dodatkami
        com.nuvio.tv.fork.diag.PlaybackDiag.onState(appContext, s) // diagnostyka testów (ForkDiag)
        com.nuvio.tv.fork.family.FamilyUsage.player(s.selectedSubtitleTrackIndex, s.selectedAudioTrackIndex, s.playbackSpeed, s.currentStreamUrl, s.isPlaying)
        if (s.error != null && s.error != lastError) com.nuvio.tv.fork.family.FamilyStats.playError(s.error, s.currentStreamName) // panel Rodzina
        lastError = s.error
    }
    fun current(): PlayerUiState? = state
    /** Kontekst aplikacji do komunikatów modułów (ustawiany przez ForkHooks.playerBridge). */
    @Volatile var appContext: android.content.Context? = null

    /** B09-6: lektor sam, gdy profil chce polskiego, brak polskiej ścieżki dźwięku, są polskie napisy i włączono „lektor_auto”. Raz na odtwarzanie. */
    fun maybeAutoLektor(c: android.content.Context) {
        val s = state ?: return
        if (autoTried || s.audioTracks.isEmpty() || !s.isPlaying) return
        if (!com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "lektor_auto", false)) return
        autoTried = true
        val hasPlAudio = AudioCheck.hasPolish(s.audioTracks.map { it.language })
        val hasPlSubs = s.addonSubtitles.any { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }
        val wantsPl = SourceChoice.answer(c).let { it == "pl_always" || it == "pl_if_good" }
        if (com.nuvio.tv.fork.lektor.LektorRules.step(wantsPl, hasPlAudio, hasPlSubs, true) != com.nuvio.tv.fork.lektor.LektorRules.Step.LEKTOR) return
        // P3/P9: bez silnika mowy z polskim głosem lektor byłby niemy (JMGO 18.11) — zamiast włączać, powiedz, co zrobić.
        if (!com.nuvio.tv.fork.lektor.LektorVoice.usable(c)) {
            // Test JMGO 18.11: przy zmianie źródła komunikat pokazywał się dwa razy — raz na uruchomienie aplikacji.
            if (com.nuvio.tv.fork.lektor.LektorVoice.notified) return
            com.nuvio.tv.fork.lektor.LektorVoice.notified = true
            android.widget.Toast.makeText(c.applicationContext, com.nuvio.tv.fork.lektor.LektorVoice.MISSING_MSG, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        com.nuvio.tv.fork.lektor.LektorController.start(c)
    }
    fun position(posMs: Long, playing: Boolean) = com.nuvio.tv.fork.lektor.LektorEngine.updatePosition(posMs, playing)
    fun clear() { state = null; send = null; queue = null; autoTried = false; TryAnotherSource.reset(); com.nuvio.tv.fork.diag.PlaybackDiag.reset() }
    val active: Boolean get() = send != null
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    /** Zdarzenia do odtwarzacza zawsze na wątku głównym (ExoPlayer: „Player is accessed on the wrong thread” — awaria lektora, JMGO 18.11). */
    fun send(e: PlayerEvent) {
        val f = send ?: return
        com.nuvio.tv.fork.family.FamilyUsage.feature("funkcja_odtwarzacza:" + e::class.simpleName)
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) f(e) else main.post { send?.invoke(e) }
    }
    /** Lektor czyta napisy z synchronizacji: wybierz polskie napisy z dodatku, jeśli żadne nie jest wybrane. */
    fun ensurePolishSubtitle() {
        val s = state ?: return
        // Pkt 7 (21.11): polskie wbudowane mają pierwszeństwo — lektor czyta je wprost ([com.nuvio.tv.fork.lektor.LektorSources]).
        if (s.subtitleTracks.any { com.nuvio.tv.fork.subs.PolishFirst.isPl(it.language) && !it.isForced }) return
        // Pkt 7: wybrane obce napisy z dodatku (np. włoskie) nie blokują już polskich.
        if (s.selectedAddonSubtitle != null && com.nuvio.tv.fork.subs.PolishFirst.isPl(s.selectedAddonSubtitle.lang)) return
        s.addonSubtitles.firstOrNull { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }?.let {
            send?.invoke(PlayerEvent.OnSelectAddonSubtitle(it))
            // Zgłoszenie 5 (19.11, „start filmu otwiera ekran napisów”): bazowe OnSelectAddonSubtitle zawsze pokazuje okno
            // napisów; wybór automatyczny (lektor) ma je zostawić zamknięte.
            if (!s.showSubtitleOverlay) send?.invoke(PlayerEvent.OnDismissTransientOverlay)
            android.util.Log.i("ForkLektor", "auto napisy PL: ${it.addonName} (okno napisów zamknięte)")
        }
    }
    fun streamUrl(): String? = state?.currentStreamUrl
    /** Fork 19.11 (lektor): czy dodatki dały polskie napisy. */
    fun hasPolishAddonSubtitle(): Boolean = state?.addonSubtitles.orEmpty().any { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }
    fun positionMs(): Long = com.nuvio.tv.fork.lektor.LektorEngine.positionMs()
    fun cues() = state?.subtitleAutoSyncCues.orEmpty()

    /** „Napisy nie pasują”: następny plik napisów w tym samym języku i ponowna synchronizacja. Zwraca komunikat dla użytkownika albo null (brak odtwarzacza). */
    fun subtitleMismatch(): String? {
        val s = state ?: return null
        if (send == null) return null
        val emit: (PlayerEvent) -> Unit = { send(it) }
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
