@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.night

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleSetting
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay

/** Tryb nocny: sterownik według godzin, ciepła nakładka obrazu, obowiązujący poziom dźwięku (B05). */
object NightModule : ForkModule {
    override val id = "night"
    override val title = "Tryb nocny (godziny, ciepła nakładka)"
    override val defaultEnabled = true

    /** Jedno wspólne źródło prawdy dla odtwarzacza i nakładki. */
    val controller = NightController()
    @Volatile var hdrPlaying: Boolean = false

    private const val K_MODE = "night_mode"; private const val K_STRENGTH = "night_strength"
    private const val K_START = "night_start_min"; private const val K_END = "night_end_min"
    private const val K_OVERLAY = "night_overlay"; private const val K_DIM = "night_dim"; private const val K_HDR = "night_skip_hdr"

    fun load(c: Context) {
        controller.mode = runCatching { NightSwitch.valueOf(ForkFeatures.str(c, K_MODE, "OFF")) }.getOrDefault(NightSwitch.OFF)
        controller.strength = runCatching { Strength.valueOf(ForkFeatures.str(c, K_STRENGTH, "MEDIUM")) }.getOrDefault(Strength.MEDIUM)
        controller.schedule = NightSchedule(
            ForkFeatures.str(c, K_START, "1320").toIntOrNull() ?: 1320, ForkFeatures.str(c, K_END, "420").toIntOrNull() ?: 420,
        )
    }

    /** Ustawienia obrazu/dźwięku w poziomie Prostym: jedna siła; w Essential można je rozdzielić (tu: wspólna). */
    fun settings(c: Context): NightSettings = NightEffects.fromCommon(controller.strength).copy(
        dim = ForkFeatures.enabled(c, K_DIM, false), skipOnHdr = ForkFeatures.enabled(c, K_HDR, true),
    )

    override fun settings() = listOf(
        ModuleSetting(K_OVERLAY, SettingsLevel.SIMPLE, "Tryb nocny", "Ciepła nakładka obrazu", "Bursztynowy odcień ekranu w godzinach nocnych", defaultValue = true),
        ModuleSetting(K_DIM, SettingsLevel.ESSENTIAL, "Tryb nocny", "Przyciemnienie", "Dodatkowo przyciemnia obraz", defaultValue = false),
        ModuleSetting(K_HDR, SettingsLevel.ADVANCED, "Tryb nocny", "Nie stosuj przy HDR i Dolby Vision", "Nakładka wyłączona przy filmach HDR", defaultValue = true),
    )

    override fun actions() = listOf(
        com.nuvio.tv.fork.registry.ModuleAction("Tryb nocny: według godzin 22:00–07:00", SettingsLevel.SIMPLE, "Włącza harmonogram") { c ->
            ForkFeatures.setStr(c, K_MODE, "SCHEDULE"); load(c)
        },
        com.nuvio.tv.fork.registry.ModuleAction("Tryb nocny: włączony", SettingsLevel.SIMPLE) { c -> ForkFeatures.setStr(c, K_MODE, "ON"); load(c) },
        com.nuvio.tv.fork.registry.ModuleAction("Tryb nocny: wyłączony", SettingsLevel.SIMPLE) { c -> ForkFeatures.setStr(c, K_MODE, "OFF"); load(c) },
        com.nuvio.tv.fork.registry.ModuleAction("Siła: lekka / średnia / mocna (kolejno)", SettingsLevel.SIMPLE) { c ->
            val next = when (controller.strength) { Strength.LIGHT -> Strength.MEDIUM; Strength.MEDIUM -> Strength.STRONG; else -> Strength.LIGHT }
            ForkFeatures.setStr(c, K_STRENGTH, next.name); load(c)
        },
    )

    override fun rootOverlays(): List<@Composable () -> Unit> = listOf({ WarmOverlay() })

    @Composable
    private fun WarmOverlay() {
        val c = LocalContext.current
        LaunchedEffect(Unit) { load(c) }
        var target by remember { mutableStateOf(NightEffects.Overlay(0f, 0f)) }
        var changedAt by remember { mutableLongStateOf(0L) }
        LaunchedEffect(Unit) {
            while (true) {
                val on = ForkFeatures.enabled(c, K_OVERLAY, true)
                val eff = controller.effective(System.currentTimeMillis())
                val next = if (on) NightEffects.overlay(eff, settings(c), hdrPlaying) else NightEffects.Overlay(0f, 0f)
                if (next != target) { target = next; changedAt = System.currentTimeMillis() }
                delay(1_000L)
            }
        }
        var ramp by remember { mutableStateOf(1f) }
        LaunchedEffect(target, changedAt) {
            while (ramp < 1f || changedAt > 0) {
                ramp = NightEffects.ramp(System.currentTimeMillis() - changedAt)
                if (ramp >= 1f) break
                delay(100L)
            }
        }
        if (target.amber > 0f || target.dim > 0f) {
            // Nakładka nie przechwytuje fokusu ani klawiszy: zwykły Box bez focusable/clickable.
            Box(Modifier.fillMaxSize().alpha(ramp).background(Color(0xFFFF9A2E).copy(alpha = target.amber)))
            if (target.dim > 0f) Box(Modifier.fillMaxSize().alpha(ramp).background(Color.Black.copy(alpha = target.dim)))
        }
    }
}
