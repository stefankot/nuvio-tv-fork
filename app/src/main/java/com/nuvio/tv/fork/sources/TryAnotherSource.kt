package com.nuvio.tv.fork.sources

import android.util.Log
import android.widget.Toast
import com.nuvio.tv.core.player.recovery.recoverySourceKey
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * „Spróbuj innego źródła” (Kicia `PlayerTryAnotherSource`, uproszczone, punkt 4 kolejki): następne źródło tytułu według
 * rankingu (KeRanking, ze strażnikiem tytułu i pamięcią złych źródeł), bez tego, co już pominięto w tym tytule.
 * Pominięte źródło jest odkładane na 30 min (Kicia BadReason.USER_SKIP). Lista źródeł = lista panelu źródeł odtwarzacza;
 * gdy pusta, najpierw jest ładowana. Wejście: polecenie głosowe „inne źródło” (fork/voice).
 */
object TryAnotherSource {
    private const val TAG = "ForkSourceSwitch"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val skipped = mutableSetOf<String>()
    private var title: Title? = null
    private var awaiting: Request? = null
    private var ranking: Job? = null

    private data class Title(val name: String, val season: Int?, val episode: Int?)
    private data class Request(val title: Title, val url: String?)

    private fun snapshot(s: PlayerUiState) =
        Request(Title(s.contentName ?: s.title, s.currentSeason, s.currentEpisode), s.currentStreamUrl)

    private fun keys(s: Stream) = buildSet { add(recoverySourceKey(s)); s.getStreamUrl()?.let { add("url:$it") } }

    /** Wywołanie użytkownika. Zwraca komunikat. */
    fun run(): String {
        val request = PlayerBridge.current()?.let(::snapshot) ?: return "Działa tylko podczas odtwarzania"
        scope.launch {
            val current = PlayerBridge.current() ?: return@launch
            if (snapshot(current) != request || awaiting == request || ranking?.isActive == true) return@launch
            if (current.sourceAllStreams.isEmpty()) {
                awaiting = request
                if (!current.isLoadingSourceStreams) PlayerBridge.send(PlayerEvent.OnReloadSourceStreams)
            } else {
                pick(current, request)
            }
        }
        return "Szukam innych źródeł…"
    }

    /** PlayerBridge.set: dokończ wybór, gdy lista źródeł doszła. */
    internal fun onState(s: PlayerUiState) {
        val request = awaiting ?: return
        if (snapshot(s) != request) {
            awaiting = null
        } else if (!s.isLoadingSourceStreams) {
            awaiting = null
            if (s.sourceAllStreams.isNotEmpty()) pick(s, request) else notify("Nie ma innych źródeł")
        }
    }

    internal fun reset() {
        awaiting = null
        ranking?.cancel()
        ranking = null
        skipped.clear()
        title = null
    }

    private fun pick(s: PlayerUiState, request: Request) {
        if (title != request.title) {
            skipped.clear()
            title = request.title
        }
        request.url?.let { skipped += "url:$it" }
        val previouslySkipped = skipped.toSet()
        ranking = scope.launch {
            try {
                val next = withContext(Dispatchers.Default) {
                    val list = s.sourceAllStreams
                    val previous = list.firstOrNull { it.getStreamUrl() == request.url }
                    val excluded = previouslySkipped + previous?.let(::keys).orEmpty()
                    val ready = list.filter {
                        !it.isExternal() && SourceReady.tier(it) == 0 &&
                            !Placeholders.isMarked(it.getStreamUrl()) && keys(it).none(excluded::contains)
                    }
                    TrackScout.verifiedPlaybackOrder(KeRanking.order(ready)).firstOrNull()?.also {
                        if (previous != null) StartupChecks.markSkipped(previous)
                    }
                }
                if (PlayerBridge.current()?.let(::snapshot) != request) return@launch
                if (next == null) {
                    notify("Nie ma innego gotowego źródła")
                    return@launch
                }
                skipped += keys(next)
                Log.i(TAG, "manual alternative selected candidates=${s.sourceAllStreams.size}")
                PlayerBridge.send(PlayerEvent.OnSourceStreamSelected(next))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "manual source selection failed: ${e.javaClass.simpleName}")
                if (PlayerBridge.current()?.let(::snapshot) == request) notify("Nie udało się zmienić źródła")
            }
        }
    }

    private fun notify(message: String) {
        PlayerBridge.appContext?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }
}
