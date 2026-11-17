package com.nuvio.tv.fork.ai

/** Modele osadzeń znane aplikacji. */
enum class EmbedModel(val id: String, val approxMb: Int, val sharedWithPack: Boolean) {
    GEMMA("embeddinggemma-300m-q", 200, true),
    E5_SMALL("multilingual-e5-small-q", 120, false),
}

/** Wybór modelu według pamięci (B06b-3): przy 2 GB mniejszy model, ale tylko do mowy; polecenia filmów zawsze model z paczki. */
object ModelPicker {
    const val LOW_RAM_MB = 2_500

    fun forSpeech(totalRamMb: Int): EmbedModel = if (totalRamMb <= LOW_RAM_MB) EmbedModel.E5_SMALL else EmbedModel.GEMMA
    fun forMovies(): EmbedModel = EmbedModel.GEMMA
}

/**
 * Cykl życia modelu na żądanie: ładuje, gdy wolno (nie podczas odtwarzania, ≥ 400 MB wolnej pamięci),
 * zwalnia po 60 s bezczynności. Czysta logika z zegarem i atrapą ładowania.
 */
class ModelLifecycle<T : Any>(
    private val clock: () -> Long,
    private val playing: () -> Boolean,
    private val freeMemoryMb: () -> Int,
    private val loader: () -> T,
    private val onRelease: (T) -> Unit = {},
) {
    private var model: T? = null
    private var lastUsed = 0L

    class Unavailable(val reason: String) : Exception(reason)

    val loaded: Boolean get() = model != null

    @Synchronized fun acquire(): T {
        model?.let { lastUsed = clock(); return it }
        if (playing()) throw Unavailable("odtwarzanie")
        if (freeMemoryMb() < MIN_FREE_MB) throw Unavailable("pamięć")
        return loader().also { model = it; lastUsed = clock() }
    }

    /** Wołać cyklicznie (np. co 10 s): zwalnia po bezczynności albo gdy zaczęło się odtwarzanie. */
    @Synchronized fun tick() {
        val m = model ?: return
        if (playing() || clock() - lastUsed >= IDLE_MS) { model = null; onRelease(m) }
    }

    companion object { const val MIN_FREE_MB = 400; const val IDLE_MS = 60_000L }
}
