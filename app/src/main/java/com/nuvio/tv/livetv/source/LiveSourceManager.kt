package com.nuvio.tv.livetv.source

import android.util.Log
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * KE adapter (replaces KE's LiveSourceManager): only the channel pack. Downloads the list repo's
 * state.tsv + streams.tsv, keeps them in [PackStore], and stores scan results in [PackScanStore].
 */
@Singleton
class LiveSourceManager @Inject constructor(
    private val fetcher: SourceFetcher,
    private val pack: PackStore,
    private val scans: PackScanStore
) {
    private val mutex = Mutex()

    /** Downloads the pack and stores it; returns its channels. Throws IOException / SourceFetchException. */
    suspend fun refreshPack(): List<PackChannel> = mutex.withLock {
        val state = fetcher.fetch(ChannelPack.STATE_URL) { kind, body -> ChannelPack.readState(tsv(kind, body)) }
        val channels = fetcher.fetch(ChannelPack.STREAMS_URL) { kind, body -> ChannelPack.readStreams(tsv(kind, body), state) }
        pack.save(channels)
        val stored = scans.read()
        val keys = channels.flatMapTo(HashSet()) { ch -> ch.streams.map { ChannelPack.scanKey(it.url) } }
        val kept = stored.results.filterKeys { it in keys }
        if (kept.size != stored.results.size) scans.save(stored.copy(results = kept))
        channels
    }

    suspend fun packSummary(): PackSummary? = withTimeoutOrNull(10_000L) {
        try {
            fetcher.fetch(ChannelPack.SUMMARY_URL) { _, body -> ChannelPack.readSummary(body.readBytes().toString(Charsets.UTF_8)) }
        } catch (e: IOException) {
            Log.w("KePack", "pack summary: ${e.javaClass.simpleName}")
            null
        }
    }

    /** Stores a running or ended scan's results (see PackScanner). False when there is no pack. */
    suspend fun applyScan(results: Map<String, PackScanResult>, cleared: Set<String>, record: PackScanRecord): Boolean =
        mutex.withLock {
            try {
                val channels = pack.read()
                if (channels.isEmpty()) return@withLock false
                val keys = channels.flatMapTo(HashSet()) { ch -> ch.streams.map { ChannelPack.scanKey(it.url) } }
                val merged = (scans.read().results - cleared + results).filterKeys { it in keys }
                scans.save(PackScans(results = merged, last = record))
            } catch (e: IOException) {
                Log.w("KePack", "save scan: ${e.javaClass.simpleName}")
            }
            true
        }

    private fun tsv(kind: SourceKind?, body: InputStream): java.io.BufferedReader =
        if (kind != null) throw SourceFetchException(ChannelPack.FORMAT_CHANGED) else body.bufferedReader(Charsets.UTF_8)
}
