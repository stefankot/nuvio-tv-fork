package com.nuvio.tv.skin.kicia.settings

import com.nuvio.tv.ui.screens.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.NightMode
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import com.nuvio.tv.skin.kicia.nav.ForgeNightMode
import com.nuvio.tv.ui.theme.NuvioTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** v7.4 (14): reduced audio dynamics is Nuvio's own player setting "night mode" (off / medium). */
@HiltViewModel
class ForgeNightViewModel @Inject constructor(private val store: PlayerSettingsDataStore) : ViewModel() {
    val audioOn = store.playerSettings.map { it.nightMode != NightMode.OFF }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setAudio(on: Boolean) {
        viewModelScope.launch { store.setNightMode(if (on) NightMode.MEDIUM else NightMode.OFF) }
    }
}

/** Settings category "Tryb nocny" (mockup 14): blue-light filter, reduced audio dynamics, schedule. */
@Composable
internal fun ForgeNightSettingsContent(initialFocusRequester: FocusRequester? = null) {
    val context = LocalContext.current
    val vm: ForgeNightViewModel = hiltViewModel()
    val audioOn by vm.audioOn.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { ForgeNightMode.load(context) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
    ) {
        SettingsDetailHeader(title = "Tryb nocny", subtitle = "")
        SettingsToggleRow(
            title = "Filtr niebieskiego światła",
            subtitle = "Ciepłe barwy ekranu wieczorem",
            checked = ForgeNightMode.enabled,
            onToggle = { ForgeNightMode.toggle() },
            modifier = if (initialFocusRequester != null) Modifier.focusRequester(initialFocusRequester) else Modifier,
        )
        SettingsToggleRow(
            title = "Zmniejszona dynamika dźwięku",
            subtitle = "Ciche sceny głośniejsze, głośne cichsze",
            checked = audioOn,
            onToggle = { vm.setAudio(!audioOn) },
        )
        SettingsActionRow(
            title = "Harmonogram",
            subtitle = null,
            value = ForgeNightMode.scheduleLabel(),
            onClick = { ForgeNightMode.cycleSchedule(context) },
        )
    }
}
