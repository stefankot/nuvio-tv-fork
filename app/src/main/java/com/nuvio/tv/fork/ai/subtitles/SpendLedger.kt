package com.nuvio.tv.fork.ai.subtitles

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate

/**
 * Prices in USD per 1M tokens (paid tier, standard), checked 18.11.2026 on aistudio.google.com/docs/pricing.
 * Thinking tokens are billed as output. Prices with a later [validFrom] replace earlier ones from that day.
 */
data class ModelPrice(val model: String, val validFrom: LocalDate, val inputPerM: Double, val outputPerM: Double, val cachedPerM: Double)

object ModelPricing {
    private val start = LocalDate.of(2026, 1, 1)
    val table = listOf(
        ModelPrice("gemini-3.8-flash", start, 0.75, 3.75, 0.075),
        ModelPrice("gemini-3.8-flash", LocalDate.of(2027, 1, 1), 1.50, 7.50, 0.15),
        ModelPrice("gemini-3.5-flash-lite", start, 0.30, 2.50, 0.03),
        ModelPrice("gemini-4-hydrogen", start, 1.50, 7.50, 0.075),
        ModelPrice("gemini-4-argon", start, 2.00, 10.00, 0.10),
        ModelPrice("gemini-4-argon", LocalDate.of(2026, 12, 1), 4.00, 20.00, 0.20),
    )
    val models: List<String> = table.map { it.model }.distinct()
    const val DEFAULT_MODEL = "gemini-4-hydrogen" // właściciel 19.11 (wcześniej gemini-3.8-flash)

    /** null = unknown model: paid work must not start without a price (no invented price). */
    fun priceFor(model: String, day: LocalDate = LocalDate.now()): ModelPrice? =
        table.filter { it.model == model && !it.validFrom.isAfter(day) }.maxByOrNull { it.validFrom }

    /** Worst case for a request: no cache hit and every output token used. */
    fun maxCost(price: ModelPrice, inputTokens: Long, maxOutputTokens: Long): Double =
        (inputTokens * price.inputPerM + maxOutputTokens * price.outputPerM) / 1_000_000.0

    fun actualCost(price: ModelPrice, usage: ModelUsage): Double {
        val cached = usage.cachedTokens.coerceIn(0, usage.inputTokens)
        return ((usage.inputTokens - cached) * price.inputPerM + cached * price.cachedPerM +
            (usage.outputTokens + usage.thoughtTokens) * price.outputPerM) / 1_000_000.0
    }

    /** Conservative token estimate before sending (no free count call): ~3 characters per token. */
    fun estimateTokens(text: String): Long = text.length / 3L + 16
}

class SpendLimitException(message: String) : IllegalStateException(message)

@Serializable
private data class SpendEntry(val id: String, val title: String, val day: String, val usd: Double, val settled: Boolean)

@Serializable
private data class SpendFile(val entries: List<SpendEntry> = emptyList())

/**
 * Persistent cost ledger: a request reserves its worst-case cost before it is sent and is settled with the
 * real usage afterwards. A request with an unknown outcome stays reserved at full cost. The limit is local:
 * the provider's own billing may be delayed or differ slightly (shown to the user in settings).
 */
class SpendLedger(private val file: File, private val perTitleUsd: () -> Double, private val perDayUsd: () -> Double) {
    private val json = Json { ignoreUnknownKeys = true }

    fun reserve(id: String, title: String, usd: Double, day: LocalDate = LocalDate.now()): Unit = edit { entries ->
        require(entries.none { it.id == id }) { "Duplicate spend id" }
        val dayKey = day.toString()
        val titleTotal = entries.filter { it.title == title }.sumOf { it.usd }
        val dayTotal = entries.filter { it.day == dayKey }.sumOf { it.usd }
        if (titleTotal + usd > perTitleUsd()) throw SpendLimitException("Limit na tytuł wyczerpany")
        if (dayTotal + usd > perDayUsd()) throw SpendLimitException("Dzienny limit wyczerpany")
        entries += SpendEntry(id, title, dayKey, usd, settled = false)
    }

    /** Real cost from usage; it may exceed the reservation only by provider rounding, it is recorded anyway. */
    fun settle(id: String, usd: Double) = edit { entries ->
        val i = entries.indexOfFirst { it.id == id }
        if (i >= 0) entries[i] = entries[i].copy(usd = usd, settled = true)
    }

    /** Only for requests that provably never left the device. */
    fun release(id: String) = edit { entries -> entries.removeAll { it.id == id && !it.settled } }

    fun spentForTitle(title: String): Double = read().filter { it.title == title }.sumOf { it.usd }
    fun spentForDay(day: LocalDate = LocalDate.now()): Double = read().filter { it.day == day.toString() }.sumOf { it.usd }

    private fun read(): List<SpendEntry> = synchronized(lock) { load() }

    private fun load(): MutableList<SpendEntry> {
        if (!file.exists()) return mutableListOf()
        // Fail closed: an unreadable ledger blocks paid work instead of resetting the spent money.
        val text = try { file.readText() } catch (e: IOException) { throw SpendLimitException("Rejestr kosztów nieczytelny") }
        return runCatching { json.decodeFromString(SpendFile.serializer(), text).entries.toMutableList() }
            .getOrElse { throw SpendLimitException("Rejestr kosztów uszkodzony") }
    }

    private fun edit(block: (MutableList<SpendEntry>) -> Unit) = synchronized(lock) {
        val entries = load()
        block(entries)
        // Keep 400 days of history at most.
        val cutoff = LocalDate.now().minusDays(400).toString()
        entries.removeAll { it.day < cutoff }
        atomicWrite(file, json.encodeToString(SpendFile.serializer(), SpendFile(entries)))
    }

    private companion object { val lock = Any() }
}

/** fsync + rename(2): atomic on one filesystem, available on API 24 (no java.nio.file). */
internal fun atomicWrite(file: File, text: String) {
    file.parentFile?.mkdirs()
    val temp = File(file.parentFile, "${file.name}.${System.nanoTime()}.tmp")
    try {
        FileOutputStream(temp).use { out -> out.write(text.toByteArray(Charsets.UTF_8)); out.flush(); out.fd.sync() }
        if (!temp.renameTo(file)) throw IOException("Atomic replace failed: ${file.name}")
    } finally {
        if (temp.exists()) temp.delete()
    }
}
