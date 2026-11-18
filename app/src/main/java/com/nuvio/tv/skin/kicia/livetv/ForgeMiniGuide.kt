package com.nuvio.tv.skin.kicia.livetv

import androidx.compose.material.icons.filled.Replay
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import com.nuvio.tv.skin.kicia.nav.forgeTagRoot
import androidx.compose.ui.platform.testTag
import android.view.KeyEvent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.window.DialogProperties
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.components.ForgeAutoLogo
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPlayerState
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.onEvent

/** The element the remote is on inside the MiniGuide (mockup S09; the card draws it, keys move it). */
internal enum class ForgeMiniSlot { GUIDE, TILE, BAR, INFO, RESTART, REMIND, FAV, NEXT }

/**
 * Remote navigation of the S09 MiniGuide. Reshaped keeps the card itself (OK shows it, its timer hides it,
 * ▲▼ zap); while it is up, this moves a highlight between [Przewodnik] [channel tile] [Info][Od początku][❚❚][☆] [Dalej]
 * and acts on OK. ←→ on the tile look ahead in the channel's schedule (Teraz → Następnie → Później).
 */
internal object ForgeMiniGuideNav {
    var slot by mutableStateOf(ForgeMiniSlot.TILE)
    /** 0 = on now; k > 0 = the k-th programme after it. */
    var ahead by mutableIntStateOf(0)
    var paused by mutableStateOf(false)
    /** The programme whose details (S08) are open over the player. */
    var detail by mutableStateOf<Pair<LiveTvChannel, LiveTvProgramme>?>(null)
    internal var onGuide: (() -> Unit)? = null
    internal var appContext: android.content.Context? = null

    fun reset() {
        slot = ForgeMiniSlot.TILE
        ahead = 0
    }

    /** The slots the card shows right now, in ◀▶ order of the button row. */
    internal fun buttons(ahead: Int, startOver: Boolean, hasNext: Boolean): List<ForgeMiniSlot> = buildList {
        add(ForgeMiniSlot.INFO)
        if (ahead == 0 && startOver) add(ForgeMiniSlot.RESTART)
        if (ahead > 0) add(ForgeMiniSlot.REMIND)
        add(ForgeMiniSlot.FAV)
        if (hasNext) add(ForgeMiniSlot.NEXT)
    }

