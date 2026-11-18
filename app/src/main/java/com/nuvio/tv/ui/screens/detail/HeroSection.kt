package com.nuvio.tv.ui.screens.detail

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.widthIn
import com.nuvio.tv.skin.kicia.nav.forgeTag
import com.nuvio.tv.ui.theme.NuvioMotion

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import android.util.Log
import com.nuvio.tv.R
import com.nuvio.tv.core.tracking.TrackingRatingState
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MDBListRatings
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.NextToWatch
import com.nuvio.tv.domain.model.isWatchedAsOneItem
import com.nuvio.tv.ui.components.ImdbRatingSourceLabel
import com.nuvio.tv.ui.components.MDBListRatingsRow
import com.nuvio.tv.ui.components.SynopsisDescription
import com.nuvio.tv.ui.theme.NuvioTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.painter.Painter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.ui.util.localizedGenreLabel
import com.nuvio.tv.ui.util.rememberLongPressKeyTracker
import com.nuvio.tv.ui.util.contentTextDirection
import com.nuvio.tv.ui.util.rememberRawSvgPainter
import java.util.Locale

private const val MAX_VISIBLE_HERO_GENRES = 6

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroContentSection(
    meta: Meta,
    nextEpisode: Video?,
    nextToWatch: NextToWatch?,
    onPlayClick: () -> Unit,
    isPlayEnabled: Boolean = true,
    onPlayLongPress: (() -> Unit)? = null,
    isInLibrary: Boolean,
    onToggleLibrary: () -> Unit,
    onLibraryLongPress: () -> Unit,
    isMovieWatched: Boolean,
    isMovieWatchedPending: Boolean,
    onToggleMovieWatched: () -> Unit,
    ratingState: TrackingRatingState = TrackingRatingState(),
    onRateClick: () -> Unit = {},
    trailerAvailable: Boolean = false,
    onTrailerClick: () -> Unit = {},
    showRandomEpisodeButton: Boolean = false,
    episodeShuffle: com.nuvio.tv.domain.model.EpisodeShuffleSettings = com.nuvio.tv.domain.model.EpisodeShuffleSettings(),
    shuffleActionPending: Boolean = false,
    onRandomEpisodeClick: () -> Unit = {},
    randomEpisodeFocusRequester: FocusRequester? = null,
    hideLogoDuringTrailer: Boolean = false,
    mdbListRatings: MDBListRatings? = null,
    mdbListRatingOrder: List<String> = com.nuvio.tv.domain.model.MDBListSettings.DEFAULT_RATING_ORDER,
    hideMetaInfoImdb: Boolean = false,
    tmdbRating: Float? = null,
    showFullReleaseDate: Boolean = true,
    isTrailerPlaying: Boolean = false,
    playButtonFocusRequester: FocusRequester? = null,
    restorePlayFocusToken: Int = 0,
    onHeroActionFocused: () -> Unit = {},
    onPlayFocusRestored: () -> Unit = {},
    onShowFullDescription: () -> Unit = {},
    onTruncationChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val isSeriesApi = remember(meta.apiType) {
        meta.apiType.equals("series", ignoreCase = true) || meta.apiType.equals("tv", ignoreCase = true)
    }
    val logoModel = remember(context, meta.logo) {
        meta.logo?.let { logo ->
            ImageRequest.Builder(context)
                .data(logo)
                .crossfade(true)
                .build()
        }
    }
    var logoLoadFailed by remember(meta.logo) { mutableStateOf(false) }
    val shouldShowLogo =
        !meta.logo.isNullOrBlank() &&
            !logoLoadFailed &&
            !(isTrailerPlaying && hideLogoDuringTrailer) &&
            !com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL // forge: the mockup writes the title (S03)
    val libraryAddPainter = rememberRawSvgPainter(rawIconRes = com.nuvio.tv.R.raw.library_add_plus)
    val trailerPainter = rememberRawSvgPainter(rawIconRes = com.nuvio.tv.R.raw.trailer_play_button)
    val strCreator = stringResource(R.string.hero_creator)
    val strDirector = stringResource(R.string.hero_director)
    val strWriter = stringResource(R.string.hero_writer)
    val creditLine = remember(meta.director, meta.writer, isSeriesApi) {
        val directorLine = meta.director.takeIf { it.isNotEmpty() }?.joinToString(", ")
        val writerLine = meta.writer.takeIf { it.isNotEmpty() }?.joinToString(", ")
        when {
            !directorLine.isNullOrBlank() && com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL && !isSeriesApi -> "Reżyseria: $directorLine" // forge: mockup wording (S03)
            !directorLine.isNullOrBlank() -> {
                if (isSeriesApi) strCreator.format(directorLine) else strDirector.format(directorLine)
            }
            !writerLine.isNullOrBlank() -> strWriter.format(writerLine)
            else -> null
        }
    }

    // Animate logo properties for trailer mode
    val logoHeight by animateDpAsState(
        targetValue = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) 38.dp else if (isTrailerPlaying) 60.dp else 100.dp, // forge hook v7.4 (04): logo tytułu wys. 38
        animationSpec = tween(600),
        label = "logoHeight"
    )
    val logoBottomPadding by animateDpAsState(
        targetValue = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) 8.dp else if (isTrailerPlaying) NuvioTheme.spacing.xl else NuvioTheme.spacing.lg, // forge hook v7.4 (04): meta 8 pod logo
        animationSpec = tween(600),
        label = "logoPadding"
    )
    val logoMaxWidth by animateFloatAsState(
        targetValue = if (isTrailerPlaying) 0.25f else 0.4f,
        animationSpec = tween(600),
        label = "logoWidth"
    )
    // During a trailer the hero fills the screen so the logo sits at the bottom
    // on screens taller than the hero (lower-density TV boxes).
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val heroHeight by animateDpAsState(
        targetValue = if (isTrailerPlaying) maxOf(540.dp, screenHeight) else 540.dp,
        animationSpec = tween(600),
        label = "heroHeight"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) { // forge: "Występują" on the right (mockup S03)
        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(
                    animationSpec = tween(600)
                )
                .padding(start = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) 40.dp else NuvioTheme.spacing.xxxl, end = NuvioTheme.spacing.xxxl, bottom = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) NuvioTheme.spacing.lg + 23.dp else NuvioTheme.spacing.lg), // forge: content edge x 58; v7.4 (04): rząd przycisków y 440
            verticalArrangement = Arrangement.Bottom
        ) {
            // Logo/Title — always visible during trailer, animates size
            if (shouldShowLogo) {
                AsyncImage(
                    model = logoModel,
                    contentDescription = meta.name,
                    onError = { logoLoadFailed = true },
                    modifier = Modifier
                        .height(logoHeight)
                        .fillMaxWidth(logoMaxWidth)
                        .padding(bottom = logoBottomPadding)
                        .forgeTag("szcz-logo"), // forge hook v7.4: nazwa z makiety
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )
            } else {
                // Text title hides entirely during trailer
                AnimatedVisibility(
                    visible = !isTrailerPlaying,
                    enter = fadeIn(tween(NuvioMotion.tokens.durations.overlay)),
                    exit = fadeOut(tween(NuvioMotion.tokens.durations.overlay))
                ) {
                    Text(
                        text = meta.name,
                        style = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) com.nuvio.tv.skin.kicia.tokens.ForgeText.display else MaterialTheme.typography.displayMedium, // forge: 40/44
                        color = NuvioTheme.colors.TextPrimary,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = NuvioTheme.spacing.sm).then(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) Modifier.height(44.dp) else Modifier).forgeTag("szcz-logo") // forge hook v7.4: brak logo = tytuł 40/44
                    )
                }
            }

            // Everything below the logo fades out during trailer
            AnimatedVisibility(
                visible = isTrailerPlaying && !hideLogoDuringTrailer,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(300))
            ) {
                Text(
                    text = stringResource(R.string.hero_press_back_trailer),
                    style = MaterialTheme.typography.labelMedium,
                    color = NuvioTheme.colors.TextTertiary,
                    modifier = Modifier.padding(bottom = NuvioTheme.spacing.sm)
                )
            }

            // Everything below the logo fades out during trailer
            AnimatedVisibility(
                visible = !isTrailerPlaying,
                enter = fadeIn(tween(NuvioMotion.tokens.durations.overlay)),
                exit = fadeOut(tween(NuvioMotion.tokens.durations.overlay))
            ) {
                Column {
                    val forgeLayout = com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL
                    if (forgeLayout) {
                        // forge (mockup v6 S03): type line, description, meta line, then buttons and "Gotowe"
                        com.nuvio.tv.skin.kicia.detail.ForgeDetailTypeLine(meta)
                        meta.description?.let { description ->
                            SynopsisDescription(
                                description = description,
                                maxLines = 4, // v7.4 (04): 4 lines
                                onShowFullDescription = onShowFullDescription,
                                upFocusRequester = null,
                                onFocused = onHeroActionFocused,
                                onTruncationChanged = onTruncationChanged,
                                modifier = (if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) Modifier.width(300.dp) else Modifier.fillMaxWidth(0.6f)).padding(bottom = NuvioTheme.spacing.md).forgeTag("szcz-opis") // forge hook v7.4 (04): opis szer. 300
                            )
                        }
                        com.nuvio.tv.skin.kicia.detail.ForgeDetailDataLine(meta, nextToWatch) // forge hook v7.4: year, time, badges, Gotowe
                    }
                    var forgePlayFocused by remember { mutableStateOf(false) } // forge hook v7.4 (04)
                    var forgePlayWidth by remember { mutableStateOf(0) }
                    val forgeSpread by androidx.compose.animation.core.animateFloatAsState(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL && forgePlayFocused) forgePlayWidth * (com.nuvio.tv.skin.kicia.tokens.ForgeDims.FocusScale - 1f) / 2f else 0f, com.nuvio.tv.skin.kicia.tokens.ForgeMotion.focusSpec(), label = "forgeHeroSpread")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.forgeTag("przycisk-glowny").onFocusChanged { forgePlayFocused = it.hasFocus }.onSizeChanged { forgePlayWidth = it.width }) { PlayButton( // forge hook v7.4: nazwa z makiety
                            text = if (isPlayEnabled) nextToWatch?.displayText else stringResource(R.string.playback_unavailable),
                            enabled = isPlayEnabled,
                            onClick = onPlayClick,
                            onLongPress = onPlayLongPress,
                            focusRequester = playButtonFocusRequester,
                            restoreFocusToken = restorePlayFocusToken,
                            onFocusRestored = {
                                onHeroActionFocused()
                                onPlayFocusRestored()
                            }
                        ) }

                        Row(Modifier.graphicsLayer { translationX = forgeSpread }, horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) { // forge hook v7.4 (04): sąsiedzi odsuwają się przy fokusie „Odtwórz” (--rozsun)
                        if (trailerAvailable) { // forge hook v7.4 (04): "Zwiastun" is a labelled secondary button right after Odtwórz
                            if (forgeLayout) com.nuvio.tv.skin.kicia.components.ForgeTagged("przycisk-dodatkowy") { com.nuvio.tv.skin.kicia.components.ForgeButton(
                                text = "Zwiastun", icon = androidx.compose.material.icons.Icons.Outlined.Movie,
                                modifier = Modifier.onFocusChanged { if (it.isFocused) onHeroActionFocused() },
                                onOk = onTrailerClick,
                            ) } else ActionIconButtonPainter(
                                painter = trailerPainter,
                                contentDescription = stringResource(R.string.hero_play_trailer),
                                onClick = onTrailerClick,
                                onFocused = onHeroActionFocused
                            )
                        }

                        Box(Modifier.forgeTag("przycisk-okragly.0")) { ActionIconButton( // forge hook v7.4
                            icon = if (isInLibrary) Icons.Default.Check else null,
                            painter = if (!isInLibrary) {
                                libraryAddPainter
                            } else {
                                null
                            },
                            contentDescription = if (isInLibrary) stringResource(R.string.hero_remove_from_library) else stringResource(R.string.hero_add_to_library),
                            onClick = onToggleLibrary,
                            onLongPress = onLibraryLongPress,
                            onFocused = onHeroActionFocused
                        ) }

                        if (meta.isWatchedAsOneItem()) Box(Modifier.forgeTag("przycisk-okragly.1")) { // forge hook v7.4
                            ActionIconButton(
                                icon = if (isMovieWatched) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (isMovieWatched) {
                                    stringResource(R.string.hero_mark_unwatched)
                                } else {
                                    stringResource(R.string.hero_mark_watched)
                                },
                                onClick = onToggleMovieWatched,
                                enabled = !isMovieWatchedPending,
                                selected = isMovieWatched,
                                toggleable = true,
                                selectedContainerColor = Color.White,
                                selectedContentColor = Color.Black,
                                onFocused = onHeroActionFocused
                            )
                        }

                        if (ratingState.isAvailable) {
                            RatingActionButton(
                                rating = ratingState.rating,
                                onClick = onRateClick,
                                onFocused = onHeroActionFocused
                            )
                        }

                        if (showRandomEpisodeButton) {
                            ActionIconButton(
                                icon = if (episodeShuffle.enabled) Icons.Default.Stop else Icons.Default.Shuffle,
                                contentDescription = stringResource(
                                    if (episodeShuffle.enabled) R.string.shuffle_stop else R.string.random_episode_title
                                ),
                                onClick = { if (!shuffleActionPending) onRandomEpisodeClick() },
                                focusRequester = randomEpisodeFocusRequester,
                                onFocused = onHeroActionFocused
                            )
                        }
                        } // forge hook v7.4 (04): koniec grupy odsuwanej
                    }

                    // forge hook: "Gotowe" + best source right under the buttons (mockup S03)
                    if (!forgeLayout) com.nuvio.tv.skin.kicia.detail.ForgeReadyLine(meta = meta, nextToWatch = nextToWatch) // v7.4: in forge layout it is in the data line
                    Spacer(modifier = Modifier.height(NuvioTheme.spacing.lg))
                    if (!forgeLayout) {

                    // Director/Writer line above description
                    if (!creditLine.isNullOrBlank()) {
                        Text(
                            text = creditLine,
                            style = MaterialTheme.typography.labelLarge.copy(
                                textDirection = creditLine.contentTextDirection()
                            ),
                            color = NuvioTheme.extendedColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        )
                        Spacer(modifier = Modifier.height(NuvioTheme.spacing.md))
                    }

                    if (mdbListRatings?.isEmpty() == false) { // forge (owner 10.11): the other ratings (MDBList) are back under the cast line
                        MDBListRatingsRow(ratings = mdbListRatings, order = mdbListRatingOrder)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    meta.description?.let { description ->
                        SynopsisDescription(
                            description = description,
                            maxLines = 3, // forge: 3 lines, OK shows the full text (mockup S03)
                            onShowFullDescription = onShowFullDescription,
                            upFocusRequester = playButtonFocusRequester,
                            onFocused = onHeroActionFocused,
                            onTruncationChanged = onTruncationChanged,
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .padding(bottom = NuvioTheme.spacing.md)
                        )
                    }

                    if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) com.nuvio.tv.skin.kicia.detail.ForgeDetailMetaLine(meta, tmdbRating) else // forge hook: one meta line (S03)
                    MetaInfoRow(
                        meta = meta,
                        hideImdbRating = hideMetaInfoImdb,
                        showFullReleaseDate = showFullReleaseDate,
                        tmdbRating = tmdbRating
                    )
                    }
                }
            }
        }
        if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL && !isTrailerPlaying && meta.cast.isNotEmpty()) {
            // v7.4 (04): one text "Występują: ..." 12/16 at 70%, width 300, right edge x 920, up to 3 lines, level with the buttons
            Text(
                "Występują: " + meta.cast.take(4).joinToString(", ") + if (meta.cast.size > 4) " i inni" else "",
                style = com.nuvio.tv.skin.kicia.tokens.ForgeText.caption, maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(340.dp).padding(end = 40.dp, bottom = 56.dp).forgeTag("szcz-prawa"), // forge hook v7.4: dół na wysokości przycisków (y 484)
            )
        }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun PlayButton(
    text: String?,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    restoreFocusToken: Int = 0,
    onFocusRestored: () -> Unit = {}
) {
    var longPressTriggered by remember { mutableStateOf(false) }
    val longPressKeyTracker = rememberLongPressKeyTracker()
    val playFallbackLabel = stringResource(R.string.hero_play)

    LaunchedEffect(restoreFocusToken) {
        if (restoreFocusToken > 0 && focusRequester != null) {
            focusRequester.requestFocusAfterFrames()
        }
    }
    val playPainter = rememberRawSvgPainter(rawIconRes = com.nuvio.tv.R.raw.ic_player_play)

    Button(
        enabled = enabled,
        onClick = {
            if (longPressTriggered) {
                longPressTriggered = false
            } else {
                onClick()
            }
        },
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocusRestored()
                }
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount == 0 && isSelectKey(native.keyCode)) {
                    longPressTriggered = false
                }
                if (enabled && onLongPress != null && native.action == AndroidKeyEvent.ACTION_DOWN) {
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                        longPressTriggered = true
                        if (native.repeatCount == 0) onLongPress()
                        return@onPreviewKeyEvent true
                    }
                }
                if (enabled && onLongPress != null &&
                    longPressKeyTracker.handle(native, ::isSelectKey) {
                        longPressTriggered = true
                        onLongPress()
                    }
                ) {
                    if (native.action == AndroidKeyEvent.ACTION_UP) {
                        longPressTriggered = false
                    }
                    return@onPreviewKeyEvent true
                }

                if (native.action == AndroidKeyEvent.ACTION_UP && longPressTriggered) {
                    if (isSelectOrMenuKey(native.keyCode)) {
                        longPressTriggered = false
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .focusProperties { up = FocusRequester.Cancel }
            .then(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) Modifier.widthIn(min = 160.dp) else Modifier) // forge hook v7.4 (04): --przycisk-min-szer 160
            .then(
                if (text == null) {
                    Modifier.semantics { this.contentDescription = playFallbackLabel }
                } else {
                    Modifier
                }
            ),
        colors = ButtonDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.White,
            focusedContainerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = androidx.compose.ui.graphics.Color.Black,
            focusedContentColor = androidx.compose.ui.graphics.Color.Black
        ),
        shape = ButtonDefaults.shape(
            shape = RoundedCornerShape(NuvioTheme.spacing.xxl)
        ),
        border = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) ButtonDefaults.border(focusedBorder = Border.None) else ButtonDefaults.border( // forge hook v7.4 (04): fokus = skala 1,125, bez obwódki
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.spacing.xxl)
            )
        ),
        scale = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) ButtonDefaults.scale(focusedScale = com.nuvio.tv.skin.kicia.tokens.ForgeDims.FocusScale) else ButtonDefaults.scale(),
        contentPadding = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) PaddingValues(horizontal = 20.dp, vertical = 12.dp) else PaddingValues(horizontal = NuvioTheme.spacing.xl, vertical = 14.dp) // forge: 44 dp (S03)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm),
            modifier = Modifier.animateContentSize(
                animationSpec = tween(
                    durationMillis = NuvioMotion.tokens.durations.fast,
                    easing = NuvioMotion.tokens.easings.standard
                )
            )
        ) {
            Icon(
                painter = playPainter,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            AnimatedVisibility(
                visible = text != null,
                enter = fadeIn(animationSpec = tween(NuvioMotion.tokens.durations.fast)),
                exit = fadeOut(animationSpec = tween(NuvioMotion.tokens.durations.quick))
            ) {
                Text(
                    text = text ?: "",
                    style = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) com.nuvio.tv.skin.kicia.tokens.ForgeText.button.copy(color = androidx.compose.ui.graphics.Color.Black) else MaterialTheme.typography.labelLarge // forge: 15 bold
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ActionIconButtonPainter(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    onFocused: () -> Unit = {},
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) 44.dp else NuvioTheme.spacing.xxxl) // forge: 44
            .onFocusChanged { state ->
                if (state.isFocused) onFocused()
            }
            .focusProperties { up = FocusRequester.Cancel },
        colors = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) IconButtonDefaults.colors( // forge: round glass buttons, white when focused (S03)
            containerColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Surface,
            focusedContainerColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Focus,
            contentColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Text,
            focusedContentColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.OnFocus
        ) else IconButtonDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.Secondary,
            contentColor = NuvioTheme.colors.TextPrimary,
            focusedContentColor = NuvioTheme.colors.OnSecondary
        ),
        border = IconButtonDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = CircleShape
            )
        ),
        shape = IconButtonDefaults.shape(
            shape = CircleShape
        )
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: Painter? = null,
    contentDescription: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    // When true this icon button is a state toggle (watched/unwatched) and announces itself as
    // such; the library add/remove button is a plain action and leaves this off.
    toggleable: Boolean = false,
    selectedContainerColor: Color = Color(0xFF7CFF9B),
    selectedContentColor: Color = Color.Black,
    onFocused: () -> Unit = {},
    focusRequester: FocusRequester? = null
) {
    var longPressTriggered by remember { mutableStateOf(false) }
    val longPressKeyTracker = rememberLongPressKeyTracker()

    IconButton(
        onClick = {
            if (longPressTriggered) {
                longPressTriggered = false
            } else {
                onClick()
            }
        },
        enabled = enabled,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) 44.dp else NuvioTheme.spacing.xxxl) // forge: 44
            .onFocusChanged { state ->
                if (state.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount == 0 && isSelectKey(native.keyCode)) {
                    longPressTriggered = false
                }
                if (onLongPress != null && native.action == AndroidKeyEvent.ACTION_DOWN) {
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                        longPressTriggered = true
                        if (native.repeatCount == 0) onLongPress()
                        return@onPreviewKeyEvent true
                    }
                }
                if (onLongPress != null &&
                    longPressKeyTracker.handle(native, ::isSelectKey) {
                        longPressTriggered = true
                        onLongPress()
                    }
                ) {
                    if (native.action == AndroidKeyEvent.ACTION_UP) {
                        longPressTriggered = false
                    }
                    return@onPreviewKeyEvent true
                }

                if (native.action == AndroidKeyEvent.ACTION_UP && longPressTriggered) {
                    if (isSelectOrMenuKey(native.keyCode)) {
                        longPressTriggered = false
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .focusProperties { up = FocusRequester.Cancel }
            .semantics {
                if (toggleable) {
                    role = Role.Checkbox
                    toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
                }
            },
        colors = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_DETAIL) IconButtonDefaults.colors( // forge: round glass buttons; on = green icon (S03)
            containerColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Surface,
            focusedContainerColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Focus,
            contentColor = if (selected || icon == Icons.Default.Check) com.nuvio.tv.skin.kicia.tokens.ForgeColors.Ok else com.nuvio.tv.skin.kicia.tokens.ForgeColors.Text,
            focusedContentColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.OnFocus
        ) else IconButtonDefaults.colors(
            containerColor = if (selected) selectedContainerColor else NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.Secondary,
            contentColor = if (selected) selectedContentColor else NuvioTheme.colors.TextPrimary,
            focusedContentColor = NuvioTheme.colors.OnSecondary
        ),
        border = IconButtonDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = CircleShape
            )
        ),
        shape = IconButtonDefaults.shape(
            shape = CircleShape
        )
    ) {
        when {
            painter != null -> Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(NuvioTheme.spacing.xl)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MetaInfoRow(
    meta: Meta,
    hideImdbRating: Boolean,
    showFullReleaseDate: Boolean = true,
    tmdbRating: Float? = null
) {
    val context = LocalContext.current
    val genresText = remember(meta.genres) {
        meta.genres
            .take(MAX_VISIBLE_HERO_GENRES)
            .joinToString(" • ") { localizedGenreLabel(context, it) }
    }
    val runtimeText = remember(meta.runtime) { meta.runtime?.let { formatRuntime(it) } }
    val yearText = remember(meta.releaseInfo, meta.released, meta.type, showFullReleaseDate) {
        if (showFullReleaseDate && meta.type == ContentType.MOVIE) {
            meta.released
                ?.let { runCatching { java.time.OffsetDateTime.parse(it).toLocalDate() }.getOrNull() }
                ?.let { val locale = java.util.Locale.getDefault(); java.text.SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMMMy"), locale).format(java.util.Date(it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())) }
                ?: formatYearRange(meta.releaseInfo)
        } else {
            formatYearRange(meta.releaseInfo)
        }
    }
    val imdbRating = if (hideImdbRating) null else meta.imdbRating
    val reserveImdbRatingHeight = meta.imdbRating != null
    val shouldShowImdbRating = imdbRating != null
    val shouldShowTmdbRating = tmdbRating != null
    val tmdbModel = remember(context) {
        ImageRequest.Builder(context)
            .data(com.nuvio.tv.R.raw.mdblist_tmdb)
            .build()
    }
    val ageRatingBadge = remember(meta.ageRating) {
        meta.ageRating?.trim()?.takeIf { it.isNotBlank() }
    }
    val isSeries = meta.type == ContentType.SERIES || meta.type == ContentType.TV
    val strStatusEnded = stringResource(if (isSeries) R.string.series_status_ended else R.string.movie_status_ended)
    val strStatusContinuing = stringResource(if (isSeries) R.string.series_status_continuing else R.string.movie_status_continuing)
    val strStatusCurrent = stringResource(if (isSeries) R.string.series_status_current else R.string.movie_status_current)
    val strStatusCancelled = stringResource(if (isSeries) R.string.series_status_cancelled else R.string.movie_status_cancelled)
    val strStatusReleased = stringResource(if (isSeries) R.string.series_status_released else R.string.movie_status_released)
    val strStatusPlanned = stringResource(if (isSeries) R.string.series_status_planned else R.string.movie_status_planned)
    val strStatusRumored = stringResource(if (isSeries) R.string.series_status_rumored else R.string.movie_status_rumored)
    val strStatusInProduction = stringResource(if (isSeries) R.string.series_status_in_production else R.string.movie_status_in_production)
    val strStatusPostProduction = stringResource(if (isSeries) R.string.series_status_post_production else R.string.movie_status_post_production)
    val statusBadge = remember(meta.status, isSeries) {
        when (meta.status?.trim()?.lowercase()) {
            "ended" -> strStatusEnded.uppercase()
            "continuing", "returning series" -> strStatusContinuing.uppercase()
            "current" -> strStatusCurrent.uppercase()
            "cancelled", "canceled" -> strStatusCancelled.uppercase()
            "released" -> strStatusReleased.uppercase()
            "planned" -> strStatusPlanned.uppercase()
            "rumored" -> strStatusRumored.uppercase()
            "in production" -> strStatusInProduction.uppercase()
            "post production" -> strStatusPostProduction.uppercase()
            else -> meta.status?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
        }
    }
    Log.d("HeroBadge", "name=${meta.name} ageRating=${meta.ageRating} status=${meta.status} ageRatingBadge=$ageRatingBadge statusBadge=$statusBadge")
    val secondaryItems = remember(runtimeText, meta.country, meta.language) {
        buildList<String> {
            runtimeText?.takeIf { it.isNotBlank() }?.let { add(it) }
            meta.country?.trim()?.takeIf { it.isNotBlank() }?.let { add(normalizeCountryLabel(it)) }
            meta.language?.trim()?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
        // Primary row: Genres, Release, Ratings
        Row(
            modifier = if (reserveImdbRatingHeight) Modifier.height(30.dp) else Modifier,
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (meta.genres.isNotEmpty()) {
                Text(
                    text = genresText,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelLarge,
                    color = NuvioTheme.extendedColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (yearText != null || shouldShowImdbRating || shouldShowTmdbRating) {
                    MetaInfoDivider()
                }
            }

            yearText?.let { year ->
                Text(
                    text = year,
                    style = MaterialTheme.typography.labelLarge,
                    color = NuvioTheme.extendedColors.textSecondary
                )
                if (shouldShowImdbRating || shouldShowTmdbRating) {
                    MetaInfoDivider()
                }
            }

            imdbRating?.let { rating ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
                ) {
                    ImdbRatingSourceLabel(
                        logoModifier = Modifier.size(30.dp),
                        textStyle = MaterialTheme.typography.labelLarge,
                        textColor = NuvioTheme.extendedColors.textSecondary
                    )
                    val ratingText = remember(rating) { String.format("%.1f", rating) }
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.labelLarge,
                        color = NuvioTheme.extendedColors.textSecondary
                    )
                }
            }

            tmdbRating?.let { rating ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
                ) {
                    AsyncImage(
                        model = tmdbModel,
                        contentDescription = null,
                        modifier = Modifier.size(NuvioTheme.spacing.xl),
                        contentScale = ContentScale.Fit
                    )
                    val ratingText = remember(rating) { (rating * 10).toInt().toString() }
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.labelLarge,
                        color = NuvioTheme.extendedColors.textSecondary
                    )
                }
            }
        }

        // Secondary row: Runtime, Age Rating, Status, Country, Language
        if (ageRatingBadge != null || statusBadge != null || secondaryItems.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ageRatingBadge != null && statusBadge != null) {
                    CombinedMetaBadge(
                        leftText = ageRatingBadge,
                        leftColor = NuvioTheme.colors.TextSecondary,
                        rightText = statusBadge,
                        rightColor = NuvioTheme.colors.TextPrimary
                    )
                } else {
                    ageRatingBadge?.let { badge ->
                        HeroMetaBadge(text = badge)
                    }
                    statusBadge?.let { badge ->
                        HeroMetaBadge(
                            text = badge,
                            contentColor = NuvioTheme.colors.TextPrimary
                        )
                    }
                }
                if ((ageRatingBadge != null || statusBadge != null) && secondaryItems.isNotEmpty()) {
                    MetaInfoDivider()
                }
                secondaryItems.forEachIndexed { index, value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        color = NuvioTheme.colors.TextPrimary
                    )
                    if (index < secondaryItems.lastIndex) {
                        MetaInfoDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetaBadge(
    text: String,
    contentColor: Color = NuvioTheme.colors.TextSecondary
) {
    Box(
        modifier = Modifier
            .border(
                border = BorderStroke(NuvioTheme.spacing.hairline, contentColor.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = NuvioTheme.spacing.sm, vertical = NuvioTheme.spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun CombinedMetaBadge(
    leftText: String,
    leftColor: Color = NuvioTheme.colors.TextSecondary,
    rightText: String,
    rightColor: Color = NuvioTheme.colors.TextPrimary
) {
    val dividerColor = leftColor.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .border(
                border = BorderStroke(NuvioTheme.spacing.hairline, dividerColor),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = NuvioTheme.spacing.sm, vertical = NuvioTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
    ) {
        Text(
            text = leftText,
            style = MaterialTheme.typography.labelMedium,
            color = leftColor,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .width(NuvioTheme.spacing.hairline)
                .height(NuvioTheme.spacing.md)
                .background(dividerColor)
        )
        Text(
            text = rightText,
            style = MaterialTheme.typography.labelMedium,
            color = rightColor,
            maxLines = 1
        )
    }
}

private fun normalizeCountryLabel(raw: String): String {
    val displayLocale = Locale.getDefault()
    return raw
        .split(",")
        .joinToString(", ") { part ->
            val code = part.trim()
            if (code.matches(Regex("[A-Za-z]{2}"))) {
                Locale("", code).getDisplayCountry(displayLocale).takeIf { it.isNotBlank() } ?: code
            } else {
                code
            }
        }
}

private fun isSelectKey(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
}

private fun isSelectOrMenuKey(keyCode: Int): Boolean {
    return isSelectKey(keyCode) || keyCode == AndroidKeyEvent.KEYCODE_MENU
}


private fun formatYearRange(releaseInfo: String?): String? {
    if (releaseInfo.isNullOrBlank()) return null
    return releaseInfo.trim()
}

/**
 * Null when the runtime is zero or unparseable-as-positive.
 *
 * TMDB answers with runtime 0, not null, for a title whose length it does not know yet, so
 * every branch below could reach the end with a total of zero and render a literal "0m" in the
 * metadata row. The caller already drops a null.
 */
private fun formatRuntime(runtime: String): String? {
    val trimmed = runtime.trim()
    // Already in "Xh Ym" or "Xh" format
    if (trimmed.contains('h') || trimmed.contains('m')) {
        val hours = Regex("(\\d+)\\s*h").find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = Regex("(\\d+)\\s*m").find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = hours * 60 + mins
        if (total > 0) return if (total >= 60) {
            val h = total / 60; val m = total % 60
            if (m > 0) "${h}h ${m}m" else "${h}h"
        } else "${total}m"
    }
    // "H:MM" or "HH:MM" format
    if (trimmed.contains(':')) {
        val parts = trimmed.split(':')
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val mins = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val total = hours * 60 + mins
        if (total > 0) return if (total >= 60) {
            val m = total % 60
            if (m > 0) "${hours}h ${m}m" else "${hours}h"
        } else "${total}m"
    }
    // Plain number (minutes)
    val minutes = trimmed.filter { it.isDigit() }.toIntOrNull() ?: return runtime
    if (minutes <= 0) return null
    return if (minutes >= 60) {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MetaInfoDivider() {
    Text(
        text = "•",
        style = MaterialTheme.typography.labelLarge,
        color = NuvioTheme.extendedColors.textTertiary
    )
}
