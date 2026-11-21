package com.nuvio.tv.ui.screens.player

import androidx.media3.common.C
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.LoadingInfo
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.source.WrappingMediaSource
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import java.io.IOException

/** Maps source-B timestamps onto source A while keeping ExoPlayer's single playback clock. */
internal class DubbingOffsetMediaSource(
    mediaSource: MediaSource,
    private val offsetMsProvider: () -> Long,
) : WrappingMediaSource(mediaSource) {
    private fun offsetMs(): Long = DubbingTiming.clampOffset(offsetMsProvider())

    override fun createPeriod(
        id: MediaSource.MediaPeriodId,
        allocator: Allocator,
        startPositionUs: Long,
    ): MediaPeriod {
        val child = mediaSource.createPeriod(id, allocator, startPositionUs.toChildTime())
        return OffsetMediaPeriod(child, ::offsetMs)
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        mediaSource.releasePeriod((mediaPeriod as OffsetMediaPeriod).child)
    }

    private fun Long.toChildTime(): Long = when (this) {
        C.TIME_END_OF_SOURCE, C.TIME_UNSET -> this
        else -> DubbingTiming.sourcePositionUs(this, offsetMs())
    }
}

private class OffsetMediaPeriod(
    internal val child: MediaPeriod,
    private val offsetMsProvider: () -> Long,
) : MediaPeriod, MediaPeriod.Callback {
    private lateinit var callback: MediaPeriod.Callback
    private val streamWrappers = mutableMapOf<SampleStream, OffsetSampleStream>()

    override fun prepare(callback: MediaPeriod.Callback, positionUs: Long) {
        this.callback = callback
        child.prepare(this, positionUs.toChildTime())
    }

    override fun onPrepared(mediaPeriod: MediaPeriod) = callback.onPrepared(this)

    override fun onContinueLoadingRequested(source: MediaPeriod) =
        callback.onContinueLoadingRequested(this)

    @Throws(IOException::class)
    override fun maybeThrowPrepareError() = child.maybeThrowPrepareError()

    override fun getTrackGroups(): TrackGroupArray = child.trackGroups

    override fun selectTracks(
        selections: Array<out ExoTrackSelection?>,
        mayRetainStreamFlags: BooleanArray,
        streams: Array<out SampleStream?>,
        streamResetFlags: BooleanArray,
        positionUs: Long,
    ): Long {
        @Suppress("UNCHECKED_CAST")
        val mutableStreams = streams as Array<SampleStream?>
        for (index in mutableStreams.indices) {
            mutableStreams[index] = (mutableStreams[index] as? OffsetSampleStream)?.child
        }
        val selectedPositionUs = child.selectTracks(
            selections,
            mayRetainStreamFlags,
            mutableStreams,
            streamResetFlags,
            positionUs.toChildTime(),
        )
        for (index in mutableStreams.indices) {
            val childStream = mutableStreams[index]
            mutableStreams[index] = childStream?.let { stream ->
                streamWrappers.getOrPut(stream) { OffsetSampleStream(stream, offsetMsProvider) }
            }
        }
        return selectedPositionUs.toResolvedParentSeekTime(positionUs)
    }

    override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) =
        child.discardBuffer(positionUs.toChildTime(), toKeyframe)

    override fun readDiscontinuity(): Long = child.readDiscontinuity().toParentTime()

    override fun seekToUs(positionUs: Long): Long =
        child.seekToUs(positionUs.toChildTime()).toResolvedParentSeekTime(positionUs)

    override fun getAdjustedSeekPositionUs(positionUs: Long, seekParameters: SeekParameters): Long =
        child.getAdjustedSeekPositionUs(positionUs.toChildTime(), seekParameters)
            .toResolvedParentSeekTime(positionUs)

    override fun getBufferedPositionUs(): Long = child.bufferedPositionUs.toParentTime()

    override fun getNextLoadPositionUs(): Long = child.nextLoadPositionUs.toParentTime()

    override fun continueLoading(loadingInfo: LoadingInfo): Boolean = child.continueLoading(
        loadingInfo.buildUpon().setPlaybackPositionUs(loadingInfo.playbackPositionUs.toChildTime()).build()
    )

    override fun isLoading(): Boolean = child.isLoading

    override fun reevaluateBuffer(positionUs: Long) = child.reevaluateBuffer(positionUs.toChildTime())

    private fun Long.toChildTime(): Long = when (this) {
        C.TIME_END_OF_SOURCE, C.TIME_UNSET -> this
        else -> DubbingTiming.sourcePositionUs(this, offsetMsProvider())
    }

    private fun Long.toParentTime(): Long = when (this) {
        C.TIME_END_OF_SOURCE, C.TIME_UNSET -> this
        else -> DubbingTiming.presentationPositionUs(this, offsetMsProvider())
    }

    private fun Long.toResolvedParentSeekTime(requestedParentUs: Long): Long = when (this) {
        C.TIME_END_OF_SOURCE, C.TIME_UNSET -> this
        else -> DubbingTiming.resolvedSeekPositionUs(requestedParentUs, this, offsetMsProvider())
    }
}

private class OffsetSampleStream(
    internal val child: SampleStream,
    private val offsetMsProvider: () -> Long,
) : SampleStream {
    override fun isReady(): Boolean = child.isReady

    @Throws(IOException::class)
    override fun maybeThrowError() = child.maybeThrowError()

    override fun readData(holder: FormatHolder, buffer: DecoderInputBuffer, readFlags: Int): Int {
        val result = child.readData(holder, buffer, readFlags)
        if (result == C.RESULT_BUFFER_READ && !buffer.isEndOfStream) {
            buffer.timeUs = DubbingTiming.presentationPositionUs(buffer.timeUs, offsetMsProvider())
        }
        return result
    }

    override fun skipData(positionUs: Long): Int =
        child.skipData(DubbingTiming.sourcePositionUs(positionUs, offsetMsProvider()))
}
