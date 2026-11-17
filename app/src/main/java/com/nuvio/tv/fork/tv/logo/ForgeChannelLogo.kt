package com.nuvio.tv.fork.tv.logo

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.request.allowHardware

/**
 * How a channel logo is turned black-and-white WITHOUT a plate behind it (owner's decision).
 * The logo files are never edited; the filter is applied when drawing.
 *
 * - [Knockout] (default): coloured and dark parts become white, white parts become transparent
 *   (e.g. the letters of a white-on-blue logo cut out of the blue field).
 * - [Silhouette]: the whole logo becomes a white shape (logos that are mostly white with colour accents).
 * - [Light]: for a white wordmark on a coloured field with white text (e.g. Canal+ Film): the light
 *   parts stay white, the field disappears.
 */
enum class ForgeLogoMode { Knockout, Silhouette, Light }

object ForgeLogoFilters {
    private val Weights = floatArrayOf(0.680f, 2.289f, 0.231f)

    /** 4x5 colour matrix in Android's 0..255 convention (offsets in 0..255 units). */
    fun matrix(mode: ForgeLogoMode): FloatArray {
        val white = floatArrayOf(
            0f, 0f, 0f, 0f, 255f,
            0f, 0f, 0f, 0f, 255f,
            0f, 0f, 0f, 0f, 255f,
        )
        val alpha = when (mode) {
            ForgeLogoMode.Knockout -> floatArrayOf(-Weights[0], -Weights[1], -Weights[2], 3.2f, 0f)
            ForgeLogoMode.Silhouette -> floatArrayOf(0f, 0f, 0f, 1f, 0f)
            ForgeLogoMode.Light -> floatArrayOf(Weights[0], Weights[1], Weights[2], 1f, -3.2f * 255f)
        }
        return white + alpha
    }

    /** Resulting alpha (0..1) for a source pixel (each 0..1), the same maths the matrix performs. Used by tests. */
    fun alphaFor(mode: ForgeLogoMode, r: Float, g: Float, b: Float, a: Float): Float {
        val lum = Weights[0] * r + Weights[1] * g + Weights[2] * b
        val raw = when (mode) {
            ForgeLogoMode.Knockout -> 3.2f * a - lum
            ForgeLogoMode.Silhouette -> a
            ForgeLogoMode.Light -> lum + a - 3.2f
        }
        return raw.coerceIn(0f, 1f)
    }
}

