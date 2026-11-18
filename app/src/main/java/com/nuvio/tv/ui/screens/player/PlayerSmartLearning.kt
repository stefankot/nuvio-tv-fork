package com.nuvio.tv.ui.screens.player

import android.util.Log
import androidx.media3.common.C
import com.nuvio.tv.core.player.recovery.BadReason
import com.nuvio.tv.core.player.recovery.SmartBadSources
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.streams.smart.SmartFamily
import com.nuvio.tv.core.streams.smart.SmartFamilyMemory
import com.nuvio.tv.core.streams.smart.SmartOutcomeStore
import com.nuvio.tv.core.streams.smart.SmartPolicy
import com.nuvio.tv.core.streams.smart.SmartSourceRanker
import com.nuvio.tv.core.streams.smart.SmartStreamRanking
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * forge M6 / M7, after the first frame of a file the automatic mode picked:
 * - N6: the try counts for its origin (with the time to the first frame); "Dlaczego ten plik" goes to logcat SmartPick
 *   and to the player's Informacje panel ([ForgeSmartWhy]).
 * - N12: 3 s later the declaration is checked against the picture (height, HDR). A "2160p" with 1080 lines or an "HDR"
 *   without HDR gets FAKE_QUALITY (24 h, last next time) and counts against its origin. The film plays on.
 * - N14: the file's family (height, audio tier, origin) is remembered for the next episode; a failure later forgets it.
 */
private const val TAG = "SmartPick"

internal object ForgeSmartWhy {
    /** The last pick's reason, shown in the player's Informacje panel. */
    @Volatile var line: String? = null
}

internal fun PlayerRuntimeController.forgeAfterFirstFrame(source: Stream, policy: SmartPolicy) {
    SmartOutcomeStore.init(context.filesDir)
    com.nuvio.tv.core.streams.smart.SmartLinkMeter.persist(context, com.nuvio.tv.core.streams.smart.SmartLinkMeter.networkKind(context))
    val candidate = SmartStreamRanking.candidate(source)
    val info = ReleaseParser.parse(candidate.text, candidate.sizeBytes)
    val ttff = forgeStartedAtMs.takeIf { it > 0 }?.let { android.os.SystemClock.elapsedRealtime() - it }
    SmartOutcomeStore.record(candidate.origin, ok = true, ttffMs = ttff)
    val audio = SmartSourceRanker.audioTier(info, policy.audioLanguage)
    SmartFamilyMemory.remember(contentId, SmartFamily(info.height, audio, candidate.origin, info.group))
    val sources = smartTrySession?.plan?.streams?.size ?: 0
    val why = buildString {
        append("Dlaczego ten plik: ")
        append(when (policy.preset) {
            com.nuvio.tv.core.streams.smart.SmartPreset.SAVER -> "Oszczędny"
            com.nuvio.tv.core.streams.smart.SmartPreset.BALANCED -> "Zbalansowany"
            com.nuvio.tv.core.streams.smart.SmartPreset.BEST -> "Najlepsza jakość"
        })
        append(" · limit ").append(policy.heightCap).append("p")
        policy.linkBps?.let { append(" · łącze ").append(it / 1_000_000).append(" Mb/s") }
        append(" · źródeł ").append(sources)
        ForgeSmartWhyReason.last?.let { append(" · ").append(it) }
    }
    ForgeSmartWhy.line = why
    Log.i(TAG, why + " origin=${candidate.origin} ttff=${ttff ?: -1}")
    // N12: the picture against the declaration, once the format is known
    scope.launch {
        delay(3_000)
        val format = _exoPlayer?.videoFormat ?: return@launch
        val declared = info.height
        val actual = format.height.takeIf { it > 0 }
        val fakeHeight = declared != null && actual != null && declared >= 2160 && actual < 1500
        val transfer = format.colorInfo?.colorTransfer
        val hdrShown = transfer == C.COLOR_TRANSFER_ST2084 || transfer == C.COLOR_TRANSFER_HLG
        val fakeHdr = info.hdr != null && transfer != null && !hdrShown && !isExperimentalDv7ToDv81ActiveForCurrentPlayback
        if (fakeHeight || fakeHdr) {
            Log.i(TAG, "fake-quality declared=${declared}p hdr=${info.hdr} actual=${actual}p transfer=$transfer")
            SmartBadSources.mark(source, BadReason.FAKE_QUALITY)
            SmartFamilyMemory.forget(contentId)
        }
    }
}

/** The reason of the last automatic change (M4), for "Dlaczego ten plik". */
internal object ForgeSmartWhyReason {
    @Volatile var last: String? = null
}
