package com.nuvio.tv.fork.tv.guide

import android.content.Context
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import java.io.File
import com.nuvio.tv.fork.logged

/** Wywoływane z repozytorium Live TV (łatka dostawca #9): adres przewodnika uzupełniającego dla kraju listy. */
object ExtraGuides {
    const val KEY = "tv_extra_guide"

    @Volatile private var cached: List<EpgSource>? = null

    private fun sources(c: Context): List<EpgSource> = cached ?: run {
        val over = File(c.filesDir, "fork/epg_sources.json").takeIf { it.isFile }?.readText()
        val text = over ?: c.assets.open("fork/epg_sources.json").bufferedReader().use { it.readText() }
        EpgSources.parse(text).also { cached = it }
    }

    @Volatile private var lastLog: String? = null

    /** Kraj rozpoznany dla każdej listy (sourceId → kod, np. „pl”); pusty, dopóki listy się nie wczytały. */
    @Volatile var countryBySource: Map<String, String> = emptyMap()
        private set

    fun urlsFor(c: Context, channels: List<LiveTvChannel>): List<String> {
        if (!ForkFeatures.enabled(c, KEY, true)) return emptyList()
        val t0 = android.os.SystemClock.elapsedRealtime()
        return runCatching {
            // Każda lista osobno (kraj listy), nie wszystkie razem — patrz EpgSources.detectPerList.
            val lists = LinkedHashMap<String, MutableList<Pair<String?, String>>>()
            channels.forEach { ch -> lists.getOrPut(ch.sourceId) { ArrayList() }.add(ch.tvgId to ch.group) }
            val all = sources(c)
            // Kraj każdej listy (np. hls.gd → pl) — też dla ustawienia „kraj IPTV” (fork/tv/country/TvCountry).
            val perList = lists.mapNotNull { (src, list) -> EpgSources.detect(list, all)?.let { src to it } }
            countryBySource = perList.associate { (src, s) -> src to s.country.lowercase() }
            val found = perList.map { it.second }.distinct()
            // Jeden wpis na zmianę wyniku (repozytorium woła to po każdej wczytanej liście).
            val line = "przewodnik uzupełniający: ${found.joinToString { it.country }.ifEmpty { "brak" }} dla ${channels.size} kanałów z ${lists.size} list"
            if (line != lastLog) {
                lastLog = line
                android.util.Log.i("ForkTv", "$line (${android.os.SystemClock.elapsedRealtime() - t0} ms)")
            }
            found.map { it.epg }
        }.logged("ForkTv", "ExtraGuides.kt:22").getOrDefault(emptyList())
    }
}
