package com.nuvio.tv.fork.sources

import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

/** Most do odtwarzacza (B04-8, B07): ostatni stan i wysyłanie zdarzeń; działa tylko, gdy ekran odtwarzacza jest otwarty. */
object PlayerBridge {
    @Volatile private var state: PlayerUiState? = null
    @Volatile private var send: ((PlayerEvent) -> Unit)? = null
    private var queue: SubtitleQueue? = null
    private var queueKey: List<String> = emptyList()

    fun set(s: PlayerUiState, e: (PlayerEvent) -> Unit) { state = s; send = e }
    fun clear() { state = null; send = null; queue = null }
    val active: Boolean get() = send != null

    /** „Napisy nie pasują”: następny plik napisów w tym samym języku i ponowna synchronizacja. Zwraca komunikat dla użytkownika albo null (brak odtwarzacza). */
    fun subtitleMismatch(): String? {
        val s = state ?: return null
        val emit = send ?: return null
        val current = s.selectedAddonSubtitle
        val lang = current?.lang ?: s.addonSubtitles.firstOrNull { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }?.lang ?: s.addonSubtitles.firstOrNull()?.lang
        val same = s.addonSubtitles.filter { it.lang == lang }
        if (same.isEmpty()) return "Brak innych napisów"
        val ids = same.map { it.id }
        if (queue == null || queueKey != ids) { queue = SubtitleQueue(ids); queueKey = ids }
        val q = queue!!
        val nextId = q.next() ?: return "Brak innych napisów"
        val next = same.firstOrNull { it.id == nextId } ?: return "Brak innych napisów"
        emit(PlayerEvent.OnSelectAddonSubtitle(next))
        emit(PlayerEvent.OnReloadSubtitleAutoSyncCues)
        return when (q.suggestion(lektorReady = false)) {
            SubtitleQueue.Suggestion.SUBTITLES_OFF -> "Trzecia próba: może wyłączyć napisy?"
            else -> "Inne napisy: ${next.addonName}"
        }
    }
}
