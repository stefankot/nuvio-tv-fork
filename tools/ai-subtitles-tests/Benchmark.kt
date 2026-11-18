import com.nuvio.tv.fork.ai.subtitles.*
import kotlin.random.Random

/** Reproducible JVM microbenchmark, not an Android-device benchmark. */
fun main() {
    fun measure(name: String, rounds: Int, work: () -> Unit) {
        repeat(3) { work() }
        val timings = LongArray(rounds) {
            val start = System.nanoTime()
            work()
            (System.nanoTime() - start) / 1_000_000L
        }.sorted()
        println("$name: median=${timings[timings.size / 2]} ms; rounds=${timings.joinToString()}")
    }
    val cues = (0 until 6_000).map { i ->
        val start = i * 3_000L
        ValidatedCue(SourceCue("c$i", start, start + 2_200L, "Original $i"), "Tłumaczenie numer $i")
    }
    measure("export-srt-and-vtt-6000", 7) {
        check(SubtitleExport.toSrt(cues).length > 1_000)
        check(SubtitleExport.toWebVtt(cues).length > 1_000)
    }
    val sourceCues = cues.map { it.source }
    val reply = TranslationBlockReply(1, "block", cues.map { TranslatedCue(it.source.id, it.translation) })
    measure("validate-6000", 7) { check(TranslationOutputContract.validate("block", sourceCues, reply).valid) }
    val langs = AuxiliaryLanguagePolicy.defaultSlavicPriority + AuxiliaryLanguagePolicy.westernLanguages.sorted()
    val random = Random(52)
    val choices = (0 until 6_000).map { i ->
        AuxiliarySubtitleCandidate("candidate-$i", langs[random.nextInt(langs.size)], true, random.nextInt(100))
    }
    measure("choose-6000", 7) {
        check(AuxiliaryLanguagePolicy.chooseForPolish("ja", choices).complete)
    }
}
