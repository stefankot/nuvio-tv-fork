package com.nuvio.tv.skin.kicia.livetv

import android.content.Context
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.fork.logged

/**
 * Panel „Kategorie” of mockup v7.4 (06, W-68; owner 13.11: categories from the EPG, not the playlist groups), with the
 * mockup's own rules ([ForgeCategoryRules], generated from docs/makieta-tv/v7/narzedzia/kategorie-epg.mjs):
 *  - Kanały: a two-level tree of channels by the channel's name (EPG.OVH names; the source has no channel groups),
 *  - Programy: a two-level tree of programmes by the guide's `<category>` (~800 raw values, PL/EN mixed).
 * Anything no rule matches goes to "Inne", so a changed guide never breaks the tree; branches without anything are hidden.
 *
 * Programme categories are recorded once per guide read for every kept programme ([record], hook in LiveTvEpg) and kept
 * in a file, so the tree is there after a start from the guide cache too (the guide reads every 12 h).
 */
object ForgeEpgCategories {
    /** Channel filter keys: "\u0000kat:sport" (branch) and "\u0000kat:sport/polskie" (leaf). */
    const val PREFIX = "\u0000kat:"
    /** Programme view keys (not a channel filter): "\u0000prog:*", "\u0000prog:sport", "\u0000prog:sport/pilka". */
    const val PROG_PREFIX = "\u0000prog:"
    const val PROG_ALL = PROG_PREFIX + "*"
    private const val OTHER = "inne"

    data class Place(val branch: String, val leaf: String?)
    data class Node(val key: String, val name: String, val count: Int, val children: List<Node> = emptyList())

    // ---- text and rules ----

