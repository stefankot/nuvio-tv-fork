package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims

/**
 * Oznaczenie serialu w IPTV (decyzja właściciela 20.11, canvas `.stack`): pod kafelkiem dwie warstwy z tym samym obrazkiem,
 * węższe o 8/16 dp i niżej o 5/10 dp, przyciemnione 0,55 / 0,38; środkowa odwrócona do góry nogami, dolna obrócona o 90°
 * (obrazek dwa razy wyższy niż warstwa, wyśrodkowany). Bez napisu. [series] = false → sam kafelek, bez kosztu.
 */
@Composable
fun NiuniaSeriesStack(series: Boolean, image: String?, w: Dp, h: Dp, content: @Composable () -> Unit) {
    if (!series) { content(); return }
    Box(Modifier.size(w, h)) {
        Layer(image, w, w - 32.dp, h, y = 10.dp, dim = 0.38f, rotate = true)
        Layer(image, w, w - 16.dp, h, y = 5.dp, dim = 0.55f, rotate = false)
        content()
    }
}

@Composable
private fun Layer(image: String?, outer: Dp, w: Dp, h: Dp, y: Dp, dim: Float, rotate: Boolean) {
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    BoxWithConstraints(
        Modifier.padding(start = (outer - w) / 2).offset(y = y).size(w, h).clip(shape).background(NiuniaColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            if (rotate) {
                // Obrazek obrócony o 90°: po obrocie szerokość = wysokość warstwy ×2, wysokość = szerokość warstwy.
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.requiredSize(h * 2, w).graphicsLayer { rotationZ = 90f })
            } else {
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleY = -1f })
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 1f - dim)))
    }
}
