package com.nuvio.tv.reshaped.livetv

import android.util.Log // Fork patch: dostawca #11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The programmes kept from the last full guide read, saved small beside the guides. Opening Live
 * TV again (after it was let go while unused, or after the app was closed) reads this in a moment
 * instead of going through the whole XMLTV file, which is often 100+ MB. It only serves while
 * the guides it came from are unchanged and none of its channels has run out of programmes.
 */
internal object LiveTvGuideCache {
    const val FILE_NAME = "guide_kept.bin.gz"
    // Fork patch: kicia-epg (Kicia 24e08b945): mały wycinek „teraz” (3 h wstecz .. 12 h naprzód) czytany przed pełnym zapisem.
    const val NOW_FILE_NAME = "guide_now.bin.gz"
    const val NOW_BEFORE_MS = 3L * 60 * 60 * 1000
    const val NOW_AFTER_MS = 12L * 60 * 60 * 1000
    private const val NOW_MIN_AHEAD_MS = 3L * 60 * 60 * 1000
    // Fork patch: kicia-epg (Kicia 14.11, JMGO C1): po jednym zapisie na klucz (profil), ostatnie 4 każdego rodzaju zostają.
    private const val KEEP_PER_KIND = 4
    // 5: playlist-scoped channel identities and complete guide imports. 6: no overlapping programmes.
    // 7: id matches rank over name matches. 8: a name match fills in for an id without programmes.
    // 9: a name match with the channel's own country tag wins.
    private const val VERSION = 11 // Fork patch: dostawca #6
    private const val MAX_TITLE = 1_000

    class Entry(val schedule: LiveTvSchedule, val logos: Map<String, String>, val nextReadAtMs: Long)

    fun fileFor(dir: File, base: String, key: Long): File = // Fork patch: kicia-epg
        File(dir, base.removeSuffix(".bin.gz") + "_" + java.lang.Long.toHexString(key) + ".bin.gz")

    fun isSavedGuide(file: File): Boolean = file.name.startsWith("guide_kept") || file.name.startsWith("guide_now") // Fork patch: kicia-epg

    fun trim(dir: File, keep: Set<File>) { // Fork patch: kicia-epg
        val saved = dir.listFiles()?.filter { isSavedGuide(it) } ?: return
        saved.filter { it.name == FILE_NAME || it.name == NOW_FILE_NAME }.forEach { it.delete() }
        listOf("guide_kept_", "guide_now_").forEach { prefix ->
            saved.filter { it.name.startsWith(prefix) && it !in keep }
                .sortedByDescending { it.lastModified() }
                .drop(KEEP_PER_KIND - 1)
                .forEach { it.delete() }
        }
    }

    /** What the saved programmes were read for: the guides, the channels and how much is kept. */
    fun key(epgUrls: List<String>, guideKeys: Set<String>, window: LiveTvGuideWindow, catchupKeys: Set<String> = emptySet()): Long {
        var channels = guideKeys.size.toLong()
        guideKeys.forEach { channels += it.hashCode() }
        catchupKeys.forEach { channels += 31L * it.hashCode() }
        var key = epgUrls.hashCode().toLong()
        key = key * 31 + channels
        key = key * 31 + window.pastMs
        key = key * 31 + window.aheadMs
        key = key * 31 + window.maxPast * 1_000 + window.maxAhead + window.maxShortAhead * 100_000L
        key = key * 31 + window.catchupPastMs + window.maxCatchupPast
        key = key * 31 + window.detailsMs + window.maxDescription * 1_000L + window.detailsPerChannel
        key = key * 31 + window.detailsBudgetChars + window.maxCatchupProgrammes
        return key
    }

