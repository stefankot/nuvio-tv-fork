package com.nuvio.tv.skin.niunia.stream

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaHeroBackdrop
import com.nuvio.tv.skin.niunia.components.NiuniaHeroKind
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaLogo
import com.nuvio.tv.skin.niunia.components.NiuniaLogoSize
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.stream.StreamScreenUiState

private const val TAG = "ForkNiunia"

/** Szybki filtr nad listą (Z-14, makieta Streams): liczony z tekstu źródła, bez zmiany stanu ekranu bazy. */
internal enum class NiuniaStreamQuick(val label: Int) { ALL(R.string.niunia_ke_src_all), LEKTOR(R.string.niunia_ke_src_lektor), SUBS(R.string.niunia_ke_src_subs), UHD(R.string.niunia_ke_src_4k), FHD(R.string.niunia_ke_src_1080) }

/** Cały tekst źródła (nazwa, tytuł, opis, plik) małymi literami — do wykrywania języka, jakości i kodeka. */
internal fun niuniaStreamText(s: Stream): String = listOfNotNull(s.name, s.title, s.description, s.behaviorHints?.filename).joinToString("\n").lowercase()

internal fun niuniaStreamMatches(s: Stream, q: NiuniaStreamQuick): Boolean {
    val t = niuniaStreamText(s)
    return when (q) {
        NiuniaStreamQuick.ALL -> true
        NiuniaStreamQuick.LEKTOR -> Regex("""lektor|dubbing|\bdub\b|🗣|🔊""").containsMatchIn(t) && Regex("""\bpl\b|polsk|polish|🇵🇱|lektor""").containsMatchIn(t)
        NiuniaStreamQuick.SUBS -> Regex("""napisy|\bpl ?sub|plsub|subs? ?pl""").containsMatchIn(t)
        NiuniaStreamQuick.UHD -> Regex("""2160p|\b4k\b|\buhd\b""").containsMatchIn(t)
        NiuniaStreamQuick.FHD -> Regex("""1080p""").containsMatchIn(t)
    }
}

/** Etykieta wiersza: „Lektor PL · 1080p · WEB-DL · HDR10”; nic nie rozpoznane → pierwsza linia nazwy. */
internal fun niuniaStreamLabel(s: Stream, ctx: android.content.Context): String {
    val t = niuniaStreamText(s)
    val lang = when {
        niuniaStreamMatches(s, NiuniaStreamQuick.LEKTOR) -> if (Regex("""dubbing|\bdub\b""").containsMatchIn(t)) ctx.getString(R.string.niunia_ke_src_dub) else ctx.getString(R.string.niunia_ke_src_lektor_pl)
        niuniaStreamMatches(s, NiuniaStreamQuick.SUBS) -> ctx.getString(R.string.niunia_ke_src_subs_pl)
        else -> null
    }
    val q = when {
        Regex("""2160p|\b4k\b|\buhd\b""").containsMatchIn(t) -> "4K"
        Regex("""1080p""").containsMatchIn(t) -> "1080p"
        Regex("""720p""").containsMatchIn(t) -> "720p"
        Regex("""480p|\bsd\b""").containsMatchIn(t) -> "SD"
        else -> s.quality?.takeIf { it.isNotBlank() }
    }
    val src = Regex("""remux|blu-?ray|web-?dl|webrip|hdtv|hdrip|dvdrip|\bcam\b|telesync|\bts\b""").find(t)?.value?.let {
        when { it.startsWith("remux") -> "REMUX"; it.startsWith("blu") -> "BluRay"; it.startsWith("web-") || it == "webdl" || it == "web-dl" -> "WEB-DL"; it == "webrip" -> "WEBRip"
            it == "hdtv" -> "HDTV"; it == "cam" || it == "telesync" || it == "ts" -> "CAM"; else -> it.uppercase() }
    }
    val hdr = when { Regex("""\bdv\b|dolby ?vision""").containsMatchIn(t) -> "Dolby Vision"; Regex("""hdr10\+|hdr10plus""").containsMatchIn(t) -> "HDR10+"; Regex("""\bhdr""").containsMatchIn(t) -> "HDR10"; else -> null }
    val parts = listOfNotNull(lang, q, src, hdr)
    return if (parts.size >= 2 || (parts.isNotEmpty() && lang != null)) parts.joinToString(" · ")
    else listOfNotNull(parts.firstOrNull(), s.name?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()).distinct().joinToString(" · ").ifBlank { s.addonName }
}