@Composable
fun ForgeChannelLogo(
    model: Any?,
    modifier: Modifier = Modifier,
    mode: ForgeLogoMode = ForgeLogoMode.Knockout,
    contentDescription: String? = null,
) {
    val filter = remember(mode) { ColorFilter.colorMatrix(ColorMatrix(ForgeLogoFilters.matrix(mode))) }
    Box(modifier) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = filter,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/** Ready-made filters, so a screen can apply one without building the matrix each time. */
object ForgeLogoFilter {
    val knockout: ColorFilter = ColorFilter.colorMatrix(ColorMatrix(ForgeLogoFilters.matrix(ForgeLogoMode.Knockout)))
    val silhouette: ColorFilter = ColorFilter.colorMatrix(ColorMatrix(ForgeLogoFilters.matrix(ForgeLogoMode.Silhouette)))
    val light: ColorFilter = ColorFilter.colorMatrix(ColorMatrix(ForgeLogoFilters.matrix(ForgeLogoMode.Light)))
}

/**
 * Picks the filter from the logo itself (owner: black-and-white logos without a plate):
 * - transparent corners, or a white plate → [ForgeLogoMode.Knockout] (the plate disappears, the marks turn white);
 * - a coloured or dark plate (IPTV "tile" logos with light lettering) → [ForgeLogoMode.Light]
 *   (the light lettering stays white, the plate disappears). Knockout would turn such a plate into a white square.
 * [corners] are the four corner pixels as ARGB ints, optionally followed by four points 20% in (rounded tiles).
 */
fun forgeLogoModeFor(corners: IntArray): ForgeLogoMode {
    var plate = 0
    var dark = 0
    for (c in corners) {
        val a = (c ushr 24) and 0xFF
        if (a < 230) continue
        plate++
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
        if (lum < 0.8f) dark++
    }
    if (plate >= 3 && dark >= 3) return ForgeLogoMode.Light
    // Rounded tiles (IPTV): the corners are transparent, so 4 more points 20% in tell a dark plate apart.
    if (corners.size >= 8) {
        val inner = (4 until 8).count { k ->
            val c = corners[k]
            val lum = (0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
            ((c ushr 24) and 0xFF) >= 230 && lum < 0.8f
        }
        if (inner == 4) return ForgeLogoMode.Light
    }
    return ForgeLogoMode.Knockout
}

/**
 * IPTV "white tile on a dark square" logos: the dark outer square has a light, solid inner tile.
 * The light filter would leave that tile as a white plate, so the inner tile is found and drawn alone with
 * [ForgeLogoMode.Knockout] (the tile disappears, its dark marks turn white).
 * Scans the middle row and column from the edges inward to the first light pixel, then checks points 20% in
 * from the four corners of that rectangle are light too (a solid, possibly rounded tile, not letters). Returns [left, top, right, bottom] or null.
 */
fun forgeLogoInnerTile(width: Int, height: Int, pixelAt: (Int, Int) -> Int): IntArray? {
    if (width < 12 || height < 12) return null
    fun light(c: Int): Boolean {
        if (((c ushr 24) and 0xFF) < 230) return false
        val lum = (0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
        return lum >= 0.8f
    }
    val corners = intArrayOf(pixelAt(2, 2), pixelAt(width - 3, 2), pixelAt(2, height - 3), pixelAt(width - 3, height - 3))
    if (forgeLogoModeFor(corners) != ForgeLogoMode.Light) return null
    val midY = height / 2
    val midX = width / 2
    val l = (2 until width - 2).firstOrNull { light(pixelAt(it, midY)) } ?: return null
    val r = (width - 3 downTo 2).firstOrNull { light(pixelAt(it, midY)) } ?: return null
    val t = (2 until height - 2).firstOrNull { light(pixelAt(midX, it)) } ?: return null
    val b = (height - 3 downTo 2).firstOrNull { light(pixelAt(midX, it)) } ?: return null
    if (r - l < width * 0.4f || b - t < height * 0.4f) return null
    // 20% in from the corners: IPTV tiles are rounded, so the very corners are still the dark square.
    val inset = maxOf(2, (minOf(r - l, b - t) * 0.2f).toInt())
    val solid = light(pixelAt(l + inset, t + inset)) && light(pixelAt(r - inset, t + inset)) &&
        light(pixelAt(l + inset, b - inset)) && light(pixelAt(r - inset, b - inset))
    return if (solid) intArrayOf(l, t, r, b) else null
}

/** Reads the corner pixels of a decoded logo (2 px in from each edge); null when the pixels can't be read. */
fun forgeLogoCorners(bitmap: android.graphics.Bitmap): IntArray? = runCatching {
    val hardware = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
        bitmap.config == android.graphics.Bitmap.Config.HARDWARE
    if (hardware || bitmap.width < 5 || bitmap.height < 5) return null
    val x1 = 2; val y1 = 2; val x2 = bitmap.width - 3; val y2 = bitmap.height - 3
    val ix1 = bitmap.width / 5; val iy1 = bitmap.height / 5; val ix2 = bitmap.width - 1 - ix1; val iy2 = bitmap.height - 1 - iy1
    intArrayOf(
        bitmap.getPixel(x1, y1), bitmap.getPixel(x2, y1), bitmap.getPixel(x1, y2), bitmap.getPixel(x2, y2),
        bitmap.getPixel(ix1, iy1), bitmap.getPixel(ix2, iy1), bitmap.getPixel(ix1, iy2), bitmap.getPixel(ix2, iy2), // 20% in
    )
}.getOrNull()

/** The inner tile of a decoded logo as its own image ([forgeLogoInnerTile]); null when there is none. */
private fun forgeLogoTileBitmap(bitmap: android.graphics.Bitmap): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
    val hardware = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
        bitmap.config == android.graphics.Bitmap.Config.HARDWARE
    if (hardware) return null
    val box = forgeLogoInnerTile(bitmap.width, bitmap.height) { x, y -> bitmap.getPixel(x, y) } ?: return null
    android.graphics.Bitmap.createBitmap(bitmap, box[0], box[1], box[2] - box[0] + 1, box[3] - box[1] + 1)
        .asImageBitmap()
}.getOrNull()

/**
 * The logo without its transparent margin (EPG.OVH logos are 330x198 canvases with the mark small in the middle, so they drew
 * small and tall); null when there is nothing to cut or the pixels can't be read.
 */
fun forgeLogoTrim(bitmap: android.graphics.Bitmap): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
    val hardware = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
        bitmap.config == android.graphics.Bitmap.Config.HARDWARE
    if (hardware || bitmap.width < 8 || bitmap.height < 8) return null
    val w = bitmap.width; val h = bitmap.height
    val px = IntArray(w * h); bitmap.getPixels(px, 0, w, 0, 0, w, h)
    var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
    for (y in 0 until h) for (x in 0 until w) if ((px[y * w + x] ushr 24) >= 24) {
        if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
    }
    if (x1 < x0 || y1 < y0) return null
    val m = 2
    val l = (x0 - m).coerceAtLeast(0); val t = (y0 - m).coerceAtLeast(0)
    val r = (x1 + m).coerceAtMost(w - 1); val b = (y1 + m).coerceAtMost(h - 1)
    if ((r - l + 1) * (b - t + 1) > w * h * 0.85f) return null
    android.graphics.Bitmap.createBitmap(bitmap, l, t, r - l + 1, b - t + 1).asImageBitmap()
}.getOrNull()

/**
 * A channel logo drawn black-and-white without a plate, choosing the filter per logo ([forgeLogoModeFor]).
 * Takes the screen's own image request (headers, size, fetcher); pixels are read in software so the corners
 * can be checked (logos are small, decoded at the size they are drawn).
 */
@Composable
fun ForgeAutoLogo(
    request: coil3.request.ImageRequest,
    modifier: Modifier = Modifier,
    onError: () -> Unit = {},
) {
    var mode by androidx.compose.runtime.remember(request.data) { androidx.compose.runtime.mutableStateOf(ForgeLogoMode.Knockout) }
    var tile by androidx.compose.runtime.remember(request.data) {
        androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    var trimmed by androidx.compose.runtime.remember(request.data) {
        androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    // Decoded larger than the box, so that cutting the margin still leaves enough pixels.
    val softwareRequest = remember(request) { request.newBuilder().allowHardware(false).size(coil3.size.Size(480, 288)).build() }
    val currentFilter = when (mode) {
        ForgeLogoMode.Knockout -> ForgeLogoFilter.knockout
        ForgeLogoMode.Silhouette -> ForgeLogoFilter.silhouette
        ForgeLogoMode.Light -> ForgeLogoFilter.light
    }
    tile?.let {
        androidx.compose.foundation.Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ForgeLogoFilter.knockout,
            modifier = modifier,
        )
        return
    }
    trimmed?.let {
        androidx.compose.foundation.Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, colorFilter = currentFilter, modifier = modifier)
        return
    }
    AsyncImage(
        model = softwareRequest,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = when (mode) {
            ForgeLogoMode.Knockout -> ForgeLogoFilter.knockout
            ForgeLogoMode.Silhouette -> ForgeLogoFilter.silhouette
            ForgeLogoMode.Light -> ForgeLogoFilter.light
        },
        onSuccess = { state ->
            val bitmap = (state.result.image as? coil3.BitmapImage)?.bitmap
            bitmap?.let(::forgeLogoCorners)?.let { mode = forgeLogoModeFor(it) }
            if (bitmap != null && mode == ForgeLogoMode.Light) tile = forgeLogoTileBitmap(bitmap)
            if (bitmap != null && tile == null) trimmed = forgeLogoTrim(bitmap)
        },
        onError = { onError() },
        modifier = modifier,
    )
}

/** [ForgeAutoLogo] for a plain logo address (screens that have no image request of their own). */
@Composable
fun ForgeAutoLogo(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val request = remember(url) { coil3.request.ImageRequest.Builder(context).data(url).build() }
    Box(modifier.then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)) {
        ForgeAutoLogo(request = request, modifier = Modifier.matchParentSize())
    }
}

/**
 * Freely: a channel logo on a programme picture sits in a soft dark shadow (it stays legible on a bright frame).
 * The shadow is a radial dark glow drawn larger than the logo's box, under the white logo.
 */
@Composable
fun ForgeLogoWithShadow(url: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { scaleX = 1.9f; scaleY = 2.3f }
                .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), androidx.compose.ui.graphics.Color.Transparent))),
        )
        ForgeAutoLogo(url = url, contentDescription = contentDescription, modifier = Modifier.matchParentSize())
    }
}
