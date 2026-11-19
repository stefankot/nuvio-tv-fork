package com.nuvio.tv.fork.ai.subtitles

import android.util.Log
import com.nuvio.tv.fork.logged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * Fork 19.11: kwestie napisów wbudowanych pobierane z pliku z wyprzedzeniem [WINDOW_MS] (zob. [MkvSubtitleText]).
 * Ścieżka MKV dobierana po języku, a gdy jest ich kilka — po zgodności tekstu z kwestiami, które odtwarzacz już
 * wczytał. Gdy plik się nie nadaje albo pobieranie zawodzi, [active] = false i zostaje bufor odtwarzacza.
 */
internal class MkvAhead(private val url: String, private val headers: Map<String, String>, private val language: String) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val got = ConcurrentHashMap<Long, DocCue>()
    @Volatile var active = false; private set

    fun cues(): List<DocCue> = got.values.sortedBy { it.startMs }

    fun start(position: suspend () -> Long, captured: () -> List<DocCue>) {
        scope.launch {
            runCatching { loop(position, captured) }
                .onFailure { if (it !is kotlinx.coroutines.CancellationException) Log.w(TAG, "MkvAhead.loop: ${it.javaClass.simpleName}: ${it.message?.take(200)}") }
            active = false
        }
    }

    fun stop() { active = false; scope.cancel() }

    private suspend fun loop(position: suspend () -> Long, captured: () -> List<DocCue>) {
        val mkv = MkvSubtitleText.open(url, headers) ?: return
        val want = MkvSubtitleText.normLang(language)
        val cands = mkv.tracks.filter { want == null || it.lang2 == want }
            .sortedWith(compareBy<MkvSubtitleText.Track>({ it.forced }, { it.hearingImpaired }, { -it.cues.size }))
        if (cands.isEmpty()) { Log.i(TAG, "brak ścieżki MKV w języku $language — zostaje bufor odtwarzacza"); return }
        val track = pick(mkv, cands, captured()) ?: return
        active = true
        Log.i(TAG, "pobieram z pliku: ścieżka #${track.number} (${track.lang2}, ${track.codec}, ${track.cues.size} kwestii), wyprzedzenie ${WINDOW_MS / 60_000} min")
        val sem = Semaphore(PARALLEL)
        var emptyRounds = 0
        while (true) {
            val pos = position()
            val todo = track.cues.indices.filter { i -> val t = track.cues[i].timeMs; t in (pos - 5_000)..(pos + WINDOW_MS) && !got.containsKey(t) }.take(BATCH)
            if (todo.isEmpty()) { delay(5_000); continue }
            val t0 = System.currentTimeMillis()
            val res = coroutineScope {
                todo.map { i -> async { sem.withPermit { fetch(mkv, track, i) } } }.awaitAll()
            }
            val ok = res.count { it != null }
            Log.i(TAG, "pobrano $ok/${todo.size} kwestii w ${System.currentTimeMillis() - t0} ms, do +${((track.cues[todo.last()].timeMs - pos) / 1000)} s")
            if (ok == 0) {
                if (++emptyRounds >= 3) { Log.w(TAG, "3 razy nic nie pobrano — wyłączam pobieranie z pliku, zostaje bufor odtwarzacza"); return }
                delay(5_000); continue
            }
            emptyRounds = 0
        }
    }

    private fun fetch(mkv: MkvSubtitleText, track: MkvSubtitleText.Track, i: Int): DocCue? {
        val e = track.cues[i]
        val cue = runCatching { mkv.fetchCue(track, e, track.cues.getOrNull(i + 1)?.timeMs) }.logged(TAG, "MkvAhead.fetch").getOrNull()
        // Pusta kwestia też zapisana (bez tekstu), żeby nie pobierać jej ponownie; do tłumaczenia trafiają tylko z tekstem.
        got[e.timeMs] = cue ?: DocCue("e%09d".format(java.util.Locale.ROOT, e.timeMs), e.timeMs, e.timeMs, "")
        return cue
    }

    /** Kilka ścieżek w tym języku: wygrywa ta, której tekst zgadza się z kwestiami już wczytanymi przez odtwarzacz. */
    private fun pick(mkv: MkvSubtitleText, cands: List<MkvSubtitleText.Track>, seen: List<DocCue>): MkvSubtitleText.Track? {
        if (cands.size == 1 || seen.size < 3) return cands.first()
        val ref = seen.filter { it.text.isNotBlank() }.takeLast(12)
        var best: MkvSubtitleText.Track? = null; var bestScore = 0
        for (t in cands.take(4)) {
            var score = 0; var tried = 0
            for (c in ref) {
                if (tried >= 3) break
                val i = t.cues.indexOfFirst { kotlin.math.abs(it.timeMs - c.startMs) <= 300 }.takeIf { it >= 0 } ?: continue
                tried++
                val text = runCatching { mkv.fetchCue(t, t.cues[i], t.cues.getOrNull(i + 1)?.timeMs)?.text }.logged(TAG, "MkvAhead.pick").getOrNull()
                if (text != null && norm(text) == norm(c.text)) score++
            }
            Log.i(TAG, "ścieżka #${t.number}: zgodność tekstu $score/$tried")
            if (score > bestScore) { best = t; bestScore = score }
        }
        return best ?: cands.first().also { Log.i(TAG, "brak zgodności tekstu — biorę #${it.number} (domyślny wybór)") }
    }

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    companion object {
        private const val TAG = "ForkAiMkv"
        const val WINDOW_MS = 600_000L
        private const val BATCH = 40
        private const val PARALLEL = 4

        /** Kwestie z pliku mają pierwszeństwo (stałe id); z bufora odtwarzacza tylko te, których nie ma w pliku. */
        fun merge(fromFile: List<DocCue>, fromPlayer: List<DocCue>): List<DocCue> {
            if (fromFile.isEmpty()) return fromPlayer
            val starts = java.util.TreeSet<Long>().apply { fromFile.forEach { add(it.startMs) } }
            fun near(t: Long) = (starts.floor(t)?.let { t - it <= 150 } == true) || (starts.ceiling(t)?.let { it - t <= 150 } == true)
            return (fromFile.filter { it.text.isNotBlank() } + fromPlayer.filterNot { near(it.startMs) }).sortedBy { it.startMs }
        }
    }
}
