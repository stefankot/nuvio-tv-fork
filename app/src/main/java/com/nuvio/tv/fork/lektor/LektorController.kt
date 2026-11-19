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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.nuvio.tv.fork.logged

/** Włączanie lektora (B09-3/4/6): napisy z synchronizacji → plan kwestii → synteza naprzód (okno 3 min) → miks w [LektorAudioProcessor]. */
object LektorController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    @Volatile var status: String = ""
        private set

    fun toggle(context: Context): String = if (LektorEngine.enabled) { stop(); "Lektor wyłączony" } else start(context)

    fun stop() { val was = LektorEngine.enabled; job?.cancel(); job = null; LektorEngine.clear(); status = ""; if (was) LektorPcm.reapply("lektor wył.") }

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
            LektorEngine.load(cues); LektorEngine.enabled = true; Condense.reset()
            LektorSpend.begin(PlayerBridge.current()?.let { "${it.contentName}|${it.title}" }.orEmpty())
            LektorPcm.reapply("lektor wł.")
            // Procesor dźwięku włącza się dopiero po flush rurociągu: seek do bieżącej pozycji go wymusza.
            PlayerBridge.send(PlayerEvent.OnSeekTo(LektorEngine.positionMs()))
            val tts = com.nuvio.tv.ainarrator.LektorVoiceChoice.create(app, LektorVoice.ready) // scalenie 19.11: głos Gemini albo systemowy
            val done = HashSet<Long>()
            var synthErrors = 0; var synthOk = 0
            var errStreak = 0 // fork 19.11: kolejne błędy z rzędu → coraz dłuższa przerwa (2, 4, 8 … 30 s)
            val sceneFallback = HashSet<Long>() // sceny, których nagrania nie dało się pociąć: te kwestie pojedynczo
            val sceneSmall = HashSet<Long>() // fork 19.11: duża scena nie dała się pociąć — te kwestie małymi scenami
            var scenesOk = 0
            try {
                var known = cues.size // podpis listy kwestii (liczba + czasy)
                var tick = 0
                var quietSaidAt = 0L // fork 19.11: „nie słyszę lektora” przy długiej scenie bez dialogu — komunikat, kiedy będzie kwestia
                while (true) {
                    // Źródło rosnące (AI na bieżąco): nowe kwestie dochodzą do planu bez kasowania gotowych głosów.
                    // Fork 19.11: zmiana także samych czasów (AutoSync przesuwa napisy), nie tylko liczby kwestii.
                    if (src.growing) src.cues().let { now -> val sig = now.size * 31 + now.sumOf { it.startMs }.toInt(); if (sig != known) {
                        val shift = (now.firstOrNull()?.startMs ?: 0) - (LektorEngine.lines.firstOrNull()?.startMs ?: 0)
                        if (known != cues.size) android.util.Log.i("ForkLektor", "kwestie zmienione: ${now.size} (przesunięcie pierwszej ${shift} ms)")
                        known = sig; LektorEngine.lines = CuePlanner.plan(now) } }
                    val pos = LektorEngine.positionMs()
                    // Fork 19.11: zbyt długie kwestie skrócone przez AI przed syntezą (Condense), żeby następna ich nie ucinała.
                    val styled = tts as? StyledLektor
                    val pending = if (styled != null) SynthWindow.pending(LektorEngine.lines, pos, done) else Condense.apply(app, SynthWindow.pending(LektorEngine.lines, pos, done))
                    if (LektorEngine.voice.isEmpty() && pending.isEmpty() && System.currentTimeMillis() - quietSaidAt > 60_000) {
                        val next = LektorEngine.lines.firstOrNull { it.startMs > pos }
                        quietSaidAt = System.currentTimeMillis()
                        val msg = if (next == null) "Lektor: w napisach nie ma już kwestii do przeczytania" else "Lektor: najbliższa kwestia za ${(next.startMs - pos) / 1000} s (scena bez dialogu)"
                        android.util.Log.i("ForkLektor", msg); say(app, msg)
                    }
                    // Fork 19.11 (build114: „nie słyszę lektora”, po starcie zero logów): stan pętli co ~10 s.
                    if (tick++ % 10 == 0) LektorEngine.lines.let { ls -> android.util.Log.i("ForkLektor", "pętla: pozycja=${pos} ms, kwestii=${ls.size} (${ls.firstOrNull()?.startMs}..${ls.lastOrNull()?.startMs}), do syntezy=${pending.size}, z głosem=${LektorEngine.voice.size}, ok=$synthOk, błędy=$synthErrors, silnik=${tts.javaClass.simpleName}") }
                    // Fork 19.11 (strategia lektora Gemini Cloud): scenariusz w tle, potem kwestia po kwestii ze stylem sceny,
                    // 3 nagrania naraz; każda kwestia gra w chwili swojego napisu (pauz nie trzeba — test 19.11: Gemini nie trzyma pauz w sekundach).
                    if (styled != null) {
                        LektorScript.ensure(app, LektorEngine.lines, pos)
                        val ready = pending.filter { LektorScript.ready(it.startMs) }.take(if (errStreak > 0) 1 else 6)
                        if (ready.isEmpty() && pending.isNotEmpty() && LektorEngine.voice.isEmpty()) status = "Lektor: Gemini przygotowuje scenariusz (${LektorScript.progress(LektorEngine.lines.size)})…"
                        val results = coroutineScope { ready.map { l -> async { l to runCatching { styledLine(app, styled, l) } } }.map { it.await() } }
                        for ((l, r) in results) {
                            r.onSuccess { pcm -> LektorEngine.voice[l.startMs] = pcm; done += l.startMs; errStreak = 0; if (synthOk++ % 20 == 0) android.util.Log.i("ForkLektor", "Gemini Cloud: $synthOk kwestii nagranych (ostatnia ${pcm.durationMs} ms, czas ${l.availableMs} ms)") }
                            .onFailure { e ->
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                android.util.Log.w("ForkLektor", "synteza nieudana: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
                                if (synthErrors++ == 0) say(app, "Lektor: ${e.message ?: "błąd syntezy głosu"} — ponawiam")
                                errStreak++
                            }
                        }
                        val kept = SynthWindow.dropOutside(done, pos); done.retainAll(kept); LektorEngine.voice.keys.retainAll(kept)
                        if (errStreak == 0 && status.startsWith("Lektor: Gemini przygotowuje").not()) status = "Lektor włączony"
                        delay(if (errStreak > 0) minOf(30_000L, 1_000L shl minOf(errStreak, 5)) else 1000)
                        continue
                    }
                    // Fork 19.11 (właściciel): sceny jednym zapytaniem, z wyprzedzeniem [SynthWindow.AHEAD_MS] (10 min).
                    val sceneLines = HashSet<Long>()
                    if (tts is SceneLektor && errStreak == 0) {
                        // Fork 19.11: duże sceny (Chirp: do ~4800 B, kontekst dla intonacji); pierwsze 30 s i sceny, których
                        // duże nagranie nie dało się pociąć — małe sceny; dopiero potem kwestia po kwestii (sceneFallback).
                        val bigOk = tts.sceneMaxBytes > 0
                        val (soon, later) = pending.filter { it.startMs !in sceneFallback }.partition { !bigOk || it.startMs < pos + 30_000 || it.startMs in sceneSmall }
                        val scenes = (Scenes.group(soon) + if (bigOk) Scenes.group(later, Scenes.BIG_MAX_GAP_MS, Int.MAX_VALUE, Int.MAX_VALUE, tts.sceneMaxBytes) else emptyList())
                            .sortedBy { it.first().startMs }.filter { sc -> sc.size >= 2 }
                        scenes.forEach { sc -> sc.forEach { sceneLines += it.startMs } }
                        for (sc in scenes.take(2)) {
                            val parts = try { tts.synthesizeScene(sc.map { it.text }) } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                android.util.Log.w("ForkLektor", "scena nieudana: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
                                // Fork 19.11 (JMGO: po błędzie dużej sceny ta sama scena w kółko): następnym razem małymi scenami, potem pojedynczo.
                                if (sc.any { it.startMs in sceneSmall }) sc.forEach { sceneFallback += it.startMs } else sc.forEach { sceneSmall += it.startMs }
                                if (synthErrors++ == 0) say(app, "Lektor: ${e.message ?: "błąd syntezy głosu"} — ponawiam")
                                errStreak++; break
                            }
                            if (parts == null || parts.size != sc.size) {
                                val big = sc.size > Scenes.MAX_LINES || sc.sumOf { it.text.length } > Scenes.MAX_CHARS
                                if (big && sc.none { it.startMs in sceneSmall }) { sc.forEach { sceneSmall += it.startMs; sceneLines -= it.startMs }; android.util.Log.i("ForkLektor", "duża scena ${sc.size} kwestii: cięcie się nie zgadza — ponownie małymi scenami") }
                                else sc.forEach { sceneFallback += it.startMs; sceneLines -= it.startMs }
                                continue
                            }
                            // Fork 19.11 (właściciel: „przerywa, gdy dużo tekstu”): kwestie ze sceny też dopasowane do czasu do
                            // następnej (przyspieszenie / cięcie na zdaniu), bez wchodzenia na nią — inaczej następna ją ucinała.
                            var refit = 0
                            sc.forEachIndexed { k, l ->
                                val part = parts[k]
                                Condense.learn(l.text, part.durationMs)
                                val fit = Tempo.fit(l.text, part.durationMs, l.availableMs, gapMs = 0)
                                val pcm = if (fit.rate > 1.01f || fit.text != l.text) { refit++; runCatching { tts.synthesize(fit.text, fit.rate) }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }.logged("ForkLektor", "scena: dopasowanie tempa").getOrDefault(part) } else part
                                LektorEngine.voice[l.startMs] = pcm; done += l.startMs
                            }
                            if (refit > 0) android.util.Log.i("ForkLektor", "scena: $refit/${sc.size} kwestii nagrane ponownie szybciej (nie mieściły się do następnej)")
                            errStreak = 0; synthOk += sc.size
                            if (scenesOk++ % 5 == 0) android.util.Log.i("ForkLektor", "scena OK: ${sc.size} kwestii (${sc.sumOf { it.text.length }} znaków) jednym zapytaniem (scen: $scenesOk, kwestii z głosem: ${LektorEngine.voice.size}, wyprzedzenie ${(sc.first().startMs - pos) / 1000} s)")
                        }
                    }
                    for (l in pending.filter { it.startMs !in sceneLines && it.startMs !in done }.take(if (errStreak > 0) 1 else 5)) { // fork 19.11: po błędzie 1 próba na obieg (było 0 → lektor nie wracał)
                        val natural = runCatching { tts.synthesize(l.text, 1f) }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }.logged("ForkLektor", "LektorController.kt:57").getOrElse { e ->
                            status = e.message ?: "Błąd syntezy"
                            // Fork 19.11: błąd syntezy był widoczny tylko w statusie — log i komunikat (pierwszy).
                            android.util.Log.w("ForkLektor", "synteza nieudana: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
                            if (synthErrors++ == 0) say(app, "Lektor: ${e.message ?: "błąd syntezy głosu"} — ponawiam, część kwestii może zostać bez głosu")
                            errStreak++
                            null
                        } ?: break
                        errStreak = 0
                        if (synthOk++ % 10 == 0) android.util.Log.i("ForkLektor", "synteza OK: $synthOk kwestii (ostatnia ${natural.durationMs} ms)")
                        Condense.learn(l.text, natural.durationMs)
                        val fit = Tempo.fit(l.text, natural.durationMs, l.availableMs, gapMs = 0) // fork 19.11: bez wchodzenia na następną kwestię
                        val pcm = if (fit.rate > 1.01f || fit.text != l.text) runCatching { tts.synthesize(fit.text, fit.rate) }.logged("ForkLektor", "LektorController.kt:66").getOrDefault(natural) else natural
                        LektorEngine.voice[l.startMs] = pcm; done += l.startMs
                    }
                    val kept = SynthWindow.dropOutside(done, pos); done.retainAll(kept); LektorEngine.voice.keys.retainAll(kept)
                    status = if (errStreak > 0) "Lektor: ponawiam po błędzie głosu" else "Lektor włączony"
                    delay(if (errStreak > 0) minOf(30_000L, 1_000L shl minOf(errStreak, 5)) else 1000)
                }
            } finally { (tts as? SystemLektor)?.shutdown() }
        }
        val first = !com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(app, "lektor_dolby_note", false)
        if (first) com.nuvio.tv.ui.screens.settings.ForkFeatures.set(app, "lektor_dolby_note", true)
        return "Włączam lektora (" + com.nuvio.tv.ainarrator.LektorVoiceChoice.label(app, LektorVoice.ready) + ")…" + if (first) " " + LektorRules.DOLBY_NOTE else ""
    }

    /**
     * Fork 19.11: jedna kwestia przez silnik ze stylem — tekst lektorski i styl ze scenariusza; za długa względem czasu do
     * następnej → drugie nagranie z poleceniem „szybciej”, bierzemy krótsze; nadal za długa → cięcie na granicy zdania (Tempo).
     */
    private suspend fun styledLine(app: Context, tts: StyledLektor, l: Line): Pcm {
        val text = LektorScript.get(l.startMs)?.text ?: l.text
        val style = LektorScript.styleFor(app, l.startMs)
        var pcm = tts.synthesizeStyled(text, style)
        Condense.learn(text, pcm.durationMs)
        if (pcm.durationMs > l.availableMs + 250) {
            val sec = "%.1f".format(java.util.Locale.ROOT, l.availableMs / 1000.0)
            val fast = runCatching { tts.synthesizeStyled(text, "$style Mów wyraźnie szybciej niż zwykle — cała kwestia musi zmieścić się w $sec s.") }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }.logged("ForkLektor", "styledLine: szybciej").getOrNull()
            if (fast != null && fast.durationMs < pcm.durationMs) pcm = fast
            if (pcm.durationMs > l.availableMs + 250) {
                val fit = Tempo.fit(text, pcm.durationMs, l.availableMs, gapMs = 0)
                if (fit.text != text) runCatching { tts.synthesizeStyled(fit.text, "$style Mów wyraźnie szybciej niż zwykle.") }
                    .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }.logged("ForkLektor", "styledLine: krócej").getOrNull()?.let { pcm = it }
                android.util.Log.i("ForkLektor", "kwestia za długa: ${pcm.durationMs} ms na ${l.availableMs} ms" + if (fit.text != text) " — skrócona do zdań" else "")
            }
        }
        return pcm
    }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    /** Komunikat na ekranie (właściciel: „nic się nie dzieje” — wynik był tylko w logu). */
    fun say(c: Context, msg: String) = main.post { android.widget.Toast.makeText(c.applicationContext, msg, android.widget.Toast.LENGTH_LONG).show() }
}
