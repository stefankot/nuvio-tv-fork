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
    private val _skin = MutableStateFlow(SkinId.NUVIO)
    val skin: StateFlow<SkinId> = _skin.asStateFlow()
    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            _skin.value = SkinId.fromKey(prefs(context).getString(KEY, null))
            loaded = true
        }
    }

    fun set(context: Context, id: SkinId) {
        ensureLoaded(context)
        val previous = _skin.value
        _skin.value = id
        prefs(context).edit().putString(KEY, id.key).apply()
        // NETFLIX = istniejący przełącznik Reshaped.
        if (id == SkinId.NETFLIX) com.nuvio.tv.ui.reshaped.netflix.NetflixUiPreferences.setEnabled(context, true)
        else if (previous == SkinId.NETFLIX) com.nuvio.tv.ui.reshaped.netflix.NetflixUiPreferences.setEnabled(context, false)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

@Composable
fun rememberSkinId(): SkinId {
    SkinPreferences.ensureLoaded(LocalContext.current)
    val id by SkinPreferences.skin.collectAsState()
    return id
}