    /** The saved programmes, or null when they are missing, stale or for other guides or channels. */
    fun read(file: File, key: Long, guideFiles: List<File>, nowMs: Long, maxGuideAgeMs: Long, checkGuides: Boolean = true): Entry? = runCatching { // Fork patch: kicia-epg (checkGuides)
        if (!file.isFile) { Log.i(LiveTvEpgTrace.TAG, "saved programmes: miss (no file)"); return null } // Fork patch: dostawca #11
        DataInputStream(GZIPInputStream(file.inputStream().buffered(), 16 * 1024)).use { input ->
            if (input.readInt() != VERSION) { Log.i(LiveTvEpgTrace.TAG, "saved programmes: miss (format version)"); return null } // Fork patch: dostawca #11
            if (input.readLong() != key) { Log.i(LiveTvEpgTrace.TAG, "saved programmes: miss (key: guides, channels, names or window changed)"); return null }
            val nextReadAtMs = input.readLong()
            if (nowMs >= nextReadAtMs) { Log.i(LiveTvEpgTrace.TAG, "saved programmes: miss (expired or a channel ran out)"); return null } // Fork patch: dostawca #11
            val files = input.readInt()
            if (files != guideFiles.size) return null
            guideFiles.forEach { guide ->
                val modified = input.readLong()
                // A guide downloaded since, or due to be downloaded again, means a full read.
                if (checkGuides && (guide.lastModified() != modified || nowMs - modified !in 0 until maxGuideAgeMs)) { Log.i(LiveTvEpgTrace.TAG, "saved programmes: miss (guide file replaced or older than refresh interval)"); return null } // Fork patch: dostawca #11 // Fork patch: kicia-epg
            }
            val logos = HashMap<String, String>()
            repeat(input.readInt()) { logos[input.readUTF()] = input.readUTF() }
            // Titles and descriptions are saved once each and named by index: repeats share one
            // copy, as after a full read, and a week of catch-up reads no more text than a day.
            val texts = Array(input.readInt()) { input.readUTF() }
            fun DataInputStream.readShared(): String = texts[readInt()]
            val channels = input.readInt()
            val schedule = HashMap<String, List<LiveTvProgramme>>(channels * 2)
            repeat(channels) {
                val guideKey = input.readUTF()
                val count = input.readInt()
                schedule[guideKey] = List(count) {
                    LiveTvProgramme(
                        title = input.readShared(),
                        startEpochMs = input.readLong(),
                        stopEpochMs = input.readLong(),
                        description = if (input.readBoolean()) input.readShared() else null,
                        image = input.readOptional(),
                        episode = input.readOptional(), // Fork patch: dostawca #6
                    )
                }
            }
            Entry(schedule, logos, nextReadAtMs)
        }
    }.getOrNull()

    /** Saves a full read's result; a failed save only means the next opening reads the XML again. */
    fun write(file: File, key: Long, guideFiles: List<File>, entry: Entry) {
        val temp = File(file.path + ".tmp")
        runCatching {
            DataOutputStream(GZIPOutputStream(temp.outputStream().buffered(), 16 * 1024)).use { out ->
                out.writeInt(VERSION)
                out.writeLong(key)
                out.writeLong(entry.nextReadAtMs)
                out.writeInt(guideFiles.size)
                guideFiles.forEach { out.writeLong(it.lastModified()) }
                out.writeInt(entry.logos.size)
                entry.logos.forEach { (guideKey, logo) ->
                    out.writeUTF(guideKey)
                    out.writeUTF(logo)
                }
                val texts = LinkedHashMap<String, Int>()
                entry.schedule.values.forEach { programmes ->
                    programmes.forEach { programme ->
                        texts.getOrPut(programme.title.take(MAX_TITLE)) { texts.size }
                        programme.description?.let { texts.getOrPut(it.take(MAX_TITLE)) { texts.size } }
                    }
                }
                out.writeInt(texts.size)
                texts.keys.forEach(out::writeUTF)
                out.writeInt(entry.schedule.size)
                entry.schedule.forEach { (guideKey, programmes) ->
                    out.writeUTF(guideKey)
                    out.writeInt(programmes.size)
                    programmes.forEach { programme ->
                        out.writeInt(texts.getValue(programme.title.take(MAX_TITLE)))
                        out.writeLong(programme.startEpochMs)
                        out.writeLong(programme.stopEpochMs)
                        out.writeBoolean(programme.description != null)
                        programme.description?.let { out.writeInt(texts.getValue(it.take(MAX_TITLE))) }
                        // A cut link would be a broken one.
                        out.writeOptional(programme.image?.takeIf { it.length <= MAX_TITLE })
                        out.writeOptional(programme.episode) // Fork patch: dostawca #6
                    }
                }
            }
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }.onFailure {
            temp.delete()
            file.delete()
        }
    }

    /** Fork patch: kicia-epg. Wycinek „teraz”; null, gdy brak, inny klucz albo za mało go zostało naprzód. */
    fun readNow(file: File, key: Long, guideFiles: List<File>, nowMs: Long): Entry? =
        read(file, key, guideFiles, nowMs + NOW_MIN_AHEAD_MS, Long.MAX_VALUE, checkGuides = false)
            ?.takeIf { entry -> entry.schedule.values.any { it.isNotEmpty() } }

    /** Fork patch: kicia-epg. Zapis wycinka „teraz”; nieudany zapis = tylko wolniejsze następne otwarcie. */
    fun writeNow(file: File, key: Long, guideFiles: List<File>, schedule: LiveTvSchedule, logos: Map<String, String>, nowMs: Long) {
        val from = nowMs - NOW_BEFORE_MS
        val until = nowMs + NOW_AFTER_MS
        val slice = HashMap<String, List<LiveTvProgramme>>(schedule.size * 2)
        schedule.forEach { (guideKey, programmes) ->
            val kept = programmes.filter { it.stopEpochMs > from && it.startEpochMs < until }
            if (kept.isNotEmpty()) slice[guideKey] = kept
        }
        if (slice.isEmpty()) return
        write(file, key, guideFiles, Entry(slice, logos, until))
    }

    private fun DataInputStream.readOptional(): String? = if (readBoolean()) readUTF() else null

    private fun DataOutputStream.writeOptional(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }
}
