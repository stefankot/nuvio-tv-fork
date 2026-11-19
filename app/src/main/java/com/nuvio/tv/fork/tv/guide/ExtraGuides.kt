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

    fun urlsFor(c: Context, channels: List<LiveTvChannel>): List<String> {
        if (!ForkFeatures.enabled(c, KEY, true)) return emptyList()
        return runCatching {
            EpgSources.detect(channels.map { it.tvgId to it.group }, sources(c))?.epg?.let(::listOf).orEmpty()
        }.logged("ForkTv", "ExtraGuides.kt:22").getOrDefault(emptyList())
    }
}
