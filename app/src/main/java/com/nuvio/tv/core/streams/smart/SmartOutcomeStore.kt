package com.nuvio.tv.core.streams.smart

import java.io.File

/**
 * forge M6 (N6): learning from results. For each origin (addon + service, e.g. "Comet/TorBox") the number of tries,
 * failures and the median time to the first frame over 14 days. The file stays under 16 KiB; no addresses, no titles.
 * In the ranking it only breaks ties (the last key before the addon's order); an origin with ≥ 5 tries and ≥ 40 %
 * failures goes after the others of the same quality group. Pure JVM apart from [init]'s directory.
 */
object SmartOutcomeStore {
    const val WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    const val MAX_BYTES = 16 * 1024
    const val MIN_TRIES = 5
    const val BAD_SHARE = 0.40
    private const val MAX_EVENTS_PER_ORIGIN = 40

    /** One try: when, whether it played, the time to its first frame (ms, or -1). */
    data class Event(val atMs: Long, val ok: Boolean, val ttffMs: Long)

    private val events = LinkedHashMap<String, ArrayDeque<Event>>()
    @Volatile private var file: File? = null

    @Synchronized
    fun init(dir: File) {
        if (file != null) return
        file = File(dir, "forge_smart_outcomes.tsv")
        runCatching { load(file!!.readLines()) }
    }

    /** "Comet/TorBox": the addon and the service it hands the file through ("direct" without one). */
    fun originOf(addon: String?, service: String?): String? {
        val a = addon?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return a.replace('\t', ' ') + "/" + (service?.trim()?.takeIf { it.isNotEmpty() }?.replace('\t', ' ') ?: "direct")
    }

    @Synchronized
    fun record(origin: String?, ok: Boolean, ttffMs: Long?, nowMs: Long = System.currentTimeMillis()) {
        val key = origin ?: return
        val list = events.getOrPut(key) { ArrayDeque() }
        list.addLast(Event(nowMs, ok, ttffMs ?: -1))
        while (list.size > MAX_EVENTS_PER_ORIGIN) list.removeFirst()
        prune(nowMs)
        save()
    }

    @Synchronized
    fun stats(origin: String?, nowMs: Long = System.currentTimeMillis()): Triple<Int, Int, Long?>? {
        val list = events[origin ?: return null] ?: return null
        val live = list.filter { nowMs - it.atMs in 0..WINDOW_MS }
        if (live.isEmpty()) return null
        val times = live.filter { it.ok && it.ttffMs >= 0 }.map { it.ttffMs }.sorted()
        return Triple(live.size, live.count { !it.ok }, times.getOrNull(times.size / 2))
    }

    /** ≥ [MIN_TRIES] tries and ≥ [BAD_SHARE] failures. */
    fun isBad(origin: String?, nowMs: Long = System.currentTimeMillis()): Boolean {
        val (tries, fails, _) = stats(origin, nowMs) ?: return false
        return tries >= MIN_TRIES && fails.toDouble() / tries >= BAD_SHARE
    }

    /** The tie key: higher is better (fewer failures, then a faster start); unknown origins sit in the middle. */
    fun tieKey(origin: String?, nowMs: Long = System.currentTimeMillis()): Long {
        val (tries, fails, median) = stats(origin, nowMs) ?: return 0L
        val okShare = ((tries - fails) * 100L) / tries.coerceAtLeast(1)
        val speed = median?.let { 60_000L - it.coerceAtMost(60_000L) } ?: 0L
        return okShare * 100_000L + speed - 5_000_000L
    }

    @Synchronized fun clear() { events.clear(); save() }

    private fun prune(nowMs: Long) {
        events.values.forEach { list -> while (list.isNotEmpty() && nowMs - list.first().atMs > WINDOW_MS) list.removeFirst() }
        events.entries.removeAll { it.value.isEmpty() }
    }

    internal fun load(lines: List<String>) {
        lines.forEach { line ->
            val p = line.split('\t')
            if (p.size != 4) return@forEach
            val at = p[1].toLongOrNull() ?: return@forEach
            events.getOrPut(p[0]) { ArrayDeque() }.addLast(Event(at, p[2] == "1", p[3].toLongOrNull() ?: -1))
        }
    }

    internal fun dump(): String = buildString {
        events.forEach { (origin, list) -> list.forEach { append(origin).append('\t').append(it.atMs).append('\t').append(if (it.ok) 1 else 0).append('\t').append(it.ttffMs).append('\n') } }
    }

    private fun save() {
        val f = file ?: return
        var text = dump()
        // keep under MAX_BYTES: drop the oldest events first
        while (text.length > MAX_BYTES && events.isNotEmpty()) {
            val oldest = events.minByOrNull { it.value.firstOrNull()?.atMs ?: Long.MAX_VALUE } ?: break
            oldest.value.removeFirstOrNull()
            if (oldest.value.isEmpty()) events.remove(oldest.key)
            text = dump()
        }
        runCatching { f.writeText(text) }
    }
}

/**
 * forge M7 (N14): the "family" of the file an episode played without trouble — resolution, audio tier, origin and group — so
 * the next episode is taken from the same family when there is one; otherwise the usual ranking.
 */
data class SmartFamily(val height: Int?, val audioTier: Int, val origin: String?, val group: String? = null)

object SmartFamilyMemory {
    private val byContent = object : LinkedHashMap<String, SmartFamily>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SmartFamily>?) = size > 32
    }
    @Synchronized fun remember(contentId: String?, family: SmartFamily) { if (contentId != null) byContent[contentId] = family }
    @Synchronized fun forget(contentId: String?) { if (contentId != null) byContent.remove(contentId) }
    @Synchronized fun get(contentId: String?): SmartFamily? = contentId?.let { byContent[it] }
}
