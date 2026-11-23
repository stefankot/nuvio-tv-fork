package com.nuvio.tv.fork.ai.subtitles

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import com.nuvio.tv.fork.logged

/**
 * Measured duration per (model, stage), persisted between runs. ETA comes only from these measurements,
 * never from the model; with no measurement yet the UI says "Szacowanie czasu…".
 */
class EtaEstimator(private val load: () -> Map<String, Double> = { emptyMap() }, private val save: (Map<String, Double>) -> Unit = {}) {
    private val msPerCue = ConcurrentHashMap<String, Double>().apply { putAll(load()) }
    private fun key(model: String, stage: String) = "$model|$stage"

    fun record(model: String, stage: String, cues: Int, elapsedMs: Long) {
        if (cues <= 0 || elapsedMs <= 0) return
        val sample = elapsedMs.toDouble() / cues
        msPerCue.merge(key(model, stage), sample) { old, new -> old * 0.6 + new * 0.4 }
        save(HashMap(msPerCue))
    }

    /** Milliseconds for [cues] cues of [stage], or null without a measurement. */
    fun estimateMs(model: String, stage: String, cues: Int): Long? = msPerCue[key(model, stage)]?.let { (it * cues).toLong() }

    companion object {
        /** Rounded range, e.g. "ok. 40 s", "1–2 min"; no false precision. */
        fun label(ms: Long): String {
            val s = (ms / 1000).coerceAtLeast(1)
            return when {
                s < 60 -> "ok. ${((s + 9) / 10 * 10).coerceAtLeast(10)} s"
                s < 600 -> { val lo = s / 60; "${lo.coerceAtLeast(1)}–${lo + 1} min" }
                else -> { val lo = s / 300 * 5; "${lo}–${lo + 5} min" }
            }
        }
    }
}

data class EngineSettings(
    val review: Boolean = true,
    val maxOutputTokens: Int = 16_000,
    val maxRetries: Int = 2,
    val requestTimeoutMs: Long = 90_000,
)

/** Observable stage for the notice and the menu. */
sealed class EngineEvent {
    data class Waiting(val reason: String, val untilMs: Long) : EngineEvent()
    data class Cost(val usd: Double) : EngineEvent()
}

class BlockFailedException(message: String) : Exception(message)

/**
 * Translates one block: translation → deterministic checks → at most one repair → semantic review → checks.
 * Every request reserves its worst-case cost first; retries reserve again (the first one may have been billed).
 */
