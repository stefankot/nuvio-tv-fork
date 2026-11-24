package com.nuvio.tv.skin.kicia.youtube

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.nuvio.tv.youtube.YtItem

/**
 * "Play this YouTube video" handed from Home's "YouTube dla Ciebie" shelf to the YouTube tab, which has the
 * navigation to the player: Home asks and opens the tab; the tab plays it and clears the request.
 */
object ForgeYouTubeLaunch {
    private val _pending = MutableStateFlow<YtItem?>(null)
    val pending: StateFlow<YtItem?> = _pending.asStateFlow()

    fun ask(item: YtItem) { _pending.value = item }

    /** The video to play now, once. */
    fun take(): YtItem? = _pending.value.also { _pending.value = null }

    // UX JMGO (ręczne testy 24.11): film YouTube z „Oglądaj dalej” na Głównej otwiera się przez zakładkę YouTube, więc
    // Wstecz z odtwarzacza lądował w YouTube. [returnRoute] = dokąd wrócić, [playerShown] = odtwarzacz naprawdę się pokazał.
    @Volatile var returnRoute: String? = null
    @Volatile var playerShown: Boolean = false

    // Fork 19.11 (właściciel: „search nie szuka w YouTube”): zapytanie z ekranu Szukaj, wykonane po otwarciu YouTube.
    private val _query = MutableStateFlow<String?>(null)
    val query: StateFlow<String?> = _query.asStateFlow()
    fun askSearch(q: String) { _query.value = q }
    fun takeSearch(): String? = _query.value.also { _query.value = null }
}
