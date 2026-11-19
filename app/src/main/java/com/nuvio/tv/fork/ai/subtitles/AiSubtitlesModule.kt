package com.nuvio.tv.fork.ai.subtitles

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.ModuleRoute
import com.nuvio.tv.fork.registry.ModuleSetting
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay
import com.nuvio.tv.fork.logged

/** AI napisy: tłumaczenie napisów na polski (Gemini). Wejście: odtwarzacz → Więcej; ustawienia: Moduły → AI napisy. */
object AiSubtitlesModule : ForkModule {
    const val ROUTE = "fork_ai_subtitles"
    override val id = "ai_subtitles"
    override val title = "AI napisy: tłumaczenie na polski"
    override val defaultEnabled = true // owner: fork features on by default; paid work still needs key + consent

    override fun settings() = listOf(
        ModuleSetting(AiSettings.K_REVIEW, SettingsLevel.ESSENTIAL, "AI napisy", "Druga kontrola każdego bloku (recenzja)", "Lepsza jakość, ok. 2× koszt i czas", defaultValue = true),
        ModuleSetting(AiSettings.K_FALLBACK, SettingsLevel.ESSENTIAL, "AI napisy", "Oryginał tam, gdzie tłumaczenie niegotowe", "Wyłączone = brak napisów w tych miejscach", defaultValue = true),
        ModuleSetting(AiSettings.K_NOTICE, SettingsLevel.ESSENTIAL, "AI napisy", "Powiadomienie w rogu podczas tłumaczenia", defaultValue = true),
    )
    override fun actions() = listOf(
        ModuleAction("AI napisy: klucz, model, limity, zapisane", SettingsLevel.SIMPLE, "Gemini: klucz API, zgoda, limit wydatków, eksport SRT/VTT") { ForkFeatures.open?.invoke(ROUTE) },
    )
    override fun routes() = listOf(ModuleRoute(ROUTE) { composable(ROUTE) { AiSubtitlesScreen() } })
    override fun rootOverlays(): List<@Composable () -> Unit> = listOf({ AiSubtitlesNotice() }, { AiOfferDialog() }) // fork 19.11: okno „napisy AI? innych brak”
}

private fun cycle(c: Context, key: String, values: List<String>, default: String) {
    val cur = ForkFeatures.str(c, key, default)
    ForkFeatures.setStr(c, key, values[(values.indexOf(cur) + 1).mod(values.size)])
}

