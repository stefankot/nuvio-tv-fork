package com.nuvio.tv.fork.picks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.ModuleRoute
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.fork.voice.Text as Norm
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Polecenia według nastroju (B10): „Na co masz ochotę?”. Kandydaci z TMDB (discover), punktacja regułowa B10-3, bez profilu gustu (pusty profil). */
object PicksModule : ForkModule {
    override val id = "picks"
    override val title = "Na co masz ochotę? (nastroje)"
    override val defaultEnabled = true
    const val ROUTE = "fork_picks"

    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun tmdb(): TmdbApi }

    /** Nazwy gatunków TMDB (filmy) → identyfikatory; klucze po normalizacji. */
    private val genreIds = mapOf(
        "akcja" to 28, "sensacja" to 28, "przygodowy" to 12, "animacja" to 16, "komedia" to 35, "kryminal" to 80, "kryminalny" to 80, "dokument" to 99,
        "dramat" to 18, "familijny" to 10751, "fantasy" to 14, "historyczny" to 36, "horror" to 27, "muzyczny" to 10402, "tajemnica" to 9648,
        "romans" to 10749, "sci-fi" to 878, "thriller" to 53, "wojenny" to 10752, "western" to 37,
    )

    override fun actions() = listOf(
        ModuleAction("Na co masz ochotę?", SettingsLevel.SIMPLE, "Wybierz nastrój, dostaniesz listę filmów") { ForkFeatures.open?.invoke(ROUTE) },
    )

    override fun routes() = listOf(ModuleRoute(ROUTE) { composable(ROUTE) { PicksScreen() } })

    /** Kandydaci dla nastroju: discover po gatunkach nastroju (lub po ocenie dla nastrojów bez gatunków). Brak klucza TMDB → pusta lista. */
    suspend fun candidates(c: android.content.Context, mood: Mood): List<PickTitle> = withContext(Dispatchers.IO) {
        val key = BuildConfig.TMDB_API_KEY
        if (key.isBlank()) return@withContext emptyList()
        val tmdb = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).tmdb()
        val ids = mood.genres.entries.sortedByDescending { it.value }.mapNotNull { genreIds[Norm.norm(it.key)] }.distinct().take(4)
        val sort = if (mood.popularityBias < 0) "vote_average.desc" else "popularity.desc"
        val out = ArrayList<PickTitle>()
        for (page in 1..2) {
            val r = runCatching {
                tmdb.discoverMovies(
                    apiKey = key, language = "pl-PL", page = page, sortBy = sort,
                    voteCountGte = if (mood.votesMin > 0) mood.votesMin else 200,
                    withGenres = ids.takeIf { it.isNotEmpty() }?.joinToString("|"),
                ).body()?.results.orEmpty()
            }.getOrDefault(emptyList())
            r.forEach { t ->
                out += PickTitle(
                    tmdbId = t.id, title = t.title ?: t.originalTitle ?: return@forEach, genres = mood.genres.keys.toList(),
                    year = t.releaseDate?.take(4)?.toIntOrNull() ?: 2000, rating = t.voteAverage ?: 6.5, votes = t.voteCount ?: 0,
                )
            }
        }
        out
    }

    @Composable
    private fun PicksScreen() {
        val c = LocalContext.current
        val scope = rememberCoroutineScope()
        val moods = remember { Moods.parse(runCatching { c.assets.open("fork/moods.json").bufferedReader().use { it.readText() } }.getOrDefault("")) }
        var picked by remember { mutableStateOf<Mood?>(null) }
        var results by remember { mutableStateOf<List<PickTitle>>(emptyList()) }
        var status by remember { mutableStateOf("") }
        Column(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Na co masz ochotę?", style = MaterialTheme.typography.headlineMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(moods, key = { it.id }) { m ->
                    Button(onClick = {
                        picked = m; status = "Szukam…"; results = emptyList()
                        scope.launch {
                            val cand = candidates(c, m)
                            results = RuleScoring.rank(cand, TasteProfile(), m).map { it.first }.take(20)
                            status = if (results.isEmpty()) (if (BuildConfig.TMDB_API_KEY.isBlank()) "Brak klucza TMDB" else "Nic nie znaleziono") else ""
                        }
                    }) { Text(m.title) }
                }
            }
            if (status.isNotEmpty()) Text(status)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.tmdbId }) { t ->
                    Card(onClick = {
                        scope.launch {
                            val imdb = withContext(Dispatchers.IO) {
                                runCatching {
                                    EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).tmdb()
                                        .getMovieExternalIds(t.tmdbId, BuildConfig.TMDB_API_KEY).body()?.imdbId
                                }.getOrNull()
                            }
                            if (imdb != null) ForkFeatures.open?.invoke(Screen.Detail.createRoute(imdb, "movie"))
                        }
                    }) { Text("${t.title} (${t.year}) ★ ${"%.1f".format(t.rating)}", Modifier.padding(12.dp)) }
                }
            }
        }
    }
}
