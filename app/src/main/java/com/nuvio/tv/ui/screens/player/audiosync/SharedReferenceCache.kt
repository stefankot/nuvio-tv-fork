package com.nuvio.tv.ui.screens.player.audiosync

/** Recent successful subtitle parses survive a player rebuild within the same access scope. */
internal object SharedReferenceCache {
    data class Key(
        val profileId: Int,
        val content: String?,
        val url: String,
        val language: String,
        val headers: Map<String, String>,
    )

    private class Entry(val dialogue: List<Triple<Long, Long, String>>, val bytes: Long, val storedAt: Long)
    private val entries = LinkedHashMap<Key, Entry>(8, 0.75f, true)
    private var bytes = 0L
    private const val MAX_ENTRIES = 8
    private const val MAX_BYTES = 2L * 1024 * 1024
    private const val RETENTION_NANOS = 10L * 60 * 1_000_000_000L

    @Synchronized
    fun get(key: Key): List<Triple<Long, Long, String>>? {
        val entry = entries[key] ?: return null
        if (System.nanoTime() - entry.storedAt < RETENTION_NANOS) return entry.dialogue
        entries.remove(key)
        bytes -= entry.bytes
        return null
    }

    @Synchronized
    fun put(key: Key, dialogue: List<Triple<Long, Long, String>>) {
        if (key.content == null || dialogue.isEmpty()) return
        val size = dialogue.sumOf { 64L + 2L * it.third.length }
        if (size > MAX_BYTES) return
        bytes -= entries.put(key, Entry(dialogue, size, System.nanoTime()))?.bytes ?: 0L
        bytes += size
        val oldest = entries.entries.iterator()
        while (entries.size > MAX_ENTRIES || bytes > MAX_BYTES) {
            bytes -= oldest.next().value.bytes
            oldest.remove()
        }
    }
}
