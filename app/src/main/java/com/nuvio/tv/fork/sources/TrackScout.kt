package com.nuvio.tv.fork.sources

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.util.LruCache
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.dublagem.DubbingAudioTrack
import com.nuvio.tv.fork.dublagem.PolishDubbing
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
    data class Tracks(
        val audio: List<String>,
        val subs: List<String>,
        val audioDetails: List<DubbingAudioTrack> = emptyList(),
        val durationMs: Long? = null,
        val frameRate: Float? = null,
        val byteRanges: Boolean = false,
    ) {
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
    private val sourcesByTitle = LruCache<String, List<Stream>>(16)
    private val polishAudioHint = Regex("""(?i)\b(?:pl|pol|polish|polski|lektor|dubbing)\b""")
    private val highQualityAudioHint = Regex("""(?i)true[ ._-]?hd|flac|dts[ ._-]?(?:hd|x)|7[ .]1|5[ .]1|eac3|ddp""")
    private val probedByTitle = LruCache<String, MutableSet<String>>(16)
    private val possiblePolish = Regex("""(?i)\b(?:pl|pol|polish|polski|lektor|napisy|multi)\b""")
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS).followRedirects(true).followSslRedirects(true).build()
    }

    private fun key(s: Stream): String? = s.url?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }

    /** Wynik sondy dla źródła; null = jeszcze nie sprawdzone / nie dało się odczytać. */
    fun of(s: Stream): Tracks? = key(s)?.let { cache.get(it) }

    internal fun candidates(type: String, videoId: String): List<Stream> =
        sourcesByTitle.get("$type|$videoId").orEmpty()

    internal fun probing(stream: Stream): Boolean = key(stream)?.let(inFlight::contains) == true

    /** The detail preload and the source picker share this bounded probe for the same title. */
    @Synchronized
    fun onSources(context: Context, type: String, videoId: String, streams: List<Stream>) {
        if (streams.isEmpty() || (!EmbeddedSourceSubtitles.enabled(context) && !PolishDubbing.enabled(context))) return
        val app = context.applicationContext
        pending?.cancel()
        pending = scope.launch { delay(250); pick(app, type, videoId, streams) }
    }

    private var pending: kotlinx.coroutines.Job? = null

    private fun pick(context: Context, type: String, videoId: String, streams: List<Stream>) {
        val available = streams.asSequence()
            .filter { !it.isTorrent() && SourceReady.tier(it) == 0 && !Placeholders.isMarked(it.getStreamUrl()) }
            .mapNotNull { stream -> key(stream)?.let { stream to it } }
            .distinctBy { it.second }
            .toList()
        // Probe early results and a mix of resolutions likely to carry Polish tracks.
        val languageCandidates = available.filter { (stream, _) ->
            possiblePolish.containsMatchIn(listOfNotNull(stream.name, stream.title, stream.description).joinToString(" "))
        }.groupBy { (stream, _) -> com.nuvio.tv.core.stream.ReleaseFactsParser.resolutionOf(stream) }
        val dubbing = PolishDubbing.enabled(context)
        val audioCandidates = if (dubbing) available.mapNotNull { item ->
            val s = item.first
            val text = listOfNotNull(s.behaviorHints?.filename, s.name, s.title, s.description).joinToString(" ")
            if (polishAudioHint.containsMatchIn(text)) item to highQualityAudioHint.containsMatchIn(text) else null
        }.sortedByDescending { it.second }.map { it.first }.take(TOP) else emptyList()
        val picks = (audioCandidates + languageCandidates.values.map { it.first() } + available).distinctBy { it.second }
        val titleKey = "$type|$videoId"
        val probeLimit = if (dubbing) TOP * 3 else TOP * 2
        val shortlist = picks.take(probeLimit)
        if (dubbing) sourcesByTitle.put(titleKey, shortlist.map { it.first })
        fun offerTrack(stream: Stream, tracks: Tracks) {
            if (tracks.hasPolishSubs) EmbeddedSourceSubtitles.offer(context, type, videoId, stream)
        }
        fun allowProbe(url: String): Boolean = synchronized(probedByTitle) {
            val tried = probedByTitle.get(titleKey) ?: mutableSetOf<String>().also { probedByTitle.put(titleKey, it) }
            tried.size < probeLimit && tried.add(url)
        }
        for ((s, k) in shortlist) {
            val cached = cache.get(k)
            if (cached != null) {
                offerTrack(s, cached)
                continue
            }
            if (!inFlight.add(k)) continue
            if (!allowProbe(k)) {
                inFlight.remove(k)
                continue
            }
            scope.launch {
                try {
                    gate.withPermit {
                        val t = withTimeoutOrNull(TIMEOUT_MS) { probe(k, s.behaviorHints?.proxyHeaders?.request.orEmpty()) }
                        if (t != null) {
                            cache.put(k, t)
                            offerTrack(s, t)
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
        val response = range(url, headers, 0, HEAD_BYTES) ?: return null
        val head = response.bytes
        val tracks = if (head.size >= 4 && head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() && head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte()) {
            Mkv.tracks(head) { pos, len -> range(url, headers, pos, len)?.bytes }
        } else {
            extractor(url, headers)
        }
        return tracks?.copy(byteRanges = response.partial)
    }

    private data class RangeRead(val bytes: ByteArray, val partial: Boolean)

    private fun range(url: String, headers: Map<String, String>, from: Long, len: Int): RangeRead? = runCatching {
        val request = com.nuvio.tv.ui.screens.player.PlayerMediaSourceFactory.normalizePlaybackRequest(
            url, com.nuvio.tv.ui.screens.player.PlayerMediaSourceFactory.sanitizeHeaders(headers)
        )
        val rb = Request.Builder().url(request.url)
        request.headers.forEach { (k, v) -> if (!k.equals("Range", ignoreCase = true)) rb.header(k, v) }
        rb.header("Range", "bytes=$from-${from + len - 1}")
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
            RangeRead(out.toByteArray(), r.code == 206)
        }
    }.logged("ForkSources", "TrackScout.kt:95").getOrNull()

    private fun extractor(url: String, headers: Map<String, String>): Tracks? {
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(url, headers)
            val audio = ArrayList<DubbingAudioTrack>()
            val subs = ArrayList<String>()
            var durationMs: Long? = null
            var frameRate: Float? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                val lang = lang(f.getString(MediaFormat.KEY_LANGUAGE))
                when {
                    mime.startsWith("audio/") -> audio += DubbingAudioTrack(
                        audio.size, lang, mime, f.optionalInt(MediaFormat.KEY_CHANNEL_COUNT),
                        f.optionalInt(MediaFormat.KEY_BIT_RATE), f.optionalInt(MediaFormat.KEY_SAMPLE_RATE),
                        f.getString("title"),
                    )
                    mime.startsWith("video/") -> {
                        if (f.containsKey(MediaFormat.KEY_DURATION)) durationMs = f.getLong(MediaFormat.KEY_DURATION) / 1_000L
                        frameRate = f.optionalFrameRate()
                    }
                    mime.startsWith("text/") || mime.startsWith("application/x-subrip") ||
                        mime.contains("ttml") || mime.contains("vtt") -> subs += lang
                }
            }
            Tracks(audio.map { it.language }.distinct(), subs.distinct(), audio, durationMs, frameRate).takeIf { ex.trackCount > 0 }
        } catch (e: Throwable) {
            android.util.Log.w("ForkSources", "TrackScout: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
            null
        } finally {
            runCatching { ex.release() }.logged("ForkSources", "TrackScout.kt:129")
        }
    }

    private fun MediaFormat.optionalInt(key: String): Int = if (containsKey(key)) getInteger(key) else 0

    private fun MediaFormat.optionalFrameRate(): Float? {
        if (!containsKey(MediaFormat.KEY_FRAME_RATE)) return null
        val value = try {
            getFloat(MediaFormat.KEY_FRAME_RATE)
        } catch (_: ClassCastException) {
            getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
        }
        return value.takeIf { it.isFinite() && it > 0 }
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
        private const val INFO = 0x1549A966L
        private const val TIMESTAMP_SCALE = 0x2AD7B1L
        private const val DURATION = 0x4489L
        private const val ENTRY = 0xAEL
        private const val TYPE = 0x83L
        private const val LANG = 0x22B59CL
        private const val IETF = 0x22B59DL
        private const val CODEC = 0x86L
        private const val NAME = 0x536EL
        private const val DEFAULT_DURATION = 0x23E383L
        private const val AUDIO = 0xE1L
        private const val CHANNELS = 0x9FL
        private const val SAMPLE_RATE = 0xB5L
        private const val FLAG_VISUAL_IMPAIRED = 0x55ACL
        private const val FLAG_COMMENTARY = 0x55AFL

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
            var durationMs: Long? = null
            var trackElement: El? = null
            while (p < head.size) {
                val e = el(head, p) ?: break
                if (e.id == CLUSTER || e.size < 0) break
                if (e.end > head.size) {
                    if (e.id == TRACKS) tracksPos = (p - segStart).toLong()
                    break
                }
                when (e.id) {
                    INFO -> durationMs = duration(head, e)
                    TRACKS -> trackElement = e
                    SEEK_HEAD -> tracksPos = seekTracks(head, e)
                }
                if (trackElement != null && durationMs != null) break
                p = e.end.toInt()
            }
            trackElement?.let { return parse(head, it, durationMs) }
            val pos = tracksPos ?: return null
            val b = fetch(segStart + pos, 512 * 1024) ?: return null
            val t = el(b, 0)?.takeIf { it.id == TRACKS && it.size >= 0 && it.end <= b.size } ?: return null
            return parse(b, t, durationMs)
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

        private inline fun children(b: ByteArray, parent: El, read: (El) -> Unit) {
            var position = parent.dataStart
            while (position < parent.end) {
                val child = el(b, position) ?: break
                if (child.size < 0 || child.end > parent.end || child.end > b.size) break
                read(child)
                position = child.end.toInt()
            }
        }

        private fun decimal(b: ByteArray, e: El): Double? = when (e.size) {
            4L -> Float.fromBits(uint(b, e).toInt()).toDouble()
            8L -> Double.fromBits(uint(b, e))
            else -> null
        }?.takeIf { it.isFinite() && it > 0.0 }

        private fun duration(b: ByteArray, info: El): Long? {
            var scale = 1_000_000L
            var ticks: Double? = null
            children(b, info) { child ->
                when (child.id) {
                    TIMESTAMP_SCALE -> scale = uint(b, child)
                    DURATION -> ticks = decimal(b, child)
                }
            }
            return ticks?.let { (it * scale / 1_000_000.0).toLong() }?.takeIf { it > 0 }
        }

        private fun parse(b: ByteArray, tracks: El, durationMs: Long?): Tracks {
            val audio = ArrayList<DubbingAudioTrack>()
            val subs = ArrayList<String>()
            var frameRate: Float? = null
            children(b, tracks) { entry ->
                if (entry.id != ENTRY) return@children
                var type = 0L
                var legacyLang = "eng" // Default for Language in Matroska.
                var ietf: String? = null
                var codec: String? = null
                var label: String? = null
                var sampleRate = 0
                var channels = 1
                var frameDuration = 0L
                var auxiliary = false
                children(b, entry) { field ->
                    when (field.id) {
                        TYPE -> type = uint(b, field)
                        LANG -> legacyLang = str(b, field)
                        IETF -> ietf = str(b, field)
                        CODEC -> codec = str(b, field)
                        NAME -> label = str(b, field)
                        DEFAULT_DURATION -> frameDuration = uint(b, field)
                        FLAG_VISUAL_IMPAIRED, FLAG_COMMENTARY -> auxiliary = auxiliary || uint(b, field) != 0L
                        AUDIO -> children(b, field) { audioField ->
                            when (audioField.id) {
                                CHANNELS -> channels = uint(b, audioField).toInt()
                                SAMPLE_RATE -> sampleRate = decimal(b, audioField)?.toInt() ?: 0
                            }
                        }
                    }
                }
                val language = lang(ietf ?: legacyLang)
                when (type) {
                    1L -> if (frameDuration > 0 && frameRate == null) frameRate = 1_000_000_000f / frameDuration
                    2L -> audio += DubbingAudioTrack(
                        audio.size, language, DubbingAudioTrack.matroskaMime(codec), channels,
                        sampleRate = sampleRate, label = label, auxiliary = auxiliary,
                    )
                    17L -> subs += language
                }
            }
            return Tracks(audio.map { it.language }.distinct(), subs.distinct(), audio, durationMs, frameRate)
        }
    }
}
