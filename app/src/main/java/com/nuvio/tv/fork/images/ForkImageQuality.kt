package com.nuvio.tv.fork.images

import android.util.Log
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.allowRgb565
import coil3.request.precision
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Size
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
 * 4. Pamięć (22.11, właściciel „zrób 1 i 2”): obrazek dekodowany dokładnie do miejsca na ekranie + [FOCUS_MARGIN] na
 *    powiększenie przy fokusie ([Precision.EXACT]; wcześniej skokami co 2×: plakat 2000 px dla 260 px → 500×750, ok. 1,5 MB,
 *    teraz ok. 300×450, ok. 0,5 MB). Pamięć podręczna na dysku większa ([diskCacheBytes]), bo pliki `original` są ~10× większe.
 * Zapas: gdy wersja `original` się nie wczyta — pierwotny adres.
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
        private const val FOCUS_MARGIN = 1.15f // plakat z fokusem i hero przy animacji są powiększane do ~1,1×
        private const val DISK_MAX = 1536L * 1024 * 1024
        @Volatile private var logged = 0

        /** Rozmiar do dekodowania: miejsce na ekranie × [FOCUS_MARGIN]; nieznany wymiar zostaje bez zmian. */
        fun decodeSize(size: Size): Size {
            val w = size.width; val h = size.height
            if (w !is Dimension.Pixels || h !is Dimension.Pixels) return size
            return Size(Math.round(w.px * FOCUS_MARGIN), Math.round(h.px * FOCUS_MARGIN))
        }

        /**
         * Pamięć obrazków na dysku: przy włączonej funkcji do 1,5 GB, ale najwyżej 1/4 wolnego miejsca i nie mniej niż
         * [default] (wyłączona = [default] Nuvio, 200 MB).
         */
        fun diskCacheBytes(c: android.content.Context, default: Long): Long {
            if (!isEnabled(c)) return default
            val free = try { c.cacheDir.usableSpace } catch (e: SecurityException) { Log.w(TAG, "wolne miejsce: ${e.javaClass.simpleName}: ${e.message}"); 0L }
            val bytes = maxOf(default, minOf(DISK_MAX, free / 4))
            Log.i(TAG, "pamięć obrazków na dysku: ${bytes / 1048576} MB (wolne ${free / 1048576} MB)")
            return bytes
        }

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
        val full = chain.request.newBuilder().allowRgb565(false).precision(Precision.EXACT).build()
        val sized = chain.withSize(decodeSize(chain.size))
        val url = full.data as? String ?: return sized.withRequest(full).proceed()
        val candidates = buildList {
            tmdbOriginal(url)?.let(::add)
            metahub(url)?.let { (kind, imdb, biggest) ->
                TmdbArtLookup.path(full.context, imdb, kind)?.let { add(TMDB_ORIGINAL + it) }
                biggest?.let(::add)
            }
        }
        for (big in candidates) {
            val result = sized.withRequest(full.newBuilder().data(big).build()).proceed()
            if (result !is ErrorResult) {
                if (logged < 8) { logged++; Log.i(TAG, "obrazek w najwyższej jakości: ${url.substringAfter("://").take(60)} → ${big.substringAfter("://").take(60)}") }
                return result
            }
            Log.w(TAG, "obrazek ${big.substringAfter("://").take(60)} nie wczytał się: ${result.throwable.javaClass.simpleName}: ${result.throwable.message} — następny zapas")
        }
        return sized.withRequest(full).proceed()
    }
}
