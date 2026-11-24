package com.nuvio.tv.skin.kicia.ambient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screensaver overlay of the mockup (S14): a big clock and the date on the lower-left of the photo,
 * over a soft bottom gradient. No help text. Any key closes the screensaver (that part is Reshaped's).
 */
@Composable
fun ForgeAmbientClock(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - (value % 60_000L) + 50L)
        }
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM", Locale("pl")).format(Date(now))
    Box(modifier.fillMaxSize()) {
        // Mockup S14 (b48): lower-left, clock top at y 380, over a soft bottom gradient.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(260.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .62f)))),
        )
        // UX JMGO (ręczne testy 24.11): logo i gatunki slajdu są w lewym dolnym rogu (AmbientSlideshow) — zegar je zasłaniał
        // („02:14” na logo „HEARTLAND”). Zegar i data w prawym dolnym rogu.
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = ForgeDims.ContentStart, bottom = 34.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(time, style = ForgeText.saverClock)
            Text(date, style = ForgeText.head.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
        }
    }
}
