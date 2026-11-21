package com.nuvio.tv.fork.yt

import com.nuvio.tv.ui.screens.player.SubtitleSyncCue
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import com.nuvio.tv.fork.logged

/**
 * Fork 19.11 (właściciel: „w YouTube nie dzieje się nic po wybraniu AI lektora ani AI tłumaczenia napisów”).
 * Lektor i AI napisy biorą tekst tylko z napisów dodatków (plik z adresem); YouTube ma napisy w swoim manifeście
 * (lokalny serwer One: `/yt/cap/<film>/<nr>` = cały plik, albo bezpośredni adres timedtext). Tu: lista ścieżek
 * napisów z manifestu odtwarzanego filmu i pobranie całego pliku → cue. Polski: najpierw tłumaczenie YouTube (tlang=pl).
 */
object YouTubeTextSource {
    data class Track(val language: String, val url: String, val translated: Boolean)

    private val http = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    /** Id filmu YouTube z adresu lokalnego manifestu (`http://127.0.0.1:port/yt/<id>.mpd`), null dla innych źródeł. */
    fun videoId(streamUrl: String?): String? = streamUrl?.let { Regex("""127\.0\.0\.1:\d+/yt/([^./]+)\.mpd""").find(it)?.groupValues?.get(1) }

    /** Ścieżki napisów z całym plikiem (BaseURL); tłumaczone segmentami (SegmentTemplate) pomijane — polski osobno w [polishCues]. */
    fun tracks(streamUrl: String): List<Track> {
        val mpd = get(streamUrl) ?: return emptyList()
        return mpd.split("<AdaptationSet").drop(1).mapNotNull { set ->
            if (!Regex("""mimeType="text/vtt"""").containsMatchIn(set.substringBefore(">"))) return@mapNotNull null
            val lang = Regex("""lang="([^"]+)"""").find(set.substringBefore(">"))?.groupValues?.get(1) ?: return@mapNotNull null
            val url = Regex("<BaseURL>([^<]+)</BaseURL>").find(set)?.groupValues?.get(1)?.replace("&amp;", "&") ?: return@mapNotNull null
            Track(lang, url, "tlang=" in url)
        }
    }

    fun cues(url: String): List<SubtitleSyncCue> {
        val body = get(url) ?: return emptyList()
        return com.nuvio.tv.ui.screens.player.PlayerSubtitleCueParser.parseFromText(body, url)
            .filter { it.text.isNotBlank() }
    }

    /** Polskie napisy całego filmu: tłumaczenie YouTube (tlang=pl), potem polska ścieżka z manifestu. */
    fun polishCues(streamUrl: String?): List<SubtitleSyncCue> {
        val id = videoId(streamUrl) ?: return emptyList()
        com.nuvio.tv.youtube.YouTubeMpdServer.translatedCaptionFile(id, "pl")?.let { file ->
            val parsed = com.nuvio.tv.ui.screens.player.PlayerSubtitleCueParser.parseFromText(file, "yt-pl.vtt").filter { it.text.isNotBlank() }
            android.util.Log.i("ForkYtText", "pl z tłumaczenia YouTube: ${parsed.size} kwestii")
            if (parsed.isNotEmpty()) return parsed
        }
        val pl = tracks(streamUrl!!).firstOrNull { it.language.startsWith("pl", true) } ?: return emptyList<SubtitleSyncCue>().also { android.util.Log.i("ForkYtText", "brak polskich napisów YouTube") }
        return cues(pl.url).also { android.util.Log.i("ForkYtText", "pl ze ścieżki ${pl.language}: ${it.size} kwestii") }
    }

    private fun get(url: String): String? = runCatching {
        http.newCall(Request.Builder().url(url).build()).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
    }.logged("ForkYt", "YouTubeTextSource.kt:51").getOrNull()
}
