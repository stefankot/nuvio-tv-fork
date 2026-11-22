package com.nuvio.tv.fork.images

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.allowRgb565
import coil3.size.Dimension
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Lepsza jakość obrazków (22.11, właściciel: „TMDB w rozmiarze original, bez kompresji — duże obrazki źle wyglądają
 * na 4K; Fanart.tv i inne jako zapas”).
 *
 * 1. Pełny kolor: Nuvio domyślnie dekoduje zdjęcia w 16-bitowym kolorze (RGB565, „mniej pamięci”) — na dużych tłach
 *    i ciemnych przejściach widać paski i „kompresję”. Przy włączonej funkcji każdy obrazek idzie w pełnym kolorze.
 * 2. Rozmiar do miejsca na ekranie (poprawka 22.11 po teście „obrazy nadal słabej jakości”): wcześniej podnoszone były
 *    tylko obrazki ≥ [LARGE_PX], więc plakaty w rzędach zostawały w metahub `small` (300 px, mocno skompresowane), choć
 *    rysowane są na ok. 260–290 px. Teraz wybierany jest najmniejszy wariant ≥ 1,25 × szerokość na ekranie
 *    (TMDB w342/w500/w780/w1280/original, metahub plakat small 300/medium 500/large 780, tło small 480/medium 1920).
 *    Duże (≥ [LARGE_PX]) z TMDB zawsze `original`. Metahub tło `large` = `medium` (1920 px, sprawdzone 22.11).
 * Zapas: gdy większa wersja się nie wczyta — pierwotny adres. Fanart.tv wymaga klucza (plan/BLOKADY.md).
 */
class ForkImageQuality(private val enabled: () -> Boolean) : Interceptor {
    companion object {
        const val KEY = "images_original"
        const val LARGE_PX = 900
        private const val MARGIN = 1.25
        private const val TAG = "ForkImages"
        private val TMDB = Regex("""^(https?://image\.tmdb\.org/t/p/)(w\d+|h\d+|original)(/.+)$""")
        private val METAHUB = Regex("""^(https?://images\.metahub\.space/)(background|poster|logo)/(small|medium|large)(/.+)$""")
        private val TMDB_SIZES = listOf(342, 500, 780, 1280)
        private val METAHUB_W = mapOf(
            "poster" to listOf("small" to 300, "medium" to 500, "large" to 780),
            "background" to listOf("small" to 480, "medium" to 1920),
            "logo" to listOf("small" to 800),
        )
        @Volatile private var logged = 0

        /** Adres w jakości wystarczającej dla [widthPx] pikseli na ekranie albo null, gdy nie ma czego podnieść. */
        fun upgrade(url: String, widthPx: Int = Int.MAX_VALUE): String? {
            val need = if (widthPx >= LARGE_PX) Int.MAX_VALUE else (widthPx * MARGIN).toInt()
            TMDB.find(url)?.let { m ->
                val cur = m.groupValues[2]
                if (cur == "original") return null
                val have = if (cur.startsWith("h")) 0 else cur.drop(1).toIntOrNull() ?: 0 // hNNN = wysokość → zawsze w górę
                if (have >= need) return null
                val pick = TMDB_SIZES.firstOrNull { it >= need && it > have }?.let { "w$it" } ?: "original"
                return m.groupValues[1] + pick + m.groupValues[3]
            }
            METAHUB.find(url)?.let { m ->
                val variants = METAHUB_W[m.groupValues[2]] ?: return null
                val have = variants.firstOrNull { it.first == m.groupValues[3] }?.second ?: return null
                if (have >= need) return null
                val pick = (variants.firstOrNull { it.second >= need } ?: variants.last()).takeIf { it.second > have } ?: return null
                return m.groupValues[1] + m.groupValues[2] + "/" + pick.first + m.groupValues[4]
            }
            return null
        }

        fun isEnabled(c: android.content.Context) = ForkFeatures.enabled(c, KEY, true)
    }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        if (!enabled()) return chain.proceed()
        val full = chain.request.newBuilder().allowRgb565(false).build()
        val url = full.data as? String ?: return chain.withRequest(full).proceed()
        val w = (chain.size.width as? Dimension.Pixels)?.px ?: 0
        val big = if (w > 0) upgrade(url, w) else null
        if (big == null) return chain.withRequest(full).proceed()
        val result = chain.withRequest(full.newBuilder().data(big).build()).proceed()
        if (result is ErrorResult) {
            Log.w(TAG, "większy obrazek (${w}px) nie wczytał się: ${result.throwable.javaClass.simpleName}: ${result.throwable.message} — zapas: pierwotny adres")
            return chain.withRequest(full).proceed()
        }
        if (logged < 8) { logged++; Log.i(TAG, "obrazek ${w}px, pełny kolor: ${url.substringAfter("://").take(60)} → ${big.substringAfter("://").take(60)}") }
        return result
    }
}
