package com.nuvio.tv.core.stream

import com.nuvio.tv.domain.model.Stream

/** W1 (fork): kopie KE w tym pakiecie czytają `stream.streamData` bez importu; baza nie ma AIOStreams (zawsze null). */
internal val Stream.streamData: com.nuvio.tv.domain.model.AioStreamData? get() = null
