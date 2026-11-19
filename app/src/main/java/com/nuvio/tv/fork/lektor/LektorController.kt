package com.nuvio.tv.fork.lektor

import android.content.Context
import com.nuvio.tv.fork.sources.PlayerBridge
import com.nuvio.tv.ui.screens.player.PlayerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Włączanie lektora (B09-3/4/6): napisy z synchronizacji → plan kwestii → synteza naprzód (okno 3 min) → miks w [LektorAudioProcessor]. */
object LektorController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    @Volatile var status: String = ""
        private set

    fun toggle(context: Context): String = if (LektorEngine.enabled) { stop(); "Lektor wyłączony" } else start(context)

    fun stop() { job?.cancel(); job = null; LektorEngine.clear(); status = "" }

    fun start(context: Context): String {
        if (!PlayerBridge.active) return "Lektor działa tylko podczas odtwarzania"
        if (!LektorVoice.usable(context)) return LektorVoice.MISSING_MSG
        val app = context.applicationContext
        job?.cancel()
        if (com.nuvio.tv.fork.yt.YouTubeTextSource.videoId(PlayerBridge.streamUrl()) == null) { // YouTube: napisy z manifestu, nie z dodatków
            PlayerBridge.ensurePolishSubtitle()
            PlayerBridge.send(PlayerEvent.OnReloadSubtitleAutoSyncCues)
        }
        job = scope.launch {
            status = "Przygotowuję lektora…"
            // Fork 19.11: YouTube nie ma napisów z dodatków — polskie napisy z manifestu YouTube (tłumaczenie YouTube).
            val yt = com.nuvio.tv.fork.yt.YouTubeTextSource.videoId(PlayerBridge.streamUrl()) != null
            var tries = 0
            if (!yt) while (PlayerBridge.cues().isEmpty() && tries++ < 20) delay(500)
            val source = if (yt) com.nuvio.tv.fork.yt.YouTubeTextSource.polishCues(PlayerBridge.streamUrl()) else PlayerBridge.cues()
            val cues = source.map { SubtitleCue(it.startTimeMs, it.endTimeMs, it.text) }
            android.util.Log.i("ForkLektor", "start: youtube=$yt kwestii=${cues.size}")
            if (cues.isEmpty()) { status = "Brak napisów do przeczytania"; return@launch }
            LektorModule.applySettings(app)
            LektorEngine.load(cues); LektorEngine.enabled = true
            // Procesor dźwięku włącza się dopiero po flush rurociągu: seek do bieżącej pozycji go wymusza.
            PlayerBridge.send(PlayerEvent.OnSeekTo(LektorEngine.positionMs()))
            val tts = com.nuvio.tv.ainarrator.LektorVoiceChoice.create(app, LektorVoice.ready) // scalenie 19.11: głos Gemini albo systemowy
            val done = HashSet<Long>()
            try {
                while (true) {
                    val pos = LektorEngine.positionMs()
                    val pending = SynthWindow.pending(LektorEngine.lines, pos, done)
                    for (l in pending.take(5)) {
                        val natural = runCatching { tts.synthesize(l.text, 1f) }.getOrElse { status = it.message ?: "Błąd syntezy"; null } ?: break
                        val fit = Tempo.fit(l.text, natural.durationMs, l.availableMs)
                        val pcm = if (fit.rate > 1.01f || fit.text != l.text) runCatching { tts.synthesize(fit.text, fit.rate) }.getOrDefault(natural) else natural
                        LektorEngine.voice[l.startMs] = pcm; done += l.startMs
                    }
                    val kept = SynthWindow.dropOutside(done, pos); done.retainAll(kept); LektorEngine.voice.keys.retainAll(kept)
                    status = "Lektor włączony"
                    delay(1000)
                }
            } finally { (tts as? SystemLektor)?.shutdown() }
        }
        val first = !com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(app, "lektor_dolby_note", false)
        if (first) com.nuvio.tv.ui.screens.settings.ForkFeatures.set(app, "lektor_dolby_note", true)
        return "Włączam lektora (" + com.nuvio.tv.ainarrator.LektorVoiceChoice.label(app, LektorVoice.ready) + ")…" + if (first) " " + LektorRules.DOLBY_NOTE else ""
    }
}
