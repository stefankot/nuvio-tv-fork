package com.nuvio.tv.fork.ai.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Cases found in the independent audit of 18.11.2026 (see docs/agent/ai-subtitles/AUDIT_REPORT.md). */
class AiSubtitleAuditTest {
    private fun candidate(id: String, lang: String, score: Int = 5) =
        AuxiliarySubtitleCandidate(id, lang, true, score)

    @Test fun addonIso6392CodesAndNamesAreCategorised() {
        val r = AuxiliaryLanguagePolicy.chooseForPolish(
            "eng", listOf(candidate("ru", "rus"), candidate("de", "ger"), candidate("pl", "pol", 99)),
        )
        assertEquals("ru", r.slavic?.sourceId)
        assertEquals("de", r.western?.sourceId)
        assertEquals("ru", AuxiliaryLanguagePolicy.chooseForPolish("en", listOf(candidate("ru", "Russian"))).slavic?.sourceId)
    }

    @Test fun originalLanguageIsExcludedAcrossCodeStyles() {
        val r = AuxiliaryLanguagePolicy.chooseForPolish("eng", listOf(candidate("en", "en", 99), candidate("fr", "fra")))
        assertEquals("fr", r.western?.sourceId)
        assertNull(AuxiliaryLanguagePolicy.chooseForPolish("rus", listOf(candidate("ru", "ru-RU"))).slavic)
    }

    private val source = listOf(
        SourceCue("1", 0, 2000, "One"), SourceCue("2", 2000, 4000, "Two"), SourceCue("3", 4000, 6000, "Three"),
    )

    private fun validate(vararg texts: String) = TranslationOutputContract.validate(
        "b", source, TranslationBlockReply(1, "b", texts.mapIndexed { i, t -> TranslatedCue("${i + 1}", t) }),
    )

    @Test fun spoofingCharactersAreRejected() {
        for (bad in listOf("a\uD800b", "a‮b", "a\u0085b", "a b", "a⁧b")) {
            assertFalse(bad, validate(bad, "x", "y").valid)
        }
        assertTrue(validate("Zażółć 😀", "x", "y").valid)
    }

    @Test fun crlfAndTrailingNewlinesAreNormalisedNotRetried() {
        val r = validate("a\r\nb\n", "x", "y")
        assertTrue(r.valid)
        assertEquals("a\nb", r.accepted!![0].translation)
        assertTrue(SubtitleExport.toSrt(r.accepted).startsWith("1\n00:00:00,000 --> 00:00:02,000\na\nb\n\n2\n"))
    }

    @Test fun untranslatedAndLoopedOutputIsFlagged() {
        assertTrue(validate("One", "x", "y").readabilityWarnings.contains("Untranslated: 1"))
        assertTrue(validate("Tak.", "Tak.", "Tak.").readabilityWarnings.contains("Repeated translation: 3"))
    }

    @Test fun exhaustionAndCorruptionAreDistinguishable() {
        val dir = Files.createTempDirectory("asb-audit").toFile()
        try {
            val budget = OrdinaryAudioBudget(dir)
            repeat(3) { budget.reserve("film", AudioSnippet("r$it", "a", 0, 20_000)) }
            assertThrows(AudioBudgetExhaustedException::class.java) {
                budget.reserve("film", AudioSnippet("r9", "a", 0, 1))
            }
            val ledger = dir.listFiles()!!.first { it.name.endsWith(".bin") }
            ledger.writeBytes(ledger.readBytes().copyOf(10))
            assertThrows(AudioBudgetCorruptedException::class.java) { OrdinaryAudioBudget(dir).snapshot("film") }
            ledger.writeBytes(ByteArray(0))
            assertThrows(AudioBudgetCorruptedException::class.java) { OrdinaryAudioBudget(dir).snapshot("film") }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun tempFilesOfKilledWritersAreCleaned() {
        val dir = Files.createTempDirectory("asb-audit").toFile()
        try {
            val budget = OrdinaryAudioBudget(dir)
            budget.reserve("film", AudioSnippet("r1", "a", 0, 1000))
            val ledger = dir.listFiles()!!.first { it.name.endsWith(".bin") }
            File(dir, "${ledger.name}.dead.tmp").writeText("x")
            assertEquals(1000L, budget.snapshot("film").accountedMs)
            assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
        } finally {
            dir.deleteRecursively()
        }
    }
}
