package com.nuvio.tv.skin.kicia.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.nuvio.tv.core.image.CustomPosterFallbackInterceptor

/**
 * A poster drawn the way Nuvio's own cards draw it: the poster address (often a custom poster service) with
 * the plain poster as fallback, handed to Nuvio's [CustomPosterFallbackInterceptor] — so a poster service that
 * does not answer still shows the poster instead of an empty card. Nothing is drawn without an address.
 */
@Composable
fun ForgePosterImage(url: String?, fallbackUrl: String?, contentDescription: String?, modifier: Modifier = Modifier.fillMaxSize()) {
    val main = url?.takeIf { it.isNotBlank() } ?: fallbackUrl?.takeIf { it.isNotBlank() } ?: return
    val context = LocalContext.current
    val request = remember(main, fallbackUrl) {
        ImageRequest.Builder(context).data(main).apply {
            val fallback = fallbackUrl?.takeIf { it.isNotBlank() && it != main }
            if (fallback != null) memoryCacheKeyExtras(mapOf(CustomPosterFallbackInterceptor.FALLBACK_URL_KEY to fallback))
        }.build()
    }
    AsyncImage(model = request, contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = modifier.forgeHazeSource(LocalForgeCardHaze.current))
}
