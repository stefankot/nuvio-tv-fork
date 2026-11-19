package com.nuvio.tv.fork.ai.subtitles

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.profile.ProtectedProfilePreferences
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.isUsingMpvEngine
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/** Settings of the AI subtitle module (prefs `fork_features`, shared fork settings category). */
object AiSettings {
    const val K_MODEL = "ai_sub_model"; const val K_TITLE = "ai_sub_limit_title"; const val K_DAY = "ai_sub_limit_day"
    const val K_REVIEW = "ai_sub_review"; const val K_FALLBACK = "ai_sub_fallback_original"; const val K_NOTICE = "ai_sub_notice"
    const val K_CONSENT_TEXT = "ai_sub_consent_text"
    val titleLimits = listOf("1.0", "0.3", "3.0"); val dayLimits = listOf("3.0", "1.0", "10.0")
    fun model(c: Context) = ForkFeatures.str(c, K_MODEL, ModelPricing.DEFAULT_MODEL).takeIf { it in ModelPricing.models } ?: ModelPricing.DEFAULT_MODEL
    fun titleLimit(c: Context) = ForkFeatures.str(c, K_TITLE, "1.0").toDoubleOrNull() ?: 1.0
    fun dayLimit(c: Context) = ForkFeatures.str(c, K_DAY, "3.0").toDoubleOrNull() ?: 3.0
    fun review(c: Context) = ForkFeatures.enabled(c, K_REVIEW, true)
    fun fallbackOriginal(c: Context) = ForkFeatures.enabled(c, K_FALLBACK, true)
    fun notice(c: Context) = ForkFeatures.enabled(c, K_NOTICE, true)
    fun textConsent(c: Context) = ForkFeatures.enabled(c, K_CONSENT_TEXT, true) // właściciel 19.11: zgoda domyślnie włączona (można wyłączyć)
}

/** API key encrypted with an Android Keystore key (it is decrypted in app memory while used). */
object AiKeyStore {
    private fun prefs(c: Context) = ProtectedProfilePreferences(c.applicationContext, "fork_ai_subtitles_secret", "fork_ai_subtitles_gemini")
    fun read(c: Context): String? = runCatching { prefs(c).read(0) }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: com.nuvio.tv.BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() } // na razie klucz wbudowany (local.properties, polecenie właściciela 19.11)
    fun write(c: Context, key: String?) = prefs(c).write(0, key?.trim()?.takeIf { it.isNotEmpty() })
    fun mask(key: String?) = key?.let { if (it.length > 8) "…" + it.takeLast(4) else "zapisany" } ?: "brak"
}

/**
 * Entry point used by the player hook, the "Więcej" menu, the notice and settings. One session at a time;
 * a new episode, a closed player or another subtitle choice ends it (results stay saved).
 */
object AiSubtitles {
    private const val TAG = "ForkAiSubtitles"
    private var controllerRef: WeakReference<PlayerRuntimeController>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var session: AiSubtitleSession? = null
    private var sessionCounter = 0L
    private var lastSource: SubtitleOption? = null
    private val _state = MutableStateFlow<AiState?>(null)
    val state: StateFlow<AiState?> = _state.asStateFlow()
    val noticeHidden = MutableStateFlow(false)

    /** Fork hook (PlayerViewModel.bindExoSubtitleView): the current Exo player. */
    fun bind(controller: PlayerRuntimeController) {
        if (controllerRef?.get() !== controller) { stop("Zmiana odtwarzacza"); controllerRef = WeakReference(controller) }
    }

    /** Fork hook (PlayerViewModel.onCleared). */
    fun unbind(controller: PlayerRuntimeController) {
        if (controllerRef?.get() === controller) { stop("Odtwarzanie zakończone (postęp zapisany)"); controllerRef = null }
    }

    fun store(c: Context) = AiSubtitleStore(File(c.filesDir, "ai_subtitles/saved"))
    fun ledger(c: Context) = SpendLedger(File(c.filesDir, "ai_subtitles/spend.json"), { AiSettings.titleLimit(c) }, { AiSettings.dayLimit(c) })
    private fun eta(c: Context): EtaEstimator {
        val p = c.getSharedPreferences("fork_ai_subtitles_eta", Context.MODE_PRIVATE)
        return EtaEstimator(
            load = { p.all.mapNotNull { (k, v) -> (v as? Float)?.let { k to it.toDouble() } }.toMap() },
            save = { m -> p.edit().apply { m.forEach { (k, v) -> putFloat(k, v.toFloat()) } }.apply() },
        )
    }

    /** Why the menu entry cannot work right now, or null when it can. */
    fun unavailableReason(c: Context): String? {
        val controller = controllerRef?.get() ?: return "Działa podczas odtwarzania"
        if (controller.isUsingMpvEngine()) return "Odtwarzacz MPV: ten wariant jeszcze nieobsługiwany (użyj ExoPlayera)"
        if (AiKeyStore.read(c) == null) return "Brak klucza API: Ustawienia → Moduły → AI napisy"
        if (!AiSettings.textConsent(c)) return "Brak zgody na wysyłanie tekstu: Ustawienia → Moduły → AI napisy"
        return null
    }

    val running: Boolean get() = job?.isActive == true

    fun savedForCurrent(c: Context): List<AiSubtitleDoc> {
        val controller = controllerRef?.get() ?: return emptyList()
        val material = listOf(controller.contentId ?: controller.title, controller.currentVideoId ?: "").joinToString("|")
        return store(c).forMaterial(material)
    }

