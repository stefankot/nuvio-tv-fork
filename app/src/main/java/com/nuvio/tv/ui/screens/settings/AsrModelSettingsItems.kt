@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.ui.screens.player.audiosync.asr.AsrModelChoice

/** Fork adapter: which speech model audio-based subtitle sync uses (English zipformer or multilingual Whisper). */
internal fun LazyListScope.asrModelSettingsItems() {
    item(key = "fork_asr_model") {
        val context = LocalContext.current
        var choice by remember { mutableStateOf(AsrModelChoice.current(context)) }
        NavigationSettingsItem(
            icon = Icons.Default.Language,
            title = "Model rozpoznawania mowy: ${choice.label}",
            subtitle = "~${choice.sizeMb} MB. Kliknij, aby zmienić; pobierz model poniżej (synchronizacja napisów z audio).",
            onClick = { val n = AsrModelChoice.next(context); AsrModelChoice.set(context, n); choice = n },
        )
    }
}
