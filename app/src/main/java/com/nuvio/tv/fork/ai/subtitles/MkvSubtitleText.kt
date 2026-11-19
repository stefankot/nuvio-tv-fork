package com.nuvio.tv.fork.ai.subtitles

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater

/**
 * Fork 19.11 (właściciel: „Tak, pobieraj”): tekst napisów WBUDOWANYCH w plik MKV pobierany z wyprzedzeniem minut,
 * zamiast czekać, aż odtwarzacz wczyta je w buforze (~50 s). Indeks AutoSync z bazy zna tylko czasy kwestii, więc tu
 * osobny, mały czytnik: nagłówek pliku (SeekHead → Info, Tracks, Cues), w Cues pozycje bloków napisów
 * (CueClusterPosition + CueRelativePosition), potem każdy blok jednym zapytaniem HTTP Range (~4 KB).
 * Bez zmian w kodzie bazy. Nie obsługiwane (wtedy zostaje bufor odtwarzacza): brak CueRelativePosition w pliku,
 * napisy obrazkowe (PGS/VobSub), pliki inne niż MKV/WebM, serwer bez zakresów.
 */
internal class MkvSubtitleText private constructor(
    private val url: String,
    private val headers: Map<String, String>,
    private val segmentStart: Long,
    private val scaleNs: Long,
    val tracks: List<Track>,
) {
    data class Track(
        val number: Int, val codec: String, val language: String?, val name: String?, val forced: Boolean,
        val hearingImpaired: Boolean, val compression: Int?, val stripPrefix: ByteArray?, val cues: List<Entry>,
    ) {
        val isText get() = codec.startsWith("S_TEXT/", ignoreCase = true)
        val lang2 get() = normLang(language)
    }

    /** Kwestia z indeksu: czas, pozycja klastra (względem danych Segmentu) i bloku (względem danych klastra). */
    data class Entry(val timeMs: Long, val cluster: Long, val relative: Long, val durationMs: Long?)

    /** Pobiera tekst jednej kwestii (null: blok nie znaleziony / nie tekst). */
    fun fetchCue(track: Track, e: Entry, nextStartMs: Long?): DocCue? {
        val clusterStart = segmentStart + e.cluster
        // Nagłówek klastra ma 5–12 bajtów (ID 4 + rozmiar 1–8): pobieramy od najkrótszego wariantu i szukamy bloku.
        val from = clusterStart + 5 + e.relative
        val bytes = fetch(from, 4096 + 8) ?: return null
        for (skip in 0..7) {
            if (skip >= bytes.size) break
            val id = bytes[skip].toInt() and 0xFF
            if (id != 0xA3 && id != 0xA0) continue
            val parsed = runCatching { parseBlockElement(bytes, skip, track.number) }.getOrNull() ?: continue
            val (payload, blockDurTicks) = parsed
            val raw = decode(track, payload) ?: return null
            val text = cleanText(track.codec, raw)
            val dur = e.durationMs ?: blockDurTicks?.let { it * scaleNs / 1_000_000 }
                ?: nextStartMs?.let { (it - e.timeMs).coerceIn(500, 4_000) } ?: 3_000
            return DocCue("e%09d".format(Locale.ROOT, e.timeMs), e.timeMs, e.timeMs + dur, text)
        }
        return null
    }

    /** Zwraca (dane bloku po nagłówku, czas trwania z BlockGroup) dla bloku ścieżki [trackNo] zaczynającego się w [o]. */
    private fun parseBlockElement(b: ByteArray, o: Int, trackNo: Int): Pair<ByteArray, Long?>? {
        val id = b[o].toInt() and 0xFF
        val (size, sl) = readSize(b, o + 1) ?: return null
        val dataStart = o + 1 + sl
        if (size <= 0 || size > 1_000_000) return null
        val dataEnd = minOf(b.size.toLong(), dataStart + size).toInt()
        if (id == 0xA3) return blockPayload(b, dataStart, dataEnd, trackNo, dataStart + size <= b.size)?.let { it to null }
        // BlockGroup: Block (0xA1) + opcjonalnie BlockDuration (0x9B).
        var p = dataStart; var payload: ByteArray? = null; var dur: Long? = null
        while (p < dataEnd) {
            val (cid, il) = readId(b, p) ?: break
            val (csize, csl) = readSize(b, p + il) ?: break
            val cs = p + il + csl; val ce = cs + csize
            when (cid) {
                0xA1L -> payload = blockPayload(b, cs, minOf(ce, b.size.toLong()).toInt(), trackNo, ce <= b.size) ?: return null
                0x9BL -> if (ce <= b.size) dur = readUInt(b, cs, csize.toInt())
            }
            if (ce > dataEnd) break
            p = ce.toInt()
        }
        return payload?.let { it to dur }
    }

    private fun blockPayload(b: ByteArray, s: Int, e: Int, trackNo: Int, complete: Boolean): ByteArray? {
        if (!complete) return null
        val (tn, tl) = readSize(b, s) ?: return null
        if (tn.toInt() != trackNo) return null
        val flags = b[s + tl + 2].toInt() and 0xFF
        if (flags and 0x06 != 0) return null // koronki (lacing) — nie dla napisów
        return b.copyOfRange(s + tl + 3, e)
    }

    private fun decode(t: Track, data: ByteArray): ByteArray? = when (t.compression) {
        null -> data
        3 -> (t.stripPrefix ?: ByteArray(0)) + data
        0 -> runCatching {
            val inf = Inflater(); inf.setInput(data)
            val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(4096)
            while (!inf.finished()) { val n = inf.inflate(buf); if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break; out.write(buf, 0, n) }
            inf.end(); out.toByteArray()
        }.onFailure { Log.w(TAG, "zlib: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }.getOrNull()
        else -> null
    }

    private fun fetch(start: Long, length: Int): ByteArray? = fetchRange(url, headers, start, length)

    companion object {
        private const val TAG = "ForkAiMkv"
        private const val HEAD_BYTES = 256 * 1024
        private const val MAX_CUES_BYTES = 24 * 1024 * 1024

        private val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(true).build()

        private val iso3to2: Map<String, String> by lazy {
            Locale.getISOLanguages().mapNotNull { l -> runCatching { Locale(l).isO3Language to l }.getOrNull() }.toMap() +
                mapOf("fre" to "fr", "ger" to "de", "dut" to "nl", "chi" to "zh", "cze" to "cs", "gre" to "el", "rum" to "ro", "slo" to "sk", "per" to "fa")
        }

        fun normLang(l: String?): String? {
            val s = l?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')?.takeIf { it.isNotEmpty() && it != "und" } ?: return null
            return if (s.length == 3) iso3to2[s] ?: s else s
        }

        /** Wczytuje nagłówek i indeks. null: nie MKV, brak zakresów albo brak pozycji bloków napisów. */
        fun open(url: String, headers: Map<String, String>): MkvSubtitleText? {
            if (!url.startsWith("http", ignoreCase = true)) return null
            val t0 = System.currentTimeMillis()
            val head = fetchRange(url, headers, 0, HEAD_BYTES) ?: return null
            if (head.size < 64 || readId(head, 0)?.first != 0x1A45DFA3L) { Log.i(TAG, "to nie MKV — zostaje bufor odtwarzacza"); return null }
            val (_, ebmlIl) = readId(head, 0)!!
            val (ebmlSize, ebmlSl) = readSize(head, ebmlIl) ?: return null
            var p = (ebmlIl + ebmlSl + ebmlSize).toInt()
            val (segId, segIl) = readId(head, p) ?: return null
            if (segId != 0x18538067L) return null
            val (_, segSl) = readSize(head, p + segIl) ?: return null
            val segStart = (p + segIl + segSl).toLong()
            p = segStart.toInt()
            val pos = HashMap<Long, Long>() // ID elementu → pozycja względem danych Segmentu
            val inHead = HashMap<Long, ByteArray>()
            while (p < head.size - 12) {
                val (id, il) = readId(head, p) ?: break
                val (size, sl) = readSize(head, p + il) ?: break
                val ds = p + il + sl
                if (id == 0x1F43B675L || size < 0) break // pierwszy klaster: koniec nagłówka
                val de = ds + size
                if (de <= head.size) {
                    val body = head.copyOfRange(ds, de.toInt())
                    inHead[id] = body
                    if (id == 0x114D9B74L) parseSeekHead(body, pos)
                } else pos.putIfAbsent(id, p - segStart)
                if (de > head.size) break
                p = de.toInt()
            }
            fun element(id: Long, max: Int): ByteArray? = inHead[id] ?: pos[id]?.let { rel ->
                val at = segStart + rel
                val hdr = fetchRange(url, headers, at, 16) ?: return@let null
                val (eid, il) = readId(hdr, 0) ?: return@let null
                if (eid != id) return@let null
                val (size, sl) = readSize(hdr, il) ?: return@let null
                if (size <= 0 || size > max) { Log.w(TAG, "element ${id.toString(16)} za duży: $size B"); return@let null }
                fetchRange(url, headers, at + il + sl, size.toInt())
            }
            val info = element(0x1549A966L, 1 shl 20)
            var scale = 1_000_000L
            info?.let { forEachChild(it, 0, it.size) { id, s, e -> if (id == 0x2AD7B1L) scale = readUInt(it, s, e - s) ?: scale } }
            val tracksEl = element(0x1654AE6BL, 4 shl 20) ?: run { Log.i(TAG, "brak Tracks"); return null }
            val subs = parseTracks(tracksEl)
            if (subs.isEmpty()) { Log.i(TAG, "brak ścieżek napisów tekstowych w MKV"); return null }
            val cuesEl = element(0x1C53BB6BL, MAX_CUES_BYTES) ?: run { Log.i(TAG, "brak indeksu Cues — zostaje bufor odtwarzacza"); return null }
            val entries = parseCues(cuesEl, subs.map { it.number }.toSet(), scale)
            val tracks = subs.map { s -> s.copy(cues = entries[s.number].orEmpty().sortedBy { it.timeMs }.distinctBy { it.timeMs }) }
                .filter { it.cues.size >= 8 }
            Log.i(TAG, "indeks MKV w ${System.currentTimeMillis() - t0} ms: " +
                tracks.joinToString { "#${it.number} ${it.lang2 ?: "?"} ${it.codec} ${it.cues.size} kwestii${if (it.forced) " wymuszone" else ""}" }.ifEmpty { "brak pozycji bloków napisów (CueRelativePosition)" })
            if (tracks.isEmpty()) return null
            return MkvSubtitleText(url, headers, segStart, scale, tracks)
        }

        private fun parseSeekHead(b: ByteArray, out: MutableMap<Long, Long>) = forEachChild(b, 0, b.size) { id, s, e ->
            if (id != 0x4DBBL) return@forEachChild
            var sid: Long? = null; var spos: Long? = null
            forEachChild(b, s, e) { cid, cs, ce ->
                when (cid) {
                    0x53ABL -> sid = readUInt(b, cs, ce - cs)
                    0x53ACL -> spos = readUInt(b, cs, ce - cs)
                }
            }
            if (sid != null && spos != null) out.putIfAbsent(sid!!, spos!!)
        }

        private fun parseTracks(b: ByteArray): List<Track> {
            val out = ArrayList<Track>()
            forEachChild(b, 0, b.size) { id, s, e ->
                if (id != 0xAEL) return@forEachChild
                var num = -1; var type = -1L; var codec = ""; var lang: String? = "eng"; var ietf: String? = null; var name: String? = null
                var forced = false; var hi = false; var comp: Int? = null; var strip: ByteArray? = null; var encodings = false
                forEachChild(b, s, e) { cid, cs, ce ->
                    when (cid) {
                        0xD7L -> num = (readUInt(b, cs, ce - cs) ?: -1).toInt()
                        0x83L -> type = readUInt(b, cs, ce - cs) ?: -1
                        0x86L -> codec = String(b, cs, ce - cs, Charsets.US_ASCII).trimEnd('\u0000')
                        0x22B59CL -> lang = String(b, cs, ce - cs, Charsets.UTF_8).trimEnd('\u0000')
                        0x22B59DL -> ietf = String(b, cs, ce - cs, Charsets.UTF_8).trimEnd('\u0000')
                        0x536EL -> name = String(b, cs, ce - cs, Charsets.UTF_8).trimEnd('\u0000')
                        0x55AAL -> forced = (readUInt(b, cs, ce - cs) ?: 0) != 0L
                        0x55ABL -> hi = (readUInt(b, cs, ce - cs) ?: 0) != 0L
                        0x6D80L -> { // ContentEncodings → ContentEncoding → ContentCompression
                            encodings = true
                            forEachChild(b, cs, ce) { eid, es, ee ->
                                if (eid == 0x6240L) forEachChild(b, es, ee) { xid, xs, xe ->
                                    if (xid == 0x5034L) { comp = 0; forEachChild(b, xs, xe) { zid, zs, ze ->
                                        when (zid) { 0x4254L -> comp = (readUInt(b, zs, ze - zs) ?: 0).toInt(); 0x4255L -> strip = b.copyOfRange(zs, ze) }
                                    } }
                                }
                            }
                        }
                    }
                }
                if (type != 17L || num < 0 || !codec.startsWith("S_TEXT/", ignoreCase = true)) return@forEachChild
                if (encodings && comp == null) { Log.i(TAG, "ścieżka #$num: szyfrowanie/nieznane kodowanie — pomijam"); return@forEachChild }
                if (comp != null && comp != 0 && comp != 3) { Log.i(TAG, "ścieżka #$num: kompresja $comp nieobsługiwana — pomijam"); return@forEachChild }
                out += Track(num, codec, ietf ?: lang, name, forced, hi, comp, strip, emptyList())
            }
            return out
        }

        private fun parseCues(b: ByteArray, wanted: Set<Int>, scaleNs: Long): Map<Int, List<Entry>> {
            val out = HashMap<Int, ArrayList<Entry>>()
            fun ms(t: Long) = t * scaleNs / 1_000_000
            forEachChild(b, 0, b.size) { id, s, e ->
                if (id != 0xBBL) return@forEachChild
                var time: Long? = null
                val pos = ArrayList<LongArray>() // track, cluster, relative, duration(-1)
                forEachChild(b, s, e) { cid, cs, ce ->
                    when (cid) {
                        0xB3L -> time = readUInt(b, cs, ce - cs)
                        0xB7L -> {
                            var tr = -1L; var cl = -1L; var rel = -1L; var du = -1L
                            forEachChild(b, cs, ce) { pid, ps, pe ->
                                when (pid) {
                                    0xF7L -> tr = readUInt(b, ps, pe - ps) ?: -1
                                    0xF1L -> cl = readUInt(b, ps, pe - ps) ?: -1
                                    0xF0L -> rel = readUInt(b, ps, pe - ps) ?: -1
                                    0xB2L -> du = readUInt(b, ps, pe - ps) ?: -1
                                }
                            }
                            pos += longArrayOf(tr, cl, rel, du)
                        }
                    }
                }
                val t = time ?: return@forEachChild
                pos.forEach { (tr, cl, rel, du) ->
                    if (tr.toInt() in wanted && cl >= 0 && rel >= 0)
                        out.getOrPut(tr.toInt()) { ArrayList() } += Entry(ms(t), cl, rel, du.takeIf { it > 0 }?.let(::ms))
                }
            }
            return out
        }

        fun cleanText(codec: String, raw: ByteArray): String {
            var s = String(raw, Charsets.UTF_8).trimEnd('\u0000')
            if (codec.contains("ASS", true) || codec.contains("SSA", true)) {
                // Blok ASS w MKV: ReadOrder,Layer,Style,Name,MarginL,MarginR,MarginV,Effect,Text
                s = s.split(',', limit = 9).getOrElse(8) { s }
                s = s.replace(Regex("\\{[^}]*\\}"), "").replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ")
            } else {
                s = s.replace(Regex("</?[a-zA-Z][^>]*>"), "")
            }
            return s.trim()
        }

        internal fun fetchRange(url: String, headers: Map<String, String>, start: Long, length: Int): ByteArray? {
            val req = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }
                .header("Range", "bytes=$start-${start + length - 1}").build()
            return runCatching {
                client.newCall(req).execute().use { r ->
                    if (r.code != 206) { Log.w(TAG, "zakres $start+$length: HTTP ${r.code} (serwer bez zakresów?)"); return@use null }
                    val body = r.body ?: return@use null
                    val src = body.byteStream(); val buf = ByteArray(length); var n = 0
                    while (n < length) { val k = src.read(buf, n, length - n); if (k < 0) break; n += k }
                    if (n == length) buf else buf.copyOf(n)
                }
            }.onFailure { Log.w(TAG, "zakres $start+$length: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }.getOrNull()
        }

        // ---- EBML ----
        internal fun readId(b: ByteArray, o: Int): Pair<Long, Int>? {
            if (o >= b.size) return null
            val first = b[o].toInt() and 0xFF
            val len = when { first and 0x80 != 0 -> 1; first and 0x40 != 0 -> 2; first and 0x20 != 0 -> 3; first and 0x10 != 0 -> 4; else -> return null }
            if (o + len > b.size) return null
            var v = 0L; for (i in 0 until len) v = (v shl 8) or (b[o + i].toLong() and 0xFF)
            return v to len
        }

        /** Rozmiar (vint bez znacznika); -1 = nieznany (same jedynki). */
        internal fun readSize(b: ByteArray, o: Int): Pair<Long, Int>? {
            if (o >= b.size) return null
            val first = b[o].toInt() and 0xFF
            if (first == 0) return null
            var len = 1; var mask = 0x80
            while (first and mask == 0) { len++; mask = mask shr 1 }
            if (o + len > b.size) return null
            var v = (first and (mask - 1)).toLong(); var allOnes = v == (mask - 1).toLong()
            for (i in 1 until len) { val x = b[o + i].toLong() and 0xFF; if (x != 0xFFL) allOnes = false; v = (v shl 8) or x }
            return (if (allOnes) -1L else v) to len
        }

        internal fun readUInt(b: ByteArray, s: Int, len: Int): Long? {
            if (len !in 0..8 || s + len > b.size) return null
            var v = 0L; for (i in 0 until len) v = (v shl 8) or (b[s + i].toLong() and 0xFF)
            return v
        }

        internal inline fun forEachChild(b: ByteArray, start: Int, end: Int, f: (id: Long, s: Int, e: Int) -> Unit) {
            var p = start
            while (p < end) {
                val (id, il) = readId(b, p) ?: return
                val (size, sl) = readSize(b, p + il) ?: return
                if (size < 0) return
                val s = p + il + sl; val e = s + size
                if (e > end) return
                f(id, s, e.toInt())
                p = e.toInt()
            }
        }
    }
}
