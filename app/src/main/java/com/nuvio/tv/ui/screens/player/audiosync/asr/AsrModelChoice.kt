package com.nuvio.tv.ui.screens.player.audiosync.asr

import android.content.Context

/**
 * Fork adapter: which speech model the audio subtitle sync uses. ENGLISH is Reshaped's zipformer;
 * the Whisper models (sherpa-onnx, int8) recognise about 100 languages. Files are fetched from the
 * sherpa-onnx author's Hugging Face repositories and checked against pinned SHA-256 sums.
 */
internal enum class AsrModelChoice(
    val id: String,
    val label: String,
    val sizeMb: Int,
    val downloadBase: String,
    val files: List<AsrModelFiles.Asset>,
    val whisperSize: String? = null,
) {
    ENGLISH("english", "Angielski (zipformer)", 74,
        "https://github.com/DavidVamaiotu/NuvioTV-Reshaped/releases/download/asr-models-ort-v1/", AsrModelFiles.optimized),
    WHISPER_TINY("whisper-tiny", "Whisper tiny (wiele języków)", 104,
        "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny/resolve/main/",
        listOf(
            AsrModelFiles.Asset("tiny-encoder.int8.onnx", 12937772L, "d24fb083ae3b1041fc24e97971d60e280c9342201fbb67b0ab428a8b4a51a434"),
            AsrModelFiles.Asset("tiny-decoder.int8.onnx", 89855401L, "d2fece8dd42771f1df975c6c0445770d0c292bf7547c2cae04a6c0cc57540925"),
            AsrModelFiles.Asset("tiny-tokens.txt", 816730L, "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126")
        ), "tiny"),
    WHISPER_BASE("whisper-base", "Whisper base (wiele języków)", 161,
        "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base/resolve/main/",
        listOf(
            AsrModelFiles.Asset("base-encoder.int8.onnx", 29120534L, "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11"),
            AsrModelFiles.Asset("base-decoder.int8.onnx", 130672026L, "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d"),
            AsrModelFiles.Asset("base-tokens.txt", 816730L, "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126")
        ), "base"),
    WHISPER_SMALL("whisper-small", "Whisper small (wiele języków, dokładniejszy)", 376,
        "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/main/",
        listOf(
            AsrModelFiles.Asset("small-encoder.int8.onnx", 112442483L, "4cbe7b22fa9026b843b60a68640c747de05bafb1a11b57edc0e66c232d9f33a9"),
            AsrModelFiles.Asset("small-decoder.int8.onnx", 262226114L, "acad50b5c782696e91b55914cc5ab4f756f1532f76e22aa6fc615f39fb69a8ee"),
            AsrModelFiles.Asset("small-tokens.txt", 816730L, "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126")
        ), "small");

    companion object {
        private const val PREFS = "asr_model_choice"
        private const val KEY = "model"

        fun current(context: Context?): AsrModelChoice {
            val id = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString(KEY, null)
            return entries.firstOrNull { it.id == id } ?: ENGLISH
        }

        fun set(context: Context, choice: AsrModelChoice) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, choice.id).apply()
            AsrModel.onChoiceChanged(context)
        }

        fun next(context: Context): AsrModelChoice = entries[(current(context).ordinal + 1) % entries.size]
    }
}
