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

    fun urlsFor(c: Context, channels: List<LiveTvChannel>): List<String> {
        if (!ForkFeatures.enabled(c, KEY, true)) return emptyList()
        val t0 = android.os.SystemClock.elapsedRealtime()
        return runCatching {
            val found = EpgSources.detect(channels.map { it.tvgId to it.group }, sources(c))
            // Jeden wpis na zmianę wyniku (repozytorium woła to po każdej wczytanej liście).
            val line = "przewodnik uzupełniający: ${found?.country ?: "brak"} dla ${channels.size} kanałów"
            if (line != lastLog) {
                lastLog = line
                android.util.Log.i("ForkTv", "$line (${android.os.SystemClock.elapsedRealtime() - t0} ms)")
            }
            found?.epg?.let(::listOf).orEmpty()
        }.logged("ForkTv", "ExtraGuides.kt:22").getOrDefault(emptyList())
    }
}
