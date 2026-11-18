@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.nuvio.tv.skin.kicia.home

import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import com.nuvio.tv.skin.kicia.nav.forgeTag // v7.4: nazwy elementów makiety
import androidx.compose.ui.platform.testTag
import com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.coerceAtLeast
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.kicia.components.ForgeCardCaption
import com.nuvio.tv.skin.kicia.components.ForgeCardFooterScrim
import com.nuvio.tv.skin.kicia.components.ForgeCard
import com.nuvio.tv.skin.kicia.components.ForgePosterCard
import com.nuvio.tv.skin.kicia.components.ForgeShelf
import com.nuvio.tv.skin.kicia.components.ForgeWideCard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeMotion
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import com.nuvio.tv.ui.screens.home.HomeUiState

/** What the big text and background at the top of Home describe: the card that has focus. */
private data class HeroInfo(
    val title: String,
    val meta: String,
    val description: String?,
    val backdrop: String?,
    /** Continue-watching only (mockup S01): how far in, and "zostało N min". */
    val progress: Float = 0f,
    val remaining: String? = null,
    /** Continue-watching only: the title to look up year, genres and IMDb for (type to id). */
    val lookup: Pair<String, String>? = null,
    /** v6: the title's logo (transparent PNG) replaces the title text when there is one. */
    val logo: String? = null,
)

/**
 * Home in the look of the mockup (S01): the focused title is described at the top (40/44 title, meta,
 * two lines of description) over its own backdrop; below, "Kontynuuj oglądanie" as wide cards with a
 * progress bar, then the catalog shelves as posters. Pressing Down from the first shelf hides the hero
 * and lifts the shelves to y = 40.
 *
 * Nuvio's own features are reused, not rebuilt: the "Nowe odcinki" shelf is Nuvio's list of upcoming /
 * newly released episodes (`upcomingItems`), and the trailer in the background uses Nuvio's trailer
 * lookup and player and its existing setting (focused poster backdrop trailer), started 1.3 s after
 * focus settles on a title.
 *
 * TODO(scalanie): shelves "Na żywo" (15 recent channels) and "YouTube dla Ciebie" need their data
 * sources wired in; options panel on long OK for continue-watching cards.
 */
