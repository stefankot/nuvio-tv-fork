package com.nuvio.tv.fork.tv

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import com.nuvio.tv.fork.logged

/**
 * Dane TV jednego profilu w `files/fork_tv/`: dziennik odcinków, stan oglądania, obserwowane, sesje kanałów.
 * Czysta logika + pliki (bez Androida). Wyłączony ([enabled] = false) nic nie czyta i nie zapisuje.
 */
class TvData(
    private val dir: File,
    private val profileId: Int,
    @Volatile var enabled: Boolean = true,
    private val rules: () -> WatchRules = { WatchRules() },
    private val autoFollow: () -> Boolean = { true },
    private val save: (() -> Unit) -> Unit = { it() },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    private var log: List<LogEntry> = emptyList()
    private var records = LinkedHashMap<String, WatchRecord>()
    /** Fork 19.11 (ANR na JMGO): kopia stanu oglądania dla komórek siatki EPG (wątek główny) czytana bez blokady. */
    @Volatile private var recSnap: List<WatchRecord> = emptyList()
    private var followed: List<Followed> = emptyList()
    private var sessions: List<ViewSession> = emptyList()
    private var current: WatchRecord? = null
    private var loaded = false

    private fun f(name: String) = File(dir, "${name}_$profileId")

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        log = EpisodeLog.read(File(f("episode_log").path + ".tsv"))
        records = LinkedHashMap(readList("watch", WatchRecord.serializer()).associateBy { it.key })
        recSnap = records.values.toList()
        followed = readList("followed", Followed.serializer())
        sessions = readList("sessions", ViewSession.serializer())
    }

    private fun <T> readList(name: String, s: kotlinx.serialization.KSerializer<T>): List<T> =
        runCatching { f(name).let { File(it.path + ".json") }.takeIf { it.isFile }?.readText()?.let { json.decodeFromString(ListSerializer(s), it) } }.logged("ForkTv", "TvData.kt:44").getOrNull().orEmpty()

    private fun <T> writeList(name: String, s: kotlinx.serialization.KSerializer<T>, data: List<T>) {
        val text = json.encodeToString(ListSerializer(s), data)
        save {
            runCatching {
                dir.mkdirs()
                val file = File(f(name).path + ".json"); val tmp = File(file.path + ".tmp")
                tmp.writeText(text); if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
            }.logged("ForkTv", "TvData.kt:49")
        }
    }

    /** Guide read: extends the episode log from archive channels ([kept] may hold only those). */
    fun onGuide(kept: Map<String, List<GuideProgramme>>, catchupDays: Map<String, Int>, nowMs: Long) {
        if (!enabled) return
        // Fork 19.11 (ANR na JMGO): normalizacja całego przewodnika i scalanie POZA blokadą — pod blokadą czekały na nią
        // komórki siatki EPG (markFor w wątku głównym).
        val base = synchronized(lock) { ensureLoaded(); log }
        val fresh = EpisodeLog.fromGuide(kept, catchupDays, nowMs)
        if (fresh.isEmpty()) return
        val merged = EpisodeLog.merge(base, fresh)
        synchronized(lock) {
            log = if (log === base) merged else EpisodeLog.merge(log, fresh)
            val snapshot = log
            save { EpisodeLog.write(File(f("episode_log").path + ".tsv"), snapshot) }
        }
    }

    /** Viewer starts a channel (live) or a past programme (archive): opens a session and the episode's record. */
    fun onPlay(guideKey: String, programme: GuideProgramme?, nowMs: Long) {
        if (!enabled) return
        synchronized(lock) {
            ensureLoaded()
            current = programme?.let { recordFor(guideKey, it) }
            sessions = ChannelStats.prune(sessions + ViewSession(guideKey, nowMs, 0), nowMs)
            writeList("sessions", ViewSession.serializer(), sessions)
        }
    }

    private fun recordFor(guideKey: String, p: GuideProgramme): WatchRecord {
        val program = ShowIdentity.normalizeTitle(p.title)
        val ep = p.episodeLabel?.takeIf { it.isNotBlank() } ?: ShowIdentity.episodeId(p.title, p.description)
        val key = WatchLog.keyFor(program, ep, guideKey, p.startMs)
        return records[key] ?: WatchRecord(key, program, ep, guideKey, p.startMs, p.stopMs - p.startMs)
    }

    /** Every ~10 s: the picture is at [playheadMs] of the channel's time. */
    fun onPosition(guideKey: String?, playheadMs: Long, nowMs: Long, lookup: (String, Long) -> GuideProgramme? = { _, _ -> null }) {
        if (!enabled || guideKey == null) return
        synchronized(lock) {
            ensureLoaded()
            var cur = current
            if (cur == null || cur.guideKey != guideKey || playheadMs !in cur.startMs..(cur.startMs + cur.durMs)) {
                val p = lookup(guideKey, playheadMs) ?: return
                cur = recordFor(guideKey, p)
            }
            val updated = WatchLog.tick(cur, playheadMs, nowMs, rules())
            current = updated
            if (updated != records[updated.key]) {
                records[updated.key] = updated
                sessions = sessions.dropLast(1) + (sessions.lastOrNull()?.let { it.copy(minutes = it.minutes + 1) } ?: ViewSession(guideKey, nowMs, 1))
                afterChange(nowMs)
            }
        }
    }

    private fun afterChange(nowMs: Long) {
        val recs = records.values.toList()
        recSnap = recs
        writeList("watch", WatchRecord.serializer(), recs)
        writeList("sessions", ViewSession.serializer(), sessions)
        val seriesKeys = recs.map { it.programKey }.distinct().filter { k ->
            ProgramTypes.classify(log.filter { it.programKey == k }, nowMs) == ProgramType.SERIES
        }.toSet()
        val titles = log.associate { it.programKey to it.title }
        val next = Following.autoFollow(followed, recs, titles, seriesKeys, nowMs, autoFollow())
        if (next !== followed) { followed = next; writeList("followed", Followed.serializer(), followed) }
    }

    fun follow(programKey: String, title: String, nowMs: Long) = synchronized(lock) {
        if (!enabled) return@synchronized
        ensureLoaded(); followed = Following.follow(followed, programKey, title, nowMs); writeList("followed", Followed.serializer(), followed)
    }

    fun unfollow(programKey: String) = synchronized(lock) {
        if (!enabled) return@synchronized
        ensureLoaded(); followed = Following.unfollow(followed, programKey); writeList("followed", Followed.serializer(), followed)
    }

    fun followedList(): List<Followed> = synchronized(lock) { ensureLoaded(); Following.active(followed) }
    /** Znacznik komórki przewodnika: stan i postęp emisji o tym tytule i starcie (null = brak śladu). */
    fun markFor(title: String, startMs: Long): Pair<WatchState, Float>? {
        // Fork 19.11 (ANR na JMGO): wołane dla każdej komórki siatki w wątku głównym — bez blokady, z kopii [recSnap]
        // (pusta, dopóki dane nie są wczytane w tle: wtedy komórka po prostu nie ma znacznika).
        if (!enabled) return null
        val recs = recSnap
        if (recs.isEmpty()) return null
        val pk = ShowIdentity.normalizeTitle(title)
        val r = recs.firstOrNull { it.programKey == pk && it.startMs == startMs } ?: return null
        return if (r.state == WatchState.UNWATCHED) null else r.state to (r.positionMs.toFloat() / r.durMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    fun records(): List<WatchRecord> = synchronized(lock) { ensureLoaded(); records.values.toList() }
    fun episodeLog(): List<LogEntry> = synchronized(lock) { ensureLoaded(); log }
    fun sessions(): List<ViewSession> = synchronized(lock) { ensureLoaded(); sessions }

    fun mark(key: String, watched: Boolean, nowMs: Long) = synchronized(lock) {
        if (!enabled) return@synchronized
        ensureLoaded()
        val r = records[key] ?: return@synchronized
        records[key] = if (watched) WatchLog.markWatched(r, nowMs) else WatchLog.markUnwatched(r)
        afterChange(nowMs)
    }

    /** „Wyczyść historię oglądania TV”: kasuje stan oglądania, obserwowane i sesje (dziennik odcinków zostaje). */
    fun clearHistory() = synchronized(lock) {
        ensureLoaded()
        records.clear(); recSnap = emptyList(); followed = emptyList(); sessions = emptyList(); current = null
        listOf("watch", "followed", "sessions").forEach { File(f(it).path + ".json").delete() }
    }
}
