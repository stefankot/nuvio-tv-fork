package com.nuvio.tv.skin.kicia.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.WatchedItemsPreferences
import com.nuvio.tv.domain.model.WatchedItem
import com.nuvio.tv.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Feeds the number on the "Biblioteka" tab: titles on the list still waiting to be watched (mockup).
 * A title stops counting once it is marked watched as a whole (a watched film, a show marked watched);
 * a show with only some episodes watched still counts.
 */
@HiltViewModel
class ForgeNavViewModel @Inject constructor(
    libraryRepository: LibraryRepository,
    watchedItems: WatchedItemsPreferences,
) : ViewModel() {
    val libraryCount: StateFlow<Int> = combine(libraryRepository.libraryItems, watchedItems.allItems) { entries, watched ->
        forgeLibraryWaitingCount(entries.map { it.id }, watched)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

/** Titles in [libraryIds] not marked watched as a whole (a mark with no season and no episode). */
internal fun forgeLibraryWaitingCount(libraryIds: List<String>, watched: List<WatchedItem>): Int {
    val whole = watched.asSequence().filter { it.season == null && it.episode == null }.mapTo(HashSet()) { it.contentId }
    return libraryIds.distinct().count { it !in whole }
}
