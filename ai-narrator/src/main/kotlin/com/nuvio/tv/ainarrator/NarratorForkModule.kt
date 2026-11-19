package com.nuvio.tv.ainarrator

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fork adapter (hybryda-zrodel): Moduły → przełącznik „Lektor AI”, wejście w odtwarzaczu: Więcej → Lektor AI.
 * Sam lektor jest domyślnie wyłączony (koszt Gemini); przełącznik modułu tylko pokazuje wejście.
 */
object NarratorForkModule : ForkModule {
    override val id = "ai_narrator"
    override val title = "Lektor AI (Gemini czyta napisy głosem)"
    override val defaultEnabled = true

    fun moduleOn(c: Context) = ForkFeatures.enabled(c, ForkRegistry.switchKey(id), defaultEnabled)
}

/** Stan współdzielony między wierszem menu a nakładką w odtwarzaczu. */
internal object NarratorEntry {
    val requestOpen = mutableStateOf(false)
    val status = MutableStateFlow("Wyłączony")
}

/** Klucz lektora; gdy brak własnego, wspólny klucz Gemini z AI napisów (QR / wbudowany). */
internal fun narratorGeminiKey(c: Context, prefs: NarratorPreferences): String =
    prefs.apiKey().ifBlank { com.nuvio.tv.fork.ai.subtitles.AiKeyStore.read(c).orEmpty() }

@Composable
fun NarratorMenuRow(close: () -> Unit) {
    val c = LocalContext.current
    if (!NarratorForkModule.moduleOn(c)) return
    val status by NarratorEntry.status.collectAsState()
    ForgeSheetRow(text = "Lektor AI (Gemini)", detail = status) {
        close()
        NarratorEntry.requestOpen.value = true
    }
}