    /** The key handler while the card is up: always consumes (Reshaped's card owns the keys then). */
    fun onKey(state: LiveTvPlayerState, event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        // Owner 10.11: a channel outside the zap list (hidden, filtered, a search) made this return false, and the key fell through
        // to the player's own controls. The channel is found among all of them too.
        val channel = state.zapList().firstOrNull { it.streamUrl == state.currentListUrl }
            ?: LiveTvRepository.uiState.value.channels.firstOrNull { it.streamUrl == state.currentListUrl }
            ?: return false
        val now = LiveTvClock.nowEpochMs()
        val onNow = LiveTvRepository.uiState.value.currentProgrammes[channel.guideKey]
        val upcoming = forgeUpcoming(LiveTvRepository.schedule(channel.guideKey), onNow, now)
        val shown = if (ahead == 0) onNow else upcoming.getOrNull(ahead - 1)
        val next = if (ahead == 0) upcoming.firstOrNull() else upcoming.getOrNull(ahead)
        val startOver = state.startOverProgramme(channel) != null
        val row = buttons(ahead, startOver, next != null)
        // The programme bar can be entered (▲ from the buttons) when the channel keeps an archive: ◀▶ / OK then open the rewind bar.
        val scrubbable = channel.catchup != null && ahead == 0
        if (slot != ForgeMiniSlot.GUIDE && slot != ForgeMiniSlot.TILE && !(slot == ForgeMiniSlot.BAR && channel.catchup != null && ahead == 0) && slot !in row) slot = row.first()
        val ok = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (!down) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (ahead > 0) { ahead = 0; state.showInfo() } else state.hideInfo()
            }
            return true
        }
        if (event.repeatCount > 0 && ok) return true
        state.showInfo() // each key keeps the card up
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> when (slot) {
                ForgeMiniSlot.TILE, ForgeMiniSlot.GUIDE -> { paused = false; ahead = 0; state.zap(-1); state.showInfo() }
                ForgeMiniSlot.BAR -> slot = ForgeMiniSlot.TILE
                else -> slot = if (scrubbable) ForgeMiniSlot.BAR else ForgeMiniSlot.TILE
            }
            // UX (owner 9.11): ▲▼ on the tile always change the channel, like the hidden UI; the buttons are reached
            // with OK, and ↓ from the buttons opens the full controls (tracks, subtitles).
            KeyEvent.KEYCODE_DPAD_DOWN -> when (slot) {
                ForgeMiniSlot.TILE, ForgeMiniSlot.GUIDE -> { paused = false; ahead = 0; state.zap(1); state.showInfo() }
                ForgeMiniSlot.BAR -> slot = row.first()
                // Owner 10.11: the player's own controls are never opened from here; ▼ on the buttons stays where it is.
                else -> Unit
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> when (slot) {
                ForgeMiniSlot.BAR -> if (event.repeatCount == 0) state.openScrub(channel, -1, keepInfo = true) // v7.4 (07): przewijanie na pasku MiniGuide
                ForgeMiniSlot.GUIDE -> Unit
                ForgeMiniSlot.TILE -> if (ahead > 0) ahead-- else slot = ForgeMiniSlot.GUIDE
                else -> { val i = row.indexOf(slot); slot = if (i <= 0) ForgeMiniSlot.TILE else row[i - 1] }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> when (slot) {
                ForgeMiniSlot.BAR -> if (event.repeatCount == 0) state.openScrub(channel, 1, keepInfo = true)
                ForgeMiniSlot.GUIDE -> slot = ForgeMiniSlot.TILE
                // Owner 9.11: ▶ on the tile always goes to the buttons (no browsing of future programmes: they cannot be played).
                ForgeMiniSlot.TILE -> slot = row.first()
                else -> { val i = row.indexOf(slot); if (i in 0 until row.lastIndex) slot = row[i + 1] }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> when (slot) {
                ForgeMiniSlot.GUIDE -> { state.hideInfo(); onGuide?.invoke() }
                ForgeMiniSlot.TILE -> if (ahead > 0) ahead = 0 else slot = row.first()
                ForgeMiniSlot.INFO -> shown?.let { detail = channel to it }
                ForgeMiniSlot.RESTART -> state.startOverProgramme(channel)?.let { state.hideInfo(); state.playCatchup(channel, it) }
                ForgeMiniSlot.REMIND -> if (shown != null) appContext?.let {
                    com.nuvio.tv.fork.tv.ForgeReminders.toggle(it, com.nuvio.tv.fork.tv.ForgeReminder(channel.streamUrl, shown.startEpochMs, shown.title, channel.name))
                }
                ForgeMiniSlot.BAR -> state.openScrub(channel, 0, keepInfo = true)
                ForgeMiniSlot.FAV -> LiveTvRepository.toggleFavorite(channel)
                ForgeMiniSlot.NEXT -> next?.let { detail = channel to it }
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> Unit // acts on release
            else -> return false
        }
        return true
    }
}

/** The channel's programmes after the one on now (up to 40, mockup: "Następnie/Później z EPG tego kanału"). */
internal fun forgeUpcoming(schedule: List<LiveTvProgramme>, onNow: LiveTvProgramme?, nowMs: Long): List<LiveTvProgramme> {
    val from = onNow?.stopEpochMs ?: nowMs
    return schedule.filter { it.startEpochMs >= from }.sortedBy { it.startEpochMs }.distinctBy { it.startEpochMs }.take(40)
}

/** "zostało 8 min" */
internal fun forgeLeft(stopMs: Long, nowMs: Long): String = "zostało " + ((stopMs - nowMs + 59_999) / 60_000).coerceAtLeast(0) + " min"

private val ScrimBase = Color(0xFF06080A)
private val MiniScrim = Brush.verticalGradient(
    0f to Color.Transparent,
    0.3f to ScrimBase.copy(alpha = .72f), // v6 fix 9.11: stream subtitles / burnt-in text no longer show through the MiniGuide texts
    0.6f to ScrimBase.copy(alpha = .93f),
    1f to ScrimBase.copy(alpha = .97f),
)
private val TitleStyle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = ForgeColors.Text, fontFamily = ForgeText.head.fontFamily)

