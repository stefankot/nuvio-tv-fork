package com.nuvio.tv.fork.tv.ekrany

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.fork.tv.WatchState
import com.nuvio.tv.fork.tv.most.TvBridge

/** Znacznik obejrzenia i cienki pasek postępu w komórce przewodnika (B03-4). Brak śladu / moduł wyłączony = nic. */
@Composable
fun CellMark(scope: BoxScope, title: String, startMs: Long) = scope.run {
    val mark = remember(title, startMs) { TvBridge.data?.markFor(title, startMs) } ?: return@run
    val (state, progress) = mark
    if (state == WatchState.WATCHED) {
        BasicText("✓", style = TextStyle(color = Color(0xFF7BD88F), fontSize = 12.sp), modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 2.dp))
    } else if (progress > 0f) {
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).drawBehind {
            drawRect(Color(0xFF7BD88F), size = Size(size.width * progress, size.height))
        })
    }
}
