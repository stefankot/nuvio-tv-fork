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
        try {
            onState("AI: tłumaczę napisy wbudowane (${language}) na bieżąco")
            while (true) {
                val all = withContext(Dispatchers.Default) { cuesOf(controller, trackKey) }
                val pos = port.positionMs()
                val todo = all.filter { it.id !in done && it.endMs >= pos - 2_000 && it.startMs <= pos + AHEAD_MS }
                val urgent = todo.firstOrNull()?.let { it.startMs - pos < 30_000 } == true
                if (todo.isEmpty() || (todo.size < 8 && !urgent)) { delay(1_500); continue }
                val chunk = todo.take(CHUNK)
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
                val result = runCatching { engine.translateBlock(doc, 0) }.logged("ForkAi", "LiveEmbeddedTranslation.kt:89")
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
                Log.i(TAG, "blok $published: ${chunk.size} kwestii, razem ${out.size}, publikacja=$r")
                if (r == PublishResult.USER_SWITCHED || r == PublishResult.PLAYER_GONE) { onState(""); return }
                onState("AI: przetłumaczono ${out.size} kwestii (na bieżąco)")
            }
        } finally {
            active = false
        }
    }
}
