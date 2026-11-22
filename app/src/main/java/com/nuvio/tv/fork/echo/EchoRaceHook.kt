package com.nuvio.tv.fork.echo

import android.content.Context
import com.nuvio.tv.R
import com.nuvio.tv.core.player.AutoPlayRace
import com.nuvio.tv.core.player.BetaFeatures
import com.nuvio.tv.core.player.StreamAutoPlaySelector
import com.nuvio.tv.core.player.raceCandidates
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.sources.TrackScout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Echo A4: wyścig źródeł przy autoodtwarzaniu (kod Echo 1:1 w AutoPlayRace.kt). Przełącznik: echo_race. */
object EchoRaceHook {
    private fun on(c: Context) = BetaFeatures.settings(c).raceAutoPlaySources

    /** Wybrane preferencje auto-play (rozdzielczość, kodek) jako pigułki Echo. */
    fun prefs(c: Context): Set<String> = setOfNotNull(
        com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, "echo_ap_quality").ifBlank { null },
        com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, "echo_ap_codec").ifBlank { null },
    )

    internal fun useQualityOrder(c: Context, settings: PlayerSettings): Boolean =
        settings.streamAutoPlayMode == com.nuvio.tv.data.local.StreamAutoPlayMode.FIRST_STREAM &&
            !com.nuvio.tv.core.player.AutoPlayPreferences.isConfigured(prefs(c))

    internal suspend fun pickQuality(pick: Stream, field: List<Stream>?, head: AutoPlayRace.HeadStart?): Stream {
        if (TrackScout.isVerifiedPlayback(pick) || field == null) {
            stop(head)
            android.util.Log.i("ForkEcho", "quality autoplay verifiedFile=${TrackScout.isVerifiedPlayback(pick)}")
            return pick
        }
        return AutoPlayRace.pickInRankOrder(field, head)
    }

    /** Echo stosuje preferencje tylko w trybie regex; tu działają też przy „pierwszym źródle”. */
    private fun modeFor(c: Context, mode: com.nuvio.tv.data.local.StreamAutoPlayMode) =
        if (mode == com.nuvio.tv.data.local.StreamAutoPlayMode.FIRST_STREAM &&
            com.nuvio.tv.core.player.AutoPlayPreferences.isConfigured(prefs(c))
        ) com.nuvio.tv.data.local.StreamAutoPlayMode.REGEX_MATCH else mode

    /** Auto-play pierwszego źródła z uwzględnieniem wybranej rozdzielczości/kodeka; null = bez preferencji. */
    fun preferred(c: Context, pick: Stream?, streams: List<Stream>, s: PlayerSettings, installed: Set<String>): Stream? {
        if (pick == null || !com.nuvio.tv.core.player.AutoPlayPreferences.isConfigured(prefs(c))) return null
        return StreamAutoPlaySelector.raceCandidates(
            streams = streams, mode = modeFor(c, s.streamAutoPlayMode), regexPattern = s.streamAutoPlayRegex,
            source = s.streamAutoPlaySource, installedAddonNames = installed,
            selectedAddons = s.streamAutoPlaySelectedAddons, selectedPlugins = s.streamAutoPlaySelectedPlugins,
            preferences = prefs(c)
        ).firstOrNull()
    }

    /** Przed ładowaniem źródeł: zaczyna wyścig od razu, gdy pojawią się pierwsze pasujące linki. */
    fun headStart(c: Context, scope: CoroutineScope, skip: Boolean): AutoPlayRace.HeadStart? =
        if (on(c) && !skip) AutoPlayRace.HeadStart(scope) else null

    fun offer(c: Context, h: AutoPlayRace.HeadStart?, streams: List<Stream>, s: PlayerSettings, installed: Set<String>) {
        if (h?.acceptsMore != true) return
        h.offer(
            StreamAutoPlaySelector.raceCandidates(
                streams = streams, mode = modeFor(c, s.streamAutoPlayMode), regexPattern = s.streamAutoPlayRegex,
                source = s.streamAutoPlaySource, installedAddonNames = installed,
                selectedAddons = s.streamAutoPlaySelectedAddons, selectedPlugins = s.streamAutoPlaySelectedPlugins,
                preferences = prefs(c),
                rankFirstStream = false // Early probes start as soon as the first links arrive.
            )
        )
    }

    /** Linki do wyścigu dla wyboru [pick]; null = gra sam. */
    fun field(
        c: Context, pick: Stream?, streams: List<Stream>, s: PlayerSettings,
        installed: Set<String>, binge: String?, firstSourceOrder: List<Stream>?
    ): List<Stream>? {
        if (pick == null || !on(c)) return null
        val mode = modeFor(c, s.streamAutoPlayMode)
        val field = AutoPlayRace.fieldFor(
            pick = pick, streams = streams, mode = mode, regexPattern = s.streamAutoPlayRegex,
            preferences = prefs(c), source = s.streamAutoPlaySource,
            installedAddonNames = installed, selectedAddons = s.streamAutoPlaySelectedAddons,
            selectedPlugins = s.streamAutoPlaySelectedPlugins, bingeGroup = binge,
            eligibleMatches = firstSourceOrder?.takeIf { mode == com.nuvio.tv.data.local.StreamAutoPlayMode.FIRST_STREAM }
        ) ?: return null
        // Fork 19.11 (JMGO: wyścig wygrywał najszybszy link, także gorszy — hiszpański 720p zamiast angielskiego 1080p):
        // ścigają się tylko źródła równe pierwszemu z rankingu (ta sama rozdzielczość, gotowość pliku i język).
        fun cls(x: Stream) = Triple(com.nuvio.tv.core.stream.ReleaseFactsParser.resolutionOf(x), com.nuvio.tv.fork.sources.SourceReady.tier(x), com.nuvio.tv.fork.sources.ForeignDub.tier(x))
        val want = cls(pick)
        return field.filter { cls(it) == want }.takeIf { it.size > 1 }
    }

    /** Uruchamia wyścig; zwycięzcę oddaje przez [onWinner], chyba że autoodtwarzanie wstrzymano. */
    fun run(
        c: Context, scope: CoroutineScope, field: List<Stream>, head: AutoPlayRace.HeadStart?, pick: Stream,
        showStatus: Boolean, handled: () -> Boolean, onMessage: (String) -> Unit, onWinner: (Stream) -> Unit
    ) {
        if (showStatus) onMessage(c.getString(R.string.stream_racing_sources, field.size))
        scope.launch {
            val winner = AutoPlayRace.pick(field, head)
            if (handled()) return@launch
            onWinner(winner)
        }
    }

    fun stop(h: AutoPlayRace.HeadStart?) { h?.stop() }
}