@Composable
fun AiSubtitlesScreen() {
    val c = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    val docs = remember(refresh) { AiSubtitles.store(c).list() }
    var pending by remember { mutableStateOf<Pair<AiSubtitleDoc, String>?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        val (doc, format) = pending ?: return@rememberLauncherForActivityResult
        pending = null
        message = if (uri == null) "Eksport anulowany" else runCatching {
            c.contentResolver.openOutputStream(uri)?.use { it.write(exportText(doc, format).toByteArray(Charsets.UTF_8)) } ?: error("no stream")
            "Zapisano ${format.uppercase()}" + if (!doc.complete) " (częściowe: nie do publikacji)" else ""
        }.logged("ForkAi", "AiSubtitlesModule.kt:84").getOrElse { "Nie udało się zapisać: ${it.javaClass.simpleName}" }
    }
    fun export(doc: AiSubtitleDoc, format: String) {
        pending = doc to format
        val name = doc.title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "").take(60) + ".pl.ai" + when (format) { "vtt" -> ".vtt"; "json" -> ".manifest.json"; else -> ".srt" }
        message = runCatching { exporter.launch(name); "" }.logged("ForkAi", "AiSubtitlesModule.kt:92").getOrElse {
            // TV without a document picker: keep a copy in app storage (still playable from the menu).
            val f = java.io.File(c.getExternalFilesDir(null) ?: c.filesDir, "ai_subtitles_export/$name")
            f.parentFile?.mkdirs(); f.writeText(exportText(doc, format)); pending = null
            "Brak menedżera plików: zapisano w ${f.absolutePath}"
        }
    }
    val ledger = remember(refresh) { AiSubtitles.ledger(c) }
    val spentToday = remember(refresh) { runCatching { ledger.spentForDay() }.logged("ForkAi", "AiSubtitlesModule.kt:100").getOrNull() }
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("AI napisy: tłumaczenie na polski", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Dostawca: Google Gemini. Wysyłany jest tylko tekst napisów (oryginał i dwa pomocnicze), bez obrazu i dźwięku. Żądania mają store=false; o retencji decydują zasady Google dla płatnego API.") }
        item { check(refresh >= 0) /* odśwież napis po zmianie */; Text("Klucz API: ${AiKeyStore.mask(AiKeyStore.read(c))} (szyfrowany kluczem Android Keystore)") }
        // Klucz wpisywany na telefonie przez kod QR (polecenie właściciela 19.11; wzór Reshaped: klucz Seekr).
        item { AiKeyPhoneEntry { sent -> if (sent.trim().length >= 20) { runCatching { AiKeyStore.write(c, sent) }.logged("ForkAi", "AiSubtitlesModule.kt:106"); message = "Klucz zapisany (z telefonu)"; refresh++ } else message = "Za krótki klucz" } }
        item { Button(onClick = { runCatching { AiKeyStore.write(c, null) }.logged("ForkAi", "AiSubtitlesModule.kt:107"); message = "Klucz usunięty"; refresh++ }) { Text("Usuń klucz") } }
        item { check(refresh >= 0) /* odśwież napis po zmianie */
            val on = AiSettings.textConsent(c)
            Button(onClick = { ForkFeatures.set(c, AiSettings.K_CONSENT_TEXT, !on); refresh++ }) {
                Text(if (on) "Zgoda na wysyłanie tekstu napisów do Google: TAK (naciśnij, aby cofnąć)" else "Zgoda na wysyłanie tekstu napisów do Google: NIE (naciśnij, aby wyrazić)")
            }
        }
        item { check(refresh >= 0) /* odśwież napis po zmianie */; Button(onClick = { cycle(c, AiSettings.K_MODEL, ModelPricing.models, ModelPricing.DEFAULT_MODEL); refresh++ }) { Text("Model: ${AiSettings.model(c)}" + (ModelPricing.priceFor(AiSettings.model(c))?.let { "  ($${it.inputPerM}/$${it.outputPerM} za 1M tokenów)" } ?: "")) } }
        item { check(refresh >= 0) /* odśwież napis po zmianie */; Button(onClick = { cycle(c, AiSettings.K_TITLE, AiSettings.titleLimits, "1.0"); refresh++ }) { Text("Limit na film/odcinek: $${AiSettings.titleLimit(c)}") } }
        item { check(refresh >= 0) /* odśwież napis po zmianie */; Button(onClick = { cycle(c, AiSettings.K_DAY, AiSettings.dayLimits, "3.0"); refresh++ }) { Text("Limit dzienny: $${AiSettings.dayLimit(c)} (dziś wydano ok. $${"%.3f".format(java.util.Locale.ROOT, spentToday ?: 0.0)})") } }
        item { Text("Limity liczy aplikacja z danych zwróconych przez Google; rozliczenie Google może się nieznacznie różnić i przychodzić z opóźnieniem.", style = MaterialTheme.typography.bodySmall) }
        if (message.isNotEmpty()) item { Text(message, style = MaterialTheme.typography.titleMedium) }
        item { Text("Zapisane tłumaczenia (${docs.size})", style = MaterialTheme.typography.titleMedium) }
        docs.forEach { doc ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${doc.title}: ${doc.status}, bloki ${doc.doneBlocks}/${doc.blocks.size}, źródło ${doc.sourceLanguage}" +
                        (if (doc.aux.isNotEmpty()) ", pomocnicze ${doc.aux.joinToString { it.language }}" else "") +
                        (if (doc.missingCategories.isNotEmpty()) ", brak: ${doc.missingCategories.joinToString()}" else ""))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { export(doc, "srt") }) { Text("Eksportuj SRT") }
                        Button(onClick = { export(doc, "vtt") }) { Text("WebVTT") }
                        Button(onClick = { export(doc, "json") }) { Text("Manifest") }
                        Button(onClick = { AiSubtitles.store(c).delete(doc.key); refresh++ }) { Text("Usuń") }
                    }
                }
            }
        }
        item { Text("Publikacja w OpenSubtitles: jeszcze niedostępna (wymaga osobnego etapu, przeglądu i potwierdzenia).", style = MaterialTheme.typography.bodySmall) }
    }
}

