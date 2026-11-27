package com.nuvio.tv.fork.subs

import android.content.Context
import android.util.Log
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Paczka „oczywiste” pkt 7 (21.11, „Beverly Hills Chihuahua”: wybrane włoskie napisy mimo polskich, lektor na zmianę PL/IT).
 * Stała decyzja (ZASADY §7): napisy wbudowane przed napisami z dodatków, polskie przed innymi. Raz na start filmu
 * (pierwsze [WINDOW_MS] odtwarzania — potem wybór użytkownika jest święty): gdy profil chce polskich napisów, a wybrane są
 * obce (albo polskie z dodatku, choć plik ma polskie wbudowane) — przełącza na polskie wbudowane, a bez nich na polskie z dodatku.
 * Nic nie wybrane = nic nie włącza. IPTV pomijane.
 */
object PolishFirst {
    const val KEY = "subs_polish_first"
    /** Poprawka istniejącej funkcji (27.11): wyłącznik tylko kluczem ForkFeatures, bez pozycji w ustawieniach. */
    const val EMBEDDED_KEY = "subs_embedded_first"
    private const val TAG = "ForkSubs"
    private const val WINDOW_MS = 90_000L

    private var url: String? = null
    private var since = 0L
    private var done = false

    fun isPl(lang: String?): Boolean = lang != null && (lang.startsWith("pl", true) || lang.startsWith("pol", true))

    private fun wantsPolish(c: Context, s: PlayerUiState): Boolean =
        isPl(s.subtitleStyle.preferredLanguage) ||
            com.nuvio.tv.fork.sources.SourceChoice.answer(c).let { it == "pl_always" || it == "pl_if_good" }

    /** Wołane z [com.nuvio.tv.fork.sources.PlayerBridge.set] przy każdej zmianie stanu odtwarzacza. */
    fun onState(c: Context?, s: PlayerUiState, send: (PlayerEvent) -> Unit) {
        val u = s.currentStreamUrl ?: return
        if (u != url) { url = u; since = android.os.SystemClock.elapsedRealtime(); done = false }
        if (done || c == null || !s.isPlaying) return
        if (!ForkFeatures.enabled(c, KEY, true)) { done = true; return }
        if (com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry.isLiveTv(u)) { done = true; return }
        if (android.os.SystemClock.elapsedRealtime() - since > WINDOW_MS) { done = true; return }
        val addon = s.selectedAddonSubtitle
        // 27.11 (właściciel, przegląd): domyślnie napisy WBUDOWANE (pewna jakość), nie z plików zewnętrznych — w każdym języku.
        // Wybrane z dodatku, a plik ma wbudowane w tym samym języku → wbudowane. Napisy AI (ai://) zostają.
        if (addon != null && !addon.url.startsWith("ai://") && ForkFeatures.enabled(c, EMBEDDED_KEY, true)) {
            val same = s.subtitleTracks.firstOrNull { !it.isForced && com.nuvio.tv.ui.screens.player.PlayerSubtitleUtils.matchesLanguageCode(it.language, addon.lang) }
            if (same != null) {
                Log.i(TAG, "napisy wbudowane: wybrane „${addon.lang}” z dodatku ${addon.addonName} → wbudowane (ścieżka ${same.index} „${same.name}”)")
                send(PlayerEvent.OnSelectSubtitleTrack(same.index))
                done = true
                return
            }
        }
        if (!wantsPolish(c, s)) { done = true; Log.i(TAG, "polskie napisy: profil nie chce polskich — bez zmian"); return }
        val embedded = s.subtitleTracks.firstOrNull { it.index == s.selectedSubtitleTrackIndex } ?: s.subtitleTracks.firstOrNull { it.isSelected }
        val selectedLang = addon?.lang ?: embedded?.language
        if (selectedLang == null) return // napisy wyłączone (jeszcze) — nic nie włączamy na siłę
        if (addon == null && isPl(selectedLang)) { done = true; return } // polskie wbudowane — tak ma być
        val plEmbedded = s.subtitleTracks.firstOrNull { isPl(it.language) && !it.isForced }
        if (plEmbedded != null) {
            Log.i(TAG, "polskie napisy: wybrane „$selectedLang”${if (addon != null) " z dodatku ${addon.addonName}" else ""} → wbudowane polskie (ścieżka ${plEmbedded.index} „${plEmbedded.name}”)")
            send(PlayerEvent.OnSelectSubtitleTrack(plEmbedded.index))
            done = true
            return
        }
        if (isPl(selectedLang)) { done = true; return } // polskie z dodatku, wbudowanych polskich brak
        if (s.isLoadingAddonSubtitles) return
        val plAddon = s.addonSubtitles.firstOrNull { isPl(it.lang) }
        if (plAddon != null) {
            Log.i(TAG, "polskie napisy: wybrane „$selectedLang” → polskie z dodatku ${plAddon.addonName}")
            send(PlayerEvent.OnSelectAddonSubtitle(plAddon))
            if (!s.showSubtitleOverlay) send(PlayerEvent.OnDismissTransientOverlay) // wybór automatyczny bez okna napisów
        } else {
            Log.i(TAG, "polskie napisy: brak polskich (wybrane „$selectedLang”, wbudowanych ${s.subtitleTracks.size}, z dodatków ${s.addonSubtitles.size})")
        }
        done = true
    }
}