/**
 * S09 MiniGuide of the mockup (v4), drawn in place of Reshaped's channel info card (same data):
 * a bottom gradient with [Przewodnik], the channel tile 220×104 with ▲▼, "Na żywo" + times, the title,
 * the progress bar, [Info][Od początku][❚❚][☆] and › Dalej with its thumbnail. Absolute positions from
 * LAYOUT_SPEC (board 960×540 dp; the band starts at y 340).
 */
@Composable
fun ForgeMiniGuideCard(
    channel: LiveTvChannel,
    logo: String?,
    now: LiveTvProgramme?,
    next: LiveTvProgramme?,
    clockMs: Long,
    startOver: Boolean,
    rewind: Boolean,
    number: Int = 0,
    /** The past programme being replayed (not live); null while watching live. */
    replay: LiveTvProgramme? = null,
    /** Where the picture is, in the channel's time (now when live), for the bar of a replay. */
    playheadMs: Long = 0L,
    /** v7.4 (07): the time picked while rewinding on this bar (the MiniGuide stays up); null when not rewinding. */
    scrubMs: Long? = null,
) {
    val nav = ForgeMiniGuideNav
    LaunchedEffect(channel.streamUrl) { nav.reset() }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    nav.onGuide = { dispatcher?.onBackPressed() }
    val context = androidx.compose.ui.platform.LocalContext.current
    nav.appContext = context.applicationContext
    LaunchedEffect(Unit) { com.nuvio.tv.fork.tv.ForgeReminders.load(context.applicationContext) }
    val reminders by com.nuvio.tv.fork.tv.ForgeReminders.list.collectAsState()
    val live by LiveTvRepository.uiState.collectAsState()
    val favorite = channel.streamUrl in live.favoriteUrls
    val upcoming = forgeUpcoming(LiveTvRepository.schedule(channel.guideKey), now, clockMs)
    val ahead = nav.ahead.coerceAtMost(upcoming.size)
    // v7.4 (07): while rewinding, the programme under the picked time (the title and hours follow the marker)
    val scrubProgramme = scrubMs?.let { t -> LiveTvRepository.schedule(channel.guideKey).firstOrNull { t >= it.startEpochMs && t < it.stopEpochMs } }
    val shown = scrubProgramme ?: if (replay != null) replay else if (ahead == 0) now else upcoming.getOrNull(ahead - 1)
    val after = if (ahead == 0) (next ?: upcoming.firstOrNull()) else upcoming.getOrNull(ahead)
    val row = ForgeMiniGuideNav.buttons(ahead, startOver, after != null)
    val slot = nav.slot

    // v7.4 (07): everything stands on the bottom line y 500 (40 above the edge); x 40: [Przewodnik] 40 [Karta/Kanał 240x135] 40
    // [Plakietka + godziny 14, tytuł 20/700, Pasek postępu, Przyciski okrągłe 36] ... [Karta/Dalej 160x90] to x 920.
    // Tiles without the focus are dimmed 30%; the focus = full brightness and scale 1.125, no outline.
    Row(
        Modifier.fillMaxWidth().height(200.dp).background(MiniScrim).forgeTagRoot()
            .padding(start = ForgeDims.ContentStart, end = ForgeDims.ContentEnd, bottom = 40.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(Modifier.height(135.dp), contentAlignment = Alignment.CenterStart) {
            ForgeMiniPill("Przewodnik", Icons.Outlined.ViewAgenda, slot == ForgeMiniSlot.GUIDE, Modifier.testTag("przycisk-maly.0"))
        }
        Spacer(Modifier.width(40.dp))

        val tileFocused = slot == ForgeMiniSlot.TILE
        val tileScale by androidx.compose.animation.core.animateFloatAsState(if (tileFocused) ForgeDims.FocusScale else 1f, com.nuvio.tv.skin.kicia.tokens.ForgeMotion.focusSpec(), label = "miniTile")
        Box(Modifier.size(240.dp, 135.dp).testTag("mini-kafel.0")) {
            val tileShape = RoundedCornerShape(ForgeDims.RadiusCard)
            Box(
                Modifier.fillMaxSize().graphicsLayer { scaleX = tileScale; scaleY = tileScale; alpha = if (tileFocused) 1f else 0.7f }
                    .clip(tileShape).background(ForgeColors.CardBase).testTag("karta.0"),
            ) {
                // The guide's picture, else the channel's last advert-free frame (ForgeChannelSnapshots); no picture: the logo on the dark surface.
                val picture: Any? = shown?.image?.takeIf { it.isNotBlank() } ?: if (ahead == 0) ForgeChannelSnapshots.fileFor(channel.guideKey) else null
                val url = logo ?: forgeChannelLogo(channel)
                if (picture != null) AsyncImage(model = picture, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else if (!url.isNullOrBlank()) ForgeAutoLogo(url = url, contentDescription = channel.name, modifier = Modifier.align(Alignment.Center).size(96.dp, 48.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = .78f))))
                Row(Modifier.padding(start = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (number > 0) Text(number.toString(), style = ForgeText.captionBold, modifier = Modifier.padding(end = 8.dp).testTag("kanal-nr.0"))
                    if (replay == null && ahead == 0) com.nuvio.tv.skin.kicia.components.ForgeBadge("Na żywo", com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Live, Modifier.testTag("plakietka.0"))
                    // Owner 12.11: no ⟲ on the tile – it looked like an „Od początku” button that cannot be picked; the ⏮ in the row beside says it
                }
                Column(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                    Text(channel.name, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("karta-podpis-tytul.0"))
                    Text(shown?.title.orEmpty(), style = ForgeText.caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("karta-podpis-meta.0"))
                }
            }
            // ▲▼ over and under the tile, outside the layout
            Icon(Icons.Filled.KeyboardArrowUp, null, tint = ForgeColors.Text2, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-26).dp).size(20.dp))
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = ForgeColors.Text2, modifier = Modifier.align(Alignment.BottomCenter).offset(y = 26.dp).size(20.dp))
        }
        Spacer(Modifier.width(40.dp))

        // Now (or the programme looked ahead to): badge + hours y 376, title y 406, bar, buttons y 464
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(22.dp).testTag("mini-meta.0")) {
                when {
                    replay != null -> com.nuvio.tv.skin.kicia.components.ForgeBadge("Z archiwum", com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Archive, Modifier.testTag("plakietka.1"))
                    ahead == 0 -> com.nuvio.tv.skin.kicia.components.ForgeBadge("Na żywo", com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Live, Modifier.testTag("plakietka.1"))
                }
                val time = when {
                    shown == null -> channel.name
                    scrubMs != null -> forgeTvTime(scrubMs) + " · " + forgeTvSlot(shown.startEpochMs, shown.stopEpochMs).replace("—", "–") // v7.4 (07): wybrany czas na początku linii (mieści się w kolumnie)
                    replay != null -> forgeDayLabel(shown.startEpochMs, clockMs) + " " + forgeTvSlot(shown.startEpochMs, shown.stopEpochMs).replace("—", "–")
                    ahead == 0 -> forgeTvSlot(shown.startEpochMs, shown.stopEpochMs).replace("—", "–")
                    else -> (if (ahead == 1) "Następnie" else "Później") + " · " + forgeDayLabel(shown.startEpochMs, clockMs) + " " + forgeTvSlot(shown.startEpochMs, shown.stopEpochMs).replace("—", "–")
                }
                Text(time, style = ForgeText.label5, maxLines = 1, modifier = Modifier.padding(start = if (ahead == 0 || replay != null) 8.dp else 0.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                shown?.title ?: channel.name, style = ForgeText.head, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().height(26.dp).testTag("mini-tytul.0"),
            )
            // Pasek postępu (4 dp; 8 dp with a round thumb while it has the focus; ◀▶ / OK open the rewind bar)
            val barFocused = slot == ForgeMiniSlot.BAR || scrubMs != null
            Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.CenterStart) {
                if (ahead == 0) {
                    val progress = when {
                        scrubMs != null && shown != null -> ((scrubMs - shown.startEpochMs).toFloat() / (shown.stopEpochMs - shown.startEpochMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
                        replay != null -> ((playheadMs - replay.startEpochMs).toFloat() / (replay.stopEpochMs - replay.startEpochMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
                        else -> now?.let { forgeTvProgress(it.startEpochMs, it.stopEpochMs, clockMs) } ?: 0f
                    }
                    val trackHeight = if (barFocused) 8.dp else 4.dp
                    Box(Modifier.fillMaxWidth().height(trackHeight).clip(RoundedCornerShape(trackHeight / 2)).background(Color(0x3DFFFFFF)).testTag("karta-postep.0")) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(ForgeColors.Text))
                    }
                    if (barFocused) {
                        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                            Box(Modifier.offset(x = maxWidth * progress - 12.dp).size(24.dp).background(Color(0x4DFFFFFF), CircleShape).padding(4.dp).background(ForgeColors.Text, CircleShape))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // v7.4: round buttons 36 co 44 (makieta 491,8 / 535,8), the name as the accessibility label
                var n = 0
                row.forEach { s ->
                    when (s) {
                        ForgeMiniSlot.INFO -> ForgeMiniCircle(Icons.Outlined.Info, slot == s, "Info i odcinki", "przycisk-okragly.${n++}")
                        ForgeMiniSlot.RESTART -> ForgeMiniCircle(Icons.Filled.SkipPrevious, slot == s, "Od początku", "przycisk-okragly.${n++}")
                        ForgeMiniSlot.REMIND -> {
                            val on = shown != null && reminders.any { it.channelUrl == channel.streamUrl && it.startMs == shown.startEpochMs }
                            ForgeMiniCircle(if (on) Icons.Filled.Notifications else Icons.Outlined.Notifications, slot == s, if (on) "Usuń przypomnienie" else "Przypomnij", "przycisk-okragly.${n++}")
                        }
                        ForgeMiniSlot.FAV -> ForgeMiniCircle(if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, slot == s, "Ulubiony", "przycisk-okragly.${n++}")
                        else -> Unit
                    }
                }
            }
        }

        // Karta/Dalej 160x90 to x 920: "Dalej 12:10 · tytuł" on the picture; "W archiwum" when the same episode is there
        if (after != null) {
            Spacer(Modifier.width(40.dp))
            val nextFocused = slot == ForgeMiniSlot.NEXT
            val nextScale by androidx.compose.animation.core.animateFloatAsState(if (nextFocused) ForgeDims.FocusScale else 1f, com.nuvio.tv.skin.kicia.tokens.ForgeMotion.focusSpec(), label = "miniNext")
            val thumbShape = RoundedCornerShape(ForgeDims.RadiusCard)
            Box(
                Modifier.size(160.dp, 90.dp).graphicsLayer { scaleX = nextScale; scaleY = nextScale; alpha = if (nextFocused) 1f else 0.7f }
                    .clip(thumbShape).background(ForgeColors.CardBase).testTag("karta.1"),
            ) {
                val image = after.image?.takeIf { it.isNotBlank() }
                val url = logo ?: forgeChannelLogo(channel)
                if (image != null) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else if (!url.isNullOrBlank()) ForgeAutoLogo(url = url, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(84.dp, 40.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = .78f))))
                // v7.4 (07): one line, "Dalej 12:10 · " 12/700 70% inside the title 14/700 (the mockup's span)
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = ForgeText.captionBold.fontSize, fontWeight = ForgeText.captionBold.fontWeight, color = ForgeColors.Text2))
                        append((if (ahead == 0) "Dalej " else "Później ") + forgeTvTime(after.startEpochMs) + " · ")
                        pop()
                        append(after.title)
                    },
                    style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth().testTag("karta-podpis-tytul.1"),
                )
            }
        }
    }
}

