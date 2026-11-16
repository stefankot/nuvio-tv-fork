package com.nuvio.tv.livetv.bridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.livetv.data.ChannelIdentity
import com.nuvio.tv.livetv.source.ChannelPack
import com.nuvio.tv.livetv.source.IptvMetadataRepository
import com.nuvio.tv.livetv.source.LiveSourceManager
import com.nuvio.tv.livetv.source.PackChannel
import com.nuvio.tv.livetv.source.PackScanStore
import com.nuvio.tv.livetv.source.PackScanner
import com.nuvio.tv.livetv.source.PackStore
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KeCatalogUi(
    val busy: Boolean = false,
    val message: String = "",
    val countries: List<Pair<String?, Int>> = emptyList(),
    val categories: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val category: String? = null,
    val language: String? = null,
)

/**
 * KE catalog adapter: the channel pack from Nuvio-KE-channels (by country and iptv-org category/language)
 * is turned into an M3U playlist per country and handed to the Reshaped Live TV (its list, guide and player).
 */
@HiltViewModel
class KeCatalogViewModel @Inject constructor(
    private val manager: LiveSourceManager,
    private val packs: PackStore,
    private val scans: PackScanStore,
    private val metadata: IptvMetadataRepository,
    val scanner: PackScanner,
) : ViewModel() {
    private val _ui = MutableStateFlow(KeCatalogUi())
    val ui: StateFlow<KeCatalogUi> = _ui.asStateFlow()
    private var channels: List<PackChannel> = emptyList()

    init { viewModelScope.launch { reload() } }

    private suspend fun reload() {
        channels = packs.read()
        metadata.load()
        val meta = metadata.metadata.value?.channels.orEmpty()
        fun metaOf(c: PackChannel) = c.tvgId?.let { ChannelIdentity.normalizeTvgId(it) }?.let(meta::get)
        val countries = channels.groupingBy { it.country }.eachCount().toList().sortedWith(compareBy({ it.first == null }, { it.first }))
        val cats = channels.flatMap { metaOf(it)?.categories ?: it.groups }.distinct().sorted()
        val langs = channels.flatMap { metaOf(it)?.languages.orEmpty() }.distinct().sorted()
        _ui.update { it.copy(countries = countries, categories = cats, languages = langs) }
    }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(busy = true, message = "Pobieranie katalogu…") }
        val result = runCatching { manager.refreshPack(); metadata.refreshIfStale() }
        reload()
        _ui.update { it.copy(busy = false, message = if (result.isSuccess) "Katalog: ${channels.size} kanałów" else "Błąd pobierania katalogu") }
    }

    fun cycleCategory() = _ui.update { s -> s.copy(category = next(s.categories, s.category)) }
    fun cycleLanguage() = _ui.update { s -> s.copy(language = next(s.languages, s.language)) }
    private fun next(list: List<String>, cur: String?): String? =
        if (list.isEmpty()) null else if (cur == null) list.first() else list.getOrNull(list.indexOf(cur) + 1)

    /** Builds the country's playlist (filtered) and adds it to Reshaped Live TV as a source. */
    fun importCountry(country: String?) = viewModelScope.launch {
        val s = _ui.value
        _ui.update { it.copy(busy = true, message = "Dodawanie…") }
        val meta = metadata.metadata.value?.channels.orEmpty()
        val results = scans.read().results
        val text = withContext(Dispatchers.Default) {
            buildString {
                append("#EXTM3U\n")
                for (c in channels) {
                    if (c.country != country) continue
                    val m = c.tvgId?.let { ChannelIdentity.normalizeTvgId(it) }?.let(meta::get)
                    val cats = m?.categories ?: c.groups
                    if (s.category != null && s.category !in cats) continue
                    if (s.language != null && s.language !in m?.languages.orEmpty()) continue
                    val stream = ChannelPack.visibleStreams(c, results).firstOrNull() ?: continue
                    val group = (cats.firstOrNull() ?: "Other").replaceFirstChar { it.uppercase() }
                    append("#EXTINF:-1 tvg-id=\"${c.tvgId.orEmpty()}\" group-title=\"$group\",${c.name.replace(',', ' ')}\n")
                    stream.headers?.forEach { (k, v) ->
                        when (k.lowercase()) {
                            "user-agent" -> append("#EXTVLCOPT:http-user-agent=$v\n")
                            "referer" -> append("#EXTVLCOPT:http-referrer=$v\n")
                            "origin" -> append("#EXTVLCOPT:http-origin=$v\n")
                        }
                    }
                    append(stream.url).append('\n')
                }
            }
        }
        val bytes = text.toByteArray()
        val ok = bytes.size > 10 && LiveTvRepository.importPlaylist("KE · ${country?.uppercase() ?: "—"}", bytes.inputStream(), 64L * 1024 * 1024)
        _ui.update { it.copy(busy = false, message = if (ok) "Dodano źródło KE · ${country?.uppercase() ?: "—"}" else "Brak kanałów dla filtra") }
    }

    fun startScan(country: String?) { scanner.start(setOf(country)) }
    fun cancelScan() = scanner.cancel()
}
