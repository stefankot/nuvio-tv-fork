package com.nuvio.tv.fork.tv

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import com.nuvio.tv.fork.logged

/** Karta programu: okładka, tło, opis, obsada, ocena, typ. Każde pole może brakować (ekran rysuje kartę zastępczą). */
@Serializable
data class ProgramCard(
    val programKey: String,
    val title: String,
    val coverUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    val cast: List<String> = emptyList(),
    val rating: Double? = null,
    val type: String? = null,
)

/** Okładka: obraz z EPG, potem plakat TMDB przy ścisłej zgodności tytułu, inaczej brak. */
fun chooseCover(epgImage: String?, tmdbPoster: String?, strictTitleMatch: Boolean): String? =
    epgImage?.takeIf { it.isNotBlank() } ?: tmdbPoster?.takeIf { strictTitleMatch && it.isNotBlank() }

/** Trwały cache kart. Brak dopasowania pamiętany [MISS_TTL_MS] (7 dni). */
class ProgramCardCache(private val file: File? = null, private val clock: () -> Long = System::currentTimeMillis) {
    sealed interface Result {
        data class Hit(val card: ProgramCard) : Result
        object KnownMissing : Result
        object Unknown : Result
    }

    @Serializable private data class Row(val key: String, val card: ProgramCard? = null, val atMs: Long)

    private val json = Json { ignoreUnknownKeys = true }
    private val rows = LinkedHashMap<String, Row>()

    init { load() }

    fun get(key: String): Result {
        val r = rows[key] ?: return Result.Unknown
        if (r.card != null) return Result.Hit(r.card)
        return if (clock() - r.atMs < MISS_TTL_MS) Result.KnownMissing else Result.Unknown
    }

    fun put(card: ProgramCard) { rows[card.programKey] = Row(card.programKey, card, clock()); save() }
    fun putMissing(key: String) { rows[key] = Row(key, null, clock()); save() }

    private fun load() {
        val f = file?.takeIf { it.isFile } ?: return
        runCatching { json.decodeFromString(ListSerializer(Row.serializer()), f.readText()).forEach { rows[it.key] = it } }.logged("ForkTv", "ProgramCard.kt:51")
    }

    private fun save() {
        val f = file ?: return
        runCatching {
            if (rows.size > MAX_ROWS) rows.entries.sortedBy { it.value.atMs }.take(rows.size - MAX_ROWS).forEach { rows.remove(it.key) }
            f.parentFile?.mkdirs()
            f.writeText(json.encodeToString(ListSerializer(Row.serializer()), rows.values.toList()))
        }.logged("ForkTv", "ProgramCard.kt:56")
    }

    companion object { const val MISS_TTL_MS = 7 * 24 * 3_600_000L; const val MAX_ROWS = 2_000 }
}
