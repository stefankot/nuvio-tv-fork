package com.nuvio.tv.fork.ai.subtitles

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class AiSubtitlePipelineTest {
    private lateinit var dir: File
    @Before fun setUp() { dir = Files.createTempDirectory("ai-pipe").toFile() }
    @After fun tearDown() { dir.deleteRecursively() }

    private fun cues(n: Int, gapEvery: Int = 0) = (0 until n).map { i ->
        val start = i * 2_000L + if (gapEvery > 0) (i / gapEvery) * 10_000L else 0L
        DocCue("c%05d".format(i + 1), start, start + 1_500L, "Line $i")
    }

    private fun doc(n: Int = 10, perBlock: Int = 5, model: String = ModelPricing.DEFAULT_MODEL): AiSubtitleDoc {
        val c = cues(n)
        val ranges = BlockPlanner.plan(c, maxCues = perBlock)
        return AiSubtitleDoc(key = "k$n", materialId = "tt1|", title = "Film", sourceLanguage = "en", sourceRole = SourceRole.ORIGINAL,
            sourceLabel = "en", availability = SourceAvailability.FULL_TEXT, cues = c,
            aux = listOf(AuxTrack("AUX_SLAVIC", "ru", "ru1", "ru", "test", c.map { it.copy(text = "Строка") })),
            missingCategories = listOf("AUX_WESTERN"),
            blocks = ranges.mapIndexed { i, r -> DocBlock("b%04d".format(i + 1), r.first, r.last + 1) },
            provider = "fake", model = model, promptVersion = 1, createdAtMs = 0, updatedAtMs = 0)
    }

    /** Answers translation requests by echoing "PL <source>" for the requested cue ids. */
    private class FakeProvider(val doc: () -> AiSubtitleDoc, val bad: Int = 0, val corrections: Boolean = false) : TextModelProvider {
        override val providerId = "fake"; override val model = ModelPricing.DEFAULT_MODEL
        override val capabilities = ProviderCapabilities(contextTokens = 1_000_000, maxOutputTokens = 32_000)
        val calls = AtomicInteger(); val contexts = mutableListOf<String>(); private var badLeft = bad
        override suspend fun complete(request: ModelRequest): ModelResult {
            calls.incrementAndGet(); synchronized(contexts) { contexts += request.context }
            val blockId = Regex("block (b\\d{4})").find(request.task)!!.groupValues[1]
            val block = doc().blocks.first { it.id == blockId }
            val usage = ModelUsage(10_000, 500, 100, 8_000)
            if (request.task.startsWith("TASK: review")) {
                val fix = if (corrections) """{"cue_id":"${doc().cues[block.first].id}","translation":"Poprawione","problem":"sens"}""" else ""
                return ModelResult("""{"schema_version":1,"block_id":"$blockId","corrections":[$fix]}""", true, usage)
            }
            if (badLeft > 0) { badLeft--; return ModelResult("""{"schema_version":1,"block_id":"$blockId","items":[]}""", true, usage) }
            val items = (block.first until block.endExclusive).joinToString(",") { i -> """{"cue_id":"${doc().cues[i].id}","translation":"PL ${doc().cues[i].text}","flags":[]}""" }
            return ModelResult("""{"schema_version":1,"block_id":"$blockId","items":[$items],"glossary":[{"source":"Bob","target":"Bob"}]}""", true, usage)
        }
    }

    private class FakePort(var position: Long = 0, var switchAfter: Int = Int.MAX_VALUE) : PlayerPort {
        override val materialId = "tt1|"; override val title = "Film"
        val published = mutableListOf<List<DocCue>>()
        override suspend fun positionMs() = position
        override suspend fun options() = emptyList<SubtitleOption>()
        override suspend fun selected(): SubtitleOption? = null
        override suspend fun audioLanguages() = listOf("en")
        override suspend fun displayedCues(option: SubtitleOption): List<DocCue>? = null
        override suspend fun loadCues(option: SubtitleOption) = emptyList<DocCue>()
        override suspend fun publish(cues: List<DocCue>, label: String): PublishResult {
            if (published.size >= switchAfter) return PublishResult.USER_SWITCHED
            published += cues; return PublishResult.OK
        }
        override suspend fun restore(option: SubtitleOption) {}
    }

    private fun ledger(title: Double = 1.0, day: Double = 3.0) = SpendLedger(File(dir, "spend.json"), { title }, { day })

    @Test fun blocksCutAtPausesAndRespectLimits() {
        val c = cues(50, gapEvery = 7)
        val blocks = BlockPlanner.plan(c, maxCues = 10, firstBlockCues = 4)
        assertEquals(0, blocks.first().first); assertTrue(blocks.first().count() <= 4)
        assertTrue(blocks.all { it.count() <= 10 })
        assertEquals((0 until 50).toList(), blocks.flatten())
        // A long pause (index 6 → 7) is preferred as the cut point.
        assertTrue(blocks.any { it.last == 6 || it.last == 13 })
    }

    @Test fun priorityStartsAtPlaybackPosition() {
        val d = doc(20, 5)
        assertEquals(listOf(2, 3, 1, 0), BlockPlanner.priority(d.blocks, d.cues, positionMs = 21_000))
    }

    @Test fun parserRejectsMalformedAndAcceptsFenced() {
        assertNull(ReplyParser.translation("{\"schema_version\":1,\"block_id\":\"b\",\"items\":[{\"cue_id\":1}]}"))
        assertNull(ReplyParser.translation("not json"))
        val ok = ReplyParser.translation("```json\n{\"schema_version\":1,\"block_id\":\"b\",\"items\":[{\"cue_id\":\"c1\",\"translation\":\"Tak\"}],\"glossary\":[{\"source\":\"Bob\",\"target\":\"Bob\"}]}\n```")
        assertEquals("Tak", ok!!.reply.items.single().translation)
        assertEquals(mapOf("Bob" to "Bob"), ok.glossary)
        assertNull(ReplyParser.review("{\"schema_version\":1,\"block_id\":\"other\",\"corrections\":[]}", "b"))
    }

    @Test fun pricingUsesDateAndCache() {
        assertEquals(0.75, ModelPricing.priceFor("gemini-3.8-flash", LocalDate.of(2026, 11, 18))!!.inputPerM, 1e-9)
        assertEquals(1.50, ModelPricing.priceFor("gemini-3.8-flash", LocalDate.of(2027, 1, 2))!!.inputPerM, 1e-9)
        assertNull(ModelPricing.priceFor("unknown-model"))
        val p = ModelPricing.priceFor("gemini-3.8-flash", LocalDate.of(2026, 11, 18))!!
        val cost = ModelPricing.actualCost(p, ModelUsage(1_000_000, 100_000, 100_000, 900_000))
        assertEquals((100_000 * 0.75 + 900_000 * 0.075 + 200_000 * 3.75) / 1e6, cost, 1e-9)
    }

    @Test fun ledgerEnforcesLimitsAndFailsClosed() {
        val l = ledger(title = 0.5, day = 0.8)
        l.reserve("a", "t1", 0.4)
        assertThrows(SpendLimitException::class.java) { l.reserve("b", "t1", 0.2) }
        l.settle("a", 0.1)
        l.reserve("b", "t1", 0.2)
        l.reserve("c", "t2", 0.45)
        assertThrows(SpendLimitException::class.java) { l.reserve("d", "t3", 0.1) } // day: 0.1+0.2+0.45+0.1 > 0.8
        l.release("c")
        assertEquals(0.3, l.spentForDay(), 1e-9)
        File(dir, "spend.json").writeText("{broken")
        assertThrows(SpendLimitException::class.java) { ledger().reserve("e", "t1", 0.01) }
    }

    @Test fun geminiRequestShapeAndResponses() {
        val server = MockWebServer(); server.start()
        try {
            val url = server.url("/v1beta/interactions").toString()
            val ok = """{"status":"completed","steps":[{"type":"model_output","content":[{"type":"text","text":"{\"a\":1}"}]}],"usage":{"total_input_tokens":100,"total_output_tokens":20,"total_thought_tokens":5,"total_cached_tokens":60}}"""
            server.enqueue(MockResponse().setBody(ok))
            server.enqueue(MockResponse().setBody("""{"status":"incomplete","steps":[]}"""))
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "7").setBody("""{"error":{"message":"quota"}}"""))
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":{"message":"bad"}}"""))
            val p = GeminiProvider("test-key-123456789012345", "gemini-3.8-flash", baseUrl = url)
            val req = ModelRequest("sys", "CONTEXT", "TASK", TranslationPrompt.translationSchema, 1000)
            val r = runBlocking { p.complete(req) }
            assertEquals("{\"a\":1}", r.text); assertTrue(r.complete); assertEquals(60L, r.usage!!.cachedTokens)
            val sent = server.takeRequest()
            assertEquals("test-key-123456789012345", sent.getHeader("x-goog-api-key"))
            assertFalse(sent.requestUrl.toString().contains("key"))
            val body = Json.parseToJsonElement(sent.body.readUtf8()).jsonObject
            assertEquals("false", body["store"]!!.jsonPrimitive.content)
            assertTrue(body.toString().indexOf("CONTEXT") < body.toString().indexOf("TASK")) // stable prefix first
            assertFalse(runBlocking { p.complete(req) }.complete)
            val e429 = assertThrows(ProviderException::class.java) { runBlocking { p.complete(req) } }
            assertTrue(e429.transient); assertEquals(7_000L, e429.retryAfterMs)
            val e400 = assertThrows(ProviderException::class.java) { runBlocking { p.complete(req) } }
            assertFalse(e400.transient); assertFalse(e400.maybeSent)
        } finally { server.shutdown() }
    }

    @Test fun engineRepairsOnceAndAppliesReview() {
        val d = doc(10, 5)
        val provider = FakeProvider({ d }, bad = 1, corrections = true)
        val engine = TranslationEngine(provider, ledger(), EtaEstimator())
        val (rev, glossary) = runBlocking { engine.translateBlock(d, 0) }
        assertEquals("Poprawione", rev.translations[0]); assertEquals("PL Line 1", rev.translations[1])
        assertTrue(rev.checks.containsAll(listOf("structure", "review")))
        assertEquals(1, rev.reviewChanges.size); assertEquals(mapOf("Bob" to "Bob"), glossary)
        assertEquals(3, provider.calls.get()) // bad answer, repair, review
    }

    @Test fun engineFailsAfterRepairAndStopsAtLimit() {
        val d = doc(10, 5)
        assertThrows(BlockFailedException::class.java) {
            runBlocking { TranslationEngine(FakeProvider({ d }, bad = 2), ledger(), EtaEstimator(), EngineSettings(review = false)).translateBlock(d, 0) }
        }
        assertThrows(SpendLimitException::class.java) {
            runBlocking { TranslationEngine(FakeProvider({ d }), ledger(title = 0.0001), EtaEstimator()).translateBlock(d, 0) }
        }
    }

    @Test fun sessionPublishesProgressivelyWithFullContextAndResumesFree() {
        val d = doc(20, 5)
        val store = AiSubtitleStore(File(dir, "saved"))
        val provider = FakeProvider({ d })
        val port = FakePort(position = 21_000)
        val eta = EtaEstimator()
        val session = AiSubtitleSession(d, TranslationEngine(provider, ledger(), eta, EngineSettings(review = false)), store, port, eta, AiMode.WATCH, 1, parallel = 1)
        session.engineReview = false
        runBlocking { session.run() }
        assertEquals(AiStage.DONE, session.state.value.stage)
        assertTrue(session.doc.complete)
        // Block at the position (index 2) went first; the original stays where not yet translated.
        val first = port.published.first()
        assertTrue(first[10].text.startsWith("PL ")); assertEquals("Line 0", first[0].text)
        assertTrue(port.published.last().all { it.text.startsWith("PL ") })
        // Every request carried the whole film (last line and the helper) in its context.
        assertTrue(provider.contexts.all { it.contains("c00020") && it.contains("HELPER AUX_SLAVIC") && it.contains("MISSING HELPERS: AUX_WESTERN") })
        // Saved: a new session for the same key pays nothing.
        val saved = store.load(d.key)!!
        val again = FakeProvider({ saved })
        val s2 = AiSubtitleSession(saved, TranslationEngine(again, ledger(), eta), store, FakePort(), eta, AiMode.WATCH, 2)
        runBlocking { s2.run() }
        assertEquals(0, again.calls.get())
        assertEquals(AiStage.DONE, s2.state.value.stage)
    }

    @Test fun sessionStopsWhenUserPicksOtherSubtitles() {
        val d = doc(20, 5)
        val provider = FakeProvider({ d })
        val port = FakePort(switchAfter = 1)
        val eta = EtaEstimator()
        val s = AiSubtitleSession(d, TranslationEngine(provider, ledger(), eta, EngineSettings(review = false)), AiSubtitleStore(File(dir, "s")), port, eta, AiMode.WATCH, 3, parallel = 1)
        runBlocking { s.run() }
        assertEquals(AiStage.STOPPED, s.state.value.stage)
        assertTrue(provider.calls.get() < 4) // did not keep paying for the rest of the film
    }

    @Test fun prepareAllPublishesOnlyComplete() {
        val d = doc(10, 5)
        val port = FakePort()
        val eta = EtaEstimator()
        val s = AiSubtitleSession(d, TranslationEngine(FakeProvider({ d }), ledger(), eta, EngineSettings(review = false)), AiSubtitleStore(File(dir, "p")), port, eta, AiMode.PREPARE_ALL, 4)
        runBlocking { s.run() }
        assertEquals(1, port.published.size)
        assertTrue(port.published.single().all { it.text.startsWith("PL ") })
    }

    @Test fun gathererKeepsCategoriesAndExcludesSourceLanguage() {
        val src = cues(40)
        val port = object : PlayerPort by FakePort() {
            override suspend fun options() = listOf(
                SubtitleOption("en", "u/en", "eng", "EN"), SubtitleOption("pt", "u/pt", "por", "PT"),
                SubtitleOption("en2", "u/en2", "en", "EN2"), SubtitleOption("pl", "u/pl", "pol", "PL"),
            )
            override suspend fun audioLanguages() = listOf("en")
            override suspend fun loadCues(option: SubtitleOption) = src.map { it.copy(text = option.id + it.text) }
        }
        val g = runBlocking { SourceGatherer.gather(port) }
        assertEquals("u/en", g.source.url)
        assertEquals(listOf("pt"), g.aux.map { it.language }) // second English is not a helper; PT only as AUX_WESTERN
        assertEquals(listOf("AUX_SLAVIC"), g.missing)
    }

    @Test fun etaNeedsMeasurementsAndRounds() {
        val e = EtaEstimator()
        assertNull(e.estimateMs("m", "translate", 10))
        e.record("m", "translate", 10, 20_000)
        assertNotNull(e.estimateMs("m", "translate", 10))
        assertEquals("ok. 40 s", EtaEstimator.label(33_000))
        assertEquals("3–4 min", EtaEstimator.label(200_000))
    }

    @Test fun keyIgnoresTimingShift() {
        val a = cues(5); val b = a.map { it.copy(startMs = it.startMs + 700, endMs = it.endMs + 700) }
        assertEquals(AiSubtitleDoc.keyFor("pl", a, emptyList(), "m", 1), AiSubtitleDoc.keyFor("pl", b, emptyList(), "m", 1))
        assertFalse(AiSubtitleDoc.keyFor("pl", a, emptyList(), "m", 1) == AiSubtitleDoc.keyFor("pl", a, emptyList(), "m", 2))
    }

    @Test fun manifestHasNoSecretsAndMarksPartial() {
        val m = ExportManifest.build(doc(), "abc", "srt")
        assertTrue(m.contains("\"partial\":true")); assertTrue(m.contains("\"ai_translated\":true")); assertFalse(m.contains("key"))
    }
}
