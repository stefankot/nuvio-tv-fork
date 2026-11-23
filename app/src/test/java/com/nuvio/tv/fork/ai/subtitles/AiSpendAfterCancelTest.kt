package com.nuvio.tv.fork.ai.subtitles

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

/**
 * 23.11 (JMGO, napisyai4): „Limit na tytuł wyczerpany” — napisy AI nie pojawiały się, choć dymek mówił „tłumaczę”.
 * Każde przerwane zapytanie (wyjście z filmu, Wstecz po automatycznym włączeniu, inne napisy) zostawiało w rejestrze
 * rezerwację najgorszego przypadku (~0,12 $) na zawsze; po kilku przerwaniach limit 1 $ na film był „wyczerpany”.
 */
class AiSpendAfterCancelTest {
    private lateinit var dir: File
    @Before fun setUp() { dir = Files.createTempDirectory("ai-spend").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun doc(): AiSubtitleDoc {
        val c = (0 until 10).map { i -> DocCue("c%05d".format(i + 1), i * 2_000L, i * 2_000L + 1_500L, "Line $i") }
        return AiSubtitleDoc(key = "k", materialId = "tt1|", title = "Film", sourceLanguage = "en", sourceRole = SourceRole.ORIGINAL,
            sourceLabel = "en", availability = SourceAvailability.FULL_TEXT, cues = c, aux = emptyList(), missingCategories = emptyList(),
            blocks = listOf(DocBlock("b0001", 0, 5), DocBlock("b0002", 5, 10)),
            provider = "fake", model = ModelPricing.DEFAULT_MODEL, promptVersion = 1, createdAtMs = 0, updatedAtMs = 0)
    }

    /** Odpowiada „PL <tekst>”; [hang] = czeka, aż zapytanie zostanie przerwane (jak wyjście z filmu w trakcie). */
    private class Provider(val doc: AiSubtitleDoc, val hang: Boolean = false, val sent: CompletableDeferred<Unit>? = null) : TextModelProvider {
        override val providerId = "fake"; override val model = ModelPricing.DEFAULT_MODEL
        override val capabilities = ProviderCapabilities(contextTokens = 1_000_000, maxOutputTokens = 32_000)
        override suspend fun complete(request: ModelRequest): ModelResult {
            if (hang) { sent?.complete(Unit); awaitCancellation() }
            val blockId = Regex("block (b\\d{4})").find(request.task)!!.groupValues[1]
            val block = doc.blocks.first { it.id == blockId }
            val items = (block.first until block.endExclusive).joinToString(",") { i -> """{"cue_id":"${doc.cues[i].id}","translation":"PL ${doc.cues[i].text}","flags":[]}""" }
            return ModelResult("""{"schema_version":1,"block_id":"$blockId","items":[$items],"glossary":[]}""", true, ModelUsage(10_000, 500, 100, 8_000))
        }
    }

    private fun engine(l: SpendLedger, p: TextModelProvider) = TranslationEngine(p, l, EtaEstimator(), EngineSettings(review = false))

    @Test fun cancelledRequestsDoNotUseUpTheTitleLimit() = runBlocking {
        val d = doc()
        val l = SpendLedger(File(dir, "spend.json"), { 1.0 }, { 3.0 })
        engine(l, Provider(d)).translateBlock(d, 0) // zwykłe zapytanie: prawdziwy koszt ~0,008 $
        repeat(12) {
            val sent = CompletableDeferred<Unit>()
            val job = launch(Dispatchers.Default) { engine(l, Provider(d, hang = true, sent = sent)).translateBlock(d, 1) }
            sent.await()
            job.cancelAndJoin()
        }
        // Przed poprawką: 12 × ~0,12 $ > 1 $ → SpendLimitException „Limit na tytuł wyczerpany”, napisy nie pojawiały się.
        val (rev, _) = engine(l, Provider(d)).translateBlock(d, 1)
        assertEquals(listOf("PL Line 5", "PL Line 6", "PL Line 7", "PL Line 8", "PL Line 9"), rev.translations)
        assertTrue("wydane na film: ${l.spentForTitle("tt1|")}", l.spentForTitle("tt1|") < 0.2)
    }

    @Test fun oldUnsettledEntriesCountAsUsualCostButRunningOnesAsWorstCase() {
        val f = File(dir, "spend.json")
        val day = LocalDate.now().toString()
        // Stan z JMGO: dwa rozliczone zapytania i 10 przerwanych (rezerwacje sprzed poprawki).
        val old = (1..10).joinToString(",") { """{"id":"x$it","title":"tt9|","day":"$day","usd":0.125,"settled":false}""" }
        f.writeText("""{"entries":[{"id":"a","title":"tt9|","day":"$day","usd":0.01,"settled":true},{"id":"b","title":"tt8|","day":"$day","usd":0.03,"settled":true},$old]}""")
        val l = SpendLedger(f, { 1.0 }, { 3.0 })
        assertEquals(0.01 + 10 * 0.02, l.spentForTitle("tt9|"), 1e-9) // przerwane liczone po średniej 0,02 $
        l.reserve("r1", "tt9|", 0.125) // przed poprawką: 1,26 $ > 1 $ → limit
        // Zapytania w toku nadal liczą się w najgorszym przypadku (równoległe wątki nie przekroczą limitu).
        repeat(5) { l.reserve("p$it", "tt9|", 0.125) }
        assertThrows(SpendLimitException::class.java) { l.reserve("p9", "tt9|", 0.125) }
        l.finished("r1"); (0 until 5).forEach { l.finished("p$it") }
        l.reserve("p9", "tt9|", 0.125)
    }
}
