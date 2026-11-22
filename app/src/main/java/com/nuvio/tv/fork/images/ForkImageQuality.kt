package com.nuvio.tv.fork.images

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.allowRgb565
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Najwyższa jakość obrazków (22.11, właściciel: „nie pobieraj z kaptain.stream, tylko TMDB w pełnej, najwyższej
 * rozdzielczości — plakaty, backdropy, wszystko”).
 *
 * 1. Pełny kolor: Nuvio domyślnie dekoduje zdjęcia w 16-bitowym kolorze (RGB565) — na tłach i ciemnych przejściach
 *    widać paski i „kompresję”. Przy włączonej funkcji każdy obrazek idzie w pełnym kolorze.
 * 2. Każdy adres TMDB (`w342`, `w500`, `w1280`, `h632`…) → `original`.
 * 3. Plakat / tło z metahub (Cinemeta, najwyżej 780 px plakatu i 1920 px tła, mocno skompresowane) → TMDB `original`
 *    po IMDb id ([TmdbArtLookup]); gdy TMDB nie zna tytułu — największy wariant metahub.
 * Zapas: gdy wersja `original` się nie wczyta — pierwotny adres. Coil i tak zmniejsza obraz do miejsca na ekranie.
 * Kaptain.stream (wzór plakatów Nuvio) wyłączony w ustawieniach JMGO 22.11 (zwracał 410).
 */
class ForkImageQuality(private val enabled: () -> Boolean) : Interceptor {
    companion object {
        const val KEY = "images_original"
        private const val TAG = "ForkImages"
        private val TMDB = Regex("""^(https?://image\.tmdb\.org/t/p/)(w\d+|h\d+)(/.+)$""")
        private val METAHUB = Regex("""^https?://images\.metahub\.space/(background|poster)/(small|medium|large)/(tt\d+)(/.*)?$""")
        private val METAHUB_MAX = mapOf("poster" to "large", "background" to "medium") // background large = medium (1920)
        const val TMDB_ORIGINAL = "https://image.tmdb.org/t/p/original"
        @Volatile private var logged = 0

        /** TMDB w dowolnym rozmiarze → `original`; null, gdy to nie TMDB albo już `original`. */
        fun tmdbOriginal(url: String): String? = TMDB.find(url)?.let { m -> m.groupValues[1] + "original" + m.groupValues[3] }

        /** Metahub → (rodzaj, IMDb id, największy wariant metahub) albo null. */
        fun metahub(url: String): Triple<String, String, String?>? = METAHUB.find(url)?.let { m ->
            val kind = m.groupValues[1]; val max = METAHUB_MAX[kind]
            val biggest = if (max != null && max != m.groupValues[2]) url.replace("/$kind/${m.groupValues[2]}/", "/$kind/$max/") else null
            Triple(kind, m.groupValues[3], biggest)
        }

        fun isEnabled(c: android.content.Context) = ForkFeatures.enabled(c, KEY, true)
    }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        if (!enabled()) return chain.proceed()
        val full = chain.request.newBuilder().allowRgb565(false).build()
        val url = full.data as? String ?: return chain.withRequest(full).proceed()
        val candidates = buildList {
            tmdbOriginal(url)?.let(::add)
            metahub(url)?.let { (kind, imdb, biggest) ->
                TmdbArtLookup.path(full.context, imdb, kind)?.let { add(TMDB_ORIGINAL + it) }
                biggest?.let(::add)
            }
        }
        for (big in candidates) {
            val result = chain.withRequest(full.newBuilder().data(big).build()).proceed()
            if (result !is ErrorResult) {
                if (logged < 8) { logged++; Log.i(TAG, "obrazek w najwyższej jakości: ${url.substringAfter("://").take(60)} → ${big.substringAfter("://").take(60)}") }
                return result
            }
            Log.w(TAG, "obrazek ${big.substringAfter("://").take(60)} nie wczytał się: ${result.throwable.javaClass.simpleName}: ${result.throwable.message} — następny zapas")
        }
        return chain.withRequest(full).proceed()
    }
}