@Composable
private fun ForgeMiniPill(text: String, icon: ImageVector?, focused: Boolean, modifier: Modifier = Modifier) {
    val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
    Row(
        modifier.height(com.nuvio.tv.skin.kicia.tokens.ForgeDims.ButtonSmHeight).clip(RoundedCornerShape(16.5.dp)).background(if (focused) ForgeColors.Focus else ForgeColors.Surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Box(Modifier.width(4.dp)) // v7.4 (07): ikona 20 + 4, „Przewodnik” 131,8 szer.
        }
        Text(text, style = ForgeText.label.copy(color = fg), maxLines = 1)
    }
}

@Composable
private fun ForgeMiniCircle(icon: ImageVector, focused: Boolean, label: String = "", tag: String = "") {
    Box(
        Modifier.requiredSize(36.dp).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .semantics { if (label.isNotEmpty()) contentDescription = label }.clip(CircleShape).background(if (focused) ForgeColors.Focus else ForgeColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (focused) ForgeColors.OnFocus else ForgeColors.Text, modifier = Modifier.size(36.dp * com.nuvio.tv.skin.kicia.tokens.ForgeDims.IconRatio))
    }
}

/**
 * S08 over the player: Info (or Dalej) in the MiniGuide opens the programme's details in their own window,
 * so the player's key handling never sees its keys. Watching live switches to the channel; from the start
 * plays the provider's archive.
 */
