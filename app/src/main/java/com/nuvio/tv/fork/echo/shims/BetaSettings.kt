package com.nuvio.tv.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.core.player.BetaFeatures
import com.nuvio.tv.data.local.BetaPerformanceSettings

/** Echo shim (Echo defines this in QualitySelectionOverlay.kt via a DataStore entry point; removed there). */
@Composable
internal fun rememberBetaPerformanceSettings(): State<BetaPerformanceSettings> {
    val context = LocalContext.current
    return remember { mutableStateOf(BetaFeatures.settings(context)) }
}
