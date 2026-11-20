package com.nuvio.tv.skin.niunia.player

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.tmdb.TmdbMetadataService
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.data.local.TmdbSettingsDataStore
import com.nuvio.tv.data.trailer.TrailerPlaybackSource
import com.nuvio.tv.data.trailer.TrailerService
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.LibraryEntryInput
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ForkNiunia"

/** Co film/serial w odtwarzaczu Niuni ma poza [com.nuvio.tv.ui.screens.player.PlayerUiState]: „Podobne”, biblioteka, zwiastun. */
data class NiuniaExtrasState(
    val similar: List<MetaPreview> = emptyList(),
    val similarSource: String? = null,
    val inLibrary: Boolean = false,
    val libraryBusy: Boolean = false,
    val trailer: TrailerPlaybackSource? = null,
    val trailerLoading: Boolean = false,
    /** Krótki komunikat na ekran (dodano do biblioteki, brak zwiastuna). */
    val message: String? = null,
)

/**
 * Dane do pkt 3 (właściciel 20.11): „Podobne”, [+ Do biblioteki], [▶ Zwiastun] w odtwarzaczu filmu/serialu.
 * Kolejność źródeł: TVDB > TMDB > EPG > playlista. TVDB (moduł `fork/tvdb`) nie daje podobnych tytułów ani zwiastunów,
 * a EPG/playlista nie dotyczą filmów z dodatków — w praktyce dane bierzemy z TMDB (ta sama usługa co strona filmu).
 * Klucze tylko z BuildConfig (local.properties); nigdy nie trafiają do logów.
 */
@HiltViewModel
class NiuniaPlayerExtrasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tmdbService: TmdbService,
    private val tmdbMetadataService: TmdbMetadataService,
    private val tmdbSettingsDataStore: TmdbSettingsDataStore,
    private val libraryRepository: LibraryRepository,
    private val trailerService: TrailerService,
) : ViewModel() {
    private val _state = MutableStateFlow(NiuniaExtrasState())
    val state: StateFlow<NiuniaExtrasState> = _state.asStateFlow()

    private var key: String? = null
    private var tmdbId: String? = null
    private var input: LibraryEntryInput? = null
    private var libraryJob: Job? = null

    /** Raz na tytuł (powtórne wywołanie z tym samym id nic nie robi). */
    fun load(contentId: String?, contentType: String?, title: String, year: String?, poster: String?, backdrop: String?, logo: String?, description: String?) {
        val id = contentId?.takeIf { it.isNotBlank() } ?: return
        val type = apiType(contentType)
        if (key == "$type:$id") return
        key = "$type:$id"
        tmdbId = null
        _state.value = NiuniaExtrasState()
        input = LibraryEntryInput(
            itemId = id, itemType = type, title = title, year = year?.take(4)?.toIntOrNull(),
            poster = poster, background = backdrop, logo = logo, description = description, releaseInfo = year,
        )
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch {
            try {
                libraryRepository.isInLibrary(id, type).collect { v -> _state.update { it.copy(inLibrary = v) } }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                Log.w(TAG, "odtwarzacz: biblioteka (stan): ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        viewModelScope.launch {
            // 1. TVDB: brak podobnych i zwiastunów w danych modułu — log, żeby było widać, że sprawdzono.
            Log.i(TAG, "odtwarzacz: podobne — TVDB ${if (com.nuvio.tv.fork.tvdb.Tvdb.enabled(context)) "bez listy podobnych" else "wyłączone/brak klucza"}, biorę TMDB")
            // 2. TMDB.
            val found = try {
                val lang = tmdbSettingsDataStore.settings.first().language
                val tid = tmdbService.ensureTmdbId(id, type)
                tmdbId = tid
                if (tid.isNullOrBlank()) emptyList() else tmdbMetadataService.fetchMoreLikeThis(tid, if (type == "series") ContentType.SERIES else ContentType.MOVIE, lang)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                Log.w(TAG, "odtwarzacz: podobne z TMDB: ${e.javaClass.simpleName}: ${e.message}"); emptyList()
            }
            Log.i(TAG, "odtwarzacz: podobne=${found.size} (źródło=${if (found.isEmpty()) "brak" else "TMDB"}, tmdb=${tmdbId != null})")
            _state.update { it.copy(similar = found, similarSource = if (found.isEmpty()) null else "TMDB") }
            tmdbId?.toIntOrNull()?.let { t -> input = input?.copy(tmdbId = t) }
        }
    }

    fun toggleLibrary() {
        val item = input ?: return
        if (_state.value.libraryBusy) return
        val was = _state.value.inLibrary
        _state.update { it.copy(libraryBusy = true) }
        viewModelScope.launch {
            try {
                val r = libraryRepository.toggleDefault(item)
                Log.i(TAG, "odtwarzacz: biblioteka (było $was, potwierdzenie=${r.requiresRemovalConfirmation})")
                _state.update {
                    it.copy(libraryBusy = false, message = context.getString(
                        if (r.requiresRemovalConfirmation) com.nuvio.tv.R.string.niunia_pinfo_library_confirm
                        else if (was) com.nuvio.tv.R.string.detail_removed_from_library else com.nuvio.tv.R.string.detail_added_to_library))
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                Log.w(TAG, "odtwarzacz: biblioteka: ${e.javaClass.simpleName}: ${e.message}")
                _state.update { it.copy(libraryBusy = false, message = context.getString(com.nuvio.tv.R.string.detail_error_update_library_failed)) }
            }
        }
    }

    fun playTrailer(title: String, year: String?, type: String?) {
        if (_state.value.trailerLoading) return
        _state.update { it.copy(trailerLoading = true) }
        viewModelScope.launch {
            val src = try {
                val tid = tmdbId ?: key?.substringAfter(':')?.let { tmdbService.ensureTmdbId(it, apiType(type)) }.also { tmdbId = it }
                trailerService.getTrailerPlaybackSourceFromTmdbId(tid, apiType(type), title, year?.take(4))
                    ?: trailerService.getTrailerPlaybackSource(title, year?.take(4), tid, apiType(type))
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                Log.w(TAG, "odtwarzacz: zwiastun: ${e.javaClass.simpleName}: ${e.message}"); null
            }
            Log.i(TAG, "odtwarzacz: zwiastun ${if (src != null) "znaleziony (TMDB)" else "brak"}")
            _state.update {
                it.copy(trailerLoading = false, trailer = src,
                    message = if (src == null) context.getString(com.nuvio.tv.R.string.niunia_pinfo_no_trailer) else null)
            }
        }
    }

    fun closeTrailer() = _state.update { it.copy(trailer = null) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun apiType(t: String?): String = when (t?.lowercase()) { "series", "tv", "show" -> "series"; else -> "movie" }
}
