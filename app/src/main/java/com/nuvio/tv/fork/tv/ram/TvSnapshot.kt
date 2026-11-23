package com.nuvio.tv.fork.tv.ram

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvCatchup
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Próbka startowa telewizji (właściciel 23.11: „w RAM trzymać tylko małą próbkę programów startową, tak żeby szybko po
 * wejściu do zakładki IPTV rysował się ekran”). Najwyżej [MAX_CHANNELS] kanałów — pierwsze z ekranu telewizji, ulubione,
 * ostatnie i te z rzędu „Dla Ciebie” — z programem od godziny temu do [AHEAD_MS] naprzód. Plik `filesDir/fork_tv/snapshot_v2.bin.gz` (v2 z obrazkami programów)
 * (kilkaset KB), w pamięci po starcie aplikacji. Ekran telewizji i „Dla Ciebie” pokazują ją, dopóki pełne dane się
 * wczytują albo gdy telewizja jest zwolniona z pamięci ([TvRam]).
 */
object TvSnapshot {
    private const val TAG = "ForkTvRam"
    // 2 (23.11, właściciel: „ulubione w większości nie pokazują miniaturek”): program z obrazkiem; plik v1 zostaje nieczytany.
    private const val VERSION = 2
    const val MAX_CHANNELS = 160
    private const val PER_CHANNEL = 14
    private const val AHEAD_MS = 12L * 60 * 60 * 1000
    private const val SAVE_EVERY_MS = 10L * 60 * 1000

    class Data(val savedAtMs: Long, val channels: List<LiveTvChannel>, val programmes: Map<String, List<LiveTvProgramme>>, val favoriteUrls: Set<String>) {
        /** Program teraz na kanale (z próbki). */
        fun currentProgrammes(nowMs: Long): Map<String, LiveTvProgramme> {
            val out = HashMap<String, LiveTvProgramme>()
            for ((k, list) in programmes) list.firstOrNull { it.startEpochMs <= nowMs && it.stopEpochMs > nowMs }?.let { out[k] = it }
            return out
        }
    }

    @Volatile var data: Data? = null
        private set

    /** Program kanału z próbki (pusty, gdy go nie ma) — dopóki przewodnik się wczytuje. */
    fun programmesFor(guideKey: String): List<LiveTvProgramme> = data?.programmes?.get(guideKey).orEmpty()
    @Volatile private var file: File? = null
    @Volatile private var lastSaveMs = 0L
    @Volatile private var lastKey = 0

    /** Wczytanie z dysku (raz, w tle; przy starcie modułu TV). */
    fun load(c: Context) {
        if (file != null) return
        loadFrom(File(File(c.applicationContext.filesDir, "fork_tv"), "snapshot_v$VERSION.bin.gz"))
    }

