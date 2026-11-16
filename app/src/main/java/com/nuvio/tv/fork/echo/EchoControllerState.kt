package com.nuvio.tv.ui.screens.player

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Echo adapter: the Echo-only fields of PlayerRuntimeController, kept beside the controller (no edit of
 * PlayerRuntimeController.kt). Echo's extension functions read them through the extension properties below.
 * Names, types and initial values are Echo's (UP/echo PlayerRuntimeController.kt 412-413, 684-729).
 */
internal class EchoControllerState {
    var autoPlayHedgeJob: Job? = null
    var autoPlayHedgeUrl: String? = null
    var sourceFallbackVideoKey: String? = null
    val sourceFallbackTriedUrls: MutableSet<String> = mutableSetOf()
    var qualityOverlayJob: Job? = null
    var audioMenuJob: Job? = null
    var pendingAudioLanguageSwitch: PendingAudioLanguageSwitch? = null
    var audioSwitchCheck: AudioSwitchCheck? = null
    var stickyAudioLanguage: com.nuvio.tv.core.streams.AudioLanguage? = null
    var sourceSwitch: SourceSwitchSession? = null
    var sourceSwitchJob: Job? = null
    var sourceSwitchWatchJob: Job? = null
    var sourceSwitchGuardJob: Job? = null
    val _switchFrame: MutableStateFlow<android.graphics.Bitmap?> = MutableStateFlow<android.graphics.Bitmap?>(null)
    var switchFrameGrabber: (suspend (Long) -> android.graphics.Bitmap?)? = null
    val autoQualityPolicy: com.nuvio.tv.core.streams.AutoQualityPolicy = com.nuvio.tv.core.streams.AutoQualityPolicy()
    var autoQualityUserChoice: Boolean? = null
    var autoQualityJob: Job? = null
    var autoQualityPendingJob: Job? = null
    var autoQualitySeenStreamUrl: String? = null
    var autoQualityLastWasDowngrade: Boolean = false
    var autoQualitySourcesRequested: Boolean = false
    var autoQualityAvailableCache: AutoQualityAvailable? = null
    val silentSourceKeys: HashSet<String> = HashSet<String>()
    val hotSwapFailedSourceKeys: HashSet<String> = HashSet<String>()
    var lastBandwidthEstimateBps: Long = 0L
    var autoQualityHotSwap: AutoQualityHotSwap? = null
    var playerBandwidthMeter: androidx.media3.exoplayer.upstream.BandwidthMeter? = null
}

private val echoStates = java.util.WeakHashMap<PlayerRuntimeController, EchoControllerState>()

internal val PlayerRuntimeController.echo: EchoControllerState
    get() = synchronized(echoStates) { echoStates.getOrPut(this) { EchoControllerState() } }

internal var PlayerRuntimeController.autoPlayHedgeJob: Job?
    get() = echo.autoPlayHedgeJob
    set(v) { echo.autoPlayHedgeJob = v }
internal var PlayerRuntimeController.autoPlayHedgeUrl: String?
    get() = echo.autoPlayHedgeUrl
    set(v) { echo.autoPlayHedgeUrl = v }
internal var PlayerRuntimeController.sourceFallbackVideoKey: String?
    get() = echo.sourceFallbackVideoKey
    set(v) { echo.sourceFallbackVideoKey = v }
internal val PlayerRuntimeController.sourceFallbackTriedUrls: MutableSet<String>
    get() = echo.sourceFallbackTriedUrls
internal var PlayerRuntimeController.qualityOverlayJob: Job?
    get() = echo.qualityOverlayJob
    set(v) { echo.qualityOverlayJob = v }
internal var PlayerRuntimeController.audioMenuJob: Job?
    get() = echo.audioMenuJob
    set(v) { echo.audioMenuJob = v }
internal var PlayerRuntimeController.pendingAudioLanguageSwitch: PendingAudioLanguageSwitch?
    get() = echo.pendingAudioLanguageSwitch
    set(v) { echo.pendingAudioLanguageSwitch = v }
