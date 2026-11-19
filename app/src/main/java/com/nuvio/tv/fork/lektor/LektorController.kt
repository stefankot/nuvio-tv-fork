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
        if (com.nuvio.tv.fork.yt.YouTubeTextSource.videoId(PlayerBridge.streamUrl()) == null && PlayerBridge.hasPolishAddonSubtitle()) { // YouTube: napisy z manifestu, nie z dodatków
            PlayerBridge.ensurePolishSubtitle()
            PlayerBridge.send(PlayerEvent.OnReloadSubtitleAutoSyncCues)
        }
        job = scope.launch {
            status = "Przygotowuję lektora…"
            // Fork 19.11 (właściciel: „lektor nie działa nigdzie”): polski tekst z pierwszego źródła, które go ma —
            // YouTube (tłumaczenie YouTube), napisy PL z dodatku, polska ścieżka wbudowana, AI napisy (też na bieżąco z wbudowanych).
            val src = LektorSources.pick(app) { status = it; say(app, it) }
            val cues = src.cues()
            android.util.Log.i("ForkLektor", "start: źródło=${src.name} kwestii=${cues.size}")
            if (cues.isEmpty()) { status = "Brak polskich napisów do przeczytania"; if (src.name != "brak") say(app, "Lektor: brak polskich napisów do przeczytania (dodatki, wbudowane i AI)"); return@launch }
            say(app, "Lektor czyta: ${src.name}")
            LektorModule.applySettings(app)
            LektorEngine.load(cues); LektorEngine.enabled = true
            // Procesor dźwięku włącza się dopiero po flush rurociągu: seek do bieżącej pozycji go wymusza.
            PlayerBridge.send(PlayerEvent.OnSeekTo(LektorEngine.positionMs()))
            val tts = com.nuvio.tv.ainarrator.LektorVoiceChoice.create(app, LektorVoice.ready) // scalenie 19.11: głos Gemini albo systemowy
            val done = HashSet<Long>()
            var synthErrors = 0; var synthOk = 0
            try {
                var known = cues.size
                while (true) {
                    // Źródło rosnące (AI na bieżąco): nowe kwestie dochodzą do planu bez kasowania gotowych głosów.
                    if (src.growing) src.cues().let { now -> if (now.size != known) { known = now.size; LektorEngine.lines = CuePlanner.plan(now) } }
                    val pos = LektorEngine.positionMs()
                    val pending = SynthWindow.pending(LektorEngine.lines, pos, done)
                    for (l in pending.take(5)) {
                        val natural = runCatching { tts.synthesize(l.text, 1f) }.getOrElse { e ->
                            status = e.message ?: "Błąd syntezy"
                            // Fork 19.11: błąd syntezy był widoczny tylko w statusie — log i komunikat (pierwszy).
                            android.util.Log.w("ForkLektor", "synteza nieudana: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
                            if (synthErrors++ == 0) say(app, "Lektor: ${e.message ?: "błąd syntezy głosu"}")
                            null
                        } ?: break
                        if (synthOk++ % 10 == 0) android.util.Log.i("ForkLektor", "synteza OK: $synthOk kwestii (ostatnia ${natural.durationMs} ms)")
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

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    /** Komunikat na ekranie (właściciel: „nic się nie dzieje” — wynik był tylko w logu). */
    fun say(c: Context, msg: String) = main.post { android.widget.Toast.makeText(c.applicationContext, msg, android.widget.Toast.LENGTH_LONG).show() }
}
