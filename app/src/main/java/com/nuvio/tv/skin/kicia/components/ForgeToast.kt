package com.nuvio.tv.skin.kicia.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * v7.4 Komunikat (SYSTEM): pill 36 dp, 14/500, glass rgba(0,0,0,.38), 3 s, never focusable.
 * Bottom variant at y 464 (centred), top variant at y 76 (when the bottom is busy, e.g. players).
 * Any screen calls [ForgeToast.show]; one [ForgeToastHost] (in the scaffold) draws it.
 */
object ForgeToast {
    data class Message(val id: Long, val text: String, val top: Boolean, val note: String? = null)

    internal val current = MutableStateFlow<Message?>(null)
    private var counter = 0L

    /** [note]: v7.6 — a plain-language explanation under the title, smaller. */
    fun show(text: String, top: Boolean = false, note: String? = null) { current.value = Message(++counter, text, top, note) }
    const val DurationMs = 3_000L
}

@Composable
fun ForgeToastHost(modifier: Modifier = Modifier) {
    val message by ForgeToast.current.collectAsState()
    LaunchedEffect(message?.id) {
        val id = message?.id ?: return@LaunchedEffect
        delay(ForgeToast.DurationMs)
        if (ForgeToast.current.value?.id == id) ForgeToast.current.value = null
    }
    Box(modifier.fillMaxSize()) {
        val m = message
        AnimatedVisibility(
            visible = m != null,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(if (m?.top == true) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(top = 76.dp, bottom = 40.dp), // y 76 (top) or y 464 (bottom: 540 - 40 - 36)
        ) {
            if (m?.note != null) ForgeToastCard(m.text, m.note) else ForgeToastPill(m?.text.orEmpty())
        }
    }
}

/** v7.4 Komunikat: 36 dp, pełna kapsuła, tło --panel, tekst 14/20 700, odstęp boczny 16. */
@Composable
fun ForgeToastPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ForgeText.label.copy(color = Color.White),
        maxLines = 1,
        modifier = modifier.height(36.dp).clip(CircleShape).background(com.nuvio.tv.skin.kicia.tokens.ForgeColors.Sheet).forgeEdge(CircleShape).padding(horizontal = 16.dp, vertical = 8.dp), // makieta 7.7 A8
    )
}

/** v7.6 Komunikat z wyjaśnieniem: tytuł 14/700 i pod nim 12 mniejszym fontem (po ludzku), max 2 linie wyjaśnienia. */
@Composable
fun ForgeToastCard(title: String, note: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(
        modifier.widthIn(max = 560.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(com.nuvio.tv.skin.kicia.tokens.ForgeColors.Sheet).forgeEdge(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 8.dp), // makieta 7.7 A8
    ) {
        Text(title, style = ForgeText.label.copy(color = Color.White), maxLines = 1)
        Text(note, style = ForgeText.caption.copy(color = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Text3), maxLines = 2, modifier = Modifier.padding(top = 2.dp))
    }
}
