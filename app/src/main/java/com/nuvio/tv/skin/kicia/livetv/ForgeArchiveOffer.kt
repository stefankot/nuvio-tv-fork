package com.nuvio.tv.skin.kicia.livetv

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPlayerState
import kotlinx.coroutines.delay

/**
 * Owner 13.11: while a programme plays live, an earlier airing of the same episode is looked for in the background;
 * when its archive copy really answers ([ForgeAiringRace.findEarlier]) a card offers it (adverts can be skipped there).
 * OK on the bare picture while the card shows plays it; Back hides the card. Never takes the focus.
 */
internal object ForgeArchiveOffer {
    data class Offer(val listUrl: String, val win: ForgeAiringRace.Win)

    var offer by mutableStateOf<Offer?>(null)
    /** Programmes already looked at (list URL | start), each once per run of the app. */
    private val asked = HashSet<String>()
    private const val SETTLE_MS = 15_000L
    private const val SHOW_MS = 15_000L

    /** Hook in Reshaped's live key handler (bare picture, MiniGuide closed): OK takes the offer, Back drops it. */
    fun onKey(state: LiveTvPlayerState, event: KeyEvent): Boolean {
        val o = offer ?: return false
        if (state.currentListUrl != o.listUrl || state.isCatchup()) { offer = null; return false }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.action == KeyEvent.ACTION_UP) { offer = null; state.playCatchup(o.win.channel, o.win.programme) }
                true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (event.action == KeyEvent.ACTION_UP) offer = null
                true
            }
            else -> false
        }
    }

    /** The background check: once per programme, after the viewer has stayed on the channel for a while. */
    suspend fun watch(state: LiveTvPlayerState) {
        while (true) {
            delay(SETTLE_MS)
            val url = state.currentListUrl ?: continue
            if (state.isCatchup()) continue
            val ui = LiveTvRepository.uiState.value
            val channel = ui.channels.firstOrNull { it.streamUrl == url } ?: continue
            val programme = ui.currentProgrammes[channel.guideKey] ?: continue
            if (!asked.add(url + "|" + programme.startEpochMs)) continue
            val win = runCatching { ForgeAiringRace.findEarlier(channel, programme) }.getOrNull() ?: continue
            if (state.currentListUrl != url || state.isCatchup()) continue
            offer = Offer(url, win)
            delay(SHOW_MS)
            if (offer?.win === win) offer = null
        }
    }
}

/** The card (top right, like the reminder card b36) and the background check; placed once in the Live TV player. */
@Composable
internal fun ForgeArchiveOfferCard(state: LiveTvPlayerState) {
    LaunchedEffect(state) { ForgeArchiveOffer.watch(state) }
    val o = ForgeArchiveOffer.offer ?: return
    if (state.currentListUrl != o.listUrl || state.infoOpen) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier.padding(top = 110.dp, end = 58.dp).widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xE6343840)).padding(16.dp),
        ) {
            Text("W archiwum · bez czekania na reklamy", style = ForgeText.label.copy(color = ForgeColors.Ok))
            Text(
                o.win.programme.title + " · " + forgeTvTime(o.win.programme.startEpochMs) + " · " + o.win.channel.name,
                style = ForgeText.button, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
            Text("OK – oglądaj od początku z przewijaniem", style = ForgeText.caption.copy(color = ForgeColors.Text3), modifier = Modifier.padding(top = 4.dp))
        }
    }
}
