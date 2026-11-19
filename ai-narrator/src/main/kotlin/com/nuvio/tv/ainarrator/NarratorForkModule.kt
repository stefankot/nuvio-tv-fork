package com.nuvio.tv.ainarrator

import android.content.Context

/**
 * Scalenie 19.11: osobny moduł/okno „Lektor AI” usunięte — jest jeden lektor (`fork/lektor`, Moduły → Lektor),
 * a ten katalog daje mu głos Gemini ([GeminiLektor], [LektorVoiceChoice]).
 */

/** Klucz lektora; gdy brak własnego, wspólny klucz Gemini z AI napisów (QR / wbudowany). */
internal fun narratorGeminiKey(c: Context, prefs: NarratorPreferences): String =
    prefs.apiKey().ifBlank { com.nuvio.tv.fork.ai.subtitles.AiKeyStore.read(c).orEmpty() }
