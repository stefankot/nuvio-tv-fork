package com.nuvio.tv.fork.lektor

import android.content.Context
import com.nuvio.tv.fork.ai.subtitles.AiSubtitles
import com.nuvio.tv.fork.ai.subtitles.LiveEmbeddedTranslation
import com.nuvio.tv.fork.sources.PlayerBridge
import kotlinx.coroutines.delay

/**
 * Fork 19.11: skąd lektor bierze polski tekst. Kolejność: YouTube (tłumaczenie YouTube) → napisy PL z dodatku →
 * polska ścieżka wbudowana → AI napisy (zwykła sesja albo na bieżąco z wbudowanych; uruchamiane, gdy nic nie działa).
 */
object LektorSources {
    class Source(val name: String, val growing: Boolean, private val read: () -> List<SubtitleCue>) { fun cues() = read() }

    suspend fun pick(app: Context, say: (String) -> Unit): Source {
        val url = PlayerBridge.streamUrl()
        if (com.nuvio.tv.fork.yt.YouTubeTextSource.videoId(url) != null) {
            val yt = com.nuvio.tv.fork.yt.YouTubeTextSource.polishCues(url).map { SubtitleCue(it.startTimeMs, it.endTimeMs, it.text) }
            return Source("napisy YouTube (PL)", false) { yt }
        }
        // Pkt 7 (21.11): wbudowane polskie przed dodatkami (ZASADY §7) i lektor czyta TYLKO jedną, wybraną polską ścieżkę.
        val controller = AiSubtitles.controller()
        if (controller != null) {
            val pl = LiveEmbeddedTranslation.polishEmbedded(controller)
            if (pl.size >= 3) {
                android.util.Log.i("ForkLektor", "źródło: polskie napisy wbudowane (${pl.size} kwestii)")
                return Source("polskie napisy wbudowane", true) { LiveEmbeddedTranslation.polishEmbedded(controller).map { SubtitleCue(it.startMs, it.endMs, it.text) } }
            }
        }
        if (PlayerBridge.hasPolishAddonSubtitle()) {
            var tries = 0
            while (PlayerBridge.cues().isEmpty() && tries++ < 20) delay(500)
            // Fork 19.11 (build118: „lektor mówi kwestie za wcześnie o minutę”): kwestie brane raz przy starcie, bez późniejszego
            // przesunięcia AutoSync. Teraz te same, które są na ekranie (sidecar, po AutoSync), czytane na bieżąco (growing).
            if (PlayerBridge.cues().isNotEmpty()) return Source("napisy PL z dodatku", true) {
                // Pkt 7: kwestie z ekranu tylko, gdy na ekranie są polskie napisy — przy włoskich lektor czytał na zmianę PL/IT.
                val plShown = com.nuvio.tv.fork.subs.PolishFirst.isPl(PlayerBridge.current()?.selectedAddonSubtitle?.lang)
                val shown = AiSubtitles.controller()?.sidecarTimedCues?.takeIf { plShown && it.isNotEmpty() }
                shown?.let { com.nuvio.tv.fork.ai.subtitles.ExoPlayerPort.toDocCues(it).map { c -> SubtitleCue(c.startMs, c.endMs, c.text) } }
                    ?: PlayerBridge.cues().map { SubtitleCue(it.startTimeMs, it.endTimeMs, it.text) }
            }
        }
        if (AiSubtitles.polishCues().isEmpty() && !AiSubtitles.running) {
            // Właściciel 19.11: AI tylko na prośbę — okno „napisy AI?”; po „Tak” lektor rusza ponownie.
            val why = AiSubtitles.unavailableReason(app)
            say(if (why != null) "Lektor: brak polskich napisów; AI: $why" else "Lektor: brak polskich napisów")
            if (why == null) com.nuvio.tv.fork.ai.subtitles.AiOffer.offer(forLektor = true)
            return Source("brak", false) { emptyList() }
        }
        var waited = 0
        while (AiSubtitles.polishCues().isEmpty() && waited++ < 90) delay(1_000) // pierwszy blok AI: zwykle kilka–kilkanaście s
        return Source("AI napisy (PL)", true) { AiSubtitles.polishCues().map { SubtitleCue(it.startMs, it.endMs, it.text) } }
    }
}
