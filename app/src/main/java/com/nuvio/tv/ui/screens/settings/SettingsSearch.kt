package com.nuvio.tv.ui.screens.settings

import androidx.compose.ui.graphics.vector.ImageVector

internal enum class SettingsSearchRoute {
    None,
    Addons,
    LiveTv
}

internal data class SettingsSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: SettingsCategory,
    val icon: ImageVector? = null,
    val route: SettingsSearchRoute = SettingsSearchRoute.None,
    val playbackSection: PlaybackSection? = null,
    val keywords: String = ""
)

/**
 * What the settings search can find. Categories are taken from the rail that is actually
 * visible (essential mode, profile and build flags already applied); the extra rows name the
 * settings people usually look for, and carry the Playback section to open.
 */
internal fun settingsSearchIndex(
    sections: List<SettingsSectionSpec>,
    isEssentialMode: Boolean
): List<SettingsSearchResult> {
    val visible = sections.associateBy { it.category }
    val results = mutableListOf<SettingsSearchResult>()

    sections.forEach { section ->
        val route = when (section.category) {
            SettingsCategory.NUVIO_RESHAPED -> SettingsSearchRoute.LiveTv
            else -> SettingsSearchRoute.None
        }
        results += SettingsSearchResult(
            id = "category:${section.category.name}",
            title = section.title,
            subtitle = when (section.category) {
                SettingsCategory.NUVIO_RESHAPED -> "Live TV: playlists, guides and recordings"
                else -> "Settings page"
            },
            category = section.category,
            icon = section.icon,
            route = route,
            keywords = categoryKeywords(section.category)
        )
    }

    if (SettingsCategory.PLAYBACK in visible && !isEssentialMode) {
        playbackEntries.forEach { entry ->
            results += SettingsSearchResult(
                id = "playback:${entry.section.name}",
                title = entry.title,
                subtitle = "Playback · ${entry.description}",
                category = SettingsCategory.PLAYBACK,
                icon = visible[SettingsCategory.PLAYBACK]?.icon,
                playbackSection = entry.section,
                keywords = entry.keywords
            )
        }
    }

    if (SettingsCategory.CONTENT_DISCOVERY in visible) {
        results += SettingsSearchResult(
            id = "content:addons",
            title = "Addons",
            subtitle = "Content & Discovery · Add-ons, catalogs and collections",
            category = SettingsCategory.CONTENT_DISCOVERY,
            icon = visible[SettingsCategory.CONTENT_DISCOVERY]?.icon,
            route = SettingsSearchRoute.Addons,
            keywords = "addon addons extension catalog collection"
        )
    }

    if (SettingsCategory.INTEGRATION in visible) {
        results += integrationEntries
    }
    if (SettingsCategory.TRACKING in visible) {
        results += trackingEntries
    }
    if (SettingsCategory.NUVIO_RESHAPED in visible) {
        results += liveTvEntries
    }
    if (SettingsCategory.ACCOUNT in visible) {
        results += accountEntries
    }
    if (SettingsCategory.PROFILES in visible) {
        results += profileEntries
    }
    if (SettingsCategory.ADVANCED in visible) {
        results += advancedEntries
    }

    return results
}

internal fun searchSettings(
    index: List<SettingsSearchResult>,
    query: String
): List<SettingsSearchResult> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return index
        .filter { result ->
            result.title.lowercase().contains(q) ||
                result.subtitle.lowercase().contains(q) ||
                result.keywords.lowercase().contains(q)
        }
        .sortedWith(
            compareByDescending<SettingsSearchResult> { it.title.lowercase().startsWith(q) }
                .thenByDescending { it.title.lowercase().contains(" $q") }
                .thenBy { it.title }
        )
        .take(30)
}

private data class PlaybackSearchEntry(
    val section: PlaybackSection,
    val title: String,
    val description: String,
    val keywords: String
)

private val playbackEntries = listOf(
    PlaybackSearchEntry(
        PlaybackSection.PLAYER, "Player",
        "Default player, internal engine and fallback", "player exo exoplayer mpv engine"
    ),
    PlaybackSearchEntry(
        PlaybackSection.STREAM_SELECTION, "Stream Selection",
        "How a stream is picked when you press play", "stream auto play source selection"
    ),
    PlaybackSearchEntry(
        PlaybackSection.UP_NEXT, "Up Next",
        "Next episode autoplay and post-play", "next episode autoplay"
    ),
    PlaybackSearchEntry(
        PlaybackSection.SKIP_SEGMENTS, "Skip Segments",
        "Skip intros, recaps and credits", "skip intro recap credits"
    ),
    PlaybackSearchEntry(
        PlaybackSection.PLAYER_INTERFACE, "Player Interface",
        "Overlays, clock, playback info and player buttons", "overlay osd clock controls buttons playback info resolution codec speed hide clean minimal"
    ),
    PlaybackSearchEntry(
        PlaybackSection.AUDIO, "Audio",
        "Languages, decoding and audio output", "audio sound passthrough downmix language"
    ),
    PlaybackSearchEntry(
        PlaybackSection.SUBTITLES, "Subtitles",
        "Language, style and render mode", "subtitle subtitles cc captions closed captions"
    ),
    PlaybackSearchEntry(
        PlaybackSection.VIDEO, "Video",
        "Frame rate matching, Dolby Vision and HDR", "video hdr dolby vision frame rate afr resolution"
    ),
    PlaybackSearchEntry(
        PlaybackSection.BUFFER_NETWORK, "Buffer & Network",
        "Memory and how streams are fetched", "buffer cache network memory"
    ),
    PlaybackSearchEntry(
        PlaybackSection.P2P, "P2P Streaming",
        "Peer-to-peer torrent streams", "p2p torrent peer"
    )
)

