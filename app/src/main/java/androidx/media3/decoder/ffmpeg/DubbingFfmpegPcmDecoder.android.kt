package androidx.media3.decoder.ffmpeg

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import java.io.Closeable
import java.nio.ByteBuffer

/** Small package-local adapter around Media3's intentionally package-private decoder. */
@OptIn(UnstableApi::class)
internal class DubbingFfmpegPcmDecoder(format: Format) : Closeable {
    private val decoder = FfmpegAudioDecoder(
        format,
        BUFFER_COUNT,
        BUFFER_COUNT,
        format.maxInputSize.takeIf { it > 0 } ?: DEFAULT_INPUT_BYTES,
        0,
        null,
        C.ENCODING_PCM_16BIT,
    )

    fun tryQueue(data: ByteArray?, timeUs: Long): Boolean {
        val input: DecoderInputBuffer = decoder.dequeueInputBuffer() ?: return false
        if (data == null) {
            input.timeUs = timeUs
            input.addFlag(C.BUFFER_FLAG_END_OF_STREAM)
        } else {
            input.ensureSpaceForWrite(data.size)
            input.data?.put(data)
            input.timeUs = timeUs
            input.flip()
        }
        decoder.queueInputBuffer(input)
        return true
    }

    fun drain(consumer: (ByteBuffer, Long, Int, Int, Int) -> Unit): DrainState {
        val output = decoder.dequeueOutputBuffer() ?: return DrainState.NO_OUTPUT
        val endOfStream = output.isEndOfStream
        output.data?.takeIf { it.hasRemaining() }?.let { data ->
            consumer(
                data,
                output.timeUs,
                decoder.sampleRate,
                decoder.channelCount,
                decoder.encoding,
            )
        }
        output.release()
        return if (endOfStream) DrainState.END_OF_STREAM else DrainState.OUTPUT
    }

    override fun close() {
        decoder.release()
    }

    enum class DrainState { NO_OUTPUT, OUTPUT, END_OF_STREAM }

    companion object {
        private const val BUFFER_COUNT = 16
        private const val DEFAULT_INPUT_BYTES = 64 * 1024
    }
}
