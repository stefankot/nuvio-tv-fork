package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.nuvio.tv.fork.images.NativeBlur
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Przegląd 27.11 (CAŁA APLIKACJA): tło pełnoekranowe = gotowa rozmyta kopia obrazu (liczona raz, pamięć Coil + dysk, ten sam
 * klucz co tło pod hero szczegółów — zwykle już policzona) z lekkim czarnym przyciemnieniem zamiast ostrego obrazu pod szarą
 * lub czarną warstwą. Zwraca false, gdy wyłączone (klucz `niunia_blur_scrims`) albo brak gotowego rozmycia — wtedy wołający
 * rysuje dawne tło.
 */
@Composable
internal fun BoxScope.niuniaBlurBackdrop(url: String?, what: String, dim: Color = Color(0x73000000)): Boolean {
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { 60.dp.toPx() }
    val req = remember(url) {
        if (url.isNullOrBlank() || !ForkFeatures.enabled(ctx, "niunia_blur_scrims", true) || !NativeBlur.enabled(ctx)) null
        else ImageRequest.Builder(ctx).data(url).transformations(NativeBlur(px, diskKey = url)).build()
            .also { Log.i("ForkNiunia", "$what: tło = rozmyty obraz zamiast przyciemnienia") }
    } ?: return false
    AsyncImage(model = req, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.08f; scaleY = 1.08f })
    Box(Modifier.fillMaxSize().background(dim))
    return true
}
