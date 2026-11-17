package com.nuvio.tv.fork.registry

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Rejestr modułów. Przełącznik modułu to klucz `module_<id>` w prefs `fork_features` (ForkFeatures).
 * Wyłączony moduł nic nie zgłasza.
 */
class ForkRegistry(
    private val modules: List<ForkModule>,
    private val isEnabled: (key: String, default: Boolean) -> Boolean,
) {
    private fun active() = modules.filter { isEnabled(switchKey(it.id), it.defaultEnabled) }

    fun settings() = active().flatMap { m -> m.settings().map { m.id to it } }
    fun routes() = active().flatMap { it.routes() }
    fun homeRows() = active().flatMap { it.homeRows() }.sortedBy { it.order }
    fun rootOverlays() = active().flatMap { it.rootOverlays() }
    fun playerHandlers() = active().flatMap { it.playerHandlers() }
    fun voiceCommands() = active().flatMap { it.voiceCommands() }
    fun moduleIds() = modules.map { it.id }

    companion object {
        fun switchKey(id: String) = "module_$id"

        fun forContext(c: Context) =
            ForkRegistry(ForkModules.all) { k, d -> ForkFeatures.enabled(c, k, d) }
    }
}
