package com.nuvio.tv.skin.kicia.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * Episode card of the mockup (S03, c12): picture 190×106, under it "1. Tytuł" and "47 min". Same inputs and
 * focus wiring as Nuvio's card (OK plays, long OK opens its options).
 */
@Composable
fun ForgeEpisodeCard(
    episode: Video,
    progress: Float,
    watched: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    focusRequester: FocusRequester,
    isFocusEnabled: Boolean,
    onFocused: () -> Unit,
) {
    Column(Modifier.width(ForgeDims.Wide)) { // v7.4: Karta/Odcinek 160 x 114, r 8
        ForgeFocusBox(
            radius = ForgeDims.RadiusCard,
            modifier = Modifier.size(ForgeDims.Wide, ForgeDims.WideHeight).focusProperties {
                canFocus = isFocusEnabled
                up = upFocusRequester
                if (downFocusRequester != null) down = downFocusRequester
            },
            focusRequester = focusRequester,
            onFocusChange = { if (it) onFocused() },
            onLongOk = onLongPress,
            onOk = onClick,
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(ForgeDims.RadiusCard)).background(ForgeColors.CardBase)) {
                episode.thumbnail?.takeIf { it.isNotBlank() }?.let {
                    AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                if (progress > 0f && !watched) {
                    Box(Modifier.align(Alignment.BottomStart).padding(8.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(ForgeColors.Surface3)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(ForgeColors.Text))
                    }
                }
                if (watched) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).clip(CircleShape).background(ForgeColors.Ok),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, null, tint = ForgeColors.OnFocus, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        Text(
            forgeEpisodeTitle(episode.episode, episode.title),
            style = ForgeText.label, // v7.4: 14/20 700, 16 pod obrazem, 8 od lewej
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp),
        )
        episode.runtime?.takeIf { it > 0 }?.let { Text("$it min", style = ForgeText.caption.copy(color = ForgeColors.Text3), modifier = Modifier.padding(horizontal = 8.dp)) } // v7.4: meta 12/16, 50%
    }
}

/** "1. Dzień dobry, panie Milchick" */
internal fun forgeEpisodeTitle(number: Int?, title: String): String =
    if (number != null) "$number. ${title.ifBlank { "Odcinek $number" }}" else title