/** Podpis: „Torrentio · Real-Debrid · 4,2 GB · H.265 · Dolby Atmos”. */
internal fun niuniaStreamSupporting(s: Stream): String {
    val t = niuniaStreamText(s)
    val size = s.behaviorHints?.videoSize?.takeIf { it > 0 }?.let { b -> String.format(java.util.Locale("pl", "PL"), "%.1f GB", b / 1_073_741_824.0) }
        ?: Regex("""(\d+(?:[.,]\d+)?)\s?(gb|mb)\b""").find(t)?.let { m -> "${m.groupValues[1].replace('.', ',')} ${m.groupValues[2].uppercase()}" }
    val codec = when { Regex("""x265|h\.?265|hevc""").containsMatchIn(t) -> "H.265"; Regex("""x264|h\.?264|avc""").containsMatchIn(t) -> "H.264"; Regex("""\bav1\b""").containsMatchIn(t) -> "AV1"; else -> null }
    val audio = when {
        Regex("""atmos""").containsMatchIn(t) -> "Dolby Atmos"; Regex("""truehd""").containsMatchIn(t) -> "TrueHD"; Regex("""dts""").containsMatchIn(t) -> "DTS"
        Regex("""ddp|dd\+|eac3|e-ac-3""").containsMatchIn(t) -> "Dolby Digital+"; Regex("""\bdd5|ac3|dd 5""").containsMatchIn(t) -> "Dolby Digital"; Regex("""\baac\b""").containsMatchIn(t) -> "AAC"; else -> null
    }
    val debrid = s.debridCacheStatus?.providerName?.takeIf { it.isNotBlank() }
    return listOfNotNull(s.addonName.takeIf { it.isNotBlank() }, debrid, size, codec, audio).distinct().joinToString(" · ")
}

/**
 * Źródła w stylu Niuni (Z-14, makieta Streams; wzór Nuvio KE 1.2.2): po lewej logo i odcinek, po prawej szybkie filtry z liczbami
 * (Wszystkie, Lektor PL, Napisy PL, 4K, 1080p) i dodatki, lista źródeł (etykieta: język · jakość · rodzaj · HDR; podpis: dodatek · debrid ·
 * rozmiar · kodek · dźwięk; znacznik „W pamięci”). Logika odtwarzania, autoodtwarzanie i okna zostają w ekranie bazy (wpięcie w StreamScreen).
 */
