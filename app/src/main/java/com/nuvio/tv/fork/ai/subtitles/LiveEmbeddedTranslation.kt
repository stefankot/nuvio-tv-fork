package com.nuvio.tv.fork.ai.subtitles

import android.util.Log
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.autosync.EmbeddedSubtitleCueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.nuvio.tv.fork.logged

/**
 * Fork 19.11 (zgłoszenie 9a, właściciel: „AI napisy i lektor nie działają nigdzie”): źródło z napisów WBUDOWANYCH w plik.
 * Tekstu wbudowanego nie da się wczytać z góry (leży w całym pliku), ale AutoSync już podsłuchuje go przy wczytywaniu
 * (`EmbeddedSubtitleCueStore`, wyprzedzenie = bufor odtwarzacza). Tu tryb „na bieżąco” (SourceAvailability.WINDOWED_TEXT):
 * co chwilę kawałek nieprzetłumaczonych kwestii tuż przed pozycją → ten sam silnik (TranslationEngine, Gemini) → publikacja
 * wszystkich gotowych polskich kwestii przez [PlayerPort.publish]. [translated] czyta lektor.
 */
object LiveEmbeddedTranslation {
    private const val TAG = "ForkAiLive"
    private const val CHUNK = 30
    /** Właściciel 19.11: „nie może to trwać tak długo bez komunikatu” — pierwszy blok mały, żeby PL pojawiło się szybko. */
    private const val FIRST_CHUNK = 8
    private const val AHEAD_MS = 240_000L

    private val _translated = MutableStateFlow<List<DocCue>>(emptyList())
    /** Polskie kwestie gotowe do tej pory (rosnąca lista, posortowana). */
    val translated: StateFlow<List<DocCue>> = _translated.asStateFlow()
    @Volatile var active = false; private set

    fun reset() { if (!active) _translated.value = emptyList() }

    /** Najlepsza wbudowana ścieżka nie po polsku (język oryginału, potem angielski). */
    fun sourceTrack(controller: PlayerRuntimeController, original: String?): Pair<String, String?>? {
        val tracks = EmbeddedSubtitleCueStore.candidateTracks(controller.currentStreamUrl, original)
            .filterNot { (it.language ?: "").lowercase().let { l -> l.startsWith("pl") || l == "pol" } }
            .filter { t -> t.cues.count { it.text.isNotBlank() } >= 3 }
        return tracks.firstOrNull()?.let { it.key to it.language }
    }

    /** Polska wbudowana ścieżka (gotowy tekst dla lektora bez tłumaczenia). */
    fun polishEmbedded(controller: PlayerRuntimeController): List<DocCue> =
        EmbeddedSubtitleCueStore.candidateTracks(controller.currentStreamUrl, "pl")
            .firstOrNull { (it.language ?: "").lowercase().let { l -> l.startsWith("pl") || l == "pol" } }
            ?.cues?.filter { it.text.isNotBlank() }?.map { DocCue(id(it.startTimeMs), it.startTimeMs, it.endTimeMs, it.text) }.orEmpty()

    private fun id(startMs: Long) = "e%09d".format(java.util.Locale.ROOT, startMs)

    private fun cuesOf(controller: PlayerRuntimeController, trackKey: String): List<DocCue> =
        EmbeddedSubtitleCueStore.candidateTracks(controller.currentStreamUrl, null)
            .filter { it.key == trackKey }.flatMap { it.cues }
            .filter { it.text.isNotBlank() }.distinctBy { it.startTimeMs }.sortedBy { it.startTimeMs }
            .map { DocCue(id(it.startTimeMs), it.startTimeMs, it.endTimeMs, it.text) }

