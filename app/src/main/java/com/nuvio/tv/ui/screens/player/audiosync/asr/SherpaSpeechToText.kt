package com.nuvio.tv.ui.screens.player.audiosync.asr

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import java.io.File

/**
 * English speech recognition with sherpa-onnx (Apache-2.0) and an int8 Zipformer transducer
 * trained on GigaSpeech (podcasts and YouTube, close to film dialogue). Runs fully on-device.
 */
internal class SherpaSpeechToText(requestedDir: File, threads: Int) : SpeechToText, AutoCloseable {
    /** Fork 26.11: model, który zmieści się w pamięci 32-bitowego procesu (JMGO padał przy 4K: Whisper base, bad_alloc). */
    private val modelDir: File = com.nuvio.tv.fork.asr.AsrMemoryGuard.pick(requestedDir) // fork adapter
    /** Whisper (multilingual) when its files are in [modelDir], else Reshaped's English zipformer. */
    private val whisperSize: String? =
        listOf("tiny", "base", "small").firstOrNull { File(modelDir, "$it-encoder.int8.onnx").isFile } // fork adapter
    val modelFormat = if (whisperSize != null) AsrModelFiles.Format.ONNX else AsrModelFiles.format(modelDir)
    private val recognizer = OfflineRecognizer(
        config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = if (whisperSize != null) {
                OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = File(modelDir, "$whisperSize-encoder.int8.onnx").absolutePath,
                        decoder = File(modelDir, "$whisperSize-decoder.int8.onnx").absolutePath,
                        language = "", // empty: detected from the audio
                        task = "transcribe",
                        enableTokenTimestamps = true,
                    ),
                    tokens = File(modelDir, "$whisperSize-tokens.txt").absolutePath,
                    numThreads = threads,
                    modelType = "whisper",
                    provider = "cpu",
                )
            } else OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = File(modelDir, "encoder.int8.${modelFormat.extension}").absolutePath,
                    decoder = File(modelDir, "decoder.int8.${modelFormat.extension}").absolutePath,
                    joiner = File(modelDir, "joiner.int8.${modelFormat.extension}").absolutePath,
                ),
                tokens = File(modelDir, AsrModel.TOKENS).absolutePath,
                numThreads = threads,
                modelType = "transducer",
                provider = AsrModelFiles.provider(modelDir, modelFormat),
            ),
            decodingMethod = "greedy_search",
        ),
    )

    override fun transcribe(samples: FloatArray): List<Pair<Double, String>> {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            recognizer.decode(stream)
            val result = recognizer.getResult(stream)
            return mergeTokens(result.tokens, result.timestamps)
        } finally {
            stream.release()
        }
    }

    override fun close() = recognizer.release()

    companion object {
        private const val SAMPLE_RATE = 16_000

        /** BPE pieces starting with "▁" (or a space) begin a new word. */
        fun mergeTokens(tokens: Array<String>, timestamps: FloatArray): List<Pair<Double, String>> {
            val words = ArrayList<Pair<Double, String>>()
            val current = StringBuilder()
            var start = 0.0
            for (i in tokens.indices) {
                val token = tokens[i]
                val startsWord = token.startsWith('▁') || token.startsWith(' ') || current.isEmpty()
                if (startsWord) {
                    if (current.isNotEmpty()) words += start to current.toString()
                    current.setLength(0)
                    start = timestamps.getOrElse(i) { 0f }.toDouble()
                }
                current.append(token.trimStart('▁', ' '))
            }
            if (current.isNotEmpty()) words += start to current.toString()
            return words
        }
    }
}
