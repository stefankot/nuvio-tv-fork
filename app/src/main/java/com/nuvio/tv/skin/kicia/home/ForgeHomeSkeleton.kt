package com.nuvio.tv.skin.kicia.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.tokens.ForgeColors

/**
 * v7.4 (03) Szkielet: shown at the first start instead of a welcome screen, until the hero and the shelves are drawn. Shapes of the hero
 * (badge, title, meta, description, buttons) and a shelf of grey cards; the same positions as the finished screen so nothing jumps.
 */
@Composable
internal fun ForgeHomeSkeleton(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 109.dp)) {
            Bar(40.dp, 22.dp, 11.dp); Spacer(Modifier.height(8.dp))
            Bar(260.dp, 44.dp, 8.dp); Spacer(Modifier.height(8.dp))
            Bar(300.dp, 14.dp, 7.dp); Spacer(Modifier.height(8.dp))
            Bar(300.dp, 14.dp, 7.dp); Spacer(Modifier.height(6.dp))
            Bar(220.dp, 14.dp, 7.dp); Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Bar(160.dp, 44.dp, 22.dp); Bar(44.dp, 44.dp, 22.dp); Bar(44.dp, 44.dp, 22.dp)
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 0.dp)) {
            Bar(150.dp, 16.dp, 8.dp)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { repeat(5) { Bar(160.dp, 38.dp, 8.dp) } } // the top of the cards peeks in at the bottom
        }
    }
}

@Composable
private fun Bar(w: Dp, h: Dp, radius: Dp) {
    Box(Modifier.size(w, h).clip(RoundedCornerShape(radius)).background(ForgeColors.Surface2))
}
