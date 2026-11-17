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
        val app = context.applicationContext
        job?.cancel()
        PlayerBridge.send(PlayerEvent.OnReloadSubtitleAutoSyncCues)
        job = scope.launch {
            status = "Przygotowuję lektora…"
            var tries = 0
            while (PlayerBridge.cues().isEmpty() && tries++ < 20) delay(500)
            val cues = PlayerBridge.cues().map { SubtitleCue(it.startTimeMs, it.endTimeMs, it.text) }
            if (cues.isEmpty()) { status = "Brak napisów do przeczytania"; return@launch }
            LektorModule.applySettings(app)
            LektorEngine.load(cues); LektorEngine.enabled = true
            val tts = SystemLektor(app)
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
            } finally { tts.shutdown() }
        }
        val first = !com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(app, "lektor_dolby_note", false)
        if (first) com.nuvio.tv.ui.screens.settings.ForkFeatures.set(app, "lektor_dolby_note", true)
        return "Włączam lektora…" + if (first) " " + LektorRules.DOLBY_NOTE else ""
    }
}
