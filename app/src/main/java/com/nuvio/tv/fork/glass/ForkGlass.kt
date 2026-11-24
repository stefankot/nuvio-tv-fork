package com.nuvio.tv.fork.glass

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.builditcode.glass.BackdropFilter
import com.builditcode.glass.BackdropLayerManager
import com.builditcode.glass.LocalBackdropLayerManager
import com.builditcode.glass.glassBorder
import com.builditcode.glass.layeredBackdropCapture
import com.builditcode.glass.layeredBackdropSource
import com.builditcode.glass.rememberBackdropManager
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay

/**
 * Szkło (biblioteka LiquidGlass 0.8.2, BuildItCode): przezroczyste menu, panele, przyciski i okna nad treścią ekranu.
 * Wartości 1:1 z makiety `~/Desktop/nuvio-tv-interface/glass-lab` (GlassSettings.kt, HeroScreen.kt, ControlPanel.kt).
 *
 * Warstwy (biblioteka nie rysuje szkła wewnątrz warstwy, którą próbkuje):
 *  - [LAYER_SCREEN]: cała treść ekranów (NavHost, wpięcie w NuvioNavHost). Próbkują ją powierzchnie leżące POZA ekranem:
 *    nawigacja, okna dialogowe (osobne okna), komunikaty.
 *  - [LAYER_BACK]: tło ekranu (obraz hero ze scrimem; [backdrop]). Próbkują ją powierzchnie w treści ekranu: przyciski, panele.
 * Powierzchnia bez dostępnej warstwy (np. nad wideo w SurfaceView) rysuje zwykłe tło z tokenów skórki i obwódkę.
 *
 * Tylko API 33+ (AGSL na GPU). Na API < 33 biblioteka liczy szkło procesorem w każdej klatce — zakazane (ZASADY §2):
 * tam ustawienie jest nieaktywne, a wszystko rysuje się zwykłym tłem z tokenów; biblioteka nie jest wtedy używana.
 */
object ForkGlass {
    const val TAG = "ForkGlass"
    const val KEY = "fork_glass"
    const val LAYER_SCREEN = "fork_ekran"
    const val LAYER_BACK = "fork_tlo"

    /** Szkło na tym urządzeniu (GPU, AGSL). */
    val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Preset 1:1 z makiety: rozmycie / załamanie / rozszczepienie / krawędź / odcień / obwódka (szer., przerwy). */
    enum class Preset(
        val id: String,
        val label: Int,
        val blur: Float,
        val refraction: Float,
        val dispersion: Float,
        val edge: Float,
        val tint: Color,
        val borderWidth: Float,
        val borderGap: Float,
        /** Odcień dużych powierzchni z tekstem (kontrast ≥ 4,5:1 białego tekstu także na białym tle). */
        val textTint: Color,
        val textBlur: Float,
    ) {
        CLEAR("CLEAR", R.string.glass_preset_clear, 1.5f, 0.38f, 0.12f, 0.32f, Color.White.copy(alpha = 0.06f), 1f, 0.18f,
            Color(0xFF0B0B0D).copy(alpha = 0.72f), 8f),
        GRAPHITE("GRAPHITE", R.string.glass_preset_graphite, 5f, 0.14f, 0.08f, 0.20f, Color(0xFF161618).copy(alpha = 0.45f), 1f, 0.15f,
            Color(0xFF161618).copy(alpha = 0.72f), 5f),
        FROST("FROST", R.string.glass_preset_frost, 8f, 0.08f, 0.04f, 0.12f, Color.White.copy(alpha = 0.14f), 1f, 0.20f,
            Color(0xFF2C2C2E).copy(alpha = 0.70f), 8f),
    }

    /** Rola powierzchni: kształt z tokenów skórki, warstwa, którą próbkuje, i czy niesie dużo tekstu. */
    enum class Role(val layer: String, val textHeavy: Boolean, private val radiusDp: Int) {
        Button(LAYER_BACK, false, -1),
        Chip(LAYER_BACK, false, -1),
        Panel(LAYER_BACK, true, 28),
        Nav(LAYER_SCREEN, true, 28),
        Dialog(LAYER_SCREEN, true, 28),
        Menu(LAYER_SCREEN, true, 16),
        Toast(LAYER_SCREEN, true, -1);

