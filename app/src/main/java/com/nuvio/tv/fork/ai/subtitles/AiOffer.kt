package com.nuvio.tv.fork.ai.subtitles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.fork.sources.PlayerBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fork 19.11 (właściciel: „napisy nie mogą się tłumaczyć przez AI bez prośby użytkownika; ma się otwierać okno
 * »napisy AI? innych brak«, gdy nie ma polskich napisów”). Okno pytania; AI startuje dopiero po „Tak”.
 * Pytanie: raz na film, gdy dodatki skończyły szukać, a nie ma polskich napisów (z dodatków ani wbudowanych);
 * albo gdy lektor nie ma polskiego tekstu (wtedy po „Tak” rusza też lektor).
 */
object AiOffer {
    data class Ask(val forLektor: Boolean)
    val ask = MutableStateFlow<Ask?>(null)
    private var askedFor: String? = null

    fun offer(forLektor: Boolean) {
        // Paczka „oczywiste” pkt 3: nigdy dla IPTV (także prośba lektora).
        if (com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry.isLiveTv(PlayerBridge.current()?.currentStreamUrl)) { android.util.Log.i("ForkAiSubtitles", "okno napisów AI: pominięte (IPTV, lektor=$forLektor)"); return }
        ask.value = Ask(forLektor)
    }

    private fun isPl(lang: String?) = lang?.let { AuxiliaryLanguagePolicy.primaryLanguage(it) == "pl" } == true

    /** Czy pytać teraz o ten film (wywoływane co 2 s podczas odtwarzania). */
    internal fun shouldAsk(c: android.content.Context): Boolean {
        val s = PlayerBridge.current() ?: return false
        val key = s.currentVideoId ?: s.currentStreamUrl ?: return false
        if (askedFor == key || ask.value != null || AiSubtitles.running) return false
        // Paczka „oczywiste” pkt 3: kanały IPTV (na żywo i archiwum) bez okna napisów.
        if (com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry.isLiveTv(s.currentStreamUrl)) { askedFor = key; android.util.Log.i("ForkAiSubtitles", "okno napisów AI: pominięte (IPTV)"); return false }
        if (!s.isPlaying || s.isLoadingAddonSubtitles) return false
        if (s.addonSubtitles.any { isPl(it.lang) } || s.subtitleTracks.any { isPl(it.language) }) { askedFor = key; return false }
        if (s.addonSubtitles.isEmpty() && s.subtitleTracks.isEmpty()) return false // jeszcze nic nie wiadomo (albo film bez żadnych napisów: AI i tak nie ma źródła)
        if (AiSubtitles.unavailableReason(c) != null || !com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "module_ai_subtitles", true)) return false
        askedFor = key
        return true
    }
}

@Composable
fun AiOfferDialog() {
    val c = LocalContext.current
    val a by AiOffer.ask.collectAsState()
    LaunchedEffect(Unit) { while (true) { delay(2_000); if (AiOffer.shouldAsk(c)) AiOffer.offer(false) } }
    val cur = a
    com.nuvio.tv.skin.kicia.components.ForgeDialog(
        visible = cur != null,
        title = "Brak polskich napisów",
        message = if (cur?.forLektor == true) "Lektor potrzebuje polskiego tekstu. Przetłumaczyć napisy przez AI?" else "Innych polskich napisów brak. Włączyć napisy AI (tłumaczenie na polski)?",
        note = "Tłumaczenie Gemini, płatne w ramach limitów z ustawień AI napisów",
        buttons = listOf(
            com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Napisy AI", primary = true) {
                AiOffer.ask.value = null
                val msg = AiSubtitles.start(c, AiMode.WATCH)
                android.widget.Toast.makeText(c, msg, android.widget.Toast.LENGTH_LONG).show()
                if (cur?.forLektor == true) android.widget.Toast.makeText(c, com.nuvio.tv.fork.lektor.LektorController.start(c), android.widget.Toast.LENGTH_SHORT).show()
            },
            com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Nie") { AiOffer.ask.value = null },
        ),
        onDismiss = { AiOffer.ask.value = null },
    )
}
