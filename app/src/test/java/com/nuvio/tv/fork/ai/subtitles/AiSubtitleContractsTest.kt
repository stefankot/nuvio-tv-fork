package com.nuvio.tv.fork.ai.subtitles

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AiSubtitleContractsTest {
    @Test fun polishRequiresBothCategories() {
        val r = AuxiliaryLanguagePolicy.chooseForPolish(
            originalLanguage = "es",
            candidates = listOf(
                AuxiliarySubtitleCandidate("ru", "ru", true, 45),
                AuxiliarySubtitleCandidate("fr", "fr", true, 90),
                AuxiliarySubtitleCandidate("pt", "pt", true, 92),
            ),
            modelChosenWesternSourceId = "fr",
        )
        assertEquals("ru", r.slavic?.sourceId)
        assertEquals("fr", r.western?.sourceId)
        assertTrue(r.complete)
        val withoutSlavic = AuxiliaryLanguagePolicy.chooseForPolish("es", listOf(
            AuxiliarySubtitleCandidate("pt", "pt", true, 92)))
        assertEquals(setOf("AUX_SLAVIC"), withoutSlavic.missingCategories)
    }

    @Test fun invalidModelSelectedCategoryIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AuxiliaryLanguagePolicy.chooseForPolish(
                "ru",
                listOf(AuxiliarySubtitleCandidate("uk", "uk", true, 99)),
                modelChosenWesternSourceId = "uk",
            )
        }
    }

    @Test fun wrongIdsOrStructureNeverProducePartialBlock() {
        val source = listOf(SourceCue("c1", 0, 1000, "Hola"), SourceCue("c2", 1000, 2000, "Adiós"))
        val reply = TranslationBlockReply(1, "block", listOf(
            TranslatedCue("c1", "Cześć"), TranslatedCue("c1", "{\\pos(2,3)}Żegnaj"),
        ))
        val result = TranslationOutputContract.validate("block", source, reply)
        assertFalse(result.valid)
        assertNull(result.accepted)
    }

    @Test fun audioReservationsSurviveReinstantiationAndCountOverlap() {
        val root = Files.createTempDirectory("nuvio-audio-budget-").toFile()
        try {
            val budget = OrdinaryAudioBudget(root)
            budget.reserve("film/version", AudioSnippet("req1", "original", 0, 20_000))
            budget.reserve("film/version", AudioSnippet("req2", "original", 0, 20_000))
            assertEquals(40_000L, OrdinaryAudioBudget(root).snapshot("film/version").accountedMs)
            assertThrows(IllegalArgumentException::class.java) {
                budget.reserve("film/version", AudioSnippet("req2", "original", 0, 20_000))
            }
            budget.reserve("film/version", AudioSnippet("req3", "original", 0, 20_000))
            assertThrows(IllegalStateException::class.java) {
                budget.reserve("film/version", AudioSnippet("req4", "original", 0, 1_000))
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun audioConcurrencyNeverExceedsSixtySeconds() {
        val root = Files.createTempDirectory("nuvio-audio-parallel-").toFile()
        val pool = Executors.newFixedThreadPool(6)
        try {
            val results = (0..11).map { i -> pool.submit<Boolean> {
                try {
                    OrdinaryAudioBudget(root).reserve("same-film", AudioSnippet("r$i", "audio", 0, 10_000))
                    true
                } catch (_: IllegalStateException) { false }
            } }
            assertEquals(6, results.count { it.get(10, TimeUnit.SECONDS) })
            assertEquals(60_000L, OrdinaryAudioBudget(root).snapshot("same-film").accountedMs)
        } finally { pool.shutdownNow(); root.deleteRecursively() }
    }

    @Test fun deterministicSelectionPreservesTieRules() {
        val candidates = listOf(
            AuxiliarySubtitleCandidate("z", "ru", true, 30),
            AuxiliarySubtitleCandidate("a", "ru", true, 30),
            AuxiliarySubtitleCandidate("west-z", "fr", true, 80),
            AuxiliarySubtitleCandidate("west-a", "de", true, 80),
        )
        val selected = AuxiliaryLanguagePolicy.chooseForPolish("es", candidates)
        assertEquals("z", selected.slavic?.sourceId) // stable input order within a priority/score tie
        assertEquals("west-a", selected.western?.sourceId) // lowest ID breaks Western ties
    }

    @Test fun unicodeReadabilityChecksVisibleCodePoints() {
        val reply = TranslationBlockReply(1, "b", listOf(TranslatedCue("c", "😀😀")))
        val result = TranslationOutputContract.validate(
            "b", listOf(SourceCue("c", 0, 1000, "Hello")), reply,
            maxCharsPerLine = 2, maxLines = 1, maxCharsPerSecond = 2.0,
        )
        assertTrue(result.valid)
        assertTrue(result.readabilityWarnings.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            TranslationOutputContract.validate(
                "b", listOf(SourceCue("c", 0, 1000, "Hello")), reply,
                maxCharsPerSecond = Double.NaN,
            )
        }
    }

    @Test fun exportSupportsOver99HoursAndStableOrdering() {
        val cues = listOf(
            ValidatedCue(SourceCue("end", 360_000_005, 360_001_006, "A"), "A"),
            ValidatedCue(SourceCue("first", 0, 9, "B"), "B"),
            ValidatedCue(SourceCue("tie", 0, 10, "C"), "C"),
        )
        val srt = SubtitleExport.toSrt(cues)
        assertTrue(srt.contains("100:00:00,005 --> 100:00:01,006"))
        assertTrue(srt.indexOf("B") < srt.indexOf("C"))
    }

    @Test fun readonlyAndIdempotentAudioOperationsAvoidDiskWrites() {
        val folder = Files.createTempDirectory("nuvio-audio-idempotent-").toFile()
        try {
            val budget = OrdinaryAudioBudget(folder)
            budget.reserve("film", AudioSnippet("one", "audio", 0, 5000))
            budget.markSent("film", "one")
            val ledger = folder.listFiles()!!.single { it.extension == "bin" }
            assertTrue(ledger.setLastModified(946684800000L))
            assertEquals(5000L, budget.snapshot("film").accountedMs)
            budget.markSent("film", "one")
            assertEquals(946684800000L, ledger.lastModified())
        } finally { folder.deleteRecursively() }
    }
}
