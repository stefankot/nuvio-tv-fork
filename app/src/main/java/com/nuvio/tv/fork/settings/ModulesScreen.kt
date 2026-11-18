package com.nuvio.tv.fork.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
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
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.ui.screens.settings.ForkFeatures

const val MODULES_ROUTE = "fork_modules"

/** Kategoria „Moduły” (B05-3): każdy moduł z rejestru, jego główny wyłącznik, pozycje i czynności. Wszystkie poziomy razem. */
@Composable
fun ModulesScreen() {
    val c = LocalContext.current
    val registry = remember { ForkRegistry.forContext(c) }
    val all = remember { registry.allModules() }
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Moduły", style = MaterialTheme.typography.headlineMedium) }
        all.forEach { m ->
            item(key = "m_" + m.id) {
                var on by remember { mutableStateOf(ForkFeatures.enabled(c, ForkRegistry.switchKey(m.id), m.defaultEnabled)) }
                Button(onClick = { on = !on; ForkFeatures.set(c, ForkRegistry.switchKey(m.id), on) }) { Text(m.title + ": " + if (on) "włączony" else "wyłączony") }
            }
            m.settings().forEach { s ->
                item(key = "s_" + s.key) {
                    var on by remember { mutableStateOf(ForkFeatures.enabled(c, s.key, s.defaultValue)) }
                    Button(onClick = { on = !on; ForkFeatures.set(c, s.key, on) }) { Text("   " + s.title + ": " + if (on) "włączone" else "wyłączone") }
                }
            }
            m.actions().forEach { a -> item(key = "a_" + m.id + a.title) { Button(onClick = { com.nuvio.tv.fork.family.FamilyUsage.feature("akcja:" + a.title); a.run(c) }) { Text("   " + a.title) } } }
        }
    }
}
