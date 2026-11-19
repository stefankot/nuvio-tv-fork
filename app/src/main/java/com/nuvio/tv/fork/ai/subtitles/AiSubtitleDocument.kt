package com.nuvio.tv.fork.ai.subtitles

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import com.nuvio.tv.fork.logged

enum class SourceAvailability { FULL_TEXT, WINDOWED_TEXT, TIMING_ONLY, BITMAP, UNAVAILABLE }
enum class SourceRole { ORIGINAL, INTERMEDIARY_SOURCE }

@Serializable
data class DocCue(val id: String, val startMs: Long, val endMs: Long, val text: String)

@Serializable
data class AuxTrack(val category: String, val language: String, val sourceId: String, val label: String, val reason: String, val cues: List<DocCue>)

@Serializable
data class ReviewChange(val cueId: String, val before: String, val after: String, val problem: String)

@Serializable
data class BlockRevision(
    val revision: Int,
    val translations: List<String>,
    val stage: String, // "text" (audio revisions come in a later stage)
    val checks: List<String>,
    val warnings: List<String>,
    val reviewChanges: List<ReviewChange> = emptyList(),
    val model: String,
    val promptVersion: Int,
    val createdAtMs: Long,
)

@Serializable
data class DocBlock(val id: String, val first: Int, val endExclusive: Int, val revisions: List<BlockRevision> = emptyList(), val lastError: String? = null) {
    val accepted: BlockRevision? get() = revisions.lastOrNull()
}

/**
 * Internal translation document: text, timing, provenance and checks. Timing belongs to the app and is
 * never taken from the model. Saved under filesDir (user data), not cache, so clearing cache keeps it.
 */
@Serializable
data class AiSubtitleDoc(
    val schema: Int = 1,
    val key: String,
    val materialId: String,
    val title: String,
    val targetLanguage: String = "pl",
    val sourceLanguage: String,
    val sourceRole: SourceRole,
    val sourceLabel: String,
    val availability: SourceAvailability,
    val cues: List<DocCue>,
    val aux: List<AuxTrack>,
    val missingCategories: List<String>,
    val blocks: List<DocBlock>,
    val glossary: Map<String, String> = emptyMap(),
    val provider: String,
    val model: String,
    val promptVersion: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) {
    val doneBlocks: Int get() = blocks.count { it.accepted != null }
    val complete: Boolean get() = blocks.isNotEmpty() && blocks.all { it.accepted != null }

    /** Honest status: automatic checks are not a human review; full source coverage ≠ every line of the film. */
    val status: String get() = when {
        !complete -> "częściowe"
        blocks.all { b -> b.accepted?.checks?.contains("review") == true } -> "kompletne względem źródła, sprawdzone automatycznie"
        else -> "kompletne względem źródła, kontrola struktury"
    }

    fun translationFor(index: Int): String? {
        val block = blocks.firstOrNull { index >= it.first && index < it.endExclusive } ?: return null
        return block.accepted?.translations?.getOrNull(index - block.first)
    }

    fun translatedCues(): List<ValidatedCue> = cues.indices.mapNotNull { i ->
        translationFor(i)?.let { ValidatedCue(SourceCue(cues[i].id, cues[i].startMs, cues[i].endMs, cues[i].text), it) }
    }

    companion object {
        /** Text-translation key: real sources (text only, so a timing shift needs no new translation), language, model, prompt. */
        fun keyFor(target: String, source: List<DocCue>, aux: List<AuxTrack>, model: String, promptVersion: Int): String {
            val md = MessageDigest.getInstance("SHA-256")
            fun feed(s: String) { md.update(s.toByteArray(Charsets.UTF_8)); md.update(0) }
            feed(target); feed(model); feed(promptVersion.toString())
            source.forEach { feed(it.text) }
            aux.sortedBy { it.category }.forEach { a -> feed(a.category); feed(a.language); a.cues.forEach { feed(it.text) } }
            return md.digest().joinToString("") { String.format(java.util.Locale.ROOT, "%02x", it) }.take(32)
        }
    }
}

/** Saved translations: one atomic JSON file per document; partial progress survives crashes without paying again. */
class AiSubtitleStore(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun save(doc: AiSubtitleDoc) = synchronized(lock) { atomicWrite(File(dir, "${doc.key}.json"), json.encodeToString(AiSubtitleDoc.serializer(), doc)) }

    fun load(key: String): AiSubtitleDoc? = synchronized(lock) {
        val f = File(dir, "$key.json")
        if (!f.exists()) null else runCatching { json.decodeFromString(AiSubtitleDoc.serializer(), f.readText()) }.logged("ForkAi", "AiSubtitleDocument.kt:104").getOrNull()
    }

    fun list(): List<AiSubtitleDoc> = synchronized(lock) {
        dir.listFiles { _, n -> n.endsWith(".json") }.orEmpty()
            .mapNotNull { runCatching { json.decodeFromString(AiSubtitleDoc.serializer(), it.readText()) }.logged("ForkAi", "AiSubtitleDocument.kt:109").getOrNull() }
            .sortedByDescending { it.updatedAtMs }
    }

    fun forMaterial(materialId: String): List<AiSubtitleDoc> = list().filter { it.materialId == materialId }

    fun delete(key: String): Boolean = synchronized(lock) { File(dir, "$key.json").delete() }

    private companion object { val lock = Any() }
}