    /** Testy JVM i [load]: plik próbki. */
    internal fun loadFrom(f: File) {
        file = f; data = null; lastKey = 0; lastSaveMs = 0
        if (!f.isFile) { Log.i(TAG, "próbka startowa: brak (powstanie przy pierwszej wizycie w telewizji)"); return }
        val t0 = System.nanoTime()
        try {
            DataInputStream(GZIPInputStream(f.inputStream().buffered(1 shl 15)).buffered(1 shl 15)).use { inp ->
                if (inp.readInt() != VERSION) return
                val saved = inp.readLong()
                val fav = HashSet<String>().apply { repeat(inp.readInt()) { add(inp.readUTF()) } }
                val channels = List(inp.readInt()) { readChannel(inp) }
                val progs = HashMap<String, List<LiveTvProgramme>>()
                repeat(inp.readInt()) {
                    val key = inp.readUTF()
                    progs[key] = List(inp.readInt()) { LiveTvProgramme(inp.readUTF(), inp.readLong(), inp.readLong(), episode = opt(inp), image = opt(inp)) }
                }
                data = Data(saved, channels, progs, fav)
            }
            Log.i(TAG, "próbka startowa: ${data?.channels?.size ?: 0} kanałów wczytane w ${(System.nanoTime() - t0) / 1_000_000} ms")
        } catch (e: Exception) {
            Log.w(TAG, "próbka startowa: odczyt: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Zapis próbki z pełnych danych ekranu telewizji (w tle; najwyżej raz na [SAVE_EVERY_MS], chyba że zmieniła się
     * pierwsza strona listy). [first] — kanały w kolejności ekranu.
     */
    fun save(first: List<LiveTvChannel>, all: List<LiveTvChannel>, favoriteUrls: Set<String>, extraGuideKeys: Collection<String>, schedule: (String) -> List<LiveTvProgramme>) {
        val f = file ?: return
        if (first.isEmpty()) return
        val now = System.currentTimeMillis()
        val key = first.take(40).fold(favoriteUrls.hashCode()) { h, c -> 31 * h + c.streamUrl.hashCode() }
        if (key == lastKey && now - lastSaveMs < SAVE_EVERY_MS) return
        try {
            val picked = LinkedHashMap<String, LiveTvChannel>()
            all.asSequence().filter { it.streamUrl in favoriteUrls }.forEach { picked.putIfAbsent(it.streamUrl, it) }
            if (extraGuideKeys.isNotEmpty()) {
                val want = extraGuideKeys.toHashSet()
                all.asSequence().filter { it.guideKey in want }.forEach { if (picked.size < MAX_CHANNELS / 2) picked.putIfAbsent(it.streamUrl, it) }
            }
            for (c in first) { if (picked.size >= MAX_CHANNELS) break; picked.putIfAbsent(c.streamUrl, c) }
            val ordered = first.filter { it.streamUrl in picked } + picked.values.filter { p -> first.none { it.streamUrl == p.streamUrl } }
            val progs = LinkedHashMap<String, List<LiveTvProgramme>>()
            for (c in ordered) if (c.guideKey !in progs) {
                val l = schedule(c.guideKey).filter { it.stopEpochMs > now - 60 * 60_000 && it.startEpochMs < now + AHEAD_MS }.take(PER_CHANNEL)
                if (l.isNotEmpty()) progs[c.guideKey] = l.map { LiveTvProgramme(it.title, it.startEpochMs, it.stopEpochMs, episode = it.episode, image = it.image?.takeIf { u -> u.length <= 1_000 }) }
            }
            f.parentFile?.mkdirs()
            val tmp = File(f.path + ".tmp")
            DataOutputStream(GZIPOutputStream(tmp.outputStream().buffered(1 shl 15)).buffered(1 shl 15)).use { o ->
                o.writeInt(VERSION); o.writeLong(now)
                o.writeInt(favoriteUrls.size); favoriteUrls.forEach { o.writeUTF(it) }
                o.writeInt(ordered.size); ordered.forEach { writeChannel(o, it) }
                o.writeInt(progs.size)
                for ((k, l) in progs) { o.writeUTF(k); o.writeInt(l.size); l.forEach { p -> o.writeUTF(p.title); o.writeLong(p.startEpochMs); o.writeLong(p.stopEpochMs); optW(o, p.episode); optW(o, p.image) } }
            }
            if (!tmp.renameTo(f)) throw java.io.IOException("nie udało się zapisać ${f.name}")
            data = Data(now, ordered, progs, favoriteUrls.toHashSet())
            lastKey = key; lastSaveMs = now
            Log.i(TAG, "próbka startowa: zapisane ${ordered.size} kanałów, przewodnik dla ${progs.size}")
        } catch (e: Exception) {
            Log.w(TAG, "próbka startowa: zapis: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun opt(i: DataInputStream): String? = if (i.readBoolean()) i.readUTF() else null
    private fun optW(o: DataOutputStream, s: String?) { o.writeBoolean(s != null); if (s != null) o.writeUTF(s) }

    private fun writeChannel(o: DataOutputStream, c: LiveTvChannel) {
        o.writeUTF(c.id); o.writeUTF(c.name); o.writeUTF(c.streamUrl); optW(o, c.tvgId); optW(o, c.logoUrl); o.writeUTF(c.group)
        o.writeInt(c.headers.size); c.headers.forEach { (k, v) -> o.writeUTF(k); o.writeUTF(v) }
        optW(o, c.stalkerCommand); o.writeUTF(c.sourceId); o.writeLong(c.hideKey); o.writeUTF(c.guideKey); optW(o, c.tvgName)
        val cu = c.catchup
        o.writeBoolean(cu != null)
        if (cu != null) { o.writeUTF(cu.kind.name); o.writeInt(cu.days); optW(o, cu.template) }
        o.writeInt(c.guideShiftMinutes)
    }

    private fun readChannel(i: DataInputStream): LiveTvChannel {
        val id = i.readUTF(); val name = i.readUTF(); val url = i.readUTF(); val tvgId = opt(i); val logo = opt(i); val group = i.readUTF()
        val headers = LinkedHashMap<String, String>().apply { repeat(i.readInt()) { put(i.readUTF(), i.readUTF()) } }
        val stalker = opt(i); val sourceId = i.readUTF(); val hideKey = i.readLong(); val guideKey = i.readUTF(); val tvgName = opt(i)
        val catchup = if (i.readBoolean()) LiveTvCatchup(LiveTvCatchup.Kind.valueOf(i.readUTF()), i.readInt(), opt(i)) else null
        return LiveTvChannel(id, name, url, tvgId, logo, group, headers, stalker, sourceId, hideKey, guideKey, tvgName, catchup, i.readInt())
    }
}
