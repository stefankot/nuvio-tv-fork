package com.nuvio.tv.fork.sources

import com.nuvio.tv.core.player.recovery.recoverySourceKey
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

/**
 * „Spróbuj innego źródła” (Kicia `PlayerTryAnotherSource`, uproszczone, punkt 4 kolejki): następne źródło tytułu według
 * rankingu (KeRanking, ze strażnikiem tytułu i pamięcią złych źródeł), bez tego, co już pominięto w tym tytule.
 * Pominięte źródło jest odkładane na 30 min (Kicia BadReason.USER_SKIP). Lista źródeł = lista panelu źródeł odtwarzacza;
 * gdy pusta, najpierw jest ładowana. Wejście: polecenie głosowe „inne źródło” (fork/voice).
 */
object TryAnotherSource {
    private val skipped = mutableSetOf<String>()
    private var listKeys: Set<String> = emptySet()
    @Volatile private var pending = false

    private fun keys(s: Stream) = buildSet { add(recoverySourceKey(s)); s.getStreamUrl()?.let { add("url:$it") } }

    /** Wywołanie użytkownika. Zwraca komunikat. */
    fun run(): String {
        val s = PlayerBridge.current() ?: return "Działa tylko podczas odtwarzania"
        if (s.sourceAllStreams.isEmpty()) {
            pending = true
            PlayerBridge.send(PlayerEvent.OnReloadSourceStreams)
            return "Szukam innych źródeł…"
        }
        return pick(s)
    }

    /** PlayerBridge.set: dokończ wybór, gdy lista źródeł doszła. */
    internal fun onState(s: PlayerUiState) {
        if (!pending || s.isLoadingSourceStreams) return
        pending = false
        val msg = if (s.sourceAllStreams.isNotEmpty()) pick(s) else "Nie ma innych źródeł"
        PlayerBridge.appContext?.let { android.widget.Toast.makeText(it, msg, android.widget.Toast.LENGTH_LONG).show() }
    }

    internal fun reset() { pending = false }

    private fun pick(s: PlayerUiState): String {
        val list = s.sourceAllStreams
        val all = list.flatMapTo(mutableSetOf()) { keys(it) }
        if (listKeys.isNotEmpty() && listKeys.none { it in all }) skipped.clear() // inny tytuł
        listKeys = all
        val url = s.currentStreamUrl
        url?.let { skipped += "url:$it" }
        list.firstOrNull { it.getStreamUrl() == url }?.let { cur -> skipped += keys(cur); StartupChecks.markSkipped(cur) }
        val next = KeRanking.order(list).firstOrNull { c -> keys(c).none { it in skipped } }
            ?: return "Nie ma więcej źródeł. Wybierz z listy źródeł."
        skipped += keys(next)
        PlayerBridge.send(PlayerEvent.OnSourceStreamSelected(next))
        return "Inne źródło: ${next.addonName ?: next.name ?: ""}".trim()
    }
}
