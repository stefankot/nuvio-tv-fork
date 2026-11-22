package com.nuvio.tv.fork.ai.subtitles

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.nuvio.tv.fork.logged

/** A subtitle file offered by the player (add-on or stream). */
data class SubtitleOption(val id: String, val url: String, val language: String, val label: String)

enum class PublishResult { OK, RETRY_LATER, USER_SWITCHED, PLAYER_GONE }

/** Everything the session needs from the player; the Exo implementation lives in [ExoPlayerPort]. */
interface PlayerPort {
    val materialId: String
    val title: String
    suspend fun positionMs(): Long
    suspend fun options(): List<SubtitleOption>
    suspend fun selected(): SubtitleOption?
    suspend fun audioLanguages(): List<String>
    /** Cues of [option] as the player shows them now (synced timing), or null when it is not the active one. */
    suspend fun displayedCues(option: SubtitleOption): List<DocCue>?
    suspend fun loadCues(option: SubtitleOption): List<DocCue>
    /** Atomically shows [cues] (immutable full set). The first call takes over the subtitle slot. */
    suspend fun publish(cues: List<DocCue>, label: String): PublishResult
    suspend fun restore(option: SubtitleOption)
}

enum class AiMode { WATCH, PREPARE_ALL }
enum class AiStage { SOURCES, TRANSLATING, WAITING, DONE, STOPPED, FAILED }

data class AiState(
    val sessionId: Long,
    val materialId: String,
    val stage: AiStage,
    val doneBlocks: Int = 0,
    val totalBlocks: Int = 0,
    val bufferEta: String? = null,
    val totalEta: String? = null,
    val message: String = "",
    val costUsd: Double = 0.0,
    val published: Boolean = false,
    val notice: String = "",
)

/** Rules for choosing the source and the two helpers, then loading them through the player. */
object SourceGatherer {
    const val MAX_CUES = 50_000
    private const val PER_CATEGORY = 3

    data class Gathered(val source: SubtitleOption, val role: SourceRole, val language: String, val cues: List<DocCue>, val aux: List<AuxTrack>, val missing: List<String>, val reasons: List<String>)

