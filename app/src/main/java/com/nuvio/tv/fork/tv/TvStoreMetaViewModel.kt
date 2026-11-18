package com.nuvio.tv.fork.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Kopia Kicii 34c80c6 `forge/tv/ForgeTvMetaViewModel.kt` (nazwy forka): dane TMDB programu na karcie programu skórki Kicia. */
@HiltViewModel
class TvStoreMetaViewModel @Inject constructor(
    private val tmdb: TvTmdb,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context, // W8: TVDB
) : ViewModel() {
    private val _meta = MutableStateFlow<TvMeta?>(null)
    val meta: StateFlow<TvMeta?> = _meta.asStateFlow()
    private var asked: String? = null

    fun load(title: String, year: Int? = null, episode: String? = null) {
        val key = title + "|" + episode
        if (asked == key) return
        asked = key
        _meta.value = null
        viewModelScope.launch {
            // W8: TVDB, gdy TMDB nie zna programu; zdjęcie i opis ODCINKA z TVDB (numer z przewodnika) w miejsce tła i opisu.
            val base = tmdb.lookup(title, year) ?: com.nuvio.tv.fork.tvdb.Tvdb.lookup(context, title, year)
            _meta.value = base
            val ep = runCatching { com.nuvio.tv.fork.tvdb.Tvdb.episode(context, title, episode, year) }.getOrNull() ?: return@launch
            if (asked == key && base != null) _meta.value = base.copy(backdropUrl = ep.image ?: base.backdropUrl, overview = ep.overview ?: base.overview)
        }
    }
}
