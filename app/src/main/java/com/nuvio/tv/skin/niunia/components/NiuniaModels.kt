package com.nuvio.tv.skin.niunia.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing

/*
 * Logika komponentów design systemu Nuvio TV v2 w czystym Kotlinie (bez Compose UI), żeby testy JVM mogły ją sprawdzić:
 * presety kart (NMediaCard), pasek postępu (NInput seek), geometria EPG (NEpgGrid), mapa fokusu ekranów (NScreen).
 */

// ---------------------------------------------------------------- NMediaCard

/** Jak karta reaguje na fokus (focus-and-keys.md). */
enum class NiuniaFocusStyle {
    /** Karty z obrazem: uniesienie 4 dp, skala 1,1, cień i biała obwódka 3 dp. */
    Lift,
    /** Przyciski: białe wypełnienie, skala 1,08. */
    Button,
    /** Wiersze, pigułki, klawisze, komórki EPG, karty tekstowe: białe wypełnienie bez skali. */
    Fill,
}

enum class NiuniaCardKind { Poster, Spotlight, Landscape, Wide, Episode, Person, Profile, Channel, Text, Result, Trailer, Tile }

enum class NiuniaCardState { Ready, Loading, Error, Empty }

/** Presety NMediaCard (presets.md, nuvio.js CARD_PRESETS). */
enum class NiuniaCardPreset(val kind: NiuniaCardKind) {
    PosterCard(NiuniaCardKind.Poster), ShelfPosterCard(NiuniaCardKind.Poster), LandscapeCard(NiuniaCardKind.Landscape),
    ContinueWatchingCard(NiuniaCardKind.Landscape), NowCard(NiuniaCardKind.Landscape), EpisodeCard(NiuniaCardKind.Episode),
    StillCard(NiuniaCardKind.Wide), CastCard(NiuniaCardKind.Person), ProfileCard(NiuniaCardKind.Profile),
    SpotlightCard(NiuniaCardKind.Spotlight), ChannelCard(NiuniaCardKind.Channel), ChapterCard(NiuniaCardKind.Text),
    ScheduleCard(NiuniaCardKind.Text), ResultCard(NiuniaCardKind.Result), TrailerCard(NiuniaCardKind.Trailer), ProgrammeTile(NiuniaCardKind.Tile);

    val width: Dp get() = size().first
    val height: Dp get() = size().second

    fun size(): Pair<Dp, Dp> = when (kind) {
        NiuniaCardKind.Poster -> NiuniaSize.PosterW to NiuniaSize.PosterH
        NiuniaCardKind.Spotlight -> NiuniaSize.SpotlightW to NiuniaSize.SpotlightH
        NiuniaCardKind.Landscape, NiuniaCardKind.Channel, NiuniaCardKind.Text -> NiuniaSize.LandscapeW to NiuniaSize.LandscapeH
        NiuniaCardKind.Wide, NiuniaCardKind.Episode, NiuniaCardKind.Trailer, NiuniaCardKind.Tile -> NiuniaSize.StillW to NiuniaSize.StillH
        NiuniaCardKind.Person -> NiuniaSize.PosterW to NiuniaSize.PosterW
        NiuniaCardKind.Profile -> NiuniaSize.Avatar72 to NiuniaSize.Avatar72
        NiuniaCardKind.Result -> NiuniaSize.ResultW to NiuniaSize.ResultH
    }

    val focusStyle: NiuniaFocusStyle get() = when (kind) {
        NiuniaCardKind.Text, NiuniaCardKind.Result -> NiuniaFocusStyle.Fill
        NiuniaCardKind.Profile -> NiuniaFocusStyle.Button
        else -> NiuniaFocusStyle.Lift
    }

    companion object {
        fun of(name: String): NiuniaCardPreset? = entries.firstOrNull { it.name == name }
    }
}

/** Status kropki na karcie (decyzja właściciela: kropka zamiast słów LIVE/Archiwum). */
enum class NiuniaCardStatus(val color: Color) { Live(NiuniaColor.Live), Archive(NiuniaColor.Archive), Delay(NiuniaColor.Delay) }

/** Co rysuje karta (nuvio.js: badge(), caption(), posterFoot()). */
data class NiuniaCardParts(
    val shimmer: Boolean,
    val fallbackLabel: String?,
    val statusDot: NiuniaCardStatus?,
    val badge: String?,
    val playIcon: Boolean,
    val progress: Float?,
    val caption: String?,
    val posterFoot: Boolean,
    val emptyText: Boolean,
) {
    val hasCaptionBand: Boolean get() = playIcon || progress != null || caption != null
    val hasTopRow: Boolean get() = statusDot != null || badge != null
}

