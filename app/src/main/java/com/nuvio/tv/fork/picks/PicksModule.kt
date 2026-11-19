package com.nuvio.tv.fork.picks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
                    poster = t.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                )
            }
        }
        out
    }

    /** Nastrój wybrany kaflem na ekranie głównym (otwiera ekran z gotową listą). */
    @Volatile var pendingMood: String? = null

    fun moods(c: android.content.Context): List<Mood> =
        Moods.parse(runCatching { c.assets.open("fork/moods.json").bufferedReader().use { it.readText() } }.getOrDefault(""))

    /** Właściciel 19.11: nastroje jako kafle na ekranie głównym (skórka Kicia: hook w ForgeHomeContent). */
    @Composable
    fun KiciaHomeShelf(onFocused: () -> Unit) {
        val c = LocalContext.current
        if (!ForkFeatures.enabled(c, "module_$id", defaultEnabled)) return
        val moods = remember { moods(c) }
        if (moods.isEmpty()) return
        com.nuvio.tv.skin.kicia.components.ForgeShelf(title = "Na co masz ochotę?") {
            items(moods, key = { "mood_${it.id}" }) { m ->
                com.nuvio.tv.skin.kicia.components.ForgeWideCard(
                    onFocusChange = { if (it) onFocused() },
                    onOk = { pendingMood = m.id; ForkFeatures.open?.invoke(ROUTE) },
                ) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = androidx.compose.ui.Alignment.BottomStart) {
                        Text(m.title, style = com.nuvio.tv.skin.kicia.tokens.ForgeText.label, maxLines = 3)
                    }
                }
            }
        }
    }

    @Composable
    private fun PicksScreen() {
        val c = LocalContext.current
        val scope = rememberCoroutineScope()
        val moods = remember { moods(c) }
        var picked by remember { mutableStateOf<Mood?>(null) }
        var results by remember { mutableStateOf<List<PickTitle>>(emptyList()) }
        var status by remember { mutableStateOf("") }
        fun choose(m: Mood) {
            picked = m; status = "Szukam…"; results = emptyList()
            scope.launch {
                val cand = candidates(c, m)
                results = RuleScoring.rank(cand, TasteProfile(), m).map { it.first }.take(40)
                status = if (results.isEmpty()) (if (BuildConfig.TMDB_API_KEY.isBlank()) "Brak klucza TMDB" else "Nic nie znaleziono") else ""
            }
        }
        LaunchedEffect(Unit) { pendingMood?.let { id -> pendingMood = null; moods.firstOrNull { it.id == id }?.let(::choose) } }
        fun open(t: PickTitle) {
            scope.launch {
                val imdb = withContext(Dispatchers.IO) {
                    runCatching {
                        EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).tmdb()
                            .getMovieExternalIds(t.tmdbId, BuildConfig.TMDB_API_KEY).body()?.imdbId
                    }.getOrNull()
                }
                if (imdb != null) ForkFeatures.open?.invoke(Screen.Detail.createRoute(imdb, "movie"))
            }
        }
        Column(Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(picked?.title ?: "Na co masz ochotę?", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.title)
            // Właściciel 19.11: ekran był pusty — ta wersja nie ma wbudowanego klucza TMDB (local.properties: TMDB_API_KEY).
            if (BuildConfig.TMDB_API_KEY.isBlank()) Text("Polecenia nie działają: ta wersja aplikacji nie ma klucza TMDB. Po dodaniu klucza i nowym buildzie pojawią się filmy.")
            if (moods.isEmpty()) Text("Brak listy nastrojów (plik fork/moods.json)")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(moods, key = { it.id }) { m ->
                    com.nuvio.tv.skin.kicia.components.ForgePillSelect(text = m.title, selected = picked?.id == m.id, bare = false, onOk = { choose(m) })
                }
            }
            if (status.isNotEmpty()) Text(status)
            // Właściciel 19.11: pozycje jak wszystkie inne filmy — okładki w siatce.
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(com.nuvio.tv.skin.kicia.tokens.ForgeDims.Poster),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            ) {
                items(results.size, key = { results[it].tmdbId }) { i ->
                    val t = results[i]
                    Column {
                        com.nuvio.tv.skin.kicia.components.ForgePosterCard(onOk = { open(t) }) {
                            com.nuvio.tv.skin.kicia.components.ForgePosterImage(t.poster, null, t.title)
                        }
                        Text(t.title, style = com.nuvio.tv.skin.kicia.tokens.ForgeText.caption, maxLines = 1, modifier = Modifier.padding(top = 8.dp).width(com.nuvio.tv.skin.kicia.tokens.ForgeDims.Poster))
                        Text("${t.year} · ★ ${"%.1f".format(java.util.Locale("pl"), t.rating)}", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.caption.copy(color = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Text3), maxLines = 1)
                    }
                }
            }
        }
    }
}
