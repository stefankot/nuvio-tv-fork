package com.nuvio.tv.skin.niunia.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.components.NiuniaIcon
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing

/**
 * 22.11 (właściciel: „omijanie reklam przenieść z Kici”): karta w stylu Niuni dla oferty z [com.nuvio.tv.skin.kicia.livetv.ForgeArchiveOffer]
 * — gdy program leci na żywo, w tle szukana jest jego wcześniejsza emisja w archiwum (ForgeAiringRace.findEarlier,
 * strumień sprawdzany); jest → karta w prawym górnym rogu, OK przełącza na archiwum tego samego odcinka, Wstecz chowa.
 * Bez fokusu (klawisze obsługuje ForgeArchiveOffer.onKey na gołym obrazie). Tło pełne (bez rozmycia — też Android 11).
 */
@Composable
internal fun NiuniaArchiveOfferCard(channelName: String) {
    val title = stringResource(R.string.niunia_adskip_title)
    Box(Modifier.fillMaxSize().padding(top = NiuniaSpacing.Space40 * 2, end = NiuniaSpacing.Space40), contentAlignment = Alignment.TopEnd) {
        Row(
            with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.semantics { contentDescription = title }.forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Panel, RoundedCornerShape(NiuniaRadius.Radius16), fallback = NiuniaColor.Panel) }
                .padding(horizontal = NiuniaSpacing.Space16, vertical = NiuniaSpacing.Space12),
            horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.clip(CircleShape).background(NiuniaColor.Fill24).padding(NiuniaSpacing.Space8)) { NiuniaIcon("history", size = NiuniaSize.Icon20) }
            Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
                NiuniaText(title, variant = NiuniaTextVariant.Label, lines = 1)
                NiuniaText(stringResource(R.string.niunia_adskip_sub, channelName), variant = NiuniaTextVariant.CaptionRegular, tone = NiuniaTone.Secondary, lines = 1)
            }
            NiuniaTag(stringResource(R.string.niunia_adskip_ok), NiuniaTagKind.BadgeStrong)
        }
    }
}