/** [progress] w procentach 0–100 jak w makiecie (`progress="40"`); poza zakresem przycinany. */
fun niuniaCardParts(
    preset: NiuniaCardPreset,
    state: NiuniaCardState = NiuniaCardState.Ready,
    title: String? = null,
    caption: String? = null,
    progress: Int? = null,
    play: Boolean = false,
    badge: String? = null,
    status: NiuniaCardStatus? = null,
): NiuniaCardParts {
    val ready = state == NiuniaCardState.Ready
    val landscape = preset.kind == NiuniaCardKind.Landscape || preset.kind == NiuniaCardKind.Wide
    return NiuniaCardParts(
        shimmer = state == NiuniaCardState.Loading,
        fallbackLabel = if (state == NiuniaCardState.Error) title.orEmpty() else null,
        statusDot = status.takeIf { ready },
        badge = badge?.takeIf { ready && it.isNotBlank() },
        playIcon = ready && landscape && play,
        progress = progress?.takeIf { ready && landscape }?.coerceIn(0, 100)?.div(100f),
        caption = caption?.takeIf { ready && landscape && it.isNotBlank() },
        posterFoot = ready && (preset.kind == NiuniaCardKind.Poster || preset.kind == NiuniaCardKind.Spotlight),
        emptyText = state == NiuniaCardState.Empty,
    )
}

// ---------------------------------------------------------------- NInput seek

enum class NiuniaSeekTone { Main, Live, Delay, Archive }

/** Pasek postępu (jeden komponent we wszystkich odtwarzaczach, decyzja 20.11). Wartości 0..1. */
data class NiuniaSeekState(
    val value: Float,
    val tone: NiuniaSeekTone = NiuniaSeekTone.Main,
    val buffer: Float? = null,
    /** Kreska „na żywo” (opóźnienie): pozycja krawędzi transmisji. */
    val edge: Float? = null,
    /** Granice rozdziałów (0..1, bez 0 i 1). */
    val chapters: List<Float> = emptyList(),
) {
    val fill: Float get() = value.coerceIn(0f, 1f)
    val fillColor: Color get() = when (tone) {
        NiuniaSeekTone.Live -> NiuniaColor.Live
        NiuniaSeekTone.Delay -> NiuniaColor.Delay
        NiuniaSeekTone.Main, NiuniaSeekTone.Archive -> NiuniaColor.White
    }
    /** Bufor rysujemy tylko przed krawędzią i za pozycją; w trybie na żywo nie ma bufora. */
    val bufferShown: Float? get() = buffer?.takeIf { tone != NiuniaSeekTone.Live }?.coerceIn(fill, edge ?: 1f)?.takeIf { it > fill }
    /** Krawędź „na żywo” tylko przy opóźnieniu. */
    val edgeShown: Float? get() = edge?.takeIf { tone == NiuniaSeekTone.Delay }?.coerceIn(0f, 1f)
    val chapterGaps: List<Float> get() = chapters.filter { it > 0f && it < 1f }.distinct().sorted()

    companion object {
        fun of(positionMs: Long, durationMs: Long, tone: NiuniaSeekTone = NiuniaSeekTone.Main): NiuniaSeekState =
            NiuniaSeekState(if (durationMs > 0) positionMs.toFloat() / durationMs else 0f, tone)
    }
}

/** Czas „52:10” / „1:02:03”; [remaining] = z minusem „−2:00:50” (znak U+2212 jak w makiecie). */
fun niuniaTime(ms: Long, remaining: Boolean = false): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    val t = if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    return if (remaining) "−$t" else t
}

// ---------------------------------------------------------------- NEpgGrid

/** Komórka programu: x i szerokość w dp od początku okna (30 min = epg-slot-w), przycięcie na początku okna. */
data class NiuniaEpgCell(val x: Dp, val width: Dp, val clipped: Boolean)

/** nuvio.css .n-btn--epg-cell: left = max(0,(f−start))·slot/30, width = (t − max(f,start))·slot/30 − space-4. Minuty od dowolnego punktu. */
fun niuniaEpgCell(fromMin: Float, toMin: Float, windowStartMin: Float, slotW: Dp = NiuniaSize.EpgSlotW): NiuniaEpgCell? {
    if (toMin <= windowStartMin || toMin <= fromMin) return null
    val start = maxOf(fromMin, windowStartMin)
    val perMin = slotW.value / 30f
    return NiuniaEpgCell(((start - windowStartMin) * perMin).dp, ((toMin - start) * perMin - NiuniaSpacing.Space4.value).coerceAtLeast(0f).dp, fromMin < windowStartMin)
}

/** Linia „teraz”: x od początku okna. */
fun niuniaEpgNowX(nowMin: Float, windowStartMin: Float, slotW: Dp = NiuniaSize.EpgSlotW): Dp = ((nowMin - windowStartMin) * slotW.value / 30f).dp

// ---------------------------------------------------------------- NScreen: mapa fokusu

enum class NiuniaDir { Up, Down, Left, Right }

/**
 * Mapa fokusu jednego stanu ekranu (focus-and-keys.md, interaction-audit.md): siatka rzędów elementów,
 * fokus startowy, cel Wstecz, cele OK i długiego OK. Ruch bez zawijania: na krawędzi fokus zostaje.
 * Elementy wyłączone są pomijane. Cele to nazwy stanów/ekranów makiety (`Player`, `Movie` …).
 */
