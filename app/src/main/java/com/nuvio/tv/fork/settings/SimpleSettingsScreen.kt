@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

object SimpleLevel {
    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun experience(): ExperienceModeDataStore }

    fun isSimple(c: Context) = ForkFeatures.str(c, FirstRunDefaults.LEVEL_KEY) == UiLevel.SIMPLE.name

    suspend fun set(c: Context, level: UiLevel) {
        val change = LevelSwitch.to(level)
        change.experienceMode?.let { EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).experience().setMode(ExperienceMode.valueOf(it)) }
        ForkFeatures.setStr(c, FirstRunDefaults.LEVEL_KEY, if (change.simpleFlag) UiLevel.SIMPLE.name else level.name)
    }
}

/** Wejście do Ustawień na poziomie Prostym (hook w nawigacji). true = ekran Prosty narysowany, rdzeń nie rysuje swojego. */
@Composable
fun SimpleSettingsGate(navigate: (String) -> Unit): Boolean {
    val c = LocalContext.current
    var simple by remember { mutableStateOf(SimpleLevel.isSimple(c) && ForkFeatures.enabled(c, "module_settings_simple", true)) }
    if (!simple) return false
    SimpleSettingsScreen(navigate, onLevelChanged = { simple = false })
    return true
}

@Composable
private fun SimpleSettingsScreen(navigate: (String) -> Unit, onLevelChanged: () -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val registry = remember { ForkRegistry.forContext(c) }
    val enabled = remember { registry.moduleIds().filter { ForkFeatures.enabled(c, ForkRegistry.switchKey(it), true) }.toSet() }
    val actions = remember { registry.actions().filter { it.second.level == SettingsLevel.SIMPLE } }
    val toggles = remember { registry.settings().filter { it.second.level == SettingsLevel.SIMPLE } }
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Ustawienia", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Ty", style = MaterialTheme.typography.titleMedium) }
        item { Button(onClick = { navigate(Screen.Account.route) }) { Text("Konto") } }
        item { Button(onClick = { navigate(Screen.ManageProfiles.route) }) { Text("Profil") } }
        item { Button(onClick = { navigate("fork_tv_search") }) { Text("Szukaj w telewizji") } }
        item { Text("Źródła", style = MaterialTheme.typography.titleMedium) }
        item { Button(onClick = { navigate(Screen.AddonManager.route) }) { Text("Dodatki") } }
        if (actions.isNotEmpty() || toggles.isNotEmpty()) item { Text("Oglądanie, tryb nocny, telewizja", style = MaterialTheme.typography.titleMedium) }
        items(actions.size) { i -> val a = actions[i].second; Button(onClick = { a.run(c) }) { Text(a.title) } }
        items(toggles.size) { i ->
            val s = toggles[i].second
            var on by remember { mutableStateOf(ForkFeatures.enabled(c, s.key, s.defaultValue)) }
            Button(onClick = { on = !on; ForkFeatures.set(c, s.key, on) }) { Text(s.title + ": " + if (on) "włączone" else "wyłączone") }
        }
        item { Text("System: poziom ustawień", style = MaterialTheme.typography.titleMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { }) { Text("✓ Prosty") }
                Button(onClick = { scope.launch { SimpleLevel.set(c, UiLevel.ESSENTIAL); onLevelChanged() } }) { Text("Essential") }
                Button(onClick = { scope.launch { SimpleLevel.set(c, UiLevel.ADVANCED); onLevelChanged() } }) { Text("Advanced") }
            }
        }
    }
}
