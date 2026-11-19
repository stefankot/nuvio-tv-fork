package com.nuvio.tv.fork.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nuvio.tv.fork.logged

/** Kopia Kicii 34c80c6 `forge/tv/ForgeTvMetaViewModel.kt` (nazwy forka): dane TMDB programu na karcie programu skórki Kicia. */
@HiltViewModel
class TvStoreMetaViewModel @Inject constructor(
    private val tmdb: TvTmdb,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context, // W8: TVDB
) : ViewModel() {
    private val _meta = MutableStateFlow<TvMeta?>(null)
    val meta: StateFlow<TvMeta?> = _meta.asStateFlow()
    /** Właściciel 19.11: dane TVDB (serial, a gdy przewodnik podał numer — odcinek) mają pierwszeństwo przed EPG (epg.ovh, tv.team). */
    private val _tvdb = MutableStateFlow<TvMeta?>(null)
    val tvdb: StateFlow<TvMeta?> = _tvdb.asStateFlow()
    private var asked: String? = null

    fun load(title: String, year: Int? = null, episode: String? = null) {
        val key = title + "|" + episode
        if (asked == key) return
        asked = key
        _meta.value = null
        _tvdb.value = null
        viewModelScope.launch {
            // W8: TVDB zawsze (pierwszeństwo przed EPG); zdjęcie i opis ODCINKA z TVDB (numer z przewodnika) w miejsce tła i opisu serialu.
            val series = runCatching { com.nuvio.tv.fork.tvdb.Tvdb.lookup(context, title, year) }.logged("ForkTv", "TvStoreMetaViewModel.kt:33").getOrNull()
            if (asked == key) _tvdb.value = series
            val base = tmdb.lookup(title, year) ?: series
            if (asked == key) _meta.value = base
            val ep = runCatching { com.nuvio.tv.fork.tvdb.Tvdb.episode(context, title, episode, year) }.logged("ForkTv", "TvStoreMetaViewModel.kt:37").getOrNull() ?: return@launch
            if (asked != key) return@launch
            if (series != null) _tvdb.value = series.copy(backdropUrl = ep.image ?: series.backdropUrl, overview = ep.overview ?: series.overview)
            if (base != null) _meta.value = base.copy(backdropUrl = ep.image ?: base.backdropUrl, overview = ep.overview ?: base.overview)
        }
    }
}
