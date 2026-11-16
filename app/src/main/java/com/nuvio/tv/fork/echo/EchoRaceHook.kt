package com.nuvio.tv.fork.echo

import android.content.Context
import com.nuvio.tv.R
import com.nuvio.tv.core.player.AutoPlayRace
import com.nuvio.tv.core.player.BetaFeatures
import com.nuvio.tv.core.player.StreamAutoPlaySelector
import com.nuvio.tv.core.player.raceCandidates
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Echo A4: wyścig źródeł przy autoodtwarzaniu (kod Echo 1:1 w AutoPlayRace.kt). Przełącznik: echo_race. */
object EchoRaceHook {
    private fun on(c: Context) = BetaFeatures.settings(c).raceAutoPlaySources

    /** Przed ładowaniem źródeł: zaczyna wyścig od razu, gdy pojawią się pierwsze pasujące linki. */
    fun headStart(c: Context, scope: CoroutineScope, skip: Boolean): AutoPlayRace.HeadStart? =
        if (on(c) && !skip) AutoPlayRace.HeadStart(scope) else null

    fun offer(h: AutoPlayRace.HeadStart?, streams: List<Stream>, s: PlayerSettings, installed: Set<String>) {
        h ?: return
        h.offer(
            StreamAutoPlaySelector.raceCandidates(
                streams = streams, mode = s.streamAutoPlayMode, regexPattern = s.streamAutoPlayRegex,
                source = s.streamAutoPlaySource, installedAddonNames = installed,
                selectedAddons = s.streamAutoPlaySelectedAddons, selectedPlugins = s.streamAutoPlaySelectedPlugins,
                preferences = emptySet()
            )
        )
    }

    /** Linki do wyścigu dla wyboru [pick]; null = gra sam. */
    fun field(c: Context, pick: Stream?, streams: List<Stream>, s: PlayerSettings, installed: Set<String>, binge: String?): List<Stream>? {
        if (pick == null || !on(c)) return null
        return AutoPlayRace.fieldFor(
            pick = pick, streams = streams, mode = s.streamAutoPlayMode, regexPattern = s.streamAutoPlayRegex,
            preferences = emptySet(), source = s.streamAutoPlaySource,
            installedAddonNames = installed, selectedAddons = s.streamAutoPlaySelectedAddons,
            selectedPlugins = s.streamAutoPlaySelectedPlugins, bingeGroup = binge
        )
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
