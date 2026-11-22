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
    PosterOptionsHost(state = state, controller = vm.controller, onNavigateToDetail = onNavigateToDetail)
    return remember(vm) {
        { item: MetaPreview, addon: String? ->
            Log.i(DS_TAG, "długie OK → opcje plakatu ${item.rawType} ${item.id}")
            vm.controller.show(item, addon)
        }
    }
}
