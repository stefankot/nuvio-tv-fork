package com.nuvio.tv.fork.ai.subtitles

import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

/** One ledger is shared across restarts, providers and text-translation attempts for a material. */
data class AudioSnippet(
    val requestId: String,
    val audioTrackKey: String,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = endMs - startMs
    init {
        require(requestId.isNotBlank() && requestId.length <= 128)
        require(audioTrackKey.isNotBlank() && audioTrackKey.length <= 512)
        require(startMs >= 0 && endMs > startMs && endMs - startMs <= 20_000L) {
            "Ordinary-mode audio sample must be in (0, 20] seconds including all context"
        }
    }
}

enum class AudioTransmissionState { RESERVED, SENT, DEFINITELY_NOT_SENT }
data class AudioBudgetEntry(val snippet: AudioSnippet, val state: AudioTransmissionState)
data class AudioBudgetSnapshot(val accountedMs: Long, val entries: List<AudioBudgetEntry>) {
    val remainingMs: Long get() = (60_000L - accountedMs).coerceAtLeast(0L)
}

/**
 * Dedicated cross-process file lock and fsync + atomic replacement on the same filesystem.
 * The caller MUST reserve before *every* transmission, including retries or a second model.
 * Never reuse a requestId: even the same audio range sent twice consumes two reservations.
 * A timeout/unknown result remains RESERVED; only proof that no bytes were sent allows release.
 * The stable materialKey must not change with provider, restart, seek or new ordinary run.
 * This implementation does NOT authorize full-track/extreme-mode sends.
 */
class OrdinaryAudioBudget(private val directory: File) {
    init { check(directory.isDirectory || directory.mkdirs()) { "Cannot create budget directory" } }

    fun reserve(materialKey: String, snippet: AudioSnippet): AudioBudgetSnapshot = modify(materialKey) { entries ->
        require(entries.none { it.snippet.requestId == snippet.requestId }) {
            "Each physical upload attempt requires a fresh requestId and a new reservation"
        }
        check(accountedMs(entries) + snippet.durationMs <= MAX_BUDGET_MS) {
            "60-second ordinary-audio limit exhausted"
        }
        entries += AudioBudgetEntry(snippet, AudioTransmissionState.RESERVED)
        true
    }

    fun markSent(materialKey: String, requestId: String): AudioBudgetSnapshot = modify(materialKey) { entries ->
        val index = entries.indexOfFirst { it.snippet.requestId == requestId }
        require(index >= 0) { "Cannot mark unreserved request as sent" }
        check(entries[index].state != AudioTransmissionState.DEFINITELY_NOT_SENT)
        if (entries[index].state == AudioTransmissionState.SENT) false else {
            entries[index] = entries[index].copy(state = AudioTransmissionState.SENT)
            true
        }
    }

    /** Call ONLY when the sending layer can establish that no transmission occurred. */
    fun markDefinitelyNotSent(materialKey: String, requestId: String): AudioBudgetSnapshot = modify(materialKey) { entries ->
        val index = entries.indexOfFirst { it.snippet.requestId == requestId }
        require(index >= 0)
        check(entries[index].state == AudioTransmissionState.RESERVED) {
            "Already transmitted audio must never be released"
        }
        entries[index] = entries[index].copy(state = AudioTransmissionState.DEFINITELY_NOT_SENT)
        true
    }

    fun snapshot(materialKey: String): AudioBudgetSnapshot = modify(materialKey) { false }

    /** Boolean result means the ledger really changed; snapshots and repeated SENT are write-free. */
    private fun modify(
        materialKey: String,
        edit: (MutableList<AudioBudgetEntry>) -> Boolean,
    ): AudioBudgetSnapshot {
        require(materialKey.isNotBlank())
        val digest = MessageDigest.getInstance("SHA-256").digest(materialKey.toByteArray(Charsets.UTF_8))
        val key = buildString(digest.size * 2) {
            digest.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(HEX[value ushr 4]).append(HEX[value and 15])
            }
        }
        val ledger = File(directory, "$key.bin")
        val lockFile = File(directory, "$key.lock") // do not rename while locked
        // 64 fixed stripes permit independent titles in the same JVM without an unbounded mutex map.
        val stripe = mutexes[(digest[0].toInt() and 0xff) % mutexes.size]
        return synchronized(stripe) {
            RandomAccessFile(lockFile, "rw").use { raf ->
                raf.channel.lock().use {
                    val entries = read(ledger)
                    check(accountedMs(entries) <= MAX_BUDGET_MS) {
                        "Persisted ordinary-audio limit violated"
                    }
                    val changed = edit(entries)
                    val charged = accountedMs(entries)
                    check(charged <= MAX_BUDGET_MS) { "Persisted ordinary-audio limit violated" }
                    if (changed) writeAtomic(ledger, entries)
                    AudioBudgetSnapshot(charged, entries.toList())
                }
            }
        }
    }

    private fun accountedMs(entries: List<AudioBudgetEntry>): Long {
        var total = 0L
        for (entry in entries) {
            if (entry.state != AudioTransmissionState.DEFINITELY_NOT_SENT) {
                total += entry.snippet.durationMs
            }
        }
        return total
    }

    private fun read(file: File): MutableList<AudioBudgetEntry> {
        if (!file.exists()) return mutableListOf()
        return DataInputStream(FileInputStream(file).buffered()).use { input ->
            check(input.readInt() == FILE_MAGIC) { "Corrupted audio budget header — fail closed" }
            val count = input.readInt()
            check(count in 0..100_000) { "Corrupted audio budget count — fail closed" }
            val seenIds = HashSet<String>(count)
            val result = ArrayList<AudioBudgetEntry>(count)
            repeat(count) {
                val snippet = AudioSnippet(
                    requestId = input.readUTF(),
                    audioTrackKey = input.readUTF(),
                    startMs = input.readLong(),
                    endMs = input.readLong(),
                )
                check(seenIds.add(snippet.requestId)) { "Duplicate audio budget request — fail closed" }
                val state = AudioTransmissionState.entries.getOrNull(input.readUnsignedByte())
                    ?: throw IllegalStateException("Corrupted audio budget state — fail closed")
                result.add(AudioBudgetEntry(snippet, state))
            }
            check(input.read() == -1) { "Trailing data in audio budget — fail closed" }
            result
        }
    }

    private fun writeAtomic(file: File, entries: List<AudioBudgetEntry>) {
        val temp = File(file.parentFile, "${file.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temp).use { stream ->
                val out = DataOutputStream(BufferedOutputStream(stream))
                out.writeInt(FILE_MAGIC)
                out.writeInt(entries.size)
                entries.forEach { (snippet, state) ->
                    out.writeUTF(snippet.requestId)
                    out.writeUTF(snippet.audioTrackKey)
                    out.writeLong(snippet.startMs)
                    out.writeLong(snippet.endMs)
                    out.writeByte(state.ordinal)
                }
                out.flush()
                // Flush the same live descriptor before atomic rename.
                stream.fd.sync()
            }
            // Fail closed if the filesystem cannot guarantee atomic replacement.
            Files.move(
                temp.toPath(), file.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    companion object {
        private const val MAX_BUDGET_MS = 60_000L
        private const val FILE_MAGIC = 0x41534231 // ASB1, compatible with existing ledgers
        private const val HEX = "0123456789abcdef"
        private val mutexes = Array(64) { Any() }
    }
}
