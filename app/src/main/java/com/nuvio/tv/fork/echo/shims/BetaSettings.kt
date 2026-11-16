package com.nuvio.tv.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.nuvio.tv.data.local.BetaPerformanceSettings

/** Echo shim: Echo keeps these in a DataStore; the fork needs only the flags its copied files read. */
@Composable
internal fun rememberBetaPerformanceSettings(): State<BetaPerformanceSettings> = remember { mutableStateOf(BetaPerformanceSettings()) }