data class NiuniaFocusMap(
    val screen: String,
    val rows: List<List<String>>,
    val initial: String,
    val back: String?,
    val ok: Map<String, String> = emptyMap(),
    val longOk: Map<String, String> = emptyMap(),
    val disabled: Set<String> = emptySet(),
) {
    init {
        require(rows.flatten().contains(initial)) { "$screen: fokus startowy $initial poza mapą" }
    }

    private fun pos(id: String): Pair<Int, Int>? {
        rows.forEachIndexed { r, row -> val c = row.indexOf(id); if (c >= 0) return r to c }
        return null
    }

    /** Następny element w kierunku [dir] albo ten sam (brak zawijania, krawędź). */
    fun move(from: String, dir: NiuniaDir): String {
        val (r, c) = pos(from) ?: return from
        when (dir) {
            NiuniaDir.Left -> for (i in c - 1 downTo 0) rows[r][i].let { if (it !in disabled) return it }
            NiuniaDir.Right -> for (i in c + 1 until rows[r].size) rows[r][i].let { if (it !in disabled) return it }
            NiuniaDir.Up, NiuniaDir.Down -> {
                val range = if (dir == NiuniaDir.Up) (r - 1 downTo 0) else (r + 1 until rows.size)
                for (rr in range) {
                    val row = rows[rr].filter { it !in disabled }
                    if (row.isNotEmpty()) return row[minOf(c, row.size - 1)]
                }
            }
        }
        return from
    }

    fun okTarget(id: String): String? = ok[id]
    fun longOkTarget(id: String): String? = longOk[id]
    val all: List<String> get() = rows.flatten()
}

/** Mapy stanów ekranów z makiety (rebuild/project, pliki .dc.html). Paczka 1: Main, HomeRows, Sidebar, Detail, Player. */
object NiuniaScreens {
    /** Główna: hero (Odtwórz ma fokus, długie OK → strona filmu), ▼ → rzędy (HomeRows). Bez Wstecz (wyjście z aplikacji). */
    val Main = NiuniaFocusMap(
        screen = "Main",
        rows = listOf(listOf("menu"), listOf("play", "library", "info", "next"), listOf("rows")),
        initial = "play", back = null,
        ok = mapOf("menu" to "Sidebar", "play" to "Player", "info" to "Movie", "next" to "Main", "rows" to "HomeRows"),
        longOk = mapOf("play" to "Movie"),
    )

    /** Rzędy Głównej: pierwszy kafel „Oglądaj dalej” ma fokus; Wstecz/▲ z pierwszego rzędu → hero. */
    val HomeRows = NiuniaFocusMap(
        screen = "HomeRows",
        rows = listOf(listOf("cw0", "cw1", "cw2"), listOf("p0", "p1", "p2")),
        initial = "cw0", back = "Main",
        ok = mapOf("cw0" to "Detail", "p0" to "Movie", "p1" to "Detail"),
    )

    /** Menu: profil, Szukaj, Główna (fokus i zaznaczenie), Live TV, YouTube, Biblioteka, Ustawienia; Wstecz → treść. */
    val Sidebar = NiuniaFocusMap(
        screen = "Sidebar",
        rows = listOf(listOf("account"), listOf("search"), listOf("home"), listOf("livetv"), listOf("youtube"), listOf("library"), listOf("settings")),
        initial = "home", back = "Main",
        ok = mapOf("account" to "Profiles", "search" to "Search", "home" to "Main", "livetv" to "LiveTV", "youtube" to "PlayerLeanback", "library" to "Library", "settings" to "Settings"),
    )

    /** Strona serialu: Wznów (fokus), biblioteka, O serialu; ▼ → odcinki (Episodes); Wstecz → Główna (rzędy). */
    val Detail = NiuniaFocusMap(
        screen = "Detail",
        rows = listOf(listOf("resume", "library", "about"), listOf("episodes")),
        initial = "resume", back = "HomeRows",
        ok = mapOf("resume" to "Player", "about" to "DetailMore", "episodes" to "Episodes"),
    )

    /** Odtwarzacz (zamknięty panel): fokus na pasku; ▲ → ikony; ▼ → rząd Podobne; długie OK na pasku → szczegóły filmu. */
    val Player = NiuniaFocusMap(
        screen = "Player",
        rows = listOf(listOf("subtitles", "quality", "audio"), listOf("seek"), listOf("similar")),
        initial = "seek", back = "Main",
        ok = mapOf("subtitles" to "PlayerSubs", "audio" to "PlayerPanel", "similar" to "PlayerSeries"),
        longOk = mapOf("seek" to "PlayerFilmInfo"),
    )

    /** Odtwarzacz z panelem „Audio, lektor i napisy” (zakładka Lektor): fokus na „Głośność lektora”; Wstecz zamyka panel. */
    val PlayerPanel = NiuniaFocusMap(
        screen = "PlayerPanel",
        rows = listOf(listOf("tab-audio", "tab-lektor", "tab-subs"), listOf("lektor"), listOf("voice"), listOf("expressive"), listOf("volume"), listOf("duck")),
        initial = "volume", back = "Player",
        ok = mapOf("tab-subs" to "PlayerSubs"),
    )

    val batch1: List<NiuniaFocusMap> = listOf(Main, HomeRows, Sidebar, Detail, Player, PlayerPanel)
}