@Composable
fun ForgeHomeContent(
    uiState: HomeUiState,
    onNavigateToDetail: (String, String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onCatalogItemLongPress: (MetaPreview, String) -> Unit,
    onItemFocus: (MetaPreview) -> Unit,
    trailerPreviewUrls: Map<String, String> = emptyMap(),
    trailerPreviewAudioUrls: Map<String, String> = emptyMap(),
    onRequestTrailerPreview: (itemId: String, title: String, releaseInfo: String?, apiType: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    heroMeta: ForgeHeroMetaViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val heroLines by heroMeta.lines.collectAsStateWithLifecycle()
    // v7.4 (3.14): Popover karty katalogu (Odtwórz, Dodaj do listy, Oznacz jako obejrzane), 8 dp pod kartą
    var cardMenu by remember { mutableStateOf<Triple<MetaPreview, String, androidx.compose.ui.geometry.Rect?>?>(null) }
    val cardBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val homeYouTube = com.nuvio.tv.skin.kicia.rememberForgeHomeYouTube()
    com.nuvio.tv.skin.kicia.wizard.ForgeWizardSourcesHost() // S16 step 3, once after the first run
    val continueItems = remember(uiState.continueWatchingItems) {
        if (uiState.continueWatchingEnabled) uiState.continueWatchingItems else emptyList()
    }
    val catalogRows = remember(uiState.catalogRows) { uiState.catalogRows.filter { it.items.isNotEmpty() } }
    val upcomingItems = remember(uiState.upcomingItems) {
        if (uiState.continueWatchingEnabled) uiState.upcomingItems else emptyList()
    }
    val liveChannels by com.nuvio.tv.fork.tv.TvStore.recentChannels.collectAsStateWithLifecycle()
    // "Nowe odcinki" of the mockup come from the TV guide (S01: EPG, rule "Nowy odcinek"); Nuvio's own upcoming
    // episodes stand in when the guide has none.
    val tvAlerts by com.nuvio.tv.fork.tv.TvStore.alerts.collectAsStateWithLifecycle()
    val liveTv by com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.collectAsStateWithLifecycle()
    val tvNew = remember(tvAlerts, liveTv.channels) { forgeHomeTvNew(tvAlerts, liveTv.channels) }
    // "Na żywo" = the same cards as the row on top of Live TV (owner 9.11): recent programmes with the frame of what is on.
    val recentShows by com.nuvio.tv.fork.tv.TvStore.recent.collectAsStateWithLifecycle()
    val liveFrames by com.nuvio.tv.skin.kicia.livetv.ForgeChannelSnapshots.frames.collectAsStateWithLifecycle()
    val liveShown = remember(recentShows, liveTv.channels) {
        val byUrl = liveTv.channels.associateBy { it.streamUrl }
        val byGuide = HashMap<String, com.nuvio.tv.reshaped.livetv.LiveTvChannel>().also { m -> liveTv.channels.forEach { m.putIfAbsent(it.guideKey, it) } }
        recentShows.mapNotNull { sh -> (byUrl[sh.channelUrl] ?: byGuide[sh.guideKey])?.let { sh to it } }.take(15)
    }
    val shelfCount = (if (continueItems.isNotEmpty()) 1 else 0) + (if (liveShown.isNotEmpty()) 1 else 0) +
        (if (upcomingItems.isNotEmpty() || tvNew.isNotEmpty()) 1 else 0) + catalogRows.size

    var hero by remember { mutableStateOf<HeroInfo?>(null) }
    var focusedShelf by remember { mutableIntStateOf(0) }
    // v6 (mockup S01): the hero fills the screen and the first shelf peeks in at the bottom; focus on a card scrolls the shelves up.
    var inHero by remember { mutableStateOf(true) }
    var focusedMeta by remember { mutableStateOf<MetaPreview?>(null) }
    val trailerEnabled = uiState.focusedPosterBackdropTrailerEnabled
    val listState = rememberLazyListState()
    // ↓ from the bar: the scaffold's fallback target is the first card (the focus search found nothing on Home).
    val scaffoldTarget = com.nuvio.tv.LocalContentFocusRequester.current
    val entryTarget = remember(scaffoldTarget) { if (scaffoldTarget == FocusRequester.Default) FocusRequester() else scaffoldTarget }

    // Hero follows the first title until something else is focused.
    // v6 (owner 9.11): the hero is a carousel of the featured titles (Nuvio's hero catalogs) and no longer follows shelf focus.
    // First slide change after 20 s, then every 10 s (as in Nuvio's HeroCarousel); without featured titles the old behaviour stays.
    val featured = remember(uiState.heroItems, uiState.heroSectionEnabled) {
        if (uiState.heroSectionEnabled) uiState.heroItems.take(8) else emptyList()
    }
    var featuredIndex by remember(featured) { mutableIntStateOf(0) }
    // v6: with the hero's buttons, ↓ from the bar (and the first focus) lands on ▶; without featured titles on the first card.
    val firstCard = remember(featured.isEmpty()) { if (featured.isEmpty()) entryTarget else FocusRequester() }
    val heroPlay = entryTarget
    LaunchedEffect(featured) {
        if (featured.size > 1) {
            kotlinx.coroutines.delay(20_000)
            while (true) {
                featuredIndex = (featuredIndex + 1) % featured.size
                kotlinx.coroutines.delay(10_000)
            }
        }
    }
    val initialHero = remember(featured, featuredIndex, continueItems, catalogRows) {
        featured.getOrNull(featuredIndex)?.let(::heroOf)
            ?: continueItems.firstOrNull()?.let(::heroOf)
            ?: catalogRows.firstOrNull()?.items?.firstOrNull()?.let(::heroOf)
    }
    val shownHero = if (featured.isNotEmpty()) initialHero else (hero ?: initialHero)

    // Shelf i (after the hero spacer) lifts to the top when focused; shelf 0 shows the hero above it.
    // WY-309: the shelf index is read from a snapshot flow, not in composition, so moving between shelves no longer recomposes the whole Home.
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { if (inHero) 0 else focusedShelf + 1 }.collectLatest { target ->
            // List item 0 is the hero spacer, shelf i is item i + 1: the first shelf stays below the hero, later ones lift to y = 40.
            listState.animateScrollToItem(target)
        }
    }
    // Trailer: ask Nuvio for the focused title's trailer once focus has rested for 1.3 s.
    LaunchedEffect(focusedMeta?.id, trailerEnabled) {
        val meta = focusedMeta ?: return@LaunchedEffect
        if (!trailerEnabled) return@LaunchedEffect
        kotlinx.coroutines.delay(TrailerDelayMs)
        onRequestTrailerPreview(meta.id, meta.name, meta.releaseInfo, meta.apiType)
    }
    // v7.4: the screen is shown only when it is drawn (shelves and, when enabled, the hero are there), never while it fills in; the focus is set once, then.
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500); timedOut = true } // never hold the screen longer than 2.5 s
    val ready = shelfCount > 0 && (!uiState.heroSectionEnabled || featured.isNotEmpty() || timedOut)
    var revealed by remember { mutableStateOf(false) }
    if (ready && !revealed) revealed = true
    val revealAlpha by androidx.compose.animation.core.animateFloatAsState(if (revealed) 1f else 0f, animationSpec = androidx.compose.animation.core.tween(ForgeMotion.FadeMs), label = "forgeHomeReveal")
    LaunchedEffect(revealed) {
        // v7.4 (1.23): entered with Back from the bar: the focus stays on the Home tab
        android.util.Log.d("ForgeNav", "home revealed=$revealed keepBar=${com.nuvio.tv.skin.kicia.nav.ForgeNavState.homeKeepsBarFocus}") // forge v7.4: diagnostyka
        if (revealed && com.nuvio.tv.skin.kicia.nav.ForgeNavState.homeKeepsBarFocus) { com.nuvio.tv.skin.kicia.nav.ForgeNavState.homeKeepsBarFocus = false; return@LaunchedEffect }
        if (revealed) { kotlinx.coroutines.delay(60); runCatching { entryTarget.requestFocus() } } // the hero's ▶ (or the first card without a hero), once
    }

    Box(modifier.fillMaxSize().background(ForgeColors.Bg)) {
      if (revealAlpha < 1f) ForgeHomeSkeleton(Modifier.graphicsLayer { alpha = 1f - revealAlpha }) // v7.4 (03): the skeleton fades out as the screen fades in
      Box(Modifier.fillMaxSize().graphicsLayer { alpha = revealAlpha }) { // v7.4: reveal gate
        ForgeBackgroundPreload(ready = shelfCount > 0) // owner 9.11: the other tabs get ready in the background
        // Backdrop of the focused title, crossfading, under the scrims.
        Crossfade(
            targetState = shownHero?.backdrop,
            animationSpec = ForgeMotion.fadeSpec(),
            label = "forgeHomeBackdrop",
            modifier = Modifier.fillMaxSize(),
        ) { url ->
            if (url != null) {
                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        val trailerUrl = if (trailerEnabled) focusedMeta?.id?.let { trailerPreviewUrls[it] } else null
        if (trailerUrl != null) {
            var firstFrame by remember(trailerUrl) { mutableStateOf(false) }
            val alpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (firstFrame) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(ForgeMotion.TrailerFadeMs),
                label = "forgeTrailerAlpha",
            )
            com.nuvio.tv.ui.components.TrailerPlayer(
                trailerUrl = trailerUrl,
                trailerAudioUrl = focusedMeta?.id?.let { trailerPreviewAudioUrls[it] },
                isPlaying = true,
                muted = uiState.focusedPosterBackdropTrailerMuted,
                onEnded = {},
                onFirstFrameRendered = { firstFrame = true },
                cropToFill = true,
                modifier = Modifier.fillMaxSize().alpha(alpha),
            )
        }
        Box(Modifier.fillMaxSize().background(ForgeColors.HeroScrimHorizontal))
        Box(Modifier.fillMaxSize().background(ForgeColors.HeroScrimVertical))
        // Owner 10.11: with the focus down on the shelves the screen is graphite (the picture belongs to the hero), as before.
        val shelfGraphite = androidx.compose.animation.core.animateFloatAsState(if (inHero) 0f else 0.96f, animationSpec = androidx.compose.animation.core.tween(ForgeMotion.FadeMs), label = "forgeHomeGraphite")
        // WY-308: the 96% scrim is read in the draw phase, so its fade no longer recomposes the whole Home on every frame
        Box(Modifier.fillMaxSize().drawBehind { drawRect(ForgeColors.Bg.copy(alpha = shelfGraphite.value)) })

        // Hero text: x 58, y 96.
        // Stays composed (so ▲ from the first shelf can reach ▶): it fades and rides up 328 dp (1.38 x the 238 dp the list scrolls).
        val heroAlpha by androidx.compose.animation.core.animateFloatAsState(if (inHero) 1f else 0f, animationSpec = androidx.compose.animation.core.tween(ForgeMotion.FadeMs), label = "forgeHeroAlpha")
        Box(Modifier.align(Alignment.BottomStart).zIndex(if (inHero) 1f else 0f) /* v7.4: nad półkami, inaczej LazyColumn zasłania hero w drzewie dostępności (pomiar, test pilota) */ .graphicsLayer { alpha = heroAlpha; translationY = -328.dp.toPx() * (1f - heroAlpha) }
            // While the shelves are scrolled up, the invisible hero must not catch ▼ / ◀▶ of the focus search.
            .focusProperties {
                @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                enter = { if (inHero) FocusRequester.Default else FocusRequester.Cancel }
            }
            // Owner 10.11: ▼ from the hero's buttons found nothing below (the shelves are a separate sibling and the first card may lie
            // partly off screen), so Home could not be left downwards. ▼ goes to the first card of the first shelf by name.
            .onPreviewKeyEvent { e ->
                if (e.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && e.key == androidx.compose.ui.input.key.Key.DirectionDown && inHero && featured.isNotEmpty()) {
                    runCatching { firstCard.requestFocus() }.isSuccess // the card's own focus callback moves the state
                } else false
            }
            .focusGroup()) {
            if (shownHero != null) {
                Column(
                    Modifier
                        .padding(start = ForgeDims.ContentStart, bottom = 108.dp) // v7.4: anchored from the bottom; dots y 426-432, buttons y 376-420
                        .width(560.dp),
                ) {
                    // v7.4: Plakietka (Film / Serial) above the title: 22 dp tall, 12 sp/700, white 12% on a capsule, text white 70%
                    com.nuvio.tv.skin.kicia.components.ForgeBadge(if (shownHero.lookup?.first == "series") "Serial" else "Film", com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Quality, Modifier.padding(bottom = 8.dp).forgeTag("plakietka")) // v7.4: plakietka typu
                    var logoFailed by remember(shownHero.logo) { mutableStateOf(false) }
                    if (!shownHero.logo.isNullOrBlank() && !logoFailed) {
                        AsyncImage(
                            model = shownHero.logo, contentDescription = shownHero.title, contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart, onError = { logoFailed = true },
                            modifier = Modifier.height(ForgeDims.HeroLogoHeight).width(300.dp).forgeTag("hero-logo"), // v7.4: logo 63
                        )
                    } else Box(Modifier.height(ForgeDims.HeroLogoHeight), contentAlignment = Alignment.BottomStart) { Text(shownHero.title, style = ForgeText.title.copy(fontSize = 40.sp, lineHeight = 44.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.forgeTag("hero-logo")) } // v7.4: bez logo tytuł w tym samym miejscu (63 dp), hero nie skacze
                    // Mockup S01 (e01): title, "2022— · Thriller, Sci-Fi · IMDb 8,7" (IMDb gold), description; no progress here.
                    LaunchedEffect(shownHero.lookup) { shownHero.lookup?.let { (type, id) -> heroMeta.request(type, id) } }
                    val line = shownHero.lookup?.let { heroLines[it.second] } ?: shownHero.meta
                    Text(forgeHeroMetaText(line), style = ForgeText.caption.copy(fontSize = 14.sp, lineHeight = 20.sp), maxLines = 1, modifier = Modifier.padding(top = 8.dp).forgeTag("hero-meta")) // v6: meta 12 sp
                    if (!shownHero.description.isNullOrBlank()) {
                        Text(
                            shownHero.description,
                            style = ForgeText.caption.copy(fontSize = 14.sp, lineHeight = 20.sp), // v7: 14 sp, 3 lines, 300 dp
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp).width(300.dp).forgeTag("hero-opis"),
                        )
                    }
                    // v6: buttons ▶ (opens the title) · ⓘ (details) · › (next slide), 30 dp circles
                    featured.getOrNull(featuredIndex)?.let { cur ->
                        com.nuvio.tv.skin.kicia.components.ForgeSpreadRow(Modifier.padding(top = 16.dp).onFocusChanged { if (it.hasFocus) { inHero = true } }, gap = 12.dp) { // v7.4: opis -> rząd 16; sąsiedzi odsuwają się przy fokusie
                            Item(0) { com.nuvio.tv.skin.kicia.components.ForgeTagged("przycisk.0") { com.nuvio.tv.skin.kicia.components.ForgeButton(
                                text = "Odtwórz", icon = androidx.compose.material.icons.Icons.Filled.PlayArrow, primary = true, large = true,
                                focusRequester = if (featured.isNotEmpty()) heroPlay else null,
                                // v7 (03): OK resumes straight away when the title is in "Kontynuuj"; otherwise it opens the details.
                                onOk = {
                                    val resume = continueItems.firstOrNull { heroOf(it).lookup?.second == cur.id }
                                    if (resume != null) onContinueWatchingClick(resume) else onNavigateToDetail(cur.id, cur.apiType, "")
                                },
                                onLongOk = { onNavigateToDetail(cur.id, cur.apiType, "") }, // v7.4 (3.9): długie OK = Szczegóły
                            ) } }
                            Item(1) { com.nuvio.tv.skin.kicia.components.ForgeTagged("przycisk.1") { com.nuvio.tv.skin.kicia.components.ForgeIconButton(
                                icon = androidx.compose.material.icons.Icons.Filled.Info, contentDescription = "Szczegóły", size = ForgeDims.IconButton,
                                onOk = { onNavigateToDetail(cur.id, cur.apiType, "") },
                            ) } }
                            val inList by heroMeta.inLibrary(cur.id, cur.apiType).collectAsStateWithLifecycle(initialValue = false)
                            Item(2) { com.nuvio.tv.skin.kicia.components.ForgeTagged("przycisk.2") { com.nuvio.tv.skin.kicia.components.ForgeIconButton(
                                icon = if (inList) androidx.compose.material.icons.Icons.Filled.Check else androidx.compose.material.icons.Icons.Filled.Add,
                                contentDescription = "Moja lista", size = ForgeDims.IconButton, active = inList,
                                onOk = { heroMeta.toggleLibrary(cur) },
                            ) } }
                            if (featured.size > 1) Item(3) { com.nuvio.tv.skin.kicia.components.ForgeTagged("przycisk.3") { com.nuvio.tv.skin.kicia.components.ForgeIconButton(
                                icon = androidx.compose.material.icons.Icons.Filled.ChevronRight, contentDescription = "Następny", size = ForgeDims.IconButton,
                                onOk = { featuredIndex = (featuredIndex + 1) % featured.size },
                            ) } }
                        }
                    }
                    // v6: carousel dots (active = 14 dp capsule, others 5 dp)
                    if (featured.size <= 1) Spacer(Modifier.height(12.dp)) // v7.4: the dots' room stays reserved, the buttons keep y 376-420
                    if (featured.size > 1) {
                        // v7.4 Kropki karuzeli: 6 dp, odstęp 8, aktywna biała, pozostałe 24%; środek x 480, y 426
                        Row(Modifier.padding(top = 6.dp).offset(x = (440 - (6 * featured.size + 8 * (featured.size - 1)) / 2f).dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            featured.indices.forEach { i ->
                                Box(
                                    Modifier.size(6.dp).testTag("kropka.$i")
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (i == featuredIndex) ForgeColors.Text else ForgeColors.Surface3),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Home scrolls itself (shelf i to the top line, see focusedShelf); Android TV's own bring-into-view
        // would also move the focused card to 30% of the screen and push the shelves over the hero text.
        val rowSpec = androidx.compose.foundation.gestures.LocalBringIntoViewSpec.current
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides NoBringIntoView,
            com.nuvio.tv.skin.kicia.components.LocalForgeRowBringIntoView provides rowSpec,
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { e ->
                // ▲ from the first shelf goes back to ▶ in the hero (it lies below the scrolled shelf, so the focus search would miss it).
                if (e.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && e.key == androidx.compose.ui.input.key.Key.DirectionUp && !inHero && focusedShelf == 0 && featured.isNotEmpty()) {
                    inHero = true; runCatching { heroPlay.requestFocus() }; true
                } else false
            },
            contentPadding = PaddingValues(top = 24.dp, bottom = 360.dp), // forge hook v7.4 (03 popover-karty): a focused shelf lifts to y 24 (the bar hides on scroll); 540 - 24 - 160 = room for the last one
        ) {
            // Space for the hero and the bar; the first shelf header sits at y = 456.
            item(key = "forge_hero_space") { Spacer(Modifier.height(432.dp)) } // v7.4: 24 dp of padding + 432 = the first shelf stays at y 456
            var shelfIndex = 0
            if (continueItems.isNotEmpty()) {
                val myIndex = shelfIndex++
                item(key = "forge_continue") {
                    ForgeShelf(tag = "polka-tytul.$myIndex", title = "Kontynuuj oglądanie") {
                        itemsIndexed(continueItems, key = { i, it -> "cw_$i${continueKey(it)}" }) { i, item ->
                            val info = cwCard(item)
                            com.nuvio.tv.skin.kicia.components.ForgeTagged("karta.$i") { ForgeWideCard(
                                focusRequester = if (i == 0) firstCard else null,
                                onFocusChange = { focused ->
                                    if (focused) {
                                        focusedShelf = myIndex; inHero = false
                                        hero = heroOf(item)
                                        focusedMeta = null
                                    }
                                },
                                onOk = { onContinueWatchingClick(item) },
                            ) {
                                // Fallback picture underneath, preferred on top: one that fails to load leaves the other.
                                info.images.asReversed().forEach { url ->
                                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.4f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = .8f))))
                                ForgeTypeBadge(series = info.series, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
                                // v6: small caption at the bottom edge, tiny 26x2 dp progress bar next to the line.
                                ForgeCardCaption(
                                    title = info.title,
                                    line = info.subtitle,
                                    progress = info.progress.takeIf { it > 0f },
                                )
                            } }
                        }
                    }
                }
            }
            if (liveShown.isNotEmpty()) {
                val myIndex = shelfIndex++
                item(key = "forge_live") {
                    ForgeShelf(tag = "polka-tytul.$myIndex", title = "Na żywo") {
                        itemsIndexed(liveShown, key = { _, p -> "live_" + p.first.key }) { i, (show, channel) ->
                            val onNow = liveTv.currentProgrammes[channel.guideKey]
                            com.nuvio.tv.skin.kicia.livetv.ForgeRecentProgrammeCard(
                                show = show,
                                frame = onNow?.image?.takeIf { it.isNotBlank() } ?: liveFrames[channel.guideKey]?.second, // owner 9.11: EPG picture first, a stream frame only when missing
                                channel = channel,
                                live = onNow != null && com.nuvio.tv.skin.kicia.livetv.forgeSameTitle(onNow.title, show.title),
                                nowMs = System.currentTimeMillis(),
                                // Opens Live TV and plays the channel there (Live TV's own player and zapping).
                                onClick = {
                                    com.nuvio.tv.skin.kicia.livetv.ForgePendingPlay.ask(channel.streamUrl)
                                    com.nuvio.tv.skin.kicia.nav.ForgeNavRequests.open(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE)
                                },
                                onLongClick = {},
                                modifier = Modifier
                                    .then(if (continueItems.isEmpty() && i == 0) Modifier.focusRequester(firstCard) else Modifier)
                                    .onFocusChanged { if (it.hasFocus) { focusedShelf = myIndex; inHero = false; focusedMeta = null } },
                            )
                        }
                    }
                }
            }
            if (tvNew.isNotEmpty()) {
                val myIndex = shelfIndex++
                item(key = "forge_tv_new") {
                    ForgeShelf(tag = "polka-tytul.$myIndex", title = "Nowe odcinki") {
                        itemsIndexed(tvNew, key = { _, it -> "tvnew_" + it.first.id }) { i, (alert, channel) ->
                            ForgeWideCard(
                                focusRequester = if (continueItems.isEmpty() && liveShown.isEmpty() && i == 0) firstCard else null,
                                onFocusChange = { focused ->
                                    if (focused) {
                                        focusedShelf = myIndex; inHero = false
                                        hero = HeroInfo(alert.showTitle, forgeTvNewSubtitle(alert, channel.name), alert.airing.description, null)
                                        focusedMeta = null
                                    }
                                },
                                // Started: plays the channel in Live TV; still to come: opens Live TV's guide.
                                onOk = {
                                    if (alert.airing.startMs <= System.currentTimeMillis()) com.nuvio.tv.skin.kicia.livetv.ForgePendingPlay.ask(channel.streamUrl)
                                    com.nuvio.tv.skin.kicia.nav.ForgeNavRequests.open(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE)
                                },
                            ) {
                                forgeChannelLogo(channel)?.takeIf { it.isNotBlank() }?.let { logo ->
                                    com.nuvio.tv.skin.kicia.components.ForgeAutoLogo(url = logo, contentDescription = channel.name, modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(width = 86.dp, height = 46.dp))
                                }
                                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.4f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = .8f))))
                                val newEp = alert.kind == com.nuvio.tv.fork.tv.AlertKind.NEW_EPISODE
                                Text(
                                    when (alert.kind) {
                                        com.nuvio.tv.fork.tv.AlertKind.NEW_EPISODE -> "Nowy odcinek"
                                        com.nuvio.tv.fork.tv.AlertKind.MISSED_IN_ARCHIVE -> "W archiwum"
                                        com.nuvio.tv.fork.tv.AlertKind.UPCOMING -> "Najbliższa emisja"
                                    },
                                    style = ForgeText.captionBold.copy(color = ForgeColors.OnFocus),
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                        .background(if (newEp) ForgeColors.Ok else ForgeColors.Text, androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                                Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                                    Text(alert.showTitle, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(forgeTvNewSubtitle(alert, channel.name), style = ForgeText.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            } else if (upcomingItems.isNotEmpty()) {
                val myIndex = shelfIndex++
                item(key = "forge_upcoming") {
                    ForgeShelf(tag = "polka-tytul.$myIndex", title = "Nowe odcinki") {
                        itemsIndexed(upcomingItems, key = { i, it -> "up_$i${continueKey(it)}" }) { i, item ->
                            val info = cwCard(item)
                            ForgeWideCard(
                                focusRequester = if (continueItems.isEmpty() && liveShown.isEmpty() && i == 0) firstCard else null,
                                onFocusChange = { focused ->
                                    if (focused) {
                                        focusedShelf = myIndex; inHero = false
                                        hero = heroOf(item)
                                        focusedMeta = null
                                    }
                                },
                                onOk = { onContinueWatchingClick(item) },
                            ) {
                                // Fallback picture underneath, preferred on top: one that fails to load leaves the other.
                                info.images.asReversed().forEach { url ->
                                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.4f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = .8f))))
                                upcomingBadge(item)?.let { text ->
                                    Text(
                                        text,
                                        style = ForgeText.captionBold.copy(color = ForgeColors.OnFocus),
                                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                            .background(ForgeColors.Ok, androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                                    Text(info.title, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(info.subtitle, style = ForgeText.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            // v6 "Wyróżnione": the featured titles (the hero carousel's own list) as large posters with the title at the bottom edge.
            if (featured.isNotEmpty()) {
                val myIndex = shelfIndex++
                item(key = "forge_featured") {
                    ForgeShelf(tag = "polka-tytul.$myIndex", title = "Wyróżnione", extraGap = 16.dp) { // owner 10.11: the focused big poster grows 12.5% and covered the heading
                        itemsIndexed(featured, key = { i, it -> "feat_${it.id}_$i" }) { _, meta ->
                            com.nuvio.tv.skin.kicia.components.ForgeBigCard(
                                onFocusChange = { focused -> if (focused) { focusedShelf = myIndex; inHero = false; focusedMeta = meta; onItemFocus(meta) } },
                                onOk = { onNavigateToDetail(meta.id, meta.apiType, "") },
                            ) {
                                com.nuvio.tv.skin.kicia.components.ForgePosterImage(meta.poster ?: meta.background, meta.rawPosterUrl, meta.name)
                                ForgeCardFooterScrim(.44f)
                                Text(
                                    meta.name.uppercase(),
                                    style = ForgeText.captionBold.copy(fontSize = 20.sp, lineHeight = 22.sp, letterSpacing = 0.07.em),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 14.dp, end = 14.dp, bottom = 26.dp),
                                )
                            }
                        }
                    }
                }
            }
            // YouTube dla Ciebie (mockup S01): between the new episodes and the catalogs.
            if (homeYouTube) {
                val myIndex = shelfIndex++
                item(key = "forge_youtube") {
                    com.nuvio.tv.skin.kicia.youtube.ForgeHomeYouTubeShelf(onFocused = {
                        focusedShelf = myIndex; inHero = false
                        focusedMeta = null
                    })
                }
            }
            catalogRows.forEach { row ->
                val myIndex = shelfIndex++
                item(key = "forge_row_${row.addonId}_${row.catalogId}_${row.rawType}") {
                    ForgeShelf(tag = "polka-tytul.$myIndex", title = row.catalogName) {
                        itemsIndexed(row.items, key = { i, it -> "${it.id}_$i" }) { i, meta ->
                            ForgePosterCard(
                                focusRequester = if (continueItems.isEmpty() && myIndex == 0 && i == 0) firstCard else null,
                                onFocusChange = { focused ->
                                    if (focused) {
                                        focusedShelf = myIndex; inHero = false
                                        hero = heroOf(meta)
                                        focusedMeta = meta
                                        onItemFocus(meta)
                                    }
                                },
                                modifier = Modifier.onGloballyPositioned { cardBounds[meta.id] = it.boundsInRoot() },
                                onLongOk = { cardMenu = Triple(meta, row.addonBaseUrl, cardBounds[meta.id]) }, // v7.4 (3.14): Popover z makiety
                                onOk = { onNavigateToDetail(meta.id, meta.apiType, row.addonBaseUrl) },
                            ) {
                                // Title underneath, poster (or the backdrop when there is none) on top: no empty cards.
                                Text(
                                    meta.name,
                                    style = ForgeText.label,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                                )
                                com.nuvio.tv.skin.kicia.components.ForgePosterImage(meta.poster ?: meta.background, meta.rawPosterUrl, meta.name)
                            }
                        }
                    }
                }
            }
        }
        }
        cardMenu?.let { (meta, addon, b) ->
            val density = androidx.compose.ui.platform.LocalDensity.current
            // Popover 148 dp (8 + 3 x 44 + 8): pod kartą, a gdy nie mieści się nad dolną granicą 524 dp, nad nią
            val px = with(density) { (b?.left ?: 0f).toDp() }.coerceAtLeast(ForgeDims.ContentStart)
            val below = with(density) { (b?.bottom ?: 0f).toDp() } + 8.dp
            val py = if (below + 148.dp <= 524.dp) below else with(density) { (b?.top ?: 0f).toDp() } - 156.dp
            val inList by heroMeta.inLibrary(meta.id, meta.apiType).collectAsStateWithLifecycle(initialValue = false)
            com.nuvio.tv.skin.kicia.components.ForgePopover(onDismiss = { cardMenu = null }, x = px, y = py.coerceAtLeast(68.dp)) {
                com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = "Odtwórz") { cardMenu = null; onNavigateToDetail(meta.id, meta.apiType, addon) }
                com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = if (inList) "Usuń z listy" else "Dodaj do listy") { cardMenu = null; heroMeta.toggleLibrary(meta) }
                com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = "Oznacz jako obejrzane") { cardMenu = null; heroMeta.markWatched(meta) }
            }
        }
      }
    }
}

private fun heroOf(meta: MetaPreview): HeroInfo {
    val bits = buildList {
        meta.releaseInfo?.takeIf { it.isNotBlank() }?.let(::add)
        if (meta.genres.isNotEmpty()) add(meta.genres.take(2).joinToString(", "))
        meta.imdbRating?.let { add("IMDb " + "%.1f".format(java.util.Locale("pl"), it)) }
    }
    return HeroInfo(meta.name, bits.joinToString(" · "), meta.description, meta.backdropUrl, logo = meta.logo)
}

private fun heroOf(item: ContinueWatchingItem): HeroInfo = when (item) {
    is ContinueWatchingItem.InProgress -> HeroInfo(
        title = item.progress.name,
        meta = episodeLabel(item.progress.season, item.progress.episode, item.progress.episodeTitle)
            .ifBlank { forgeFilmMeta(item.progress.duration) },
        description = item.episodeDescription,
        backdrop = forgeCardImages(item.progress.backdrop, item.episodeThumbnail, item.progress.poster).firstOrNull(),
        progress = cwCard(item).progress,
        remaining = item.progress.duration.takeIf { it > 0 }?.let { "zostało ${((it - item.progress.position) / 60_000L).coerceAtLeast(0)} min" },
        lookup = item.progress.contentType to item.progress.contentId,
    )
    is ContinueWatchingItem.NextUp -> HeroInfo(
        title = item.info.name,
        meta = episodeLabel(item.info.season, item.info.episode, item.info.episodeTitle),
        description = item.info.episodeDescription,
        backdrop = forgeCardImages(item.info.backdrop, item.info.thumbnail, item.info.poster).firstOrNull(),
        lookup = item.info.contentType to item.info.contentId,
    )
}

/** The hero line with "IMDb 8,7" in gold, as the mockup draws it. */
private fun forgeHeroMetaText(line: String): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    val i = line.indexOf("IMDb ")
    if (i < 0) { append(line); return@buildAnnotatedString }
    append(line.substring(0, i))
    val end = line.indexOf(" · ", i).let { if (it < 0) line.length else it }
    pushStyle(androidx.compose.ui.text.SpanStyle(color = ForgeColors.Gold))
    append(line.substring(i, end))
    pop()
    append(line.substring(end))
}

private fun episodeLabel(season: Int?, episode: Int?, title: String?): String =
    listOfNotNull(
        if (season != null && episode != null) "S$season:O$episode" else null,
        title?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

private fun continueKey(item: ContinueWatchingItem): String = when (item) {
    is ContinueWatchingItem.InProgress -> "${item.progress.contentId}_${item.progress.season}_${item.progress.episode}"
    is ContinueWatchingItem.NextUp -> "${item.info.contentId}_${item.info.season}_${item.info.episode}_n"
}

private data class CwCard(val title: String, val subtitle: String, val images: List<String>, val progress: Float, val series: Boolean = false)

/** Pictures for a wide card, best first: the first two usable addresses (blank ones are skipped). */
internal fun forgeCardImages(vararg candidates: String?): List<String> =
    candidates.mapNotNull { it?.takeIf(String::isNotBlank) }.distinct().take(2)

private fun cwCard(item: ContinueWatchingItem): CwCard = when (item) {
    is ContinueWatchingItem.InProgress -> {
        val p = item.progress
        val fraction = when {
            p.duration > 0 -> (p.position.toFloat() / p.duration).coerceIn(0f, 1f)
            p.progressPercent != null -> (p.progressPercent!! / 100f).coerceIn(0f, 1f)
            else -> 0f
        }
        val minutesLeft = if (p.duration > 0) ((p.duration - p.position) / 60_000L).coerceAtLeast(0) else null
        val sub = listOfNotNull(
            if (p.season != null && p.episode != null) "S${p.season}:O${p.episode}" else null,
            minutesLeft?.let { "zostało $it min" },
        ).joinToString(" · ")
        CwCard(p.name, sub, forgeCardImages(item.episodeThumbnail, p.backdrop, p.poster), fraction, series = p.season != null || p.contentType == "series")
    }
    is ContinueWatchingItem.NextUp -> CwCard(
        item.info.name,
        "S${item.info.season}:O${item.info.episode}",
        forgeCardImages(item.info.thumbnail, item.info.backdrop, item.info.poster),
        0f,
        series = true,
    )
}

private const val TrailerDelayMs = 1_300L

/** One badge per card: "Nowy odcinek" only for a truly new release, otherwise the air date Nuvio already worked out. */
private fun upcomingBadge(item: ContinueWatchingItem): String? = when (item) {
    is ContinueWatchingItem.NextUp ->
        if (item.info.isReleaseAlert || item.info.isNewSeasonRelease) "Nowy odcinek" else item.info.airDateLabel?.takeIf { it.isNotBlank() }
    is ContinueWatchingItem.InProgress -> null
}

/** Never scrolls on focus: used for Home's column, which moves shelves itself. */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
private val NoBringIntoView = object : androidx.compose.foundation.gestures.BringIntoViewSpec {
    override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float> = androidx.compose.animation.core.spring()
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/** Hero meta for a film in progress (the progress record has no year or genre): "Film · 1 godz. 5 min". */
internal fun forgeFilmMeta(durationMs: Long): String {
    val minutes = durationMs / 60_000L
    if (minutes <= 0) return "Film"
    val h = minutes / 60
    val m = minutes % 60
    return "Film · " + listOfNotNull(h.takeIf { it > 0 }?.let { "$it godz." }, m.takeIf { it > 0 || h == 0L }?.let { "$it min" }).joinToString(" ")
}

/** Round type badge on a wide card (mockup S01): a TV for a series, a clapperboard for a film. */
@Composable
private fun ForgeTypeBadge(series: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(28.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .55f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = if (series) Icons.Outlined.Tv else Icons.Outlined.Movie,
            contentDescription = if (series) "Serial" else "Film",
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** TV guide alerts with their channel, newest first by kind (new episodes, then the rest), at most 12. */
internal fun forgeHomeTvNew(
    alerts: List<com.nuvio.tv.fork.tv.EpisodeAlert>,
    channels: List<com.nuvio.tv.reshaped.livetv.LiveTvChannel>,
): List<Pair<com.nuvio.tv.fork.tv.EpisodeAlert, com.nuvio.tv.reshaped.livetv.LiveTvChannel>> {
    if (alerts.isEmpty() || channels.isEmpty()) return emptyList()
    val byGuide = HashMap<String, com.nuvio.tv.reshaped.livetv.LiveTvChannel>()
    channels.forEach { byGuide.putIfAbsent(it.guideKey, it) }
    return alerts.mapNotNull { a -> byGuide[a.airing.guideKey]?.let { a to it } }
        .sortedWith(compareBy({ it.first.kind != com.nuvio.tv.fork.tv.AlertKind.NEW_EPISODE }, { it.first.airing.startMs }))
        .take(12)
}

/** "jutro 23:05 · TVP 1" */
internal fun forgeTvNewSubtitle(alert: com.nuvio.tv.fork.tv.EpisodeAlert, channelName: String, nowMs: Long = System.currentTimeMillis()): String =
    com.nuvio.tv.skin.kicia.livetv.forgeDayLabel(alert.airing.startMs, nowMs) + " " + com.nuvio.tv.skin.kicia.livetv.forgeTvTime(alert.airing.startMs) + " · " + channelName