class TranslationEngine(
    private val provider: TextModelProvider,
    private val ledger: SpendLedger,
    private val eta: EtaEstimator,
    private val settings: EngineSettings = EngineSettings(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val onEvent: (EngineEvent) -> Unit = {},
) {
    private val price = ModelPricing.priceFor(provider.model)

    suspend fun translateBlock(doc: AiSubtitleDoc, blockIndex: Int): Pair<BlockRevision, Map<String, String>> {
        val block = doc.blocks[blockIndex]
        val source = (block.first until block.endExclusive).map { i -> doc.cues[i].let { SourceCue(it.id, it.startMs, it.endMs, it.text) } }
        val context = TranslationPrompt.context(doc)
        val system = TranslationPrompt.system(doc.targetLanguage)
        val previous = (maxOf(0, block.first - 6) until block.first).mapNotNull { i -> doc.translationFor(i)?.let { doc.cues[i].id to it } }

        var task = TranslationPrompt.translateTask(doc, block, previous)
        var parsed: ParsedTranslation? = null
        var validation: TranslationValidation? = null
        for (attempt in 0..1) { // first answer + one repair
            val result = request("${doc.key}:${block.id}:t$attempt", doc.materialId, system, context, task, TranslationPrompt.translationSchema, "translate", source.size)
            parsed = if (result.complete) ReplyParser.translation(result.text) else null
            val errors = when {
                !result.complete -> listOf("Truncated answer")
                parsed == null -> listOf("Malformed JSON")
                else -> TranslationOutputContract.validate(block.id, source, parsed.reply).also { validation = it }.blockingErrors
            }
            if (errors.isEmpty()) break
            validation = null
            task = TranslationPrompt.repairTask(doc, block, errors)
        }
        val accepted = validation?.accepted ?: throw BlockFailedException("Odpowiedź odrzucona po naprawie")
        var translations = accepted.map { it.translation }
        var warnings = validation.readabilityWarnings
        val checks = mutableListOf("structure")
        val changes = mutableListOf<ReviewChange>()

        if (settings.review) {
            val reviewed = runCatching {
                val result = request("${doc.key}:${block.id}:r", doc.materialId, system, context,
                    TranslationPrompt.reviewTask(doc, block, translations), TranslationPrompt.reviewSchema, "review", source.size)
                if (!result.complete) null else ReplyParser.review(result.text, block.id)
            }.logged("ForkAi", "TranslationEngine.kt:96").getOrElse { if (it is CancellationException || it is SpendLimitException) throw it; null }
            if (reviewed != null) {
                val byId = source.withIndex().associate { (i, c) -> c.id to i }
                val candidate = translations.toMutableList()
                for (c in reviewed) {
                    val i = byId[c.cueId] ?: continue // corrections outside the block are ignored
                    if (c.translation.isBlank() || c.translation == candidate[i]) continue
                    changes += ReviewChange(c.cueId, candidate[i], c.translation, c.problem)
                    candidate[i] = c.translation
                }
                val recheck = TranslationOutputContract.validate(block.id, source,
                    TranslationBlockReply(1, block.id, source.mapIndexed { i, s -> TranslatedCue(s.id, candidate[i]) }))
                if (recheck.valid) {
                    translations = recheck.accepted!!.map { it.translation }
                    warnings = recheck.readabilityWarnings
                    checks += "review"
                } else {
                    changes.clear(); checks += "review_rejected"
                }
            } else checks += "review_unavailable"
        }
        val revision = BlockRevision(
            revision = (block.revisions.lastOrNull()?.revision ?: 0) + 1,
            translations = translations, stage = "text", checks = checks, warnings = warnings, reviewChanges = changes,
            model = provider.model, promptVersion = TranslationPrompt.VERSION, createdAtMs = clock(),
        )
        return revision to parsed!!.glossary
    }

    private suspend fun request(
        id: String, title: String, system: String, context: String, task: String,
        schema: kotlinx.serialization.json.JsonObject, stage: String, cues: Int,
    ): ModelResult {
        val p = price ?: throw SpendLimitException("Brak ceny dla modelu ${provider.model}")
        val input = ModelPricing.estimateTokens(system) + ModelPricing.estimateTokens(context) + ModelPricing.estimateTokens(task)
        if (input + settings.maxOutputTokens > provider.capabilities.contextTokens) throw BlockFailedException("Materiał nie mieści się w kontekście modelu")
        var attempt = 0
        while (true) {
            val spendId = "$id:a$attempt:${java.util.UUID.randomUUID()}"
            val reserve = ModelPricing.maxCost(p, input, settings.maxOutputTokens.toLong())
            ledger.reserve(spendId, title, reserve)
            val started = clock()
            try {
                val request = ModelRequest(system, context, task, schema, settings.maxOutputTokens)
                (provider as? GeminiProvider)?.let { com.nuvio.tv.fork.lektor.LektorDump.save("napisy", it.buildBody(request)) } // fork 20.11: 3 ostatnie surowe zapytania (bez klucza)
                val result = provider.complete(request)
                val usd = result.usage?.let { ModelPricing.actualCost(p, it) } ?: reserve // no usage = keep the worst case
                ledger.settle(spendId, usd)
                onEvent(EngineEvent.Cost(usd))
                eta.record(provider.model, stage, cues, clock() - started)
                return result
            } catch (e: CancellationException) {
                android.util.Log.i("ForkAi", "TranslationEngine: zapytanie przerwane ($stage, $cues kwestii) — koszt nieznany, liczony jak zwykłe zapytanie")
                throw e // reservation kept: the request may have been sent and billed
            } catch (e: ProviderException) {
                android.util.Log.w("ForkAi", "TranslationEngine: dostawca http=${e.httpCode} transient=${e.transient} próba=$attempt: ${e.message?.take(200)}")
                if (!e.maybeSent) ledger.release(spendId)
                ledger.finished(spendId) // 23.11: nieznany wynik nie blokuje już limitu najgorszym przypadkiem (SpendLedger)
                if (!e.transient || attempt >= settings.maxRetries) throw BlockFailedException(e.message ?: "Błąd dostawcy")
                attempt++
                val wait = (e.retryAfterMs ?: (2_000L shl attempt)).coerceIn(1_000L, 60_000L)
                onEvent(EngineEvent.Waiting(if (e.httpCode == 429) "limit dostawcy" else "przerwa połączenia", clock() + wait))
                delay(wait)
            } finally {
                ledger.finished(spendId) // 23.11: przerwane zapytanie przestaje się liczyć w najgorszym przypadku
            }
        }
    }
}
