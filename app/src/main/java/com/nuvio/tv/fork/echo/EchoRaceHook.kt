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

    /** Wybrane preferencje auto-play (rozdzielczość, kodek) jako pigułki Echo. */
    fun prefs(c: Context): Set<String> = setOfNotNull(
        com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, "echo_ap_quality").ifBlank { null },
        com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, "echo_ap_codec").ifBlank { null },
    )

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
        h ?: return
        h.offer(
            StreamAutoPlaySelector.raceCandidates(
                streams = streams, mode = modeFor(c, s.streamAutoPlayMode), regexPattern = s.streamAutoPlayRegex,
                source = s.streamAutoPlaySource, installedAddonNames = installed,
                selectedAddons = s.streamAutoPlaySelectedAddons, selectedPlugins = s.streamAutoPlaySelectedPlugins,
                preferences = prefs(c)
            )
        )
    }

    /** Linki do wyścigu dla wyboru [pick]; null = gra sam. */
    fun field(c: Context, pick: Stream?, streams: List<Stream>, s: PlayerSettings, installed: Set<String>, binge: String?): List<Stream>? {
        if (pick == null || !on(c)) return null
        return AutoPlayRace.fieldFor(
            pick = pick, streams = streams, mode = modeFor(c, s.streamAutoPlayMode), regexPattern = s.streamAutoPlayRegex,
            preferences = prefs(c), source = s.streamAutoPlaySource,
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