        fun shape(): Shape = if (radiusDp < 0) CircleShape else RoundedCornerShape(radiusDp.dp)
    }

    /** Wybór z ustawień (OFF / CLEAR / GRAPHITE / FROST); stan, więc zmiana działa od razu. */
    var choice by mutableStateOf("CLEAR")
        private set
    private var loaded = false

    /** Aktywny preset albo null (wyłączone ustawieniem albo API < 33). */
    val active: Preset?
        get() = if (!supported) null else Preset.entries.firstOrNull { it.id == choice }

    /** Jedyny manager aktywności (tworzy [Host]); nawigacja i nakładki spoza NavHost dostają go przez [Provide]. */
    var manager by mutableStateOf<BackdropLayerManager?>(null)
        private set

    private var surfaces by mutableIntStateOf(0)

    fun load(c: Context) {
        if (loaded) return
        loaded = true
        choice = ForkFeatures.str(c, KEY, "CLEAR")
        if (!supported) Log.i(TAG, "szkło wyłączone: API ${Build.VERSION.SDK_INT} < 33 (biblioteka liczyłaby procesorem) — zwykłe tło z tokenów")
        else if (active == null) Log.i(TAG, "szkło wyłączone: ustawienie ($choice)")
        else Log.i(TAG, "szkło: preset ${active?.id}")
    }

    /** Kolejny wybór w ustawieniach: Wyłączone → Czyste szkło → Nuvio grafit → Mróz → Wyłączone. */
    fun cycle(c: Context) {
        load(c)
        if (!supported) {
            Log.i(TAG, "ustawienie Szkło: niedostępne (API ${Build.VERSION.SDK_INT} < 33), bez zmiany")
            return
        }
        val order = listOf("OFF") + Preset.entries.map { it.id }
        val next = order[(order.indexOf(choice) + 1).mod(order.size)]
        Log.i(TAG, "ustawienie Szkło: $choice → $next" + if (next == "OFF") " (szkło wyłączone ustawieniem)" else "")
        choice = next
        ForkFeatures.setStr(c, KEY, next)
    }

    /** Tekst wartości w ustawieniach. */
    fun label(c: Context): String {
        load(c)
        if (!supported) return c.getString(R.string.glass_unavailable)
        return active?.let { c.getString(it.label) } ?: c.getString(R.string.glass_off)
    }

    /**
     * Wpięcie NuvioNavHost: jeden manager (skala 0,5, 16 ms) i warstwa [LAYER_SCREEN] na całej treści ekranów.
     * Wyłączone (ustawienie albo API < 33): sama treść, bez managera i bez warstw (biblioteka nieużyta).
     */
    @Composable
    fun Host(content: @Composable () -> Unit) {
        val c = LocalContext.current
        remember(c) { load(c) }
        val p = active
        val m = if (p != null) rememberBackdropManager(defaultScaleFactor = 0.5f, defaultDebounceMs = 16L) else null
        if (m != null) DisposableEffect(m) {
            manager = m
            Log.i(TAG, "start managera: skala 0.5, 16 ms, preset ${p?.id}")
            onDispose { if (manager === m) manager = null; Log.i(TAG, "manager zatrzymany") }
        }
        if (m != null) LaunchedEffect(m) {
            var last = -1
            snapshotFlow { surfaces }.collect { n ->
                delay(1500)
                if (n != last) { last = n; Log.i(TAG, "powierzchnie ze szkłem na ekranie: $n (preset ${active?.id})") }
            }
        }
        CompositionLocalProvider(LocalBackdropLayerManager provides m) {
            Box(if (m != null) Modifier.fillMaxSize().layeredBackdropSource(LAYER_SCREEN) else Modifier.fillMaxSize()) { content() }
        }
    }

