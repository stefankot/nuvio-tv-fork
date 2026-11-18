package com.nuvio.tv.ui.screens.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.core.player.recovery.AutoSourceRecoverySettings
import com.nuvio.tv.core.player.recovery.SmartBadSources

/**
 * forge M3b (P6): two rows under the preset in the automatic mode, with the existing components —
 * "Change the source when it fails" and "Clear the memory of sources" (how many are put aside now).
 */
@Composable
internal fun SmartRecoveryRows() {
    val context = LocalContext.current
    val switchOn by AutoSourceRecoverySettings.smartSwitch(context).collectAsStateWithLifecycle()
    var count by remember { mutableIntStateOf(SmartBadSources.liveCount()) }

    SettingsToggleRow(
        title = stringResource(R.string.forge_smart_switch_title),
        subtitle = stringResource(R.string.forge_smart_switch_sub),
        checked = switchOn,
        onToggle = { AutoSourceRecoverySettings.setSmartSwitch(context, !switchOn) },
    )
    SettingsActionRow(
        title = stringResource(R.string.forge_smart_memory_title),
        subtitle = null,
        value = if (count == 0) stringResource(R.string.forge_smart_memory_empty)
            else pluralStringResource(R.plurals.forge_smart_memory_count, count, count),
        enabled = count > 0,
        onClick = {
            SmartBadSources.clearAll()
            count = SmartBadSources.liveCount()
            Toast.makeText(context, R.string.forge_smart_memory_cleared, Toast.LENGTH_SHORT).show()
        },
    )
}
