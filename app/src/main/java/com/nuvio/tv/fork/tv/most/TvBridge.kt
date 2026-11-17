package com.nuvio.tv.fork.tv.most

import android.content.Context
import com.nuvio.tv.fork.tv.EpgExtras
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Most do Reshaped: jedyne miejsce, przez które Live TV woła moduł TV. Wyłączony moduł (`module_tv`)
 * nic nie zapisuje. Wołane z łatek `Fork patch: dostawca` / `Fork hook: tv` w Reshaped.
 */
object TvBridge {
    @Volatile private var enabled = true

    fun attach(context: Context, profileId: Int) {
        enabled = ForkFeatures.enabled(context, "module_tv", true)
        if (enabled) {
            ForkTvHooks.sink = { title, start, extra -> EpgExtras.put(title, start, extra) }
            TvStore.attach(context, profileId)
        }
    }

    fun onGuideRead(channels: List<LiveTvChannel>, kept: Map<String, List<LiveTvProgramme>>, nowMs: Long) {
        if (enabled) TvStore.updateSchedule(channels, kept, nowMs)
    }

    fun onPlay(channel: LiveTvChannel, programme: LiveTvProgramme?) {
        if (enabled) TvStore.onWatched(channel, programme)
    }

    /** Co ~10 s z odtwarzacza: kanał, czy z archiwum, i miejsce w czasie kanału. */
    fun onPosition(listUrl: String?, catchup: Boolean, playheadEpochMs: Long) {
        if (enabled) positionSink?.invoke(listUrl, catchup, playheadEpochMs)
    }

    @Volatile var positionSink: ((listUrl: String?, catchup: Boolean, playheadEpochMs: Long) -> Unit)? = null
}
