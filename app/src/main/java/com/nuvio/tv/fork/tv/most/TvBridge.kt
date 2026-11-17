package com.nuvio.tv.fork.tv.most

import android.content.Context
import com.nuvio.tv.fork.tv.EpgExtras
import com.nuvio.tv.fork.tv.GuideProgramme
import com.nuvio.tv.fork.tv.TvData
import com.nuvio.tv.fork.tv.WatchRules
import java.io.File
import java.util.concurrent.Executors
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
    @Volatile var data: TvData? = null
        private set
    @Volatile private var lastKept: Map<String, List<LiveTvProgramme>> = emptyMap()
    private val io = Executors.newSingleThreadExecutor { Thread(it, "fork-tv-io").apply { isDaemon = true } }

    private fun LiveTvProgramme.toGuide() = GuideProgramme(title, startEpochMs, stopEpochMs, description, episode, image)

    fun attach(context: Context, profileId: Int) {
        enabled = ForkFeatures.enabled(context, "module_tv", true)
        if (!enabled) { data = null; return }
        ForkTvHooks.sink = { title, start, extra -> EpgExtras.put(title, start, extra) }
        TvStore.attach(context, profileId)
        val app = context.applicationContext
        data = TvData(
            File(app.filesDir, "fork_tv"), profileId, enabled = true,
            rules = { WatchRules(watchedFraction = (if (ForkFeatures.enabled(app, "tv_watched_strict", false)) 80 else 50) / 100.0) },
            autoFollow = { ForkFeatures.enabled(app, "tv_auto_follow", true) },
            save = { job -> io.execute(job) },
        )
    }

    fun onGuideRead(channels: List<LiveTvChannel>, kept: Map<String, List<LiveTvProgramme>>, nowMs: Long) {
        if (!enabled) return
        TvStore.updateSchedule(channels, kept, nowMs)
        lastKept = kept
        val days = channels.mapNotNull { c -> c.catchup?.days?.takeIf { it > 0 }?.let { c.guideKey to it } }.toMap()
        if (days.isEmpty()) return
        data?.onGuide(days.keys.associateWith { k -> kept[k].orEmpty().map { it.toGuide() } }, days, nowMs)
    }

    fun onPlay(channel: LiveTvChannel, programme: LiveTvProgramme?) {
        if (!enabled) return
        TvStore.onWatched(channel, programme)
        data?.onPlay(channel.guideKey, programme?.toGuide(), System.currentTimeMillis())
    }

    /** Co ~10 s z odtwarzacza: kanał (adres listy), czy z archiwum, i miejsce w czasie kanału. */
    fun onPosition(listUrl: String?, catchup: Boolean, playheadEpochMs: Long) {
        if (!enabled || listUrl == null) return
        val guideKey = com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.value.channels.firstOrNull { it.streamUrl == listUrl }?.guideKey ?: return
        data?.onPosition(guideKey, playheadEpochMs, System.currentTimeMillis()) { key, ms ->
            lastKept[key]?.firstOrNull { it.startEpochMs <= ms && ms < it.stopEpochMs }?.toGuide()
        }
    }
}