    /** Pętla tłumaczenia; kończy się anulowaniem zadania (stop / zmiana odtwarzacza). */
    suspend fun run(
        controller: PlayerRuntimeController, port: PlayerPort, engine: TranslationEngine, model: String,
        trackKey: String, language: String, onState: (String) -> Unit,
    ) {
        active = true
        _translated.value = emptyList()
        val done = LinkedHashMap<String, String>() // id kwestii → tekst polski
        var glossary = emptyMap<String, String>()
        var failures = 0
        var published = 0
        var lastBlockMs = 20_000L // szacowany czas tłumaczenia bloku (pierwszy: ~20 s), potem mierzony
        val startedAt = System.currentTimeMillis()
        try {
            onState("AI: tłumaczę napisy wbudowane (${language}) na bieżąco")
            while (true) {
                val all = withContext(Dispatchers.Default) { cuesOf(controller, trackKey) }
                val pos = port.positionMs()
                // Fork 19.11: kwestie, które miną, zanim tłumaczenie wróci, zostają po angielsku (do pierwszej publikacji
                // EN są widoczne) — tłumaczymy od miejsca, w którym film będzie po czasie tłumaczenia (+5 s zapasu).
                val lead = if (published == 0) minOf(lastBlockMs + 5_000, 30_000L) else 0L
                val todo = all.filter { it.id !in done && it.endMs >= pos - 2_000 + lead && it.startMs <= pos + AHEAD_MS }
                val urgent = todo.firstOrNull()?.let { it.startMs - pos < 30_000 } == true
                if (todo.isEmpty() || (todo.size < 8 && !urgent)) {
                    if (published == 0) onState("AI: czekam na napisy do tłumaczenia… ${(System.currentTimeMillis() - startedAt) / 1000} s")
                    delay(1_500); continue
                }
                val chunk = todo.take(if (published == 0) FIRST_CHUNK else CHUNK)
                val firstIdx = all.indexOfFirst { it.id == chunk.first().id }
                val lastIdx = all.indexOfFirst { it.id == chunk.last().id }
                // Kontekst: kwestie wokół kawałka (bez całego filmu — jest tylko to, co już wczytane).
                val from = maxOf(0, firstIdx - 40); val to = minOf(all.size, lastIdx + 41)
                val window = all.subList(from, to)
                val block = DocBlock("live%04d".format(java.util.Locale.ROOT, published + 1), firstIdx - from, lastIdx - from + 1)
                val now = System.currentTimeMillis()
                val doc = AiSubtitleDoc(
                    key = "live-" + port.materialId.hashCode().toUInt().toString(16), materialId = port.materialId, title = port.title,
                    sourceLanguage = AuxiliaryLanguagePolicy.primaryLanguage(language), sourceRole = SourceRole.ORIGINAL,
                    sourceLabel = "wbudowane $language", availability = SourceAvailability.WINDOWED_TEXT,
                    cues = window, aux = emptyList(), missingCategories = emptyList(), blocks = listOf(block), glossary = glossary,
                    provider = "gemini", model = model, promptVersion = TranslationPrompt.VERSION, createdAtMs = now, updatedAtMs = now,
                )
                val blockStart = System.currentTimeMillis()
                val result = coroutineScope {
                    // Licznik na ekranie co sekundę, dopóki blok się tłumaczy (przed pierwszym blokiem EN zostają widoczne).
                    val ticker = launch {
                        while (true) {
                            val sec = (System.currentTimeMillis() - blockStart) / 1000
                            onState(if (published == 0) "AI: tłumaczę pierwsze kwestie na polski… $sec s (do tego czasu napisy oryginalne)"
                                    else "AI: tłumaczę dalej… $sec s (gotowe ${done.size} kwestii)")
                            delay(1_000)
                        }
                    }
                    runCatching { engine.translateBlock(doc, 0) }.logged("ForkAi", "LiveEmbeddedTranslation.translateBlock")
                        .also { ticker.cancel() }
                }
                lastBlockMs = System.currentTimeMillis() - blockStart
                result.exceptionOrNull()?.let { e ->
                    if (e is CancellationException) throw e
                    failures++
                    Log.w(TAG, "blok nieudany ($failures): ${e.javaClass.simpleName}: ${e.message?.take(160)}")
                    onState("AI: błąd tłumaczenia (${e.message?.take(80) ?: "?"}) — ponawiam")
                    if (e is SpendLimitException || failures >= 5) throw BlockFailedException(e.message ?: "Błąd tłumaczenia")
                    delay(3_000L * failures); continue
                }
                failures = 0
                val (rev, gl) = result.getOrThrow()
                glossary = glossary + gl
                window.subList(block.first, block.endExclusive).forEachIndexed { i, c -> rev.translations.getOrNull(i)?.let { done[c.id] = it } }
                val out = all.filter { it.id in done }.map { it.copy(text = done.getValue(it.id)) }
                _translated.value = out
                val r = port.publish(out, "AI PL (wbudowane)")
                published++
                Log.i(TAG, "blok $published: ${chunk.size} kwestii w ${lastBlockMs} ms, od ${chunk.first().startMs - pos} ms przed filmem, razem ${out.size}, publikacja=$r")
                if (r == PublishResult.USER_SWITCHED || r == PublishResult.PLAYER_GONE) { onState(""); return }
                onState("AI: przetłumaczono ${out.size} kwestii (na bieżąco)")
            }
        } finally {
            active = false
        }
    }
}