    /** Nawigacja i nakładki rysowane poza NavHost: ten sam manager co ekrany. */
    @Composable
    fun Provide(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalBackdropLayerManager provides (if (active != null) manager else null), content = content)
    }

    /** Tło ekranu (obraz hero + scrim) jako warstwa [LAYER_BACK] dla przycisków i paneli w treści. */
    fun Modifier.forkGlassBackdrop(): Modifier = if (active != null && manager != null) layeredBackdropSource(LAYER_BACK) else this

    /**
     * Szkło powierzchni [role]. Wyłączone / API < 33: dokładnie dzisiejszy wygląd ([fallback] + [fallbackBorder] 1 dp).
     * Włączone: obwódka szkła, pod spodem [fallback] (widoczny tylko, gdy warstwy nie ma: wideo, własna warstwa) i próbka warstwy.
     * Fokus rysuje wywołujący PO tym modyfikatorze (białe wypełnienie na szkle).
     */
    @Composable
    fun Modifier.forkGlass(
        role: Role,
        shape: Shape = role.shape(),
        fallback: Color,
        fallbackBorder: Color? = null,
        layer: String = role.layer,
    ): Modifier {
        val p = active
        val m = LocalBackdropLayerManager.current
        if (p == null || m == null) {
            val base = this.clip(shape).background(fallback)
            return if (fallbackBorder != null) base.border(1.dp, fallbackBorder, shape) else base
        }
        DisposableEffect(Unit) { surfaces++; onDispose { surfaces-- } }
        val filter = remember(p, role, shape) {
            if (role.textHeavy) BackdropFilter.Glass(
                blurRadiusIntensity = maxOf(p.blur, p.textBlur), refraction = minOf(p.refraction, 0.1f), dispersion = minOf(p.dispersion, 0.05f),
                edge = minOf(p.edge, 0.15f), tint = p.textTint, shape = shape,
            ) else BackdropFilter.Glass(
                blurRadiusIntensity = p.blur, refraction = p.refraction, dispersion = p.dispersion, edge = p.edge, tint = p.tint, shape = shape,
            )
        }
        return this
            .glassBorder(shape = shape, borderColor = Color.White.copy(alpha = 0.8f), borderWidth = p.borderWidth.dp, gapSize = p.borderGap, softness = 0.05f)
            .clip(shape)
            .background(fallback)
            .layeredBackdropCapture(layerName = layer, filter = filter)
    }

    /** Wpięcie NuvioDialog (wspólne okno bazy): szkło nad zwykłym tłem okna; wyłączone = bez zmian. */
    @Composable
    fun dialogSurface(shape: Shape): Modifier = if (active == null) Modifier else Modifier.forkGlass(Role.Dialog, shape, fallback = Color.Transparent)

    // ------------------------------------------------------------------ komunikaty (ForkLog.say)

    private var toast by mutableStateOf<Pair<String, Long>?>(null)
    private var toastHost = false

    /** ForkLog.say: true = komunikat pokaże szkło (nakładka jest na ekranie), false = zwykły Toast systemu. */
    fun showToast(msg: String): Boolean {
        if (!toastHost || active == null || manager == null) return false
        toast = msg to System.nanoTime()
        return true
    }

    /** Nakładka komunikatu (ForkHooks.moduleOverlays, nad całą aplikacją): pigułka ze szkła u góry ekranu. */
    @Composable
    fun ToastOverlay() {
        DisposableEffect(Unit) { toastHost = true; onDispose { toastHost = false } }
        val t = toast ?: return
        LaunchedEffect(t) { delay(3500); if (toast === t) toast = null }
        Provide {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                Box(
                    Modifier.padding(top = 24.dp).heightIn(min = 48.dp).widthIn(max = 640.dp)
                        .forkGlass(Role.Toast, fallback = Color(0xF03A3A3C))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    androidx.compose.material3.Text(
                        t.first, color = Color.White, maxLines = 2,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}