    fun start(c: Context, mode: AiMode, retranslate: Boolean = false): String {
        unavailableReason(c)?.let { return it }
        val controller = controllerRef?.get() ?: return "Działa podczas odtwarzania"
        stop(null)
        val app = c.applicationContext
        val id = ++sessionCounter
        var docKey = "pending"
        val port = ExoPlayerPort(controller) { docKey }
        noticeHidden.value = false
        _state.value = AiState(id, port.materialId, AiStage.SOURCES, notice = "AI: przygotowanie tekstu — wczytuję napisy")
        job = scope.launch {
            try {
                val gathered = SourceGatherer.gather(port)
                lastSource = gathered.source
                val model = AiSettings.model(app)
                val auxKey = gathered.aux
                var key = AiSubtitleDoc.keyFor("pl", gathered.cues, auxKey, model, TranslationPrompt.VERSION)
                val store = store(app)
                if (retranslate) key = key.take(24) + java.lang.Long.toHexString(System.currentTimeMillis()).takeLast(8)
                val doc = store.load(key) ?: newDoc(key, port, gathered, model)
                docKey = key
                store.save(doc)
                Log.i(TAG, "start session=$id blocks=${doc.blocks.size} done=${doc.doneBlocks} aux=${doc.aux.map { it.language }} missing=${doc.missingCategories}")
                val provider = GeminiProvider(AiKeyStore.read(app) ?: error("no key"), model)
                lateinit var s: AiSubtitleSession
                val engine = TranslationEngine(provider, ledger(app), eta(app), EngineSettings(review = AiSettings.review(app))) { e -> s.onEngineEvent(e) }
                s = AiSubtitleSession(doc, engine, store, port, eta(app), mode, id, fallbackOriginal = AiSettings.fallbackOriginal(app))
                s.engineReview = AiSettings.review(app)
                session = s
                val mirror = launch { s.state.collect { st -> if (session === s) _state.value = st } }
                s.run()
                mirror.cancel()
                _state.value = s.state.value
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "session=$id failed: ${e.javaClass.simpleName}: ${e.message?.take(160)}") // own messages only (no dialogue text, no key)
                _state.value = AiState(id, port.materialId, AiStage.FAILED, message = e.message ?: "Błąd", notice = "AI: ${e.message ?: "błąd"}")
            }
        }
        return if (mode == AiMode.WATCH) "AI: tłumaczenie startuje (napisy pojawią się blok po bloku)" else "AI: przygotowuję cały materiał"
    }

    private fun newDoc(key: String, port: PlayerPort, g: SourceGatherer.Gathered, model: String): AiSubtitleDoc {
        val now = System.currentTimeMillis()
        val ranges = BlockPlanner.plan(g.cues, maxCues = 120, maxSpanMs = 300_000L, firstBlockCues = 40)
        return AiSubtitleDoc(
            key = key, materialId = port.materialId, title = port.title, sourceLanguage = g.language, sourceRole = g.role,
            sourceLabel = g.source.label, availability = SourceAvailability.FULL_TEXT, cues = g.cues, aux = g.aux,
            missingCategories = g.missing,
            blocks = ranges.mapIndexed { i, r -> DocBlock("b%04d".format(java.util.Locale.ROOT, i + 1), r.first, r.last + 1) },
            provider = "gemini", model = model, promptVersion = TranslationPrompt.VERSION, createdAtMs = now, updatedAtMs = now,
        )
    }

    /** Reuse a saved translation: shown again without any new AI request. */
    fun showSaved(c: Context, doc: AiSubtitleDoc): String {
        val controller = controllerRef?.get() ?: return "Działa podczas odtwarzania"
        if (controller.isUsingMpvEngine()) return "Odtwarzacz MPV: ten wariant jeszcze nieobsługiwany"
        stop(null)
        val port = ExoPlayerPort(controller) { doc.key }
        val fallback = AiSettings.fallbackOriginal(c)
        scope.launch {
            val cues = doc.cues.mapIndexedNotNull { i, cue -> doc.translationFor(i)?.let { cue.copy(text = it) } ?: cue.takeIf { fallback } }
            val r = port.publish(cues, "AI PL (zapisane)")
            _state.value = AiState(++sessionCounter, doc.materialId, AiStage.DONE, doc.doneBlocks, doc.blocks.size, published = r == PublishResult.OK,
                message = if (r == PublishResult.OK) "Zapisane napisy AI włączone" else "Nie udało się włączyć", notice = "")
        }
        return "Włączam zapisane napisy AI (${doc.status})"
    }

    fun stop(reason: String?) {
        job?.cancel(); job = null; session = null
        if (reason != null) _state.value?.let { st -> if (st.stage != AiStage.DONE) _state.value = st.copy(stage = AiStage.STOPPED, message = reason, notice = "") }
    }

    fun cancel(): String { stop("Anulowano (postęp zapisany)"); return "Tłumaczenie AI anulowane, postęp zapisany" }

    fun showOriginal(): String {
        val controller = controllerRef?.get() ?: return "Działa podczas odtwarzania"
        val source = lastSource ?: return "Brak oryginału"
        stop("Pokazano oryginał (postęp zapisany)")
        scope.launch { ExoPlayerPort(controller) { "" }.restore(source) }
        return "Oryginalne napisy"
    }
}
