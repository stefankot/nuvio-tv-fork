package com.nuvio.tv.skin.api

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Wybrany wygląd. Zmiana działa od razu (stan Compose), bez restartu. */
object SkinPreferences {
    private const val PREFS = "fork_skin"
    private const val KEY = "skin"
    private val _skin = MutableStateFlow(SkinId.KICIA) // domyślnie wygląd Kicia (właściciel 18.11)
    val skin: StateFlow<SkinId> = _skin.asStateFlow()
    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val saved = prefs(context).getString(KEY, null)
            _skin.value = if (saved == null) SkinId.KICIA else SkinId.fromKey(saved)
            loaded = true
            // Pierwsze uruchomienie: zapisz Kicię jako wybór i włącz jej górny pasek (jak przy wyborze w Ustawieniach).
            if (saved == null) {
                prefs(context).edit().putString(KEY, SkinId.KICIA.key).apply()
                com.nuvio.tv.ui.reshaped.pillnav.PillNavPreferences.setEnabled(context, true)
            }
        }
    }

    fun set(context: Context, id: SkinId) {
        ensureLoaded(context)
        val previous = _skin.value
        _skin.value = id
        prefs(context).edit().putString(KEY, id.key).apply()
        // Fork 26.11: wygląd Netflix z Reshaped usunięty razem z bazą 1.2.0-beta.4 (pliki bazy 1:1)
        // KICIA = górny pasek Reshaped (pill nav) z poprawkami Kicii.
        if (id == SkinId.KICIA || id == SkinId.NIUNIA) com.nuvio.tv.ui.reshaped.pillnav.PillNavPreferences.setEnabled(context, true) // NIUNIA: jej menu rysuje się w miejscu paska
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

@Composable
fun rememberSkinId(): SkinId {
    SkinPreferences.ensureLoaded(LocalContext.current)
    val id by SkinPreferences.skin.collectAsState()
    return id
}
