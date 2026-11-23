package com.nuvio.tv.fork.tv.vod

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvChannel

/**
 * Filmy i seriale z list IPTV nie są kanałami (JMGO 23.11: aplikacja zawieszała się po kilkunastu sekundach od startu).
 *
 * Zrzut pamięci z JMGO (23.11 16:30, `.stage/jmgo/anr-20261123/`): sterta 496 z 512 MB, 3,4 mln tekstów; lista M3U
 * FlixNest (194 726 pozycji) to prawie same pliki filmów i seriali (adresy pośrednika `…%3Amkv.m3u8`, `…%3Amp4.m3u8`,
 * grupy „FILME”, „Series”, „NETFLIX MOVIES”; w próbce 2008 pozycji tylko 1 kanał `.ts`). Każda taka pozycja szła przez
 * przewodnik, indeksy szukania, kraj i kategorie jak kanał — razem 254 049 „kanałów”, choć prawdziwych było ok. 59 tys.
 *
 * Tu: pozycje, których adres jest plikiem wideo (rozszerzenie filmu, także zakodowane przed `.m3u8` przez pośrednika)
 * albo ścieżką VOD panelu Xtream (`/movie/`, `/series/`), nie trafiają na listę kanałów. Przełącznik [KEY] w „Telewizja”
 * (domyślnie włączony); wyłączony — lista bez zmian, bez liczenia. Właściciel 23.11: VOD docelowo osobno, jako źródła
 * plików z indeksem do szukania (jak Torbox) — `plan/TERAZ.md`.
 */
object LiveVod {
    const val KEY = "tv_skip_vod"
    private const val TAG = "ForkTvVod"

    /** Rozszerzenia plików filmów; `.ts` i `.m3u8` to kanały na żywo. */
    private val FILE_EXT = setOf("mkv", "mp4", "avi", "m4v", "mov", "wmv", "flv", "webm", "mpg", "mpeg", "divx", "xvid", "3gp", "vob", "ogv", "rmvb")

    fun enabled(c: Context): Boolean = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)

    /** Lista bez filmów i seriali (ta sama lista, gdy nic do odsiania albo przełącznik wyłączony). */
    fun live(c: Context, channels: List<LiveTvChannel>): List<LiveTvChannel> {
        if (channels.isEmpty() || !enabled(c)) return channels
        return try {
            val t0 = System.nanoTime()
            var vod = 0
            for (ch in channels) if (isVodUrl(ch.streamUrl)) vod++
            if (vod == 0) return channels
            val out = ArrayList<LiveTvChannel>(channels.size - vod)
            for (ch in channels) if (!isVodUrl(ch.streamUrl)) out += ch
            Log.i(TAG, "lista: pominięte filmy i seriale $vod z ${channels.size} pozycji, zostaje ${out.size} kanałów (${(System.nanoTime() - t0) / 1_000_000} ms)")
            out
        } catch (e: Exception) {
            Log.w(TAG, "lista: ${e.javaClass.simpleName}: ${e.message} — lista bez zmian")
            channels
        }
    }

    /**
     * 23.11 (JMGO: „brak programu” przez ~26 s po wejściu): lista zapisana na dysku przed odsiewem miała 194 tys. filmów —
     * każde wejście czytało ją całą (~11 s przy pełnej pamięci) i dopiero potem ruszał przewodnik. Zapisana lista z filmami
     * jest odsiewana i przepisywana bez nich (raz, w tle); kolejne wejścia czytają tylko kanały.
     */
    internal fun saved(c: Context, file: java.io.File, source: com.nuvio.tv.reshaped.livetv.LiveTvSource, entry: com.nuvio.tv.reshaped.livetv.LiveTvListCache.Entry): com.nuvio.tv.reshaped.livetv.LiveTvListCache.Entry {
        val live = live(c, entry.channels)
        if (live === entry.channels) return entry
        val clean = com.nuvio.tv.reshaped.livetv.LiveTvListCache.Entry(live, entry.epgUrls, entry.groupOrder, entry.savedAtMs)
        Thread({
            try {
                com.nuvio.tv.reshaped.livetv.LiveTvListCache.write(file, source, clean)
                Log.i(TAG, "lista: zapis bez filmów i seriali (${live.size} kanałów) — następne wejście szybsze")
            } catch (e: Exception) {
                Log.w(TAG, "lista: zapis bez filmów: ${e.javaClass.simpleName}: ${e.message}")
            }
        }, "fork-tv-vod-save").apply { isDaemon = true }.start()
        return clean
    }

    /** Adres pliku filmu/serialu, nie kanału na żywo. Bez wyrażeń regularnych (setki tysięcy pozycji). */
    internal fun isVodUrl(url: String): Boolean {
        var p = url.substringBefore('?').substringBefore('#')
        val scheme = p.indexOf("://")
        if (scheme >= 0) {
            val slash = p.indexOf('/', scheme + 3)
            p = if (slash < 0) "" else p.substring(slash)
        }
        if (p.isEmpty()) return false
        val lower = p.lowercase()
        // Panel Xtream: /movie/<login>/<hasło>/<id>.<ext>, /series/…
        if (lower.startsWith("/movie/") || lower.startsWith("/series/") || "/movie/" in lower || "/series/" in lower) return true
        var last = lower.substring(lower.lastIndexOf('/') + 1)
        if ('%' in last) last = last.replace("%3a", ":").replace("%2e", ".")
        // Pośrednik (FlixNest): „Tytuł (2020)%3Amkv.m3u8” — rozszerzenie pliku przed .m3u8.
        if (last.endsWith(".m3u8")) last = last.dropLast(5)
        val cut = maxOf(last.lastIndexOf('.'), last.lastIndexOf(':'))
        if (cut < 0 || cut == last.length - 1) return false
        return last.substring(cut + 1) in FILE_EXT
    }
}