private val integrationEntries = listOf(
    SettingsSearchResult(
        "integration:debrid", "Debrid", "Integration · Real-Debrid, TorBox and other services",
        SettingsCategory.INTEGRATION, keywords = "debrid real debrid torbox premium"
    ),
    SettingsSearchResult(
        "integration:tmdb", "TMDB", "Integration · Metadata and artwork",
        SettingsCategory.INTEGRATION, keywords = "tmdb metadata posters artwork"
    ),
    SettingsSearchResult(
        "integration:mdblist", "MDBList", "Integration · Ratings and lists",
        SettingsCategory.INTEGRATION, keywords = "mdblist ratings list"
    ),
    SettingsSearchResult(
        "integration:animeskip", "AnimeSkip", "Integration · Skip anime intros and credits",
        SettingsCategory.INTEGRATION, keywords = "anime skip intro outro"
    )
)

private val trackingEntries = listOf(
    SettingsSearchResult(
        "tracking:trakt", "Trakt", "Tracking · Watch progress and scrobbling",
        SettingsCategory.TRACKING, keywords = "trakt scrobble progress"
    ),
    SettingsSearchResult(
        "tracking:simkl", "Simkl", "Tracking · Watch progress and lists",
        SettingsCategory.TRACKING, keywords = "simkl scrobble progress"
    ),
    SettingsSearchResult(
        "tracking:mdblist", "MDBList", "Tracking · Ratings and lists",
        SettingsCategory.TRACKING, keywords = "mdblist ratings list"
    )
)

private val liveTvEntries = listOf(
    SettingsSearchResult(
        "livetv:playlists", "Live TV Playlists", "Live TV · M3U and Xtream sources",
        SettingsCategory.NUVIO_RESHAPED, route = SettingsSearchRoute.LiveTv,
        keywords = "live tv playlist m3u xtream stalker provider"
    ),
    SettingsSearchResult(
        "livetv:guides", "TV Guide", "Live TV · XMLTV guide and EPG",
        SettingsCategory.NUVIO_RESHAPED, route = SettingsSearchRoute.LiveTv,
        keywords = "live tv guide epg xmltv"
    ),
    SettingsSearchResult(
        "livetv:sync", "Live TV Setup Sync", "Live TV · Keep the setup the same on every TV",
        SettingsCategory.NUVIO_RESHAPED, route = SettingsSearchRoute.LiveTv,
        keywords = "live tv sync setup backup nuvio account"
    ),
    SettingsSearchResult(
        "livetv:recordings", "Recordings", "Live TV · Scheduled and finished recordings",
        SettingsCategory.NUVIO_RESHAPED, route = SettingsSearchRoute.LiveTv,
        keywords = "live tv recordings dvr"
    )
)

private val accountEntries = listOf(
    SettingsSearchResult(
        "account:signin", "Nuvio Account", "Account · Sign in and sync status",
        SettingsCategory.ACCOUNT, keywords = "account sign in login qr nuvio sync"
    ),
    SettingsSearchResult(
        "account:profiles", "Profiles", "Account · Profiles, PIN and kids",
        SettingsCategory.PROFILES, keywords = "profile profiles pin kids avatar"
    )
)

private val profileEntries = listOf(
    SettingsSearchResult(
        "profiles:manage", "Manage Profiles", "Profiles · Add, edit and lock profiles",
        SettingsCategory.PROFILES, keywords = "profile profiles pin kids avatar manage"
    )
)

private val advancedEntries = listOf(
    SettingsSearchResult(
        "advanced:mode", "Switch to Advanced", "Advanced · Show every setting",
        SettingsCategory.ADVANCED, keywords = "advanced essential mode experience"
    )
)

private fun categoryKeywords(category: SettingsCategory): String = when (category) {
    SettingsCategory.ACCOUNT -> "account sign in login qr nuvio sync"
    SettingsCategory.PROFILES -> "profile profiles pin kids avatar"
    SettingsCategory.WATCH_PARTY -> "watch party together"
    SettingsCategory.NIGHT_MODE -> "night mode tryb nocny quiet"
    SettingsCategory.YOUTUBE -> "youtube onetube"
    SettingsCategory.FORGE -> "kicia forge fork moduly modules"
    SettingsCategory.APPEARANCE -> "appearance theme colors"
    SettingsCategory.LAYOUT -> "layout home posters rows"
    SettingsCategory.CONTENT_DISCOVERY -> "content discovery addons plugins media servers"
    SettingsCategory.PLAYBACK -> "playback player video audio subtitles buffer network"
    SettingsCategory.NUVIO_RESHAPED -> "live tv playlists m3u xtream guide epg youtube onetube remote buttons speech whisper autosync subtitles sync"
    SettingsCategory.INTEGRATION -> "integration debrid tmdb mdblist anime skip"
    SettingsCategory.TRACKING -> "tracking trakt simkl mdblist scrobble"
    SettingsCategory.ADVANCED -> "advanced developer logs"
    SettingsCategory.ABOUT -> "about version updates"
    SettingsCategory.DEBUG -> "debug logs"
    SettingsCategory.EXPERIENCE -> "experience essential advanced"
}
