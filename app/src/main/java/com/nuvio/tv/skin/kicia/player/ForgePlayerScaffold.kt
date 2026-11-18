package com.nuvio.tv.skin.kicia.player

import com.nuvio.tv.skin.kicia.nav.forgeTagRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeMotion
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Clock "21:07", refreshed on the minute. */
@Composable
fun ForgeClock(modifier: Modifier = Modifier) {
    val text by produceState(initialValue = formatClock(System.currentTimeMillis())) {
        while (true) {
            val now = System.currentTimeMillis()
            value = formatClock(now)
            delay(60_000L - (now % 60_000L) + 50L)
        }
    }
    Text(
        text,
        style = ForgeText.button.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .7f), blurRadius = 8f)),
        modifier = modifier,
    )
}

private fun formatClock(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

/**
 * Shared skeleton of all players (movies, YouTube, live TV): top and bottom scrims, title block on
 * the left, clock (and "Koniec o HH:MM") on the right, and the controls at the bottom.
 * The controls ([bottom]) sit above y = 513 of the 540 dp board and never overlap the header.
 */
@Composable
fun ForgePlayerScaffold(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    endsAt: String? = null,
    /** v7.4 (05/13): round buttons 36 to the right of the title (y 364, x 836 / 884). */
    endActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().forgeTagRoot()) { // v7.4: tagi sterowania widoczne w uiautomator
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(ForgeMotion.fadeSpec()),
            exit = fadeOut(ForgeMotion.fadeSpec()),
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(ForgeDims.PlayerScrimTopHeight)
                        .background(ForgeColors.PlayerScrimTop),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(ForgeDims.PlayerScrimBottomHeight)
                        .background(ForgeColors.PlayerScrimBottom),
                )
                Column(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = ForgeDims.ContentEnd, top = 32.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    ForgeClock()
                    if (endsAt != null) Text(endsAt, style = ForgeText.label5)
                }
                // v7.4 (05/13 sterowanie): line 14/700 y 346, title 32/700 y 366, round buttons right (y 364); bar y 412, times y 432, row y 460 (bottom 496)
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = ForgeDims.ContentStart, end = ForgeDims.ContentEnd, bottom = 44.dp),
                ) {
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(subtitle.orEmpty(), style = ForgeText.label.copy(color = ForgeColors.Text2), maxLines = 1, modifier = Modifier.height(20.dp).testTag("ster-linia.0"))
                            Text(title, style = ForgeText.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.height(38.dp).testTag("ster-tytul.0"))
                        }
                        androidx.compose.foundation.layout.Row(
                            Modifier.padding(start = 12.dp, bottom = 4.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                            content = endActions,
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    bottom()
                }
            }
        }
    }
}