internal fun exportText(doc: AiSubtitleDoc, format: String): String {
    val cues = doc.translatedCues()
    return when (format) {
        "vtt" -> SubtitleExport.toWebVtt(cues)
        "json" -> ExportManifest.build(doc, ExportManifest.sha256(SubtitleExport.toSrt(cues)), "srt")
        else -> SubtitleExport.toSrt(cues)
    }
}

/** Discreet corner notice in Forge style: no focus, no dialog, top-right safe area away from subtitles and controls. */
@Composable
fun AiSubtitlesNotice() {
    val c = LocalContext.current
    val st by AiSubtitles.state.collectAsState()
    val hidden by AiSubtitles.noticeHidden.collectAsState()
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(st?.notice, st?.stage) {
        val s = st
        shown = s?.notice.orEmpty()
        if (s != null && s.stage in setOf(AiStage.DONE, AiStage.STOPPED, AiStage.FAILED) && shown.isNotEmpty()) { delay(5_000); shown = "" }
    }
    if (shown.isEmpty() || hidden || !AiSettings.notice(c)) return
    Box(Modifier.fillMaxSize().padding(top = 27.dp, end = 48.dp), contentAlignment = Alignment.TopEnd) {
        val accent = when (st?.stage) { AiStage.DONE -> ForgeColors.Ok; AiStage.FAILED, AiStage.WAITING -> ForgeColors.Gold; else -> ForgeColors.Text }
        Text(shown, style = ForgeText.captionBold.copy(color = accent),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xEB28282A)).padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

/** Rows for the Kicia player's "Więcej" sheet (one-line hook in ForgePlayerControls). */
@Composable
fun AiSubtitlesMenuRows(close: () -> Unit) {
    val c = LocalContext.current
    if (!ForkFeatures.enabled(c, "module_ai_subtitles", true)) return
    val st by AiSubtitles.state.collectAsState()
    fun toast(msg: String) = android.widget.Toast.makeText(c, msg, android.widget.Toast.LENGTH_LONG).show()
    val running = AiSubtitles.running
    if (running) {
        // fork 19.11: tryb na bieżąco nie ma bloków
        ForgeSheetRow(text = "AI napisy: anuluj", detail = if (LiveEmbeddedTranslation.active) "na bieżąco: ${LiveEmbeddedTranslation.translated.value.size} kwestii" else st?.let { "${it.doneBlocks}/${it.totalBlocks} bloków" }) { close(); toast(AiSubtitles.cancel()) }
        ForgeSheetRow(text = if (AiSubtitles.noticeHidden.value) "AI napisy: pokaż powiadomienie" else "AI napisy: ukryj powiadomienie") { close(); AiSubtitles.noticeHidden.value = !AiSubtitles.noticeHidden.value }
    } else {
        val reason = AiSubtitles.unavailableReason(c)
        val hasPolish = com.nuvio.tv.fork.sources.PlayerBridge.current()?.addonSubtitles?.any { AuxiliaryLanguagePolicy.primaryLanguage(it.lang) == "pl" } == true
        ForgeSheetRow(text = if (hasPolish) "AI napisy: przetłumacz ponownie na polski" else "AI napisy: tłumacz na polski i oglądaj", detail = reason) {
            close(); toast(reason ?: AiSubtitles.start(c, AiMode.WATCH, retranslate = false))
        }
        if (reason == null) ForgeSheetRow(text = "AI napisy: przygotuj cały film przed pokazaniem") { close(); toast(AiSubtitles.start(c, AiMode.PREPARE_ALL)) }
        AiSubtitles.savedForCurrent(c).firstOrNull()?.let { doc ->
            ForgeSheetRow(text = "AI napisy: zapisane (${doc.doneBlocks}/${doc.blocks.size})", detail = doc.status) { close(); toast(AiSubtitles.showSaved(c, doc)) }
        }
    }
    if (st?.published == true) ForgeSheetRow(text = "Pokaż oryginalne napisy") { close(); toast(AiSubtitles.showOriginal()) }
}
