package com.nuvio.tv.fork.ai.subtitles

import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.TrackInfo
import com.nuvio.tv.ui.screens.player.autosync.AutoSyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

/**
 * Zgłoszenia właściciela 23.11 po poprawce d26d63cf0:
 *  - „Pottery”: napisy AI tylko przez chwilę (tryb na bieżąco pokazywał wyłącznie przetłumaczone kwestie),
 *  - „Made in Heaven”: po przewinięciu tylko angielskie (blok w nowym miejscu czekał na wolny wątek),
 *  - nowa zasada: automat AI dopiero po zakończeniu szukania napisów (dodatki + AutoSync Reshaped).
 */
class AiSubtitlesSeekAndLiveTest {
    private lateinit var dir: File
    @Before fun setUp() { dir = Files.createTempDirectory("ai-seek").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun cues(n: Int) = (0 until n).map { i -> DocCue("c%05d".format(i + 1), i * 2_000L, i * 2_000L + 1_500L, "Line $i") }

    @Test
    fun `na bieżąco do odtwarzacza idą wszystkie znane kwestie, nieprzetłumaczone w oryginale`() {
        val all = cues(5)
        val done = mapOf("c00002" to "Linia 1", "c00004" to "Linia 3")
        val out = LiveEmbeddedTranslation.combine(all, done, fallbackOriginal = true)
        assertEquals(listOf("Line 0", "Linia 1", "Line 2", "Linia 3", "Line 4"), out.map { it.text })
        // Ustawienie „Oryginał tam, gdzie tłumaczenie niegotowe” wyłączone: tylko polskie.
        assertEquals(listOf("Linia 1", "Linia 3"), LiveEmbeddedTranslation.combine(all, done, fallbackOriginal = false).map { it.text })
    }

    /** Tłumaczy „PL <tekst>” z opóźnieniem; zapisuje kolejność i liczbę zapytań w toku. */
    private class SlowProvider(val doc: () -> AiSubtitleDoc, val delayMs: Long) : TextModelProvider {
        override val providerId = "fake"; override val model = ModelPricing.DEFAULT_MODEL
        override val capabilities = ProviderCapabilities(contextTokens = 1_000_000, maxOutputTokens = 32_000)
        val started = java.util.Collections.synchronizedList(mutableListOf<Pair<String, Int>>()) // blok → ile w toku po starcie
        private val running = AtomicInteger()
        override suspend fun complete(request: ModelRequest): ModelResult {
            val blockId = Regex("block (b\\d{4})").find(request.task)!!.groupValues[1]
            started += blockId to running.incrementAndGet()
            try {
                delay(delayMs)
                val block = doc().blocks.first { it.id == blockId }
                val items = (block.first until block.endExclusive).joinToString(",") { i ->
                    """{"cue_id":"${doc().cues[i].id}","translation":"PL ${doc().cues[i].text}","flags":[]}"""
                }
                return ModelResult("""{"schema_version":1,"block_id":"$blockId","items":[$items],"glossary":[]}""", true, ModelUsage(1_000, 100, 0, 0))
            } finally {
                running.decrementAndGet()
            }
        }
    }

    private class MovablePort : PlayerPort {
        @Volatile var position = 0L
        override val materialId = "tt2|"; override val title = "Serial"
        val published = java.util.Collections.synchronizedList(mutableListOf<List<DocCue>>())
        override suspend fun positionMs() = position
        override suspend fun options() = emptyList<SubtitleOption>()
        override suspend fun selected(): SubtitleOption? = null
        override suspend fun audioLanguages() = listOf("en")
        override suspend fun displayedCues(option: SubtitleOption): List<DocCue>? = null
        override suspend fun loadCues(option: SubtitleOption) = emptyList<DocCue>()
        override suspend fun publish(cues: List<DocCue>, label: String): PublishResult { published += cues; return PublishResult.OK }
        override suspend fun restore(option: SubtitleOption) {}
    }

    @Test
    fun `po przewinięciu blok w nowym miejscu startuje od razu, bez czekania na wolny wątek`() {
        val c = cues(30)
        val ranges = BlockPlanner.plan(c, maxCues = 5)
        val d = AiSubtitleDoc(key = "seek", materialId = "tt2|", title = "Serial", sourceLanguage = "en", sourceRole = SourceRole.ORIGINAL,
            sourceLabel = "en", availability = SourceAvailability.FULL_TEXT, cues = c, aux = emptyList(), missingCategories = emptyList(),
            blocks = ranges.mapIndexed { i, r -> DocBlock("b%04d".format(i + 1), r.first, r.last + 1) },
            provider = "fake", model = ModelPricing.DEFAULT_MODEL, promptVersion = 1, createdAtMs = 0, updatedAtMs = 0)
        assertEquals(6, d.blocks.size)
        val provider = SlowProvider({ d }, delayMs = 1_800)
        val port = MovablePort()
        val eta = EtaEstimator()
        val ledger = SpendLedger(File(dir, "spend.json"), { 10.0 }, { 10.0 })
        val session = AiSubtitleSession(d, TranslationEngine(provider, ledger, eta, EngineSettings(review = false)),
            AiSubtitleStore(File(dir, "saved")), port, eta, AiMode.WATCH, 1, parallel = 2)
        session.engineReview = false
        runBlocking {
            launch { delay(150); port.position = 51_000 } // przewinięcie do ostatniego bloku (kwestie 25–29)
            session.run()
        }
        assertEquals(AiStage.DONE, session.state.value.stage)
        // Bloki 1 i 2 wystartowały od początku; ostatni (b0006) jako trzeci — gdy tamte dwa były jeszcze w toku.
        assertEquals(listOf("b0001", "b0002"), provider.started.take(2).map { it.first }.sorted())
        assertEquals("b0006", provider.started[2].first)
        assertEquals(3, provider.started[2].second)
    }

    @Test
    fun `automat AI czeka, aż dodatki i AutoSync skończą szukać`() {
        val loading = PlayerUiState(isLoadingAddonSubtitles = true)
        assertTrue(AiAuto.stillSearching(loading, null))
        assertTrue(AiAuto.stillSearching(PlayerUiState(), AutoSyncState.IN_PROGRESS))
        assertFalse(AiAuto.stillSearching(PlayerUiState(), AutoSyncState.NO_FIT))
        assertFalse(AiAuto.stillSearching(PlayerUiState(), null))
    }

    @Test
    fun `polski plik, którego AutoSync nie dopasował, to powód do AI, a dopasowany nie`() {
        val pl = Subtitle(id = "pl1", url = "https://s/pl.srt", lang = "pol", addonName = "OpenSubtitles", addonLogo = null)
        val s = PlayerUiState(addonSubtitles = listOf(pl), selectedAddonSubtitle = pl, subtitleTracks = listOf(TrackInfo(0, "English", "en")))
        assertEquals("AutoSync nie dopasował polskiego pliku", AiAuto.polishProblem(s, autoSyncNoFit = true))
        assertNull(AiAuto.polishProblem(s, autoSyncNoFit = false))
        assertEquals("brak polskich napisów", AiAuto.polishProblem(PlayerUiState(subtitleTracks = listOf(TrackInfo(0, "English", "en")))))
    }
}
