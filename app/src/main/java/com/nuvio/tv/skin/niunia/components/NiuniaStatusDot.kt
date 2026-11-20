package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.tokens.NiuniaStatus

/**
 * Kropka statusu zamiast napisów „LIVE” / „Archiwum” (decyzja właściciela 20.11, cała Niunia):
 * 10 dp, bez obwódki, w lewym górnym rogu kafelka; czerwona = leci teraz, zielona = w archiwum.
 * Czytnik ekranu dostaje słowo.
 */
@Composable
fun NiuniaStatusDot(live: Boolean, modifier: Modifier = Modifier) {
    val label = stringResource(if (live) R.string.niunia_status_live else R.string.niunia_status_archive)
    Box(
        modifier
            .padding(8.dp)
            .size(10.dp)
            .background(if (live) NiuniaStatus.Live else NiuniaStatus.Archive, CircleShape)
            .semantics { contentDescription = label },
    )
}
