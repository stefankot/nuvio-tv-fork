package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText

/** Przycisk Niunii: w spoczynku ciemny półprzezroczysty, z fokusem biały z ciemnym tekstem. */
@Composable
fun NiuniaButton(
    text: String,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(NiuniaDims.ButtonH)
            .widthIn(min = if (primary) 200.dp else 0.dp)
            .niuniaFocusable(shape, scale = NiuniaDims.ScaleButton, lift = false, focusRequester = focusRequester, onFocus = { focused = it }, onOk = onOk)
            .background(if (focused) NiuniaColors.Focus else NiuniaColors.Btn, shape)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = NiuniaText.Button, color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text)
    }
}

/** Okrągły przycisk 40×40 ze znakiem (›, +, i). */
@Composable
fun NiuniaRoundButton(symbol: String, description: String, onOk: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .size(NiuniaDims.ButtonH)
            .niuniaFocusable(CircleShape, scale = NiuniaDims.ScaleButton, lift = false, onFocus = { focused = it }, onOk = onOk)
            .background(if (focused) NiuniaColors.Focus else NiuniaColors.Btn, CircleShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, style = NiuniaText.Button, color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text)
    }
}
