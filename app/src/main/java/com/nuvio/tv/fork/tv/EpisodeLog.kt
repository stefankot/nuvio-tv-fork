package com.nuvio.tv.fork.tv

import java.io.File
import com.nuvio.tv.fork.logged

/** Jedna emisja kanału z identyfikatorem odcinka zapisanym w chwili czytania EPG (archiwum Reshaped nie trzyma opisów). */
data class LogEntry(
    val programKey: String,
    val guideKey: String,
    val startMs: Long,
    val durMs: Long,
    val episodeId: String,
    val title: String,
)

/** Dziennik odcinków: zwarty plik tekstowy na profil (`files/fork_tv/episode_log_<profil>.tsv`). */
object EpisodeLog {
    const val LIMIT = 200_000
    private const val DAY = 24 * 3_600_000L

    fun encode(entries: List<LogEntry>): String = buildString {
        entries.forEach { e ->
            append(clean(e.programKey)).append('\t').append(clean(e.guideKey)).append('\t').append(e.startMs).append('\t')
                .append(e.durMs).append('\t').append(clean(e.episodeId)).append('\t').append(clean(e.title)).append('\n')
        }
    }

    /** Uszkodzone linie są pomijane; cały uszkodzony plik daje pustą listę. */
    fun decode(text: String?): List<LogEntry> {
        if (text.isNullOrEmpty()) return emptyList()
        return text.lineSequence().mapNotNull { line ->
            val p = line.split('\t')
            if (p.size < 6) return@mapNotNull null
            val start = p[2].toLongOrNull() ?: return@mapNotNull null
            val dur = p[3].toLongOrNull() ?: return@mapNotNull null
            LogEntry(p[0], p[1], start, dur, p[4], p[5])
        }.toList()
    }

    /** Dołącza nowe wpisy (ta sama emisja = nowszy wygrywa), trzyma najwyżej [limit], najstarsze wypadają. */
    fun merge(old: List<LogEntry>, new: List<LogEntry>, limit: Int = LIMIT): List<LogEntry> {
        val map = LinkedHashMap<Pair<String, Long>, LogEntry>(old.size + new.size)
        old.forEach { map[it.guideKey to it.startMs] = it }
        new.forEach { map[it.guideKey to it.startMs] = it }
        val all = map.values.sortedBy { it.startMs }
        return if (all.size > limit) all.takeLast(limit) else all
    }

    /**
     * Wpisy dla kanałów z archiwum ([catchupDays] po guideKey) z okna 7 dni wstecz do 8 naprzód.
     * [extraEpisode] podaje etykietę odcinka z EPG (np. S3E168), jeśli ją znamy.
     */
    fun fromGuide(
        kept: Map<String, List<com.nuvio.tv.fork.tv.GuideProgramme>>,
        catchupDays: Map<String, Int>,
        nowMs: Long,
    ): List<LogEntry> {
        val out = ArrayList<LogEntry>()
        for ((guideKey, list) in kept) {
            if ((catchupDays[guideKey] ?: 0) <= 0) continue
            for (p in list) {
                if (p.stopMs < nowMs - 7 * DAY || p.startMs > nowMs + 8 * DAY) continue
                val key = ShowIdentity.normalizeTitle(p.title)
                if (key.isEmpty()) continue
                val ep = p.episodeLabel?.takeIf { it.isNotBlank() } ?: ShowIdentity.episodeId(p.title, p.description)
                out += LogEntry(key, guideKey, p.startMs, p.stopMs - p.startMs, ep, p.title.take(80))
            }
        }
        return out
    }

    fun read(file: File): List<LogEntry> = runCatching { decode(file.takeIf { it.isFile }?.readText()) }.logged("ForkTv", "EpisodeLog.kt:71").getOrDefault(emptyList())

    fun write(file: File, entries: List<LogEntry>) {
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".tmp")
            tmp.writeText(encode(entries))
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }.logged("ForkTv", "EpisodeLog.kt:74")
    }

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}

/** Minimalny opis emisji dla logiki rdzenia (bez typów Reshaped; most je tłumaczy). */
data class GuideProgramme(
    val title: String,
    val startMs: Long,
    val stopMs: Long,
    val description: String? = null,
    val episodeLabel: String? = null,
    val image: String? = null,
)
