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

    fun load(title: String, year: Int? = null) {
        if (asked == title) return
        asked = title
        _meta.value = null
        viewModelScope.launch { _meta.value = tmdb.lookup(title, year) ?: com.nuvio.tv.fork.tvdb.Tvdb.lookup(context, title, year) } // W8: TVDB, gdy TMDB nie zna programu
    }
}
