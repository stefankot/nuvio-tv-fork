package com.nuvio.tv.fork.images

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.size.Dimension
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Lepsza jakość dużych obrazków (22.11, właściciel: „TMDB w rozmiarze original, bez kompresji — duże obrazki źle wyglądają
 * na 4K; Fanart.tv i inne jako zapas”).
 *
 * Dla obrazków rysowanych szerzej niż [LARGE_PX] (tło, hero, duże plakaty) adres TMDB `…/t/p/w1280/…` → `…/t/p/original/…`,
 * a metahub `…/background/medium/…` → `…/background/large/…`. Małe plakaty w rzędach zostają w swoim rozmiarze (szybciej,
 * mniej pamięci na JMGO). Gdy wersja duża się nie wczyta — zapas: pierwotny adres (z dodatku / TMDB). Fanart.tv wymaga
 * klucza API, którego aplikacja nie ma (plan/BLOKADY.md).
 */
class ForkImageQuality(private val enabled: () -> Boolean) : Interceptor {
    companion object {
        const val KEY = "images_original"
        const val LARGE_PX = 900
        private const val TAG = "ForkImages"
        private val TMDB = Regex("""^(https?://image\.tmdb\.org/t/p/)(w\d+|h\d+)(/.+)$""")
        private val METAHUB = Regex("""^(https?://images\.metahub\.space/(?:background|poster|logo)/)(small|medium)(/.+)$""")
        @Volatile private var logged = 0

        /** Adres w największej jakości albo null, gdy nie ma czego podnieść. */
        fun upgrade(url: String): String? =
            TMDB.find(url)?.let { m -> m.groupValues[1] + "original" + m.groupValues[3] }
                ?: METAHUB.find(url)?.let { m -> m.groupValues[1] + "large" + m.groupValues[3] }

        fun isEnabled(c: android.content.Context) = ForkFeatures.enabled(c, KEY, true)
    }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val url = chain.request.data as? String ?: return chain.proceed()
        val w = (chain.size.width as? Dimension.Pixels)?.px ?: 0
        if (w < LARGE_PX || !enabled()) return chain.proceed()
        val big = upgrade(url) ?: return chain.proceed()
        val result = chain.withRequest(chain.request.newBuilder().data(big).build()).proceed()
        if (result is ErrorResult) {
            Log.w(TAG, "duży obrazek (${w}px) nie wczytał się: ${result.throwable.javaClass.simpleName}: ${result.throwable.message} — zapas: pierwotny adres")
            return chain.proceed()
        }
        if (logged < 5) { logged++; Log.i(TAG, "obrazek ${w}px w pełnej jakości: ${big.substringAfter("://").take(80)}") }
        return result
    }
}
