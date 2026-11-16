@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.data.local.RemoteButtonStore

/** Arc remote-button mapping (whole app: player, Live TV, ...), in the one fork settings category. */
internal fun LazyListScope.remoteButtonsSettingsItems() {
    item(key = "fork_remote_buttons") {
        val context = LocalContext.current
        RemoteButtonStore.ensureLoaded(context)
        val map by RemoteButtonStore.map.collectAsStateWithLifecycle()
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(com.nuvio.tv.ui.theme.NuvioTheme.spacing.md)
        ) {
            PlaybackRemoteButtonsSection(map) { RemoteButtonStore.set(context, it) }
        }
    }
}
