package com.nuvio.tv.fork.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.nuvio.tv.fork.tv.most.TvBridge
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Fork 19.11 (ANR na JMGO po wpisaniu „tvn” w Szukaj): indeks TV budował się w głównym wątku przy każdym
 * pokazaniu półki (normalizacja całego przewodnika EPG = kilka sekund na 32-bitowym TV). Teraz w tle,
 * raz na 10 min albo po zmianie liczby kanałów; do czasu zbudowania półka jest pusta.
 */
object TvSearchIndexCache {
    private const val TTL_MS = 10 * 60_000L
    private val lock = Mutex()
    @Volatile private var cached: Triple<Int, Long, TvSearchIndex>? = null

    suspend fun get(): TvSearchIndex = lock.withLock {
        val channels = LiveTvRepository.uiState.value.channels
        val now = System.currentTimeMillis()
        cached?.let { (n, at, idx) -> if (n == channels.size && now - at < TTL_MS) return@withLock idx }
        val idx = withContext(Dispatchers.Default) {
            TvSearchIndex(
                channels.map { SearchChannel(it.guideKey, it.name, it.group) },
                (TvBridge.data?.episodeLog().orEmpty() + TvBridge.guideAirings()).distinctBy { it.guideKey to it.startMs }
                    .map { SearchAiring(it.programKey, it.title, it.guideKey, it.startMs, it.startMs + it.durMs) },
            )
        }
        cached = Triple(channels.size, now, idx)
        idx
    }
}

/** Indeks TV dla ekranu; null, dopóki buduje się w tle. */
@Composable
fun rememberTvSearchIndex(channelCount: Int): State<TvSearchIndex?> =
    produceState<TvSearchIndex?>(null, channelCount) { value = TvSearchIndexCache.get() }
