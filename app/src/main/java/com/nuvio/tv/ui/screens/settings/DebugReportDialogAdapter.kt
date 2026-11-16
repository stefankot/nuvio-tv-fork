package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// Nuvio One adapter: the fork has no debug-report service; the dialog closes immediately.
@Composable
internal fun DebugReportDialog(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { onDismiss() }
}
