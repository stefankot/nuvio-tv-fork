import com.nuvio.tv.fork.ai.subtitles.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private fun verify(cond: Boolean, msg: String) { if (!cond) error(msg) }
private inline fun <reified T: Throwable> fails(block: () -> Unit) {
    try { block() } catch (e: Throwable) { if (e is T) return else throw e }
    error("Expected ${T::class.simpleName}")
}

fun main() {
    var passed = 0
    fun case(name: String, block: () -> Unit) {
        block(); println("PASS: $name"); passed++
    }
    case("PL: Russian and Western are both mandatory") {
        val candidates = listOf(
            AuxiliarySubtitleCandidate("r", "ru-RU", true, 40),
            AuxiliarySubtitleCandidate("e", "en", true, 95),
            AuxiliarySubtitleCandidate("f", "fr", true, 80),
        )
        val selection = AuxiliaryLanguagePolicy.chooseForPolish("es", candidates, modelChosenWesternSourceId = "f")
        verify(selection.complete && selection.slavic?.sourceId == "r" && selection.western?.sourceId == "f", "Invalid categories")
        val missing = AuxiliaryLanguagePolicy.chooseForPolish("es", candidates.filter { it.sourceId != "r" })
        verify(!missing.complete && missing.missingCategories == setOf("AUX_SLAVIC"), "Missing Slavic must be explicit")
    }
    case("RU original, PL and original excluded, model cannot select second Western") {
        val candidates = listOf(
            AuxiliarySubtitleCandidate("r", "ru", true, 90),
            AuxiliarySubtitleCandidate("u", "uk", true, 82),
            AuxiliarySubtitleCandidate("p", "pl", true, 100),
            AuxiliarySubtitleCandidate("d", "de", true, 65),
        )
        val selected = AuxiliaryLanguagePolicy.chooseForPolish("ru", candidates)
        verify(selected.slavic?.sourceId == "u" && selected.western?.sourceId == "d", "Invalid fallback")
        fails<IllegalArgumentException> { AuxiliaryLanguagePolicy.chooseForPolish("ru", candidates, modelChosenWesternSourceId = "u") }
    }
    case("Reject unverified high-scored and excluded languages") {
        val candidates = listOf(
            AuxiliarySubtitleCandidate("r", "ru", false, 100),
            AuxiliarySubtitleCandidate("c", "cs", true, 20),
            AuxiliarySubtitleCandidate("e", "en", true, 100),
            AuxiliarySubtitleCandidate("p", "pt", true, 60),
        )
        val s = AuxiliaryLanguagePolicy.chooseForPolish("es", candidates, excludedLanguages = setOf("en"))
        verify(s.slavic?.sourceId == "c" && s.western?.sourceId == "p", "Misleading source selection")
    }
    val source = listOf(SourceCue("c0001", 0, 2000, "Hello"), SourceCue("c0002", 2000, 4000, "World"))
    val good = TranslationBlockReply(1, "b001", listOf(TranslatedCue("c0001", "Cześć"), TranslatedCue("c0002", "Świecie")))
    case("Atomic block validation and export preserve timing") {
        val checked = TranslationOutputContract.validate("b001", source, good)
        verify(checked.valid && checked.accepted?.size == 2, "Expected valid block")
        val srt = SubtitleExport.toSrt(checked.accepted!!)
        val vtt = SubtitleExport.toWebVtt(checked.accepted)
        verify("00:00:00,000 --> 00:00:02,000" in srt && "00:00:02.000 --> 00:00:04.000" in vtt, "Export timing damaged")
    }
    case("Bad output must not be partially accepted") {
        val duplicate = good.copy(items = listOf(good.items[0], good.items[0]))
        verify(!TranslationOutputContract.validate("b001", source, duplicate).valid, "Duplicates accepted")
        val injected = good.copy(items = listOf(good.items[0], TranslatedCue("c0002", "{\\pos(1,2)}Hello")))
        verify(!TranslationOutputContract.validate("b001", source, injected).valid, "ASS instruction accepted")
        val vttBreak = good.copy(items = listOf(good.items[0], TranslatedCue("c0002", "A\n\n00:00:04 --> 00:05:00")))
        verify(!TranslationOutputContract.validate("b001", source, vttBreak).valid, "Timing injection accepted")
        verify(!TranslationOutputContract.validate("wrong", source, good).valid, "Wrong block accepted")
    }
    case("Readability issues warn, without deleting source meaning") {
        val long = good.copy(items = listOf(TranslatedCue("c0001", "A".repeat(70)), good.items[1]))
        val checked = TranslationOutputContract.validate("b001", source, long)
        verify(checked.valid && checked.readabilityWarnings.isNotEmpty(), "Expected warning")
    }
    val tmp = Files.createTempDirectory("nuvio-budget-test-").toFile()
    try {
        case("Reserve persists across new process/object and restart") {
            val budget = OrdinaryAudioBudget(tmp)
            val a = AudioSnippet("request-01", "original-audio", 0L, 20_000L)
            verify(budget.reserve("film/edition/a", a).accountedMs == 20_000L, "First reservation")
            verify(OrdinaryAudioBudget(tmp).snapshot("film/edition/a").accountedMs == 20_000L, "Persistent")
            fails<IllegalArgumentException> { budget.reserve("film/edition/a", a) } // no free retry
        }
        case("Uncertain transmissions stay charged, overlapping uploads double count") {
            val budget = OrdinaryAudioBudget(tmp)
            budget.reserve("film/edition/a", AudioSnippet("request-02", "original-audio", 0L, 20_000L))
            budget.markSent("film/edition/a", "request-01")
            budget.reserve("film/edition/a", AudioSnippet("request-03", "original-audio", 2_000L, 20_000L))
            verify(budget.snapshot("film/edition/a").accountedMs == 58_000L, "Overlapping samples must be charged twice")
            fails<IllegalStateException> {
                budget.reserve("film/edition/a", AudioSnippet("request-04", "original-audio", 21_000L, 25_000L))
            }
            verify(budget.snapshot("film/edition/a").accountedMs == 58_000L, "Rejected request changed state")
        }
        case("Releasing only positively unsent reservations") {
            val budget = OrdinaryAudioBudget(tmp)
            budget.markDefinitelyNotSent("film/edition/a", "request-02")
            verify(budget.snapshot("film/edition/a").accountedMs == 38_000L, "Did not release definitely unsent")
            fails<IllegalStateException> { budget.markDefinitelyNotSent("film/edition/a", "request-01") }
            fails<IllegalArgumentException> {
                budget.reserve("film/edition/a", AudioSnippet("request-01", "original-audio", 0L, 10_000L))
            }
        }
        case("Concurrent reservations cannot exceed budget") {
            val service = Executors.newFixedThreadPool(8)
            try {
                val tasks = (0..15).map { i ->
                    service.submit<Boolean> {
                        try {
                            OrdinaryAudioBudget(tmp).reserve("concurrent/title", AudioSnippet("req-$i", "audio", 0, 10_000))
                            true
                        } catch (e: IllegalStateException) { false }
                    }
                }
                val successes = tasks.count { it.get(10, TimeUnit.SECONDS) }
                verify(successes == 6, "Expected exactly six 10s reservations, got $successes")
                verify(OrdinaryAudioBudget(tmp).snapshot("concurrent/title").accountedMs == 60_000L, "Concurrent overrun")
            } finally { service.shutdownNow() }
        }
        case("Corruption fails closed instead of resetting spent budget") {
            val budget = OrdinaryAudioBudget(tmp)
            budget.reserve("corrupt/title", AudioSnippet("corrupt-1", "audio", 0, 5_000))
            val corruptDir = Files.createTempDirectory("nuvio-corrupt-test-").toFile()
            try {
                val other = OrdinaryAudioBudget(corruptDir)
                other.reserve("corrupt/title", AudioSnippet("bad-1", "audio", 0, 5_000))
                val stored = corruptDir.listFiles()!!.first { it.extension == "bin" }
                stored.writeBytes(byteArrayOf(0, 0, 0, 0))
                fails<IllegalStateException> { other.snapshot("corrupt/title") }
                fails<IllegalStateException> { other.reserve("corrupt/title", AudioSnippet("bad-2", "audio", 0, 5_000)) }
            } finally { corruptDir.deleteRecursively() }
        }
        case("Material key isolates episodes, never resets a single episode") {
            val budget = OrdinaryAudioBudget(tmp)
            verify(budget.snapshot("series/episode-2").accountedMs == 0L, "Cross-episode leakage")
            verify(budget.snapshot("film/edition/a").accountedMs == 38_000L, "Accidental reset")
        }
    } finally { tmp.deleteRecursively() }
    case("Priority and Western tie-breaking agree with legacy selection") {
        val languages = AuxiliaryLanguagePolicy.defaultSlavicPriority + AuxiliaryLanguagePolicy.westernLanguages.sorted()
        val random = kotlin.random.Random(20261118)
        repeat(750) {
            val candidates = (0 until random.nextInt(1, 30)).map { i ->
                AuxiliarySubtitleCandidate("id-$i", languages[random.nextInt(languages.size)], true, random.nextInt(0, 100))
            }
            val referenceSlavic = candidates.filter { it.languageTag in AuxiliaryLanguagePolicy.defaultSlavicPriority }
                .sortedWith(compareBy<AuxiliarySubtitleCandidate> {
                    AuxiliaryLanguagePolicy.defaultSlavicPriority.indexOf(it.languageTag)
                }.thenByDescending { it.matchingScore }).firstOrNull()
            val referenceWestern = candidates.filter { it.languageTag in AuxiliaryLanguagePolicy.westernLanguages }
                .maxWithOrNull(compareBy<AuxiliarySubtitleCandidate> { it.matchingScore }.thenByDescending { it.sourceId })
            val chosen = AuxiliaryLanguagePolicy.chooseForPolish("ja", candidates)
            verify(chosen.slavic == referenceSlavic && chosen.western == referenceWestern, "Selection order changed")
        }
    }
    case("Timestamp padding and stable source-order export") {
        val cues = listOf(
            ValidatedCue(SourceCue("later", 360_000_005, 360_001_006, "Later"), "Później"),
            ValidatedCue(SourceCue("first", 0, 9, "Start"), "Start"),
            ValidatedCue(SourceCue("tie", 0, 10, "Tie"), "Ten sam czas"),
        )
        val srt = SubtitleExport.toSrt(cues)
        val vtt = SubtitleExport.toWebVtt(cues)
        verify("100:00:00,005 --> 100:00:01,006" in srt, "Hours >= 100 were changed")
        verify("00:00:00.000 --> 00:00:00.009" in vtt, "Millisecond padding changed")
        verify(srt.indexOf("Start") < srt.indexOf("Ten sam czas"), "Stable sorting changed")
    }
    case("Unicode readability scan counts surrogate pairs once") {
        val cue = listOf(SourceCue("emoji", 0, 1000, "Hello"))
        val reply = TranslationBlockReply(1, "emoji-block", listOf(TranslatedCue("emoji", "😀😀")))
        val validated = TranslationOutputContract.validate(
            "emoji-block", cue, reply, maxCharsPerLine = 2, maxLines = 1, maxCharsPerSecond = 2.0,
        )
        verify(validated.valid && validated.readabilityWarnings.isEmpty(), "Unicode code points miscounted")
        fails<IllegalArgumentException> {
            TranslationOutputContract.validate("emoji-block", cue, reply, maxCharsPerSecond = Double.NaN)
        }
    }
    case("Budget snapshots and idempotent SENT do not rewrite the ledger") {
        val folder = Files.createTempDirectory("nuvio-budget-readonly-").toFile()
        try {
            val budget = OrdinaryAudioBudget(folder)
            budget.reserve("material", AudioSnippet("only", "audio", 0, 5_000))
            budget.markSent("material", "only")
            val ledger = folder.listFiles()!!.single { it.extension == "bin" }
            verify(ledger.setLastModified(946684800000L), "Cannot set test file timestamp")
            budget.snapshot("material")
            budget.markSent("material", "only")
            verify(ledger.lastModified() == 946684800000L, "Read-only or idempotent operation rewrote ledger")
            verify(budget.snapshot("material").accountedMs == 5_000L, "Charge changed")
        } finally { folder.deleteRecursively() }
    }
    println("TOTAL PASS: $passed")
}