internal var PlayerRuntimeController.audioSwitchCheck: AudioSwitchCheck?
    get() = echo.audioSwitchCheck
    set(v) { echo.audioSwitchCheck = v }
internal var PlayerRuntimeController.stickyAudioLanguage: com.nuvio.tv.core.streams.AudioLanguage?
    get() = echo.stickyAudioLanguage
    set(v) { echo.stickyAudioLanguage = v }
internal var PlayerRuntimeController.sourceSwitch: SourceSwitchSession?
    get() = echo.sourceSwitch
    set(v) { echo.sourceSwitch = v }
internal var PlayerRuntimeController.sourceSwitchJob: Job?
    get() = echo.sourceSwitchJob
    set(v) { echo.sourceSwitchJob = v }
internal var PlayerRuntimeController.sourceSwitchWatchJob: Job?
    get() = echo.sourceSwitchWatchJob
    set(v) { echo.sourceSwitchWatchJob = v }
internal var PlayerRuntimeController.sourceSwitchGuardJob: Job?
    get() = echo.sourceSwitchGuardJob
    set(v) { echo.sourceSwitchGuardJob = v }
internal val PlayerRuntimeController._switchFrame: MutableStateFlow<android.graphics.Bitmap?>
    get() = echo._switchFrame
internal var PlayerRuntimeController.switchFrameGrabber: (suspend (Long) -> android.graphics.Bitmap?)?
    get() = echo.switchFrameGrabber
    set(v) { echo.switchFrameGrabber = v }
internal val PlayerRuntimeController.autoQualityPolicy: com.nuvio.tv.core.streams.AutoQualityPolicy
    get() = echo.autoQualityPolicy
internal var PlayerRuntimeController.autoQualityUserChoice: Boolean?
    get() = echo.autoQualityUserChoice
    set(v) { echo.autoQualityUserChoice = v }
internal var PlayerRuntimeController.autoQualityJob: Job?
    get() = echo.autoQualityJob
    set(v) { echo.autoQualityJob = v }
internal var PlayerRuntimeController.autoQualityPendingJob: Job?
    get() = echo.autoQualityPendingJob
    set(v) { echo.autoQualityPendingJob = v }
internal var PlayerRuntimeController.autoQualitySeenStreamUrl: String?
    get() = echo.autoQualitySeenStreamUrl
    set(v) { echo.autoQualitySeenStreamUrl = v }
internal var PlayerRuntimeController.autoQualityLastWasDowngrade: Boolean
    get() = echo.autoQualityLastWasDowngrade
    set(v) { echo.autoQualityLastWasDowngrade = v }
internal var PlayerRuntimeController.autoQualitySourcesRequested: Boolean
    get() = echo.autoQualitySourcesRequested
    set(v) { echo.autoQualitySourcesRequested = v }
internal var PlayerRuntimeController.autoQualityAvailableCache: AutoQualityAvailable?
    get() = echo.autoQualityAvailableCache
    set(v) { echo.autoQualityAvailableCache = v }
internal val PlayerRuntimeController.silentSourceKeys: HashSet<String>
    get() = echo.silentSourceKeys
internal val PlayerRuntimeController.hotSwapFailedSourceKeys: HashSet<String>
    get() = echo.hotSwapFailedSourceKeys
internal var PlayerRuntimeController.lastBandwidthEstimateBps: Long
    get() = echo.lastBandwidthEstimateBps
    set(v) { echo.lastBandwidthEstimateBps = v }
internal var PlayerRuntimeController.autoQualityHotSwap: AutoQualityHotSwap?
    get() = echo.autoQualityHotSwap
    set(v) { echo.autoQualityHotSwap = v }
internal var PlayerRuntimeController.playerBandwidthMeter: androidx.media3.exoplayer.upstream.BandwidthMeter?
    get() = echo.playerBandwidthMeter
    set(v) { echo.playerBandwidthMeter = v }
internal val PlayerRuntimeController.switchFrame: StateFlow<android.graphics.Bitmap?>
    get() = echo._switchFrame.asStateFlow()
