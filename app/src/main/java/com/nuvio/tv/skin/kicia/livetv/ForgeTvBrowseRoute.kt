package com.nuvio.tv.skin.kicia.livetv

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.reshaped.livetv.LiveTvRepository

/**
 * Fork 19.11 (zgłoszenie 2): „Live TV → Odkrywaj” = drugi układ Live TV Kicii (półki [ForgeTvBrowse]:
 * nowe odcinki, ostatnie kanały, ostatnio oglądane) jako osobna trasa, bez zmian w LiveTvScreen.
 * Wybór kanału: prośba [ForgePendingPlay] + powrót do przewodnika, który ją odbiera (ForgeGuideHeaderA).
 */
@Composable
fun ForgeTvBrowseRoute() {
    val state by LiveTvRepository.uiState.collectAsStateWithLifecycle()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val onBack: () -> Unit = { dispatcher?.onBackPressed() } // Wstecz = zwykły powrót trasy do przewodnika
    ForgeTvBrowse(
        channels = state.channels,
        onPlay = { channel, programme ->
            ForgePendingPlay.ask(channel.streamUrl, programme = programme, guideKey = channel.guideKey)
            onBack()
        },
        onOpenGuide = onBack,
    )
}
