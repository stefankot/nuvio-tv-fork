@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.domain.model.ExperienceMode
import com.nuvio.tv.data.local.ExperienceModeDataStore
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import com.nuvio.tv.fork.settings.nav.LevelPicker
import com.nuvio.tv.fork.settings.nav.NavGroupHeader
import com.nuvio.tv.fork.settings.nav.NavRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

object SimpleLevel {
    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun experience(): ExperienceModeDataStore }

    fun isSimple(c: Context) = ForkFeatures.str(c, FirstRunDefaults.LEVEL_KEY) == UiLevel.SIMPLE.name
    /** Fork 19.11 (właściciel: „z wersji średniej/rozszerzonej nie ma powrotu do najprostszych”): licznik zmian poziomu — ekran Ustawień przełącza się od razu. */
    val changes = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** Wejście „Najprostsze ustawienia” z ustawień podstawowych i pełnych. */
    fun toSimple(c: Context) { ForkFeatures.setStr(c, FirstRunDefaults.LEVEL_KEY, UiLevel.SIMPLE.name); ForkFeatures.set(c, "module_settings_simple", true); changes.value++ }

    suspend fun set(c: Context, level: UiLevel) {
        val change = LevelSwitch.to(level)
        change.experienceMode?.let { EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).experience().setMode(ExperienceMode.valueOf(it)) }
        ForkFeatures.setStr(c, FirstRunDefaults.LEVEL_KEY, if (change.simpleFlag) UiLevel.SIMPLE.name else level.name)
        changes.value++
    }
}

/** Wejście do Ustawień na poziomie Prostym (hook w nawigacji). true = ekran Prosty narysowany, rdzeń nie rysuje swojego. */
@Composable
fun SimpleSettingsGate(navigate: (String) -> Unit): Boolean {
    val c = LocalContext.current
    val levelChanges by SimpleLevel.changes.collectAsState()
    var simple by remember(levelChanges) { mutableStateOf(SimpleLevel.isSimple(c) && ForkFeatures.enabled(c, "module_settings_simple", true)) }
    if (!simple) return false
    SimpleSettingsScreen(navigate, onLevelChanged = { simple = false })
    return true
}

@Composable
private fun SimpleSettingsScreen(navigate: (String) -> Unit, onLevelChanged: () -> Unit) {
    val c = LocalContext.current
    val registry = remember { ForkRegistry.forContext(c) }
    val actions = remember { registry.actions().filter { it.second.level == SettingsLevel.SIMPLE } }
    val toggles = remember { registry.settings().filter { it.second.level == SettingsLevel.SIMPLE } }
    val first = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        android.util.Log.i("ForkSettingsNav", "tryb NOOB: ${actions.size} czynności, ${toggles.size} przełączników")
        androidx.compose.runtime.withFrameNanos { }
        runCatching { first.requestFocus() }.onFailure { android.util.Log.w("ForkSettingsNav", "fokus NOOB: ${it.javaClass.simpleName}: ${it.message}") }
    }
    // 19.11 (właściciel): wygląd jak reszta menu (tokeny skórki), jedna kolumna — wszystko na jednym ekranie.
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(com.nuvio.tv.skin.kicia.tokens.ForgeColors.Bg), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 880.dp).fillMaxSize().padding(start = 40.dp, end = 40.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 84.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { Text("Ustawienia", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.title) }
            item { Text("Tryb NOOB: tylko najważniejsze", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.body, modifier = Modifier.padding(bottom = 8.dp)) }
            item { NavGroupHeader("Ty") }
            item { NavRow("Konto", "Logowanie", icon = Icons.Outlined.AccountCircle, focusRequester = first, onOk = { navigate(Screen.Account.route) }) }
            item { NavRow("Profil", "Kto ogląda", icon = Icons.Outlined.People, onOk = { navigate(Screen.ManageProfiles.route) }) }
            item { NavRow("Szukaj w telewizji", "Programy i kanały", icon = Icons.Outlined.Search, onOk = { navigate("fork_tv_search") }) }
            item { NavGroupHeader("Źródła") }
            item { NavRow("Dodatki", "Skąd biorą się filmy", icon = Icons.Outlined.Extension, onOk = { navigate(Screen.AddonManager.route) }) }
            if (actions.isNotEmpty() || toggles.isNotEmpty()) item { NavGroupHeader("Oglądanie, tryb nocny, telewizja") }
            items(actions.size) { i -> val a = actions[i].second; NavRow(a.title, a.description, onOk = { com.nuvio.tv.fork.family.FamilyUsage.feature("akcja:" + a.title); a.run(c) }) }
            items(toggles.size) { i ->
                val s = toggles[i].second
                var on by remember { mutableStateOf(ForkFeatures.enabled(c, s.key, s.defaultValue)) }
                NavRow(s.title, s.description, value = if (on) "Włączone" else "Wyłączone", on = on, chevron = false,
                    onOk = { on = !on; ForkFeatures.set(c, s.key, on); android.util.Log.i("ForkSettingsNav", "NOOB: ${s.key} = $on") })
            }
            item { NavRow("Wszystkie moduły", "Każdy dodatek tej wersji", icon = Icons.Outlined.Apps, onOk = { navigate(MODULES_ROUTE) }) }
            item { NavGroupHeader("Poziom ustawień") }
            item { LevelPicker(current = UiLevel.SIMPLE, onChanged = onLevelChanged) }
        }
    }
}
