package com.nuvio.tv.fork.echo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.NextToWatch
import com.nuvio.tv.domain.repository.StreamRepository
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.nuvio.tv.fork.logged

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface EchoPreloadEntryPoint {
    fun streamRepository(): StreamRepository
}

/**
 * Preloading źródeł na ekranie szczegółów: gdy tytuł się otwiera, w tle startuje to samo wyszukiwanie,
 * które potem uruchomi ekran źródeł (StreamRepository trzyma sesję wyszukiwania pod tym samym kluczem).
 * Przełącznik: fork_detail_preload (domyślnie włączony).
 */
object EchoSourcePreload {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile var lastKey: String? = null

    @Composable
    fun Run(meta: Meta?, next: NextToWatch?) {
        val c = LocalContext.current
        val firstEpisode = meta?.videos?.firstOrNull { (it.season ?: 0) > 0 && it.episode != null }
        val season = next?.nextSeason ?: firstEpisode?.season
        val episode = next?.nextEpisode ?: firstEpisode?.episode
        val videoId = next?.nextVideoId ?: firstEpisode?.id
        LaunchedEffect(meta?.id, videoId) { start(c, meta, videoId, season, episode) }
        androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.nuvio.tv.fork.sources.ReadyState.clear(); lastKey = null } }
    }

    private fun start(c: Context, meta: Meta?, nextVideoId: String?, season: Int?, episode: Int?) {
        if (meta == null || !ForkFeatures.enabled(c, "fork_detail_preload", default = true)) return
        val type = meta.rawType.ifBlank { meta.type.toApiString() }
        val isSeries = type.equals("series", true)
        if (!isSeries && !type.equals("movie", true)) return
        val videoId = if (isSeries) (nextVideoId ?: return) else meta.id
        val key = "$type|$videoId|$season|$episode"
        if (key == lastKey) return
        lastKey = key
        val repo = EntryPointAccessors.fromApplication(c.applicationContext, EchoPreloadEntryPoint::class.java).streamRepository()
        job?.cancel()
        job = scope.launch {
            android.util.Log.i("EchoPreload", "start $key")
            val t0 = System.currentTimeMillis()
            runCatching {
                repo.getStreamsFromAllAddons(
                    type = type, videoId = videoId,
                    season = season.takeIf { isSeries }, episode = episode.takeIf { isSeries }
                ).collect { result ->
                    if (result is com.nuvio.tv.core.network.NetworkResult.Success) {
                        com.nuvio.tv.fork.sources.ReadyState.update(result.data)
                        com.nuvio.tv.fork.sources.TrackScout.onSources(c, type, videoId, result.data.flatMap { it.streams })
                    }
                }
            }.logged("ForkEcho", "EchoSourcePreload.kt:61")
            android.util.Log.i("EchoPreload", "done $key in ${System.currentTimeMillis() - t0}ms")
        }
    }
}
