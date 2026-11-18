package com.nuvio.tv.core.stream

import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.badgeMergeKey as keBadgeMergeKey

/** W1 (fork): kopie KE w tym pakiecie czytają `stream.streamData` bez importu; baza nie ma AIOStreams (zawsze null). */
internal val Stream.streamData: com.nuvio.tv.domain.model.AioStreamData? get() = null

/** W1 (fork): u KE `badgeMergeKey()` to metoda Stream; tu przekierowanie do shimu z domain.model (StreamListView.limitKey). */
internal fun Stream.badgeMergeKey(): String = keBadgeMergeKey()
