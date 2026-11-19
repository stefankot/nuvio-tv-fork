package com.nuvio.tv.fork.sources

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.util.LruCache
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import com.nuvio.tv.fork.logged

/**
 * Fork 19.11 (właściciel, zgłoszenia 11/11a): sonda ścieżek PRZED wyborem źródła. Podczas ładowania źródeł czyta w tle
 * nagłówek kontenera kilku najlepszych plików (MKV: element Tracks z początku pliku zapytaniem Range; inne: MediaExtractor
 * jak Echo AudioTrackProbe) i zapamiętuje języki dźwięku oraz napisów WBUDOWANYCH. Nie blokuje niczego: ranking
 * ([KeRanking]) korzysta z wyników, które zdążyły przyjść (zwykle przed odpowiedzią ostatniego dodatku).
 * Tylko źródła gotowe (debrid w pamięci / bezpośrednie http), nigdy P2P.
 */
object TrackScout {
    data class Tracks(val audio: List<String>, val subs: List<String>) {
        val hasPolishAudio get() = "pl" in audio
        val hasPolishSubs get() = "pl" in subs
    }

    private const val TAG = "ForkTrackScout"
    private const val TOP = 6
    private const val PARALLEL = 3
    private const val TIMEOUT_MS = 5_000L
    private const val HEAD_BYTES = 1024 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(PARALLEL)
    private val cache = LruCache<String, Tracks>(128)
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS).followRedirects(true).followSslRedirects(true).build()
    }

    private fun key(s: Stream): String? = s.url?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }

    /** Wynik sondy dla źródła; null = jeszcze nie sprawdzone / nie dało się odczytać. */
    fun of(s: Stream): Tracks? = key(s)?.let { cache.get(it) }

    /** Hook ekranu źródeł (każda partia odpowiedzi dodatków): sonda najlepszych jeszcze niesprawdzonych. */
    fun onSources(streams: List<Stream>) {
        if (streams.isEmpty()) return
        // Fork 19.11 (ANR na JMGO przy 917 źródłach): wołane z wątku głównego przy każdej partii — ranking w tle, raz na 0,5 s.
        pending?.cancel()
        pending = scope.launch { delay(500); pick(streams) }
    }
    @Volatile private var pending: kotlinx.coroutines.Job? = null

    private fun pick(streams: List<Stream>) {
        val picks = runCatching { KeRanking.order(streams) }.logged("ForkSources", "TrackScout.kt:64").getOrDefault(streams).asSequence()
            .filter { !it.isTorrent() && SourceReady.tier(it) == 0 }
            .mapNotNull { s -> key(s)?.let { k -> s to k } }
            .filter { (_, k) -> cache.get(k) == null && k !in inFlight }
            .take(TOP).toList()
        for ((s, k) in picks) {
            if (!inFlight.add(k)) continue
            scope.launch {
                try {
                    gate.withPermit {
                        val t = withTimeoutOrNull(TIMEOUT_MS) { probe(k, s.behaviorHints?.proxyHeaders?.request.orEmpty()) }
                        if (t != null) {
                            cache.put(k, t)
                            Log.i(TAG, "${s.addonName} | audio=${t.audio} subs=${t.subs} | ${listOfNotNull(s.name, s.title).joinToString(" ").replace('\n', ' ').take(80)}")
                        }
                    }
                } finally {
                    inFlight.remove(k)
                }
            }
        }
    }

    private fun probe(url: String, headers: Map<String, String>): Tracks? {
        val head = range(url, headers, 0, HEAD_BYTES) ?: return null
        if (head.size >= 4 && head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() && head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte()) {
            return Mkv.tracks(head) { pos, len -> range(url, headers, pos, len) }
        }
        return extractor(url, headers)
    }

    private fun range(url: String, headers: Map<String, String>, from: Long, len: Int): ByteArray? = runCatching {
        val rb = Request.Builder().url(url).header("Range", "bytes=$from-${from + len - 1}")
        headers.forEach { (k, v) -> rb.header(k, v) }
        client.newCall(rb.build()).execute().use { r ->
            if (!r.isSuccessful) return@use null
            if (r.code == 200 && from > 0) return@use null // serwer bez Range: nie pobieramy całego pliku
            val src = r.body?.source() ?: return@use null
            val out = java.io.ByteArrayOutputStream(len)
            val buf = ByteArray(64 * 1024)
            while (out.size() < len) {
                val n = src.read(buf, 0, minOf(buf.size, len - out.size()))
                if (n <= 0) break
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    }.logged("ForkSources", "TrackScout.kt:95").getOrNull()

    private fun extractor(url: String, headers: Map<String, String>): Tracks? {
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(url, headers)
            val audio = ArrayList<String>(); val subs = ArrayList<String>()
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                val lang = lang(f.getString(MediaFormat.KEY_LANGUAGE))
                if (mime.startsWith("audio/")) audio += lang
                else if (mime.startsWith("text/") || mime.startsWith("application/x-subrip") || mime.contains("ttml") || mime.contains("vtt")) subs += lang
            }
            Tracks(audio.distinct(), subs.distinct()).takeIf { ex.trackCount > 0 }
        } catch (e: Throwable) {
            android.util.Log.w("ForkSources", "TrackScout: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
            null
        } finally {
            runCatching { ex.release() }.logged("ForkSources", "TrackScout.kt:129")
        }
    }

    private val ISO3 = mapOf(
        "pol" to "pl", "eng" to "en", "ger" to "de", "deu" to "de", "fre" to "fr", "fra" to "fr", "spa" to "es", "ita" to "it",
        "rus" to "ru", "ukr" to "uk", "cze" to "cs", "ces" to "cs", "slo" to "sk", "slk" to "sk", "por" to "pt", "jpn" to "ja",
        "kor" to "ko", "chi" to "zh", "zho" to "zh", "dut" to "nl", "nld" to "nl", "swe" to "sv", "nor" to "no", "dan" to "da",
        "fin" to "fi", "hun" to "hu", "rum" to "ro", "ron" to "ro", "bul" to "bg", "hrv" to "hr", "srp" to "sr", "slv" to "sl",
        "tur" to "tr", "gre" to "el", "ell" to "el", "ara" to "ar", "heb" to "he", "hin" to "hi", "tha" to "th", "vie" to "vi",
    )

    /** „pol”, „pl-PL”, „pl” → „pl”; brak → „und”. */
    fun lang(raw: String?): String {
        val l = raw?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_').orEmpty()
        if (l.isEmpty() || l == "und") return "und"
        return ISO3[l] ?: l
    }

    /** Minimalny odczyt Matroski: Segment → (SeekHead →) Tracks → TrackEntry (typ, język, LanguageIETF). */
    private object Mkv {
        private const val SEGMENT = 0x18538067L
        private const val SEEK_HEAD = 0x114D9B74L
        private const val SEEK = 0x4DBBL
        private const val SEEK_ID = 0x53ABL
        private const val SEEK_POS = 0x53ACL
        private const val TRACKS = 0x1654AE6BL
        private const val CLUSTER = 0x1F43B675L
        private const val ENTRY = 0xAEL
        private const val TYPE = 0x83L
        private const val LANG = 0x22B59CL
        private const val IETF = 0x22B59DL

        private class El(val id: Long, val dataStart: Int, val size: Long) { val end get() = dataStart + size }

        private fun vint(b: ByteArray, p: Int, keepMarker: Boolean): Pair<Long, Int>? {
            if (p >= b.size) return null
            val first = b[p].toInt() and 0xFF
            if (first == 0) return null
            val len = Integer.numberOfLeadingZeros(first) - 23
            if (len < 1 || len > 8 || p + len > b.size) return null
            var v = (if (keepMarker) first else first and (0xFF shr len)).toLong()
            for (i in 1 until len) v = (v shl 8) or (b[p + i].toLong() and 0xFF)
            if (!keepMarker && v == (1L shl (7 * len)) - 1) v = -1 // nieznany rozmiar
            return v to len
        }

        private fun el(b: ByteArray, p: Int): El? {
            val (id, il) = vint(b, p, true) ?: return null
            val (sz, sl) = vint(b, p + il, false) ?: return null
            return El(id, p + il + sl, sz)
        }

        private fun uint(b: ByteArray, e: El): Long { var v = 0L; for (i in 0 until e.size.toInt().coerceAtMost(8)) v = (v shl 8) or (b[e.dataStart + i].toLong() and 0xFF); return v }
        private fun str(b: ByteArray, e: El) = String(b, e.dataStart, e.size.toInt().coerceAtMost(b.size - e.dataStart), Charsets.UTF_8).trimEnd('\u0000')

        fun tracks(head: ByteArray, fetch: (Long, Int) -> ByteArray?): Tracks? {
            val ebml = el(head, 0) ?: return null
            val seg = el(head, ebml.end.toInt()) ?: return null
            if (seg.id != SEGMENT) return null
            val segStart = seg.dataStart
            var p = segStart
            var tracksPos: Long? = null
            while (p < head.size) {
                val e = el(head, p) ?: break
                when (e.id) {
                    TRACKS -> return if (e.end <= head.size) parse(head, e) else fetch(p.toLong(), (e.end - p).toInt().coerceAtMost(4 * 1024 * 1024))?.let { b -> el(b, 0)?.let { parse(b, it) } }
                    SEEK_HEAD -> if (e.end <= head.size) tracksPos = seekTracks(head, e)
                    CLUSTER -> break
                }
                if (e.size < 0) break
                p = e.end.toInt()
            }
            val pos = tracksPos ?: return null
            val at = segStart + pos
            val b = fetch(at, 512 * 1024) ?: return null
            val t = el(b, 0)?.takeIf { it.id == TRACKS } ?: return null
            if (t.end > b.size) return null
            return parse(b, t)
        }

        private fun seekTracks(b: ByteArray, sh: El): Long? {
            var p = sh.dataStart
            while (p < sh.end) {
                val s = el(b, p) ?: return null
                if (s.id == SEEK) {
                    var q = s.dataStart; var id: Long? = null; var pos: Long? = null
                    while (q < s.end) {
                        val c = el(b, q) ?: break
                        if (c.id == SEEK_ID) id = uint(b, c) else if (c.id == SEEK_POS) pos = uint(b, c)
                        q = c.end.toInt()
                    }
                    if (id == TRACKS) return pos
                }
                p = s.end.toInt()
            }
            return null
        }

        private fun parse(b: ByteArray, tracks: El): Tracks {
            val audio = ArrayList<String>(); val subs = ArrayList<String>()
            var p = tracks.dataStart
            while (p < tracks.end && p < b.size) {
                val e = el(b, p) ?: break
                if (e.id == ENTRY && e.end <= b.size) {
                    var q = e.dataStart; var type = 0L; var lang = "eng"; var ietf: String? = null // Matroska: brak Language = „eng”
                    while (q < e.end) {
                        val c = el(b, q) ?: break
                        when (c.id) { TYPE -> type = uint(b, c); LANG -> lang = str(b, c); IETF -> ietf = str(b, c) }
                        q = c.end.toInt()
                    }
                    val l = lang(ietf ?: lang)
                    if (type == 2L) audio += l else if (type == 17L) subs += l
                }
                p = e.end.toInt()
            }
            return Tracks(audio.distinct(), subs.distinct())
        }
    }
}
