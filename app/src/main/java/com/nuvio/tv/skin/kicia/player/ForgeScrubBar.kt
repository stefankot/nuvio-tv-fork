package com.nuvio.tv.skin.kicia.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.absoluteOffset
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** A coloured stretch of the bar, as fractions 0..1 of the duration (SponsorBlock segment, ...). */
data class ForgeScrubSegment(val start: Float, val end: Float, val color: Color = ForgeColors.Ok)

/**
 * The seek bar of every player. Draws the state of a [ForgeScrubDriver]; keys are fed in by
 * [Modifier.forgeScrubKeys]. While scrubbing only the bar and the preview card are meant to stay on screen.
 *
 * [previewCard] draws the frame preview above the marker (196x110 dp, 3 dp white outline, time under it).
 * [chapterBreaks] are fractions where a small gap is cut into the bar.
 */
@Composable
fun ForgeScrubBar(
    state: ForgeScrubState,
    focused: Boolean,
    modifier: Modifier = Modifier,
    segments: List<ForgeScrubSegment> = emptyList(),
    chapterBreaks: List<Float> = emptyList(),
    previewCard: (@Composable (positionMs: Long) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }
    val trackHeight = if (focused) ForgeDims.ScrubTrackFocused else ForgeDims.ScrubTrack
    val duration = state.durationMs.coerceAtLeast(1L).toFloat()
    val playedFraction = (state.positionMs / duration).coerceIn(0f, 1f)
    val edgeFraction = (state.edgeMs / duration).coerceIn(0f, 1f)

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ForgeDims.PreviewCardHeight + 40.dp)
                .onSizeChanged { widthPx = it.width },
        ) {
            val marker = state.markerMs
            if (marker != null && previewCard != null) {
                val cardW = with(density) { ForgeDims.PreviewCardWidth.roundToPx() }
                val x = ((marker / duration) * widthPx - cardW / 2f).toInt().coerceIn(0, (widthPx - cardW).coerceAtLeast(0))
                Column(
                    Modifier.absoluteOffset { IntOffset(x, 0) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(ForgeDims.PreviewCardWidth, ForgeDims.PreviewCardHeight)
                            .border(2.dp, ForgeColors.Focus, RoundedCornerShape(ForgeDims.RadiusCard))
                            .clip(RoundedCornerShape(ForgeDims.RadiusCard))
                            .background(ForgeColors.CardBase),
                    ) { previewCard(marker) }
                    Text(formatScrubTime(marker), style = ForgeText.captionBold, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Canvas(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(24.dp),
            ) {
                val h = trackHeight.toPx()
                val y = (size.height - h) / 2f
                val r = CornerRadius(h / 2f)
                // Track, then the part that can be reached, then what has been played.
                drawRoundRect(Color.White.copy(alpha = .30f), Offset(0f, y), Size(size.width, h), r)
                if (edgeFraction < 1f) {
                    drawRoundRect(Color.White.copy(alpha = .18f), Offset(size.width * edgeFraction, y), Size(size.width * (1f - edgeFraction), h), r)
                }
                segments.forEach { s ->
                    val x0 = size.width * s.start.coerceIn(0f, 1f)
                    val x1 = size.width * s.end.coerceIn(0f, 1f)
                    if (x1 > x0) drawRoundRect(s.color, Offset(x0, y), Size(x1 - x0, h), r)
                }
                drawRoundRect(Color.White, Offset(0f, y), Size(size.width * playedFraction, h), r)
                // Chapters: a thin cut into the bar.
                val cut = 2.dp.toPx()
                chapterBreaks.forEach { f ->
                    drawRect(Color.Black.copy(alpha = .55f), Offset(size.width * f.coerceIn(0f, 1f) - cut / 2f, y), Size(cut, h))
                }
                // Preview marker detaches from the playing position (accent colour).
                state.markerMs?.let { m ->
                    val mw = ForgeDims.ScrubMarkerWidth.toPx()
                    val mh = ForgeDims.ScrubMarkerHeight.toPx()
                    drawRoundRect(
                        ForgeColors.Accent,
                        Offset(size.width * (m / duration).coerceIn(0f, 1f) - mw / 2f, (size.height - mh) / 2f),
                        Size(mw, mh),
                        CornerRadius(mw / 2f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(formatScrubTime(state.positionMs), style = ForgeText.label, modifier = Modifier.weight(1f))
            Text(formatScrubRemaining(state.positionMs, state.durationMs), style = ForgeText.label)
        }
    }
}
