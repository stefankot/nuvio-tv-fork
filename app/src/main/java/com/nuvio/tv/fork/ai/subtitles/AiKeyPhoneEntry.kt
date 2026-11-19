package com.nuvio.tv.fork.ai.subtitles

import androidx.compose.runtime.Composable
import com.nuvio.tv.reshaped.phoneentry.PhoneEntryPage
import com.nuvio.tv.ui.reshaped.phoneentry.PhoneEntryQr

/**
 * Klucz Gemini wpisywany na telefonie: kod QR otwiera stronę w sieci domowej (serwer działa tylko,
 * gdy ekran jest widoczny). Gotowy mechanizm Reshaped (jak klucz Seekr), bez zmian w jego kodzie.
 */
@Composable
internal fun AiKeyPhoneEntry(onKey: (String) -> Unit) {
    val page = PhoneEntryPage(
        title = "Klucz Gemini dla AI napisów",
        subtitle = "Wklej klucz z aistudio.google.com → Get API key. Trafi tylko do telewizora (zaszyfrowany).",
        fieldLabel = "Klucz API Gemini",
        send = "Wyślij do telewizora",
        sending = "Wysyłanie…",
        sent = "Wysłano. Telewizor zapisał klucz.",
        failed = "Nie udało się wysłać. Telefon musi być w tej samej sieci Wi-Fi co telewizor.",
        secret = true,
    )
    PhoneEntryQr(
        page = page,
        instruction = "Zeskanuj telefonem i wklej klucz Gemini (ta sama sieć Wi-Fi)",
        onValue = onKey,
    )
}
