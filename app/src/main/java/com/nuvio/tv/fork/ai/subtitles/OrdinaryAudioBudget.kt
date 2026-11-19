package com.nuvio.tv.fork.ai.subtitles

import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
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

/** The audio limit for this material is used up (expected, not an error). */
class AudioBudgetExhaustedException(message: String) : IllegalStateException(message)

/** The ledger cannot be trusted; uploads stay blocked for this material (fail closed). */
class AudioBudgetCorruptedException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

enum class AudioTransmissionState { RESERVED, SENT, DEFINITELY_NOT_SENT }
data class AudioBudgetEntry(val snippet: AudioSnippet, val state: AudioTransmissionState)
data class AudioBudgetSnapshot(val accountedMs: Long, val entries: List<AudioBudgetEntry>, val limitMs: Long = OrdinaryAudioBudget.DEFAULT_LIMIT_MS) {
    val remainingMs: Long get() = (limitMs - accountedMs).coerceAtLeast(0L)
}

/**
 * Dedicated cross-process file lock and fsync + atomic rename on the same filesystem.
 * The parent directory is not fsynced (no portable API before Android 26): a power cut right
 * after a write may lose that single latest change (worst case one sample, at most 20 s, over).
 * Process crashes and kills never lose or half-write the ledger.
 * The caller MUST reserve before *every* transmission, including retries or a second model.
 * Never reuse a requestId: even the same audio range sent twice consumes two reservations.
 * A timeout/unknown result remains RESERVED; only proof that no bytes were sent allows release.
 * The stable materialKey must not change with provider, restart, seek or new ordinary run.
 * This implementation does NOT authorize full-track/extreme-mode sends.
 */
class OrdinaryAudioBudget(private val directory: File) {
    init { check(directory.isDirectory || directory.mkdirs()) { "Cannot create budget directory" } }

    /** [limitMs]: limit tego materiału, zwykle [limitForRuntime] (właściciel 19.11: 180 s na godzinę filmu). */
    fun reserve(materialKey: String, snippet: AudioSnippet, limitMs: Long = DEFAULT_LIMIT_MS): AudioBudgetSnapshot = modify(materialKey, limitMs) { entries ->
        require(entries.none { it.snippet.requestId == snippet.requestId }) {
            "Each physical upload attempt requires a fresh requestId and a new reservation"
        }
        if (accountedMs(entries) + snippet.durationMs > limitMs.coerceIn(DEFAULT_LIMIT_MS, MAX_BUDGET_MS)) {
            throw AudioBudgetExhaustedException("Ordinary-audio limit exhausted (${limitMs / 1000} s)")
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
        require(index >= 0) { "Cannot release unreserved request" }
        check(entries[index].state == AudioTransmissionState.RESERVED) {
            "Already transmitted audio must never be released"
        }
        entries[index] = entries[index].copy(state = AudioTransmissionState.DEFINITELY_NOT_SENT)
        true
    }

    fun snapshot(materialKey: String, limitMs: Long = DEFAULT_LIMIT_MS): AudioBudgetSnapshot = modify(materialKey, limitMs) { false }

    /** Boolean result means the ledger really changed; snapshots and repeated SENT are write-free. */
    private fun modify(
        materialKey: String,
        limitMs: Long = DEFAULT_LIMIT_MS,
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
                    deleteStaleTemps(ledger)
                    val entries = read(ledger)
                    if (accountedMs(entries) > MAX_BUDGET_MS) {
                        throw AudioBudgetCorruptedException("Persisted ordinary-audio limit violated")
                    }
                    val changed = edit(entries)
                    val charged = accountedMs(entries)
                    check(charged <= MAX_BUDGET_MS) { "Persisted ordinary-audio limit violated" }
                    if (changed) writeAtomic(ledger, entries)
                    AudioBudgetSnapshot(charged, entries.toList(), limitMs.coerceIn(DEFAULT_LIMIT_MS, MAX_BUDGET_MS))
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

    /** Temp files of a writer killed before rename; safe to drop while holding the lock. */
    private fun deleteStaleTemps(ledger: File) {
        val prefix = "${ledger.name}."
        directory.listFiles { _, name -> name.startsWith(prefix) && name.endsWith(".tmp") }
            ?.forEach { it.delete() }
    }

    private fun read(file: File): MutableList<AudioBudgetEntry> {
        if (!file.exists()) return mutableListOf()
        return try {
            readUnchecked(file)
        } catch (e: AudioBudgetCorruptedException) {
            throw e
        } catch (e: IOException) {
            throw AudioBudgetCorruptedException("Unreadable audio budget — fail closed", e)
        } catch (e: IllegalStateException) {
            throw AudioBudgetCorruptedException(e.message ?: "Corrupted audio budget — fail closed", e)
        } catch (e: IllegalArgumentException) {
            throw AudioBudgetCorruptedException("Invalid audio budget entry — fail closed", e)
        }
    }

    private fun readUnchecked(file: File): MutableList<AudioBudgetEntry> {
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
            // File.renameTo is rename(2) on Android/Linux: atomic replacement within one
            // filesystem and available on API 24-25, unlike java.nio.file.Files (API 26+,
            // not covered by desugar_jdk_libs). Fail closed when it does not succeed.
            if (!temp.renameTo(file)) throw IOException("Atomic ledger replacement failed")
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    companion object {
        /** Najmniejszy limit (krótki materiał) i dotychczasowa wartość domyślna. */
        const val DEFAULT_LIMIT_MS = 60_000L
        /** Twardy sufit zapisu (film 5 h × 180 s): większa suma w rejestrze = uszkodzenie. */
        private const val MAX_BUDGET_MS = 900_000L
        /** Właściciel 19.11: 180 s dźwięku na każdą godzinę filmu (proporcjonalnie), nie mniej niż 60 s. */
        fun limitForRuntime(durationMs: Long): Long =
            if (durationMs <= 0) DEFAULT_LIMIT_MS else (durationMs * 180L / 3_600L).coerceIn(DEFAULT_LIMIT_MS, MAX_BUDGET_MS)
        private const val FILE_MAGIC = 0x41534231 // ASB1, compatible with existing ledgers
        private const val HEX = "0123456789abcdef"
        private val mutexes = Array(64) { Any() }
    }
}
