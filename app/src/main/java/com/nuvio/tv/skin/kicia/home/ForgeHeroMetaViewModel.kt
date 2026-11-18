package com.nuvio.tv.skin.kicia.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.repository.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Year, genres and IMDb of a title in Continue watching, for the hero line of the mockup (S01:
 * "2022— · Thriller, Sci-Fi · IMDb 8,7"). The progress record has none of them; the repository's own
 * cache answers most at once, the primary add-on the rest (one request per title, only when focused).
 */
@HiltViewModel
class ForgeHeroMetaViewModel @Inject constructor(
    private val metas: MetaRepository,
    private val library: com.nuvio.tv.domain.repository.LibraryRepository,
    private val watched: com.nuvio.tv.data.local.WatchedItemsPreferences, // v7.4 (03): Popover karty „Oznacz jako obejrzane”
    // test 15.11 (JMGO): Home's TV rows (archive resumes, recent channels) read TvStore, which only Live TV used to load
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val profiles: com.nuvio.tv.core.profile.ProfileManager,
) : ViewModel() {
    init {
        viewModelScope.launch {
            profiles.activeProfileId.collect { id -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { com.nuvio.tv.fork.tv.TvStore.attach(appContext, id) } } }
        }
    }

    fun markWatched(item: com.nuvio.tv.domain.model.MetaPreview) {
        viewModelScope.launch {
            runCatching {
                watched.markAsWatched(com.nuvio.tv.domain.model.WatchedItem(contentId = item.id, contentType = item.apiType, title = item.name, watchedAt = System.currentTimeMillis(), poster = item.poster, releaseInfo = item.releaseInfo))
            }
        }
    }

    /** v7.4 (10): Popover karty Biblioteki: „Usuń z biblioteki” (the entry is in the default list, so the toggle removes it). */
    fun removeFromLibrary(e: com.nuvio.tv.domain.model.LibraryEntry) {
        viewModelScope.launch {
            runCatching {
                library.toggleDefault(com.nuvio.tv.domain.model.LibraryEntryInput(
                    itemId = e.id, itemType = e.type, title = e.name, imdbId = e.imdbId, tmdbId = e.tmdbId, traktId = e.traktId, simklId = e.simklId,
                    poster = e.poster, posterShape = e.posterShape, background = e.background, logo = e.logo, description = e.description, releaseInfo = e.releaseInfo,
                ))
            }
        }
    }

    /** v7.4 (10): Popover karty Biblioteki: „Oznacz jako obejrzane”. */
    fun markWatched(e: com.nuvio.tv.domain.model.LibraryEntry) {
        viewModelScope.launch {
            runCatching { watched.markAsWatched(com.nuvio.tv.domain.model.WatchedItem(contentId = e.id, contentType = e.type, title = e.name, watchedAt = System.currentTimeMillis(), poster = e.poster, releaseInfo = e.releaseInfo)) }
        }
    }

    private val _lines = MutableStateFlow<Map<String, String>>(emptyMap())
    val lines: StateFlow<Map<String, String>> = _lines.asStateFlow()
    /** Landscape picture (backdrop) per title, for wide cards that have none (Biblioteka › Historia). */
    private val _images = MutableStateFlow<Map<String, String>>(emptyMap())
    val images: StateFlow<Map<String, String>> = _images.asStateFlow()
    private val asked = HashSet<String>()

    /** v6 hero ＋ (Moja lista): is the title in the default list? */
    fun inLibrary(id: String, type: String): kotlinx.coroutines.flow.Flow<Boolean> = library.isInLibrary(id, type)

    /** v6 hero ＋: toggles the default list (a removal that needs confirmation is left to the details screen). */
    fun toggleLibrary(item: com.nuvio.tv.domain.model.MetaPreview) {
        viewModelScope.launch {
            runCatching {
                library.toggleDefault(
                    com.nuvio.tv.domain.model.LibraryEntryInput(
                        itemId = item.id, itemType = item.apiType, title = item.name,
                        poster = item.poster, posterShape = item.posterShape, background = item.background, logo = item.logo,
                        description = item.description, releaseInfo = item.releaseInfo, imdbRating = item.imdbRating, genres = item.genres,
                    )
                )
            }
        }
    }

    fun request(type: String, id: String) {
        if (!asked.add(id)) return
        metas.getCachedMeta(type, id)?.let { put(id, it); return }
        viewModelScope.launch {
            runCatching {
                metas.getMetaFromPrimaryAddon(type, id).first { it !is NetworkResult.Loading }
            }.getOrNull()?.let { if (it is NetworkResult.Success) put(id, it.data) }
        }
    }

    private fun put(id: String, meta: Meta) {
        val line = forgeHeroLine(meta.releaseInfo, meta.genres, meta.imdbRating)
        if (line.isNotBlank()) _lines.update { it + (id to line) }
        (meta.background ?: meta.landscapePoster)?.takeIf { it.isNotBlank() }?.let { img -> _images.update { it + (id to img) } }
    }
}

/** "2022— · Thriller, Sci-Fi · IMDb 8,7" (any part may be missing). */
internal fun forgeHeroLine(releaseInfo: String?, genres: List<String>, imdb: Float?): String = buildList {
    releaseInfo?.takeIf { it.isNotBlank() }?.let(::add)
    if (genres.isNotEmpty()) add(genres.take(2).joinToString(", "))
    imdb?.takeIf { it > 0f }?.let { add("IMDb " + "%.1f".format(java.util.Locale("pl"), it)) }
}.joinToString(" · ")