@Composable
internal fun ForgeMiniGuideDetailHost(state: LiveTvPlayerState) {
    ForgeReminderToast()
    val (channel, programme) = ForgeMiniGuideNav.detail ?: return
    val close = { ForgeMiniGuideNav.detail = null }
    val nowMs = LiveTvClock.nowEpochMs()
    val canStart = channel.catchup != null && programme.startEpochMs <= nowMs
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // v6: a drawer of 340 dp under the picture; the stream and the MiniGuide stay visible above it (no dimming).
        (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f)
        Box(Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize()) { // v7.4 (08): the whole screen (no bar, no MiniGuide), the video plays on under the glass
            ForgeInfoDrawer( // v6.2: the drawer of the mockup (Freely): season / episode, other airings, "Więcej odcinków"
                channel = channel,
                programme = programme,
                onWatchLive = {
                    close()
                    state.hideInfo()
                    // v7.4 (08 z MiniGuide, zgłoszenie właściciela): also on the same channel when it plays time-shifted
                    // (a replay from the archive, a rewind, a pause behind live): back to the live picture
                    if (channel.streamUrl != state.currentListUrl || state.replayingProgramme != null || state.isCatchup() || ForgeMiniGuideNav.paused) {
                        ForgeMiniGuideNav.paused = false
                        state.switchTo(channel, force = true)
                    }
                },
                onWatchFromStart = if (channel.catchup != null) ({ shownProgramme -> close(); state.hideInfo(); state.playCatchup(channel, shownProgramme) }) else null,
                playing = state.replayingProgramme ?: if (state.currentListUrl == channel.streamUrl) LiveTvRepository.uiState.value.currentProgrammes[channel.guideKey] else null,
                onPlayAiring = { airing ->
                    val at = LiveTvClock.nowEpochMs()
                    when {
                        airing.startEpochMs <= at && at < airing.stopEpochMs -> {
                            close(); state.hideInfo()
                            if (channel.streamUrl != state.currentListUrl || state.replayingProgramme != null) state.switchTo(channel)
                        }
                        airing.stopEpochMs <= at && channel.catchup != null -> { close(); state.hideInfo(); state.playCatchup(channel, airing) }
                        airing.stopEpochMs <= at -> android.widget.Toast.makeText(ctx, "Ten kanał nie ma archiwum — nagrania nie da się odtworzyć", android.widget.Toast.LENGTH_SHORT).show()
                        else -> {
                            com.nuvio.tv.fork.tv.ForgeReminders.toggle(ctx.applicationContext, com.nuvio.tv.fork.tv.ForgeReminder(channel.streamUrl, airing.startEpochMs, airing.title, channel.name))
                            android.widget.Toast.makeText(ctx, "Przypomnę o: " + airing.title, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = close,
            )
          }
        }
    }
}

/** "21:00—21:30" */
internal fun forgeTvSlot(startMs: Long, stopMs: Long): String = forgeTvTime(startMs) + "—" + forgeTvTime(stopMs)

internal fun forgeTvTime(ms: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): String {
    val c = java.util.Calendar.getInstance(zone).apply { timeInMillis = ms }
    return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
}

/** How far into the programme we are, 0..1. */
internal fun forgeTvProgress(startMs: Long, stopMs: Long, nowMs: Long): Float =
    if (stopMs <= startMs) 0f else ((nowMs - startMs).toFloat() / (stopMs - startMs)).coerceIn(0f, 1f)

/** The reminder to announce now: one starting within a minute either side, not announced yet. */
internal fun forgeDueReminder(list: List<com.nuvio.tv.fork.tv.ForgeReminder>, announced: Set<String>, nowMs: Long): com.nuvio.tv.fork.tv.ForgeReminder? =
    list.firstOrNull { it.key !in announced && it.startMs in (nowMs - 60_000)..(nowMs + 60_000) }

/**
 * Reminder card of the mockup (b36): top right over the picture for 6 s, "Za chwilę" / "Teraz" in green,
 * then "Tytuł · 21:00 · kanał". Never takes the focus.
 */
@Composable
internal fun ForgeReminderToast() {
    if (!com.nuvio.tv.skin.kicia.rememberForgeReminderCard()) return
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { com.nuvio.tv.fork.tv.ForgeReminders.load(context.applicationContext) }
    val reminders by com.nuvio.tv.fork.tv.ForgeReminders.list.collectAsState()
    var shown by androidx.compose.runtime.remember { mutableStateOf<com.nuvio.tv.fork.tv.ForgeReminder?>(null) }
    LaunchedEffect(reminders) {
        while (true) {
            val due = forgeDueReminder(reminders, ForgeReminderLog.announced, System.currentTimeMillis())
            if (due != null) {
                ForgeReminderLog.announced += due.key
                shown = due
                kotlinx.coroutines.delay(6_000)
                shown = null
            }
            kotlinx.coroutines.delay(15_000)
        }
    }
    val r = shown ?: return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Row(
            Modifier.padding(top = 110.dp, end = 58.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xE6343840)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(if (r.startMs <= System.currentTimeMillis()) "Teraz" else "Za chwilę", style = ForgeText.label.copy(color = ForgeColors.Ok))
                Text(r.title + " · " + forgeTvTime(r.startMs) + " · " + r.channelName, style = ForgeText.button, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** Reminders announced while the app runs (each once). */
internal object ForgeReminderLog { val announced = mutableSetOf<String>() }

/**
 * Owner 10.11: while an archive programme plays, ▲▼ / CH± would leave it for another channel and it is hard to come back to,
 * so the change is confirmed first. The safe answer ("Zostań") has the focus; Back also stays.
 */
@Composable
internal fun ForgeZapConfirm(programmeTitle: String?, toLive: Boolean = false, onStay: () -> Unit, onChange: () -> Unit) {
    val stay = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { stay.requestFocus() } }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onStay,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(520.dp).clip(RoundedCornerShape(16.dp)).background(ForgeColors.Bg).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(if (toLive) "Wrócić do transmisji na żywo?" else "Zmienić kanał?", style = ForgeText.head)
                Text(
                    "Oglądasz program z archiwum" + (programmeTitle?.let { ": $it" } ?: "") +
                        (if (toLive) ". Po powrocie na żywo wrócisz do niego przez przewodnik albo „Info i odcinki”." else ". Po zmianie kanału wrócisz do niego przez przewodnik albo „Info i odcinki”."),
                    style = ForgeText.body,
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Zostań", primary = true, focusRequester = stay, onOk = onStay)
                    com.nuvio.tv.skin.kicia.components.ForgeButton(text = if (toLive) "Na żywo" else "Zmień kanał", onOk = onChange)
                }
            }
        }
    }
}
