package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.posteroptions.PosterOptionsHost
import com.nuvio.tv.ui.components.posteroptions.PosterOptionsViewModel

/**
 * Długie OK na plakacie Niuni (UX 22.11: „w większości elementów nie działa długie naciśnięcie”) → menu opcji bazy
 * (szczegóły, biblioteka / listy, obejrzane) — ten sam [PosterOptionsHost] co w zwykłych ekranach Nuvio.
 * Okno bazy (NuvioDialog) zjada pierwsze puszczenie OK, więc puszczenie po przytrzymaniu nie wybiera od razu opcji.
 * Zwraca funkcję do podania jako `onLongOk` plakatu: (pozycja, adres dodatku).
 */
@Composable
fun rememberNiuniaPosterOptions(onNavigateToDetail: (String, String, String) -> Unit): (MetaPreview, String?) -> Unit {
    val vm: PosterOptionsViewModel = hiltViewModel()
    val state by vm.controller.state.collectAsState()
    // Z-14 (makieta TitleActions): okno akcji plakatu w stylu Niuni; wybór list (Trakt/Simkl) zostaje oknem bazy.
    val target = state.target
    if (target != null && !state.listPickerActive) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val series = com.nuvio.tv.domain.model.isSeriesLikeType(target.apiType)
        val local = state.librarySourceMode == com.nuvio.tv.domain.model.LibrarySourceMode.LOCAL
        NiuniaActionsDialog(
            eyebrow = ctx.getString(if (series) com.nuvio.tv.R.string.niunia_ke_series else com.nuvio.tv.R.string.niunia_ke_movie),
            title = target.name,
            state = listOfNotNull(target.releaseInfo?.takeIf { it.isNotBlank() }, target.genres.take(2).joinToString(" · ").ifBlank { null }).joinToString(" · ").ifBlank { null },
            onDismiss = { vm.controller.dismiss() },
            actions = listOf(
                NiuniaDialogAction(ctx.getString(if (series) com.nuvio.tv.R.string.niunia_ke_details_series else com.nuvio.tv.R.string.niunia_ke_details_movie), icon = "info", chevron = true) {
                    onNavigateToDetail(target.id, target.apiType, state.addonBaseUrl)
                },
                if (!local) NiuniaDialogAction(ctx.getString(com.nuvio.tv.R.string.niunia_ke_lists), icon = "playlist-add", chevron = true, dismiss = false) { vm.controller.openListPicker() }
                else NiuniaDialogAction(ctx.getString(if (state.isInLibrary) com.nuvio.tv.R.string.niunia_ke_remove_library else com.nuvio.tv.R.string.niunia_ke_add_library),
                    icon = if (state.isInLibrary) "check" else "plus") { vm.controller.toggleLibrary() },
                NiuniaDialogAction(ctx.getString(if (state.isWatched) com.nuvio.tv.R.string.niunia_ke_mark_unwatched else com.nuvio.tv.R.string.niunia_ke_mark_watched), icon = "check") {
                    if (series) vm.controller.toggleSeriesWatched() else vm.controller.toggleMovieWatched()
                },
            ),
        )
    }
    // Okno bazy rysuje już tylko wybór list (target = null → bez okna opcji).
    PosterOptionsHost(state = if (state.listPickerActive) state else state.copy(target = null), controller = vm.controller, onNavigateToDetail = onNavigateToDetail)
    return remember(vm) {
        { item: MetaPreview, addon: String? ->
            Log.i(DS_TAG, "długie OK → opcje plakatu ${item.rawType} ${item.id}")
            vm.controller.show(item, addon)
        }
    }
}