    suspend fun gather(port: PlayerPort, target: String = "pl"): Gathered {
        val options = port.options()
        val original = port.audioLanguages().firstOrNull()?.let(AuxiliaryLanguagePolicy::primaryLanguage)
        fun lang(o: SubtitleOption) = AuxiliaryLanguagePolicy.primaryLanguage(o.language)
        val notTarget = options.filter { lang(it) != target }
        // Zgłoszenie 9a (Tip Toe): film miał tylko napisy wbudowane w plik — moduł czyta na razie wyłącznie napisy z dodatków.
        if (options.isEmpty()) throw BlockFailedException("Brak napisów z dodatków (np. OpenSubtitles). Napisów wbudowanych w plik AI jeszcze nie czyta")
        if (notTarget.isEmpty()) throw BlockFailedException("Brak napisów źródłowych w innym języku niż polski")
        // The user's own non-Polish choice first, then the original language, then English as an intermediary.
        val selected = port.selected()?.takeIf { lang(it) != target }
        val ordered = (listOfNotNull(selected) + notTarget.filter { original != null && lang(it) == original } +
            notTarget.filter { lang(it) == "en" } + notTarget).distinctBy { it.url }
        var source: SubtitleOption? = null
        var cues: List<DocCue> = emptyList()
        for (o in ordered.take(4)) {
            cues = runCatching { port.displayedCues(o) ?: port.loadCues(o) }.logged("ForkAi", "AiSubtitleSession.kt:73").getOrDefault(emptyList())
            if (cues.size >= 20) { source = o; break }
        }
        if (source == null) throw BlockFailedException("Nie udało się wczytać napisów źródłowych")
        if (cues.size > MAX_CUES) throw BlockFailedException("Za dużo kwestii (${cues.size})")
        val sourceLang = lang(source)
        val role = if (original == null || original == sourceLang) SourceRole.ORIGINAL else SourceRole.INTERMEDIARY_SOURCE
        val reasons = mutableListOf("Źródło: ${source.label} (${sourceLang}${if (role == SourceRole.INTERMEDIARY_SOURCE) ", pośrednie" else ""})")

        // Helpers: up to 3 files per category are downloaded and verified against the source timing.
        val slavicOrder = AuxiliaryLanguagePolicy.defaultSlavicPriority
        val excluded = setOf(sourceLang)
        fun eligible(o: SubtitleOption) = o.url != source.url && lang(o) != target && lang(o) != sourceLang && lang(o) != original
        val slavic = notTarget.filter { eligible(it) && lang(it) in slavicOrder }.sortedBy { slavicOrder.indexOf(lang(it)) }.take(PER_CATEGORY)
        // 22.11: film spoza Europy (oryginał np. ja/ko/hi) — angielskie pliki pierwsze w kolejce do sprawdzenia i preferowane.
        val nonEuropean = original != null && !AuxiliaryLanguagePolicy.isEuropean(original)
        val western = notTarget.filter { eligible(it) && lang(it) in AuxiliaryLanguagePolicy.westernLanguages }
            .sortedBy { if (lang(it) == "en") 0 else 1 }.take(PER_CATEGORY)
        val loaded = HashMap<String, List<DocCue>>()
        val candidates = (slavic + western).map { o ->
            val c = runCatching { port.loadCues(o) }.logged("ForkAi", "AiSubtitleSession.kt:90").getOrDefault(emptyList()).take(MAX_CUES)
            loaded[o.id] = c
            val coverage = coverage(cues, c)
            AuxiliarySubtitleCandidate(o.id, o.language, acceptedAfterVerification = c.size >= cues.size / 2 && coverage >= 0.6, matchingScore = (coverage * 100).toInt())
        }
        val pick = AuxiliaryLanguagePolicy.chooseForPolish(original ?: sourceLang, candidates, excluded, preferredWestern = if (nonEuropean) "en" else null)
        if (nonEuropean) reasons += "Film spoza Europy (oryginał $original): pomocnicze najlepiej rosyjski i angielski"
        val byId = (slavic + western).associateBy { it.id }
        val aux = listOfNotNull(pick.slavic?.let { "AUX_SLAVIC" to it }, pick.western?.let { "AUX_WESTERN" to it }).map { (cat, cand) ->
            val o = byId.getValue(cand.sourceId)
            val reason = "dopasowanie czasu ${cand.matchingScore}%"
            reasons += "$cat: ${o.label} (${lang(o)}, $reason)"
            AuxTrack(cat, lang(o), o.id, o.label, reason, loaded.getValue(o.id))
        }
        pick.missingCategories.forEach { reasons += "$it: brak poprawnego pliku" }
        return Gathered(source, role, sourceLang, cues, aux, pick.missingCategories.sorted(), reasons)
    }

    /** Share of source cues overlapped in time by some helper cue (both lists sorted by start). */
    fun coverage(source: List<DocCue>, other: List<DocCue>): Double {
        if (source.isEmpty() || other.isEmpty()) return 0.0
        var j = 0; var hit = 0
        for (s in source) {
            while (j < other.size && other[j].endMs <= s.startMs) j++
            if (j < other.size && other[j].startMs < s.endMs) hit++
        }
        return hit.toDouble() / source.size
    }
}

/**
 * One translation run for one material. At most [parallel] requests; the block at the playback position goes
 * first and a seek re-plans the queue. In WATCH mode each checked block is published as soon as it is ready;
 * PREPARE_ALL publishes only the complete text. Lines already on screen keep their old text until they end.
 */
