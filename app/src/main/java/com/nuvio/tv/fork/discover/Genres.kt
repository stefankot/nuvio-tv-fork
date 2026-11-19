package com.nuvio.tv.fork.discover

import com.nuvio.tv.fork.voice.Text
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.nuvio.tv.fork.logged

@Serializable
private data class GenreFile(val buckets: Map<String, List<String>> = emptyMap(), val groupHints: Map<String, String> = emptyMap())

/** 8 koszyków gatunków (B08-1). Słownik słów kluczowych w `assets/fork/genres.json`. */
class GenreBuckets(text: String) {
    private val file = runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<GenreFile>(text) }.logged("ForkDiscover", "Genres.kt:12").getOrDefault(GenreFile())
    private val keywords: List<Pair<String, String>> = file.buckets.flatMap { (b, words) -> words.map { Text.norm(it) to b } }
    private val hints = file.groupHints.mapKeys { Text.norm(it.key) }

    /** Koszyki dla kategorii z EPG (PL/EN). Bez kategorii: koszyk z nazwy grupy kanału; inaczej null. */
    fun bucketOf(categories: List<String>, channelGroup: String? = null): String? {
        for (c in categories) {
            val n = Text.norm(c)
            if (n.isEmpty()) continue
            // Dłuższe słowa kluczowe wygrywają ("dla dzieci" przed "dzieci", "kryminalny dokument" przed "kryminal").
            keywords.filter { (k, _) -> k.isNotEmpty() && n.contains(k) }.maxByOrNull { it.first.length }?.let { return it.second }
        }
        if (categories.isEmpty() || categories.all { it.isBlank() }) channelGroup?.let { g ->
            val tokens = Text.norm(g).split(' ')
            tokens.firstNotNullOfOrNull { hints[it] }?.let { return it }
        }
        return null
    }

    companion object { val ALL = listOf("Film", "Serial", "Sport", "Dla dzieci", "Dokument", "Rozrywka", "Informacje", "Muzyka") }
}