@Composable
fun NiuniaStreams(
    uiState: StreamScreenUiState,
    resumeText: String?,
    onAddonFilter: (String?) -> Unit,
    onRetry: () -> Unit,
    onPlay: (Stream) -> Unit,
) {
    val ctx = LocalContext.current
    var quick by rememberSaveable { mutableStateOf(NiuniaStreamQuick.ALL) }
    val base = uiState.filteredStreams
    val counts = remember(base) { NiuniaStreamQuick.values().associateWith { q -> base.count { niuniaStreamMatches(it, q) } } }
    val shown = remember(base, quick) { base.filter { niuniaStreamMatches(it, quick) } }
    val first = remember { FocusRequester() }
    LaunchedEffect(uiState.isLoading, base.size, quick) {
        Log.i(TAG, "źródła: ${if (uiState.isLoading) "wczytuję" else "${base.size} źródeł"}, filtr=${quick.name} (${shown.size}), dodatek=${uiState.selectedAddonFilter ?: "wszystkie"}, błąd=${uiState.error != null}")
        if (!uiState.isLoading && shown.isNotEmpty()) {
            kotlinx.coroutines.delay(80)
            try { first.requestFocus(); Log.i(TAG, "źródła: fokus → 1. źródło") } catch (e: IllegalStateException) { Log.w(TAG, "źródła: fokus: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
    Box(Modifier.fillMaxSize().background(NiuniaColor.Bg)) {
        NiuniaHeroBackdrop(NiuniaHeroKind.Blurred, uiState.backdrop ?: uiState.poster)
        com.nuvio.tv.skin.niunia.components.NiuniaClockCorner() // Z-15
        Row(Modifier.fillMaxSize().padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, top = 76.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40)) {
            NiuniaContentBlock(NiuniaBlockKind.Info, Modifier.width(300.dp)) {
                NiuniaLogo(uiState.logo, uiState.title, NiuniaLogoSize.Player)
                val head = if (uiState.isEpisode) listOfNotNull(stringResource(R.string.niunia_ke_series), "S${uiState.season} E${uiState.episode}", uiState.episodeName?.takeIf { it.isNotBlank() }).joinToString(" · ")
                    else stringResource(R.string.niunia_ke_movie)
                NiuniaText(head, variant = NiuniaTextVariant.BodySmall, lines = 2)
                val meta = listOfNotNull(uiState.year?.takeIf { it.isNotBlank() }, uiState.runtime?.takeIf { it > 0 }?.let { "$it min" }, uiState.genres?.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (meta.isNotBlank()) NiuniaText(meta, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
                if (!resumeText.isNullOrBlank()) NiuniaText(resumeText, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 1)
            }
            Column(Modifier.fillMaxHeight().weight(1f), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8), contentPadding = PaddingValues(end = NiuniaSpacing.Space40)) {
                    items(NiuniaStreamQuick.values().filter { it == NiuniaStreamQuick.ALL || (counts[it] ?: 0) > 0 }) { q ->
                        NiuniaButton(onClick = { quick = q; Log.i(TAG, "źródła: szybki filtr → ${q.name}") }, label = "${stringResource(q.label)} · ${counts[q] ?: 0}",
                            kind = NiuniaButtonKind.Chip, selected = quick == q)
                    }
                    if (uiState.availableAddons.size > 1) items(listOf<String?>(null) + uiState.availableAddons) { a ->
                        NiuniaButton(onClick = { Log.i(TAG, "źródła: dodatek → ${a ?: "wszystkie"}"); onAddonFilter(a) },
                            label = a ?: stringResource(R.string.niunia_ke_src_all_addons), kind = NiuniaButtonKind.Chip, selected = uiState.selectedAddonFilter == a)
                    }
                }
                when {
                    uiState.isLoading && base.isEmpty() -> Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = NiuniaColor.White, strokeWidth = 2.dp)
                        NiuniaText(stringResource(R.string.niunia_ke_src_loading), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary)
                    }
                    base.isEmpty() -> NiuniaContentBlock(NiuniaBlockKind.Empty) {
                        NiuniaText(stringResource(if (uiState.error != null) R.string.niunia_ke_src_error else R.string.niunia_ke_src_none), variant = NiuniaTextVariant.TitleSection)
                        NiuniaText(uiState.error ?: stringResource(R.string.niunia_ke_src_none_hint), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 3)
                        NiuniaButton(onClick = { Log.i(TAG, "źródła: ponów"); onRetry() }, label = stringResource(R.string.niunia_ke_src_retry), icon = "restart", compact = true, focusRequester = first)
                    }
                    else -> {
                        NiuniaCollectionHeader(stringResource(R.string.niunia_ke_src_count, shown.size, uiState.availableAddons.size.coerceAtLeast(1)), active = true,
                            sub = if (uiState.isLoading) stringResource(R.string.niunia_ke_src_more) else null)
                        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), contentPadding = PaddingValues(bottom = NiuniaSpacing.Space40)) {
                            itemsIndexed(shown, key = { i, s -> "$i|${s.addonName}|${s.url ?: s.infoHash ?: s.ytId}" }) { i, s ->
                                val cached = s.debridCacheStatus?.state == StreamDebridCacheState.CACHED
                                Box {
                                    NiuniaListRow(
                                        niuniaStreamLabel(s, ctx),
                                        onClick = { Log.i(TAG, "źródła: OK → ${s.addonName} „${s.name?.lineSequence()?.firstOrNull()}”"); onPlay(s) },
                                        kind = NiuniaRowKind.Track,
                                        supporting = niuniaStreamSupporting(s).ifBlank { null },
                                        value = if (cached) stringResource(R.string.niunia_ke_src_cached) else null,
                                        focusRequester = if (i == 0) first else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