class AiSubtitleSession(
    initial: AiSubtitleDoc,
    private val engine: TranslationEngine,
    private val store: AiSubtitleStore,
    private val port: PlayerPort,
    private val eta: EtaEstimator,
    private val mode: AiMode,
    val sessionId: Long,
    private val fallbackOriginal: Boolean = true,
    private val parallel: Int = 2,
    private val bufferMs: Long = 180_000L,
) {
    private val docLock = Mutex()
    private val publishLock = Mutex()
    @Volatile var doc: AiSubtitleDoc = initial; private set
    private val inFlight = HashMap<Int, Long>()
    private val failures = HashMap<Int, Int>()
    private var lastPublished: List<DocCue>? = null
    private var cost = 0.0
    private val _state = MutableStateFlow(AiState(sessionId, initial.materialId, AiStage.TRANSLATING, initial.doneBlocks, initial.blocks.size))
    val state: StateFlow<AiState> = _state
    @Volatile private var stopReason: String? = null
    @Volatile private var waitingUntil = 0L

    fun onEngineEvent(e: EngineEvent) {
        when (e) {
            is EngineEvent.Cost -> { cost += e.usd; _state.update { it.copy(costUsd = cost) } }
            is EngineEvent.Waiting -> { waitingUntil = e.untilMs; _state.update { it.copy(stage = AiStage.WAITING, notice = "AI: oczekiwanie (${e.reason})") } }
        }
    }

    suspend fun run() {
        try {
            if (mode == AiMode.WATCH && doc.doneBlocks > 0) publish()
            refreshState()
            coroutineScope {
                val ticker = launch { while (true) { delay(5_000); refreshState() } } // label refresh, no flicker
                val workers = List(parallel.coerceIn(1, 2)) { launch { worker() } }
                workers.forEach { it.join() }
                ticker.cancel()
            }
            if (stopReason != null) return finish(AiStage.STOPPED, stopReason!!)
            if (mode == AiMode.PREPARE_ALL && doc.complete || mode == AiMode.WATCH) publish()
            if (doc.complete) finish(AiStage.DONE, if (_state.value.published) "Napisy gotowe" else "Napisy gotowe (zapisane)")
            else finish(AiStage.FAILED, "Część bloków nie powiodła się: pokazany oryginał w tych miejscach")
        } catch (e: CancellationException) {
            _state.update { it.copy(stage = AiStage.STOPPED, message = "Anulowano", notice = "") }
            throw e
        } catch (e: SpendLimitException) {
            android.util.Log.w("ForkAi", "AiSubtitleSession: limit kosztów: ${e.message?.take(200)}")
            finish(AiStage.STOPPED, e.message ?: "Limit kosztów")
        }
    }

    private fun finish(stage: AiStage, message: String) {
        _state.update { it.copy(stage = stage, message = message, notice = message, bufferEta = null, totalEta = null) }
    }

    private suspend fun nextBlock(): Int? = docLock.withLock {
        if (stopReason != null) return null
        val position = port.positionMs()
        val order = BlockPlanner.priority(doc.blocks, doc.cues, position)
        order.firstOrNull { i -> doc.blocks[i].accepted == null && i !in inFlight && (failures[i] ?: 0) < 2 }
            ?.also { inFlight[it] = System.currentTimeMillis() }
    }

    private suspend fun worker() {
        while (true) {
            val index = nextBlock() ?: return
            try {
                val (revision, glossary) = engine.translateBlock(doc, index)
                docLock.withLock {
                    val blocks = doc.blocks.toMutableList()
                    blocks[index] = blocks[index].copy(revisions = blocks[index].revisions + revision, lastError = null)
                    val merged = doc.glossary.toMutableMap().apply { glossary.forEach { (k, v) -> putIfAbsent(k, v) } }
                    doc = doc.copy(blocks = blocks, glossary = merged, updatedAtMs = System.currentTimeMillis())
                    inFlight.remove(index)
                    store.save(doc)
                }
                if (mode == AiMode.WATCH) publish()
            } catch (e: BlockFailedException) {
                android.util.Log.w("ForkAi", "AiSubtitleSession: blok $index nieudany: ${e.message?.take(200)}")
                docLock.withLock {
                    inFlight.remove(index); failures[index] = (failures[index] ?: 0) + 1
                    val blocks = doc.blocks.toMutableList(); blocks[index] = blocks[index].copy(lastError = e.message)
                    doc = doc.copy(blocks = blocks); store.save(doc)
                }
            } finally {
                docLock.withLock { inFlight.remove(index) }
            }
            refreshState()
        }
    }

    /** Full immutable set: checked translations, and the original (or nothing) where a block is not ready. */
    fun combined(d: AiSubtitleDoc = doc): List<DocCue> = d.cues.mapIndexedNotNull { i, c ->
        val t = d.translationFor(i)
        when {
            t != null -> c.copy(text = t)
            fallbackOriginal -> c
            else -> null
        }
    }

    private suspend fun publish() = publishLock.withLock {
        val next = combined()
        val previous = lastPublished
        if (previous == next) return@withLock
        // Do not change a line that is on screen: wait (bounded) until it ends.
        if (previous != null) {
            val changed = next.filter { n -> previous.firstOrNull { it.id == n.id }?.text != n.text }
            for (i in 0 until 40) {
                val pos = port.positionMs()
                if (changed.none { pos >= it.startMs && pos < it.endMs }) break
                delay(250)
            }
        }
        var result: PublishResult
        var tries = 0
        do {
            result = port.publish(next, "AI PL")
            if (result == PublishResult.RETRY_LATER) delay(300)
        } while (result == PublishResult.RETRY_LATER && ++tries < 40)
        when (result) {
            PublishResult.OK -> { lastPublished = next; _state.update { it.copy(published = true) } }
            PublishResult.USER_SWITCHED -> stopReason = "Wybrano inne napisy: tłumaczenie zatrzymane (postęp zapisany)"
            PublishResult.PLAYER_GONE -> stopReason = "Odtwarzanie zakończone (postęp zapisany)"
            PublishResult.RETRY_LATER -> Unit
        }
    }

    private suspend fun refreshState() {
        val d = doc
        val pos = runCatching { port.positionMs() }.logged("ForkAi", "AiSubtitleSession.kt:255").getOrDefault(0L)
        val model = d.model
        fun estimate(indices: List<Int>): Long? {
            var total = 0L
            for (i in indices) {
                val b = d.blocks[i]
                val n = b.endExclusive - b.first
                total += eta.estimateMs(model, "translate", n) ?: return null
                if (engineReview) total += eta.estimateMs(model, "review", n) ?: return null
            }
            return total / parallel.coerceIn(1, 2)
        }
        val remaining = d.blocks.indices.filter { d.blocks[it].accepted == null && (failures[it] ?: 0) < 2 }
        val buffer = remaining.filter { i -> d.cues[d.blocks[i].first].startMs < pos + bufferMs && d.cues[d.blocks[i].endExclusive - 1].endMs >= pos }
        val bufferEta = if (buffer.isEmpty()) null else estimate(buffer)?.let(EtaEstimator::label) ?: "Szacowanie czasu…"
        val totalEta = if (remaining.isEmpty()) null else estimate(remaining)?.let(EtaEstimator::label) ?: "Szacowanie czasu…"
        val notice = when {
            remaining.isEmpty() -> ""
            mode == AiMode.WATCH && buffer.isNotEmpty() -> "AI: tłumaczenie — zapas napisów " + (if (bufferEta!!.startsWith("Szac")) bufferEta else "za $bufferEta")
            else -> "AI: tłumaczenie — całość " + (if (totalEta!!.startsWith("Szac")) totalEta else "za $totalEta")
        }
        val waiting = System.currentTimeMillis() < waitingUntil
        _state.update { s ->
            if (s.stage == AiStage.DONE || s.stage == AiStage.STOPPED || s.stage == AiStage.FAILED) return@update s
            s.copy(stage = if (waiting) AiStage.WAITING else AiStage.TRANSLATING,
                doneBlocks = d.doneBlocks, totalBlocks = d.blocks.size, bufferEta = bufferEta, totalEta = totalEta,
                notice = if (waiting) s.notice else notice)
        }
    }

    var engineReview: Boolean = true
}
