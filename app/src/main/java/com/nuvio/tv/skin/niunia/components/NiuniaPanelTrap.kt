package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties

/**
 * Panel albo okno nad ekranem (UX JMGO 24.11, F4/F5; pętla 0, ścieżka 41): fokus zostaje w panelu, a strzałka na jego krawędzi nic
 * nie robi. Samo [niuniaTrapArrows] tego nie gwarantowało — `moveFocus` szuka dalej poza grupą, więc ▼ z ostatniej pozycji „Opcji kanału”
 * przeskakiwało na kanały leżące pod panelem (w logu: „podgląd → kolejny kanał” przy otwartym panelu). `exit = Cancel` zamyka grupę
 * od środka, a pułapka zjada strzałkę. Kolejność modyfikatorów jest ważna: `focusProperties` tuż przed `focusGroup`.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.niuniaPanelTrap(
    name: String,
    dirs: Set<FocusDirection> = setOf(FocusDirection.Left, FocusDirection.Right, FocusDirection.Up, FocusDirection.Down),
): Modifier = this.niuniaTrapArrows(name, dirs).focusProperties { exit = { FocusRequester.Cancel } }.focusGroup()