    /**
     * As the mockup's norm(): lower case, ł → l, no diacritics.
     * JMGO 22.11 (59 349 kanałów): bez wyrażenia regularnego — każde `replace(Regex)` tworzyło natywny Matcher ICU,
     * co przy dziesiątkach tysięcy nazw wymuszało odśmiecanie pamięci. Usuwa te same znaki co `\p{Mn}`.
     */
    fun norm(s: String): String {
        val d = Normalizer.normalize(s.lowercase().replace('ł', 'l'), Normalizer.Form.NFD)
        val out = StringBuilder(d.length)
        var i = 0
        while (i < d.length) {
            val cp = d.codePointAt(i)
            if (Character.getType(cp) != Character.NON_SPACING_MARK.toInt()) out.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return out.toString()
    }

    /**
     * `containsMatchIn` bez nowego Matchera na każde wywołanie (JMGO 22.11: 252 827 nazw × kilkanaście reguł = miliony
     * natywnych Matcherów ICU, kolejność kanałów liczyła się 143 s). Jeden Matcher na regułę i wątek, ten sam wynik.
     */
    private val matchers = ThreadLocal<HashMap<Regex, java.util.regex.Matcher>>()
    private fun Regex.hit(text: String): Boolean {
        val cache = matchers.get() ?: HashMap<Regex, java.util.regex.Matcher>().also(matchers::set)
        return cache.getOrPut(this) { toPattern().matcher("") }.reset(text).find()
    }

    private fun placeIn(tree: ForgeCategoryRules.Tree, text: String): Place {
        val t = norm(text)
        for (id in tree.order) {
            val b = tree.branches.firstOrNull { it.id == id } ?: continue
            if (b.rx.hit(t)) return Place(b.id, b.leaves.firstOrNull { it.rx.hit(t) }?.id)
        }
        return Place(OTHER, null)
    }

    private fun branchName(tree: ForgeCategoryRules.Tree, id: String) = tree.branches.firstOrNull { it.id == id }?.name ?: "Inne"
    private fun leafName(tree: ForgeCategoryRules.Tree, b: String, l: String?) =
        l?.let { id -> tree.branches.firstOrNull { it.id == b }?.leaves?.firstOrNull { it.id == id }?.name } ?: "Pozostałe"

    // ---- channels ----

    private val channelCache = ConcurrentHashMap<String, Place>()
    fun channelPlace(ch: LiveTvChannel): Place {
        val name = ch.tvgName?.takeIf { it.isNotBlank() } ?: ch.name
        return channelCache.getOrPut(name + "\u0000" + ch.name) {
            // Paczka „oczywiste” pkt 6 (21.11): reguły makiety są pisane dla czystych nazw („tvn$”, „^tvp ?1”), a playlisty dają
            // „PL: TVN HD”, „TVN.pl”, „Polsat (PL) FHD” — kanały trafiały do złej gałęzi albo do „Inne”. Najpierw oczyszczona
            // nazwa z przewodnika, potem z playlisty; pierwsza, która pasuje do gałęzi innej niż „Inne”, wygrywa.
            val tries = listOf(name, ch.name).map(::cleanChannelName).filter { it.isNotEmpty() }.distinct()
            val place = tries.asSequence().map { placeIn(ForgeCategoryRules.CHANNELS, it) }.firstOrNull { it.branch != OTHER }
                ?: Place(OTHER, null)
            val raw = placeIn(ForgeCategoryRules.CHANNELS, name)
            if (raw != place) android.util.Log.d("ForkKiciaLivetv", "kategoria kanału „${ch.name}”: ${raw.branch}/${raw.leaf} → ${place.branch}/${place.leaf} (nazwa „${tries.firstOrNull()}”)")
            place
        }
    }

    private val countryTag = Regex("^\\[?[a-z]{2,3}\\]?\\s*[:|]\\s*")
    private val bracketed = Regex("\\s*[(\\[][^)\\]]*[)\\]]")
    private val qualityTail = Regex("\\s+(fhd|uhd|hd|sd|4k|8k|hevc|h\\.?26[45]|hdr|raw|backup|alt|vip|50 ?fps|\\+1|pl)$")
    private val domainTail = Regex("\\.(pl|tv|com|uk|de|cz|ua)$")
    private val spaces = Regex("\\s+")

    /** Nazwa kanału do reguł: bez „PL:”, „(PL)”, „[FHD]”, „HD/4K…” na końcu i „.pl” z identyfikatora przewodnika. */
    internal fun cleanChannelName(raw: String): String {
        var t = norm(raw).replace('_', ' ').trim()
        t = countryTag.replace(t, "")
        t = bracketed.replace(t, " ")
        t = domainTail.replace(t, "")
        t = t.replace(spaces, " ").trim()
        var prev: String
        do { prev = t; t = qualityTail.replace(t, "").trim() } while (t != prev && t.isNotEmpty())
        return t
    }

    /**
     * 22.11 (właściciel: „jeden kanał może być w kilku kategoriach, np. informacyjne i zagraniczne”): wszystkie gałęzie,
     * których reguła pasuje do nazwy (nie tylko pierwsza); kanał z liścia zagranicznego (np. Informacje · Zagraniczne,
     * Sport · Zagraniczne sportowe) trafia też do gałęzi „Zagraniczne”. „Inne” tylko, gdy nic innego nie pasuje.
     */
    fun channelPlaces(ch: LiveTvChannel): List<Place> {
        val name = ch.tvgName?.takeIf { it.isNotBlank() } ?: ch.name
        return channelPlacesCache.getOrPut(name) {
            val tree = ForgeCategoryRules.CHANNELS
            val t = norm(name)
            val out = ArrayList<Place>(2)
            for (id in tree.order) {
                val b = tree.branches.firstOrNull { it.id == id } ?: continue
                if (b.rx.hit(t)) out += Place(b.id, b.leaves.firstOrNull { it.rx.hit(t) }?.id)
            }
            if (out.any { it.leaf in FOREIGN_LEAVES } && out.none { it.branch == FOREIGN }) out += Place(FOREIGN, null)
            val real = out.filter { it.branch != OTHER }
            real.ifEmpty { listOf(out.firstOrNull() ?: Place(OTHER, null)) }
        }
    }
    private val channelPlacesCache = ConcurrentHashMap<String, List<Place>>()
    private const val FOREIGN = "zagraniczne"
    private val FOREIGN_LEAVES = setOf("zagraniczne", "zagraniczna", "zachodnie", "ukrainskie", "czesko-slowackie")

    /** Kanał zagraniczny (gałąź „Zagraniczne” albo liść zagraniczny). */
    fun isForeign(ch: LiveTvChannel): Boolean = channelPlaces(ch).any { it.branch == FOREIGN }

    /** 22.11 (właściciel): najpierw kanały polskojęzyczne, potem zagraniczne; w obu grupach kolejność listy bez zmian. */
    fun polishFirst(channels: List<LiveTvChannel>): List<LiveTvChannel> {
        val (foreign, polish) = channels.partition(::isForeign)
        if (foreign.isNotEmpty() && polish.isNotEmpty()) android.util.Log.i("ForkKiciaLivetv", "kanały: ${polish.size} polskich przed ${foreign.size} zagranicznymi")
        return polish + foreign
    }

    fun isKey(key: String): Boolean = key.startsWith(PREFIX)
    fun isProgrammeKey(key: String): Boolean = key.startsWith(PROG_PREFIX)
    private fun keyMatches(p: Place, sel: String): Boolean =
        if ('/' in sel) p.branch == sel.substringBefore('/') && (p.leaf ?: "") == sel.substringAfter('/') else p.branch == sel

    /** The name a key shows ("Sport", "Sport · Polskie sportowe"). */
    fun nameOf(key: String): String {
        val programmes = key.startsWith(PROG_PREFIX)
        val tree = if (programmes) ForgeCategoryRules.PROGRAMMES else ForgeCategoryRules.CHANNELS
        val sel = key.removePrefix(if (programmes) PROG_PREFIX else PREFIX)
        if (sel == "*") return "Wszystkie programy"
        val b = sel.substringBefore('/')
        return if ('/' in sel) branchName(tree, b) + " · " + leafName(tree, b, sel.substringAfter('/').ifEmpty { null }) else branchName(tree, b)
    }

    /** Hook for Reshaped's filterChannels: the channels of a channel-tree key, or null for other keys. */
    fun filter(channels: List<LiveTvChannel>, key: String, hiddenChannels: Set<Long>, query: String): List<LiveTvChannel>? {
        if (!isKey(key)) return null
        val sel = key.removePrefix(PREFIX)
        val needle = query.trim()
        return channels.filter { ch ->
            ch.hideKey !in hiddenChannels && channelPlaces(ch).any { keyMatches(it, sel) } &&
                (needle.isEmpty() || ch.name.contains(needle, ignoreCase = true))
        }
    }

    /** Kanały: the branches with their channel counts and leaves, in the mockup's order; empty branches left out. */
    fun channelTree(channels: List<LiveTvChannel>, hiddenGroups: Set<String>): List<Node> {
        val counts = HashMap<String, Int>()
        channels.forEach { ch ->
            if (ch.group in hiddenGroups) return@forEach
            channelPlaces(ch).forEach { p ->
                counts.merge(p.branch, 1, Int::plus)
                counts.merge(p.branch + "/" + (p.leaf ?: ""), 1, Int::plus)
            }
        }
        return buildTree(ForgeCategoryRules.CHANNELS, PREFIX, counts)
    }

    private fun buildTree(tree: ForgeCategoryRules.Tree, prefix: String, counts: Map<String, Int>): List<Node> =
        (tree.branches.map { it.id } + OTHER).distinct().mapNotNull { b ->
            val n = counts[b] ?: return@mapNotNull null
            val leaves = (tree.branches.firstOrNull { it.id == b }?.leaves.orEmpty().map { it.id } + "")
                .mapNotNull { l -> counts["$b/$l"]?.let { c -> Node("$prefix$b/$l", leafName(tree, b, l.ifEmpty { null }), c) } }
            // One leaf only: no second level.
            Node(prefix + b, branchName(tree, b), n, if (leaves.size > 1) leaves else emptyList())
        }

    // ---- programmes ----

    /** "guideKey|start" → normalised raw category, for every kept programme of the last guide read. */
    private val programmeCats = ConcurrentHashMap<String, String>()
    private val placeOfCat = ConcurrentHashMap<String, Place>()
    private val version = AtomicInteger()
    @Volatile private var file: File? = null
    @Volatile private var loaded = false
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private const val MAX_RECORDS = 200_000
    private const val KEEP_MS = 9L * 86_400_000

    /** Programme categories change only with a guide read; screens key their lists on this. */
    fun programmeVersion(): Int = version.get()

    /** Called once at start (ForgePrefs.ensure): reads the categories kept from the last guide read. */
    fun ensure(context: Context) {
        if (file != null) return
        val f = File(context.applicationContext.filesDir, "forge_epg_categories.tsv")
        file = f
        io.launch {
            runCatching {
                // Wiele rdzeni / wydajność 19.11: do 200 tys. linii — bez split (lista na linię), kategorie wspólne (mało różnych).
                val cats = HashMap<String, String>()
                var n = 0
                if (f.exists()) f.forEachLine { line ->
                    val t1 = line.indexOf('\t'); val t2 = if (t1 < 0) -1 else line.indexOf('\t', t1 + 1)
                    if (t2 > 0 && line.indexOf('\t', t2 + 1) < 0) {
                        val cat = line.substring(t2 + 1).let { c -> cats.getOrPut(c) { c } }
                        programmeCats.putIfAbsent(line.substring(0, t1) + "|" + line.substring(t1 + 1, t2), cat); n++
                    }
                }
                android.util.Log.i("ForkKiciaLivetv", "kategorie EPG: $n wpisów, ${cats.size} kategorii")
            }.logged("ForkKiciaLivetv", "ForgeEpgCategories.kt:137")
            loaded = true
            version.incrementAndGet()
        }
    }

    /**
     * forge fix 13.11 (JMGO: Kategorie → Programy showed „Wszystkie programy 0”): categories are only met while the XMLTV
     * file is read, and a start served by the saved programmes (guide_kept) never reads it, so a build that brought this
     * file, or a lost file, left the tree empty until the next download. True once the start-up load found no file:
     * the guide is then read again from its saved file.
     */
    fun needsGuideRead(): Boolean = loaded && programmeCats.isEmpty() && file?.exists() == false

    /** After a full guide read: the file exists even when the guide has no categories, so it is not read again for them. */
    fun markRead() {
        val f = file ?: return
        io.launch { runCatching { if (!f.exists()) f.createNewFile() }.logged("ForkKiciaLivetv", "ForgeEpgCategories.kt:159") }
    }

    /** Hook in LiveTvEpg: a kept programme of the guide (its channel's guide keys, start, first <category>). */
    fun record(keys: List<String>, start: Long, raw: String?) {
        val cat = raw?.trim()?.trimEnd('.', ',', ';')?.takeIf { it.isNotEmpty() && it.length <= 60 }?.lowercase() ?: return
        if (programmeCats.size >= MAX_RECORDS) return
        keys.forEach { programmeCats[it + "|" + start] = cat }
        version.incrementAndGet()
        scheduleSave()
    }

    private fun scheduleSave() {
        val f = file ?: return
        synchronized(this) {
            saveJob?.cancel()
            saveJob = io.launch {
                delay(20_000) // after the guide read has finished
                val cutoff = LiveTvClock.nowEpochMs() - KEEP_MS
                runCatching {
                    val tmp = File(f.parentFile, f.name + ".tmp")
                    tmp.bufferedWriter().use { w ->
                        programmeCats.entries.forEach { (k, v) ->
                            val start = k.substringAfterLast('|').toLongOrNull() ?: return@forEach
                            if (start < cutoff) { programmeCats.remove(k); return@forEach }
                            w.append(k.substringBeforeLast('|')).append('\t').append(start.toString()).append('\t').append(v).append('\n')
                        }
                    }
                    tmp.renameTo(f)
                }.logged("ForkKiciaLivetv", "ForgeEpgCategories.kt:178")
            }
        }
    }

    /** The raw category of a programme (as the guide wrote it, lower case), or null. */
    fun categoryOf(guideKey: String, p: LiveTvProgramme): String? = programmeCats[guideKey + "|" + p.startEpochMs]

    fun programmePlace(guideKey: String, p: LiveTvProgramme): Place? =
        categoryOf(guideKey, p)?.let { c -> placeOfCat.getOrPut(c) { placeIn(ForgeCategoryRules.PROGRAMMES, c) } }

    /** Fork 20.11 (Niunia, odtwarzacz IPTV): nazwy gałęzi i liścia programu („Informacje i publicystyka” › „Wiadomości”), null bez kategorii. */
    fun programmePlaceNames(guideKey: String, p: LiveTvProgramme): Pair<String, String?>? =
        programmePlace(guideKey, p)?.let { pl -> branchName(ForgeCategoryRules.PROGRAMMES, pl.branch) to pl.leaf?.let { leafName(ForgeCategoryRules.PROGRAMMES, pl.branch, it) } }

    /** Branch name of a programme ("Sport"), or null when the guide gives no category. */
    fun programmeBranchName(guideKey: String, p: LiveTvProgramme): String? =
        programmePlace(guideKey, p)?.takeIf { it.branch != OTHER }?.let { branchName(ForgeCategoryRules.PROGRAMMES, it.branch) }

    fun programmeMatches(guideKey: String, p: LiveTvProgramme, key: String): Boolean {
        val sel = key.removePrefix(PROG_PREFIX)
        if (sel == "*") return programmePlace(guideKey, p) != null
        return programmePlace(guideKey, p)?.let { keyMatches(it, sel) } == true
    }

    /**
     * Programy: per branch and leaf, how many programmes (one per title, like the programme view's tiles) can be
     * played from the archive or air within 2 days (each guide channel once). forge fix 14.11 (JMGO): it counted
     * airings of the next 24 h, and the view's header (Komedia • 394) disagreed with the tree (123).
     */
    fun programmeTree(channels: List<LiveTvChannel>, nowMs: Long = LiveTvClock.nowEpochMs()): List<Node> {
        val titles = HashMap<String, HashSet<String>>()
        val seen = HashSet<String>()
        channels.forEach { ch ->
            if (!seen.add(ch.guideKey)) return@forEach
            LiveTvRepository.schedule(ch.guideKey).forEach { p ->
                if (p.startEpochMs > nowMs + 2 * 86_400_000L) return@forEach
                if (p.stopEpochMs <= nowMs && !LiveTvCatchupLinks.isPlayable(ch.catchup, p, nowMs)) return@forEach
                val pl = programmePlace(ch.guideKey, p) ?: return@forEach
                val id = p.title.trim().lowercase()
                titles.getOrPut(pl.branch) { HashSet() } += id
                titles.getOrPut(pl.branch + "/" + (pl.leaf ?: "")) { HashSet() } += id
            }
        }
        return buildTree(ForgeCategoryRules.PROGRAMMES, PROG_PREFIX, titles.mapValues { it.value.size })
    }

    /** Branch ids of the programme tree in the mockup's order, with names (for the rows of the programme view). */
    fun programmeBranches(): List<Pair<String, String>> = ForgeCategoryRules.PROGRAMMES.branches.map { it.id to it.name }
    fun programmeLeaves(branch: String): List<Pair<String, String>> =
        ForgeCategoryRules.PROGRAMMES.branches.firstOrNull { it.id == branch }?.leaves.orEmpty().map { it.id to it.name } + ("" to "Pozostałe")
}
