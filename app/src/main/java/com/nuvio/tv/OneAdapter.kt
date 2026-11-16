package com.nuvio.tv

import androidx.compose.runtime.compositionLocalOf

// Nuvio One adapter: sidebar opener used by the YouTube page (no-op unless provided).
val LocalOpenSidebar = compositionLocalOf<() -> Unit> { {} }
