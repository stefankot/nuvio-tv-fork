package com.nuvio.tv.fork.voice

import org.junit.Assert.assertTrue
import org.junit.Test

/** Dokładność polecenia (po parserze reguł) na transkrypcjach Whisper small/base głosu syntetycznego (B11-1, 17.11.2026): wypowiedź wzorcowa → rozpoznany tekst. */
class WhisperTranscriptsTest {
    private val small = listOf(
        "pauza" to "pausa",
        "włącz napisy" to "Włączne pisy.",
        "wyłącz napisy" to "Wyłącz napisy.",
        "przewiń o minutę do tyłu" to "Przewin go minutę do tyłu.",
        "przewiń do przodu" to "Przewin do przodu.",
        "następny odcinek" to "Następny odcinek.",
        "poprzedni odcinek" to "poprzedni odcinek",
        "włącz TVP jeden" to "Włącz TVP1.",
        "włącz TVN dwadzieścia cztery" to "Włącz TVN24",
        "włącz Polsat Sport dwa" to "Włącz Polsak Sport 2.",
        "przełącz na następny kanał" to "Przyłącz na następny kanał.",
        "poprzedni kanał" to "poprzedni kanał.",
        "pokaż przewodnik" to "Pokaż przewodnik.",
        "co leci wieczorem" to "co leci wieczorem.",
        "włącz tryb nocny" to "Włącz tryp nocny.",
        "wyłącz tryb nocny" to "Wyłącz tryb nocny.",
        "zrób ciszej dialogi" to "Zrób ciszej dialogi.",
        "zrób głośniej dialogi" to "Zrób głośniej dialogi.",
        "włącz lektora" to "Włącz lektora.",
        "wyłącz lektora" to "Wyłącz lektora.",
        "wróć do ekranu głównego" to "Wróć do ekranu głównego.",
        "otwórz ustawienia" to "Otwórz ustawienia.",
        "szukaj Wiedźmin" to "Szukaj wiedźmin.",
        "włącz Pana Tadeusza" to "Włącz panę ta dusza.",
        "pokaż telewizję" to "Pokaż telewizję.",
        "dodaj do ulubionych" to "Dodaj do ulubionych.",
        "usuń z ulubionych" to "Usoń z ulubionych.",
        "pokaż ulubione kanały" to "Pokaż ulubiony kanały.",
        "zmień język dźwięku na angielski" to "Zmienię język dźwięku na angielski.",
        "napisy nie pasują" to "Napisuję niepasują.",
        "duże napisy" to "Duże napisy.",
        "mniejsze napisy" to "Mniejsze napisy.",
        "kontynuuj oglądanie" to "kontynuuję oglądanie.",
        "co teraz leci" to "Co teraz leci?",
        "co będzie po tym programie" to "Co będzie po tym programie?",
        "oglądaj od początku" to "Oglądaj od początku.",
        "wróć na żywo" to "Wróć na żywo.",
        "pomiń reklamy" to "pomimo reklamy.",
        "wstecz" to "Wstycz.",
        "zatrzymaj" to "Zatrzymaj",
        "odtwarzaj" to "Odtwarzaj.",
        "włącz coś lekkiego" to "Włącz coś lekkiego.",
        "pokaż perełki" to "Pokaż perełki.",
        "pokaż filmy dokumentalne" to "Pokaż filmy dokumentalne.",
        "pokaż sport" to "Pokaż sport",
        "szukaj Matrix" to "Szukaj Matrix.",
        "włącz Breaking Bad" to "Wątbracking bad.",
        "przypomnij mi o tym programie" to "przypomnij mi o tym programie.",
        "obserwuj ten serial" to "Okserwuj ten serial.",
        "przełącz profil" to "Przyłącz profil",
        "otwórz aplikacje" to "Otwórz aplikację.",
        "pomoc" to "pomóc.",
        "włącz kanał jeden" to "Włącz kanał jeden.",
        "włącz HBO" to "Włącz HBO.",
        "włącz Canal plus Premium" to "włącz kanal plus premium",
        "włącz MiniMini plus" to "Włącz mi ni mini plus.",
        "w dół" to "w doł.",
        "w górę" to "w górę.",
        "wybierz trzeci" to "Wybierz trzeci.",
        "odcinek piąty" to "odcinek 5",
        "zwiastun" to "Zwiastum.",
        "kto gra w tym filmie" to "To grę w tym filmie."
    )
    private val base = listOf(
        "pauza" to "Pausa.",
        "włącz napisy" to "Włącz napisy.",
        "wyłącz napisy" to "Wyłącz napisę.",
        "przewiń o minutę do tyłu" to "Przewin o miną te do tyłu.",
        "przewiń do przodu" to "Przewim do przodu.",
        "następny odcinek" to "następny odcinek.",
        "poprzedni odcinek" to "poprzedni odcinek.",
        "włącz TVP jeden" to "Włącz TVP1.",
        "włącz TVN dwadzieścia cztery" to "Włącz TVN24.",
        "włącz Polsat Sport dwa" to "Włącz polsach sporder 2.",
        "przełącz na następny kanał" to "Przyłącz na następny kanał.",
        "poprzedni kanał" to "poprzednik kanału.",
        "pokaż przewodnik" to "Pokaż przewodnik.",
        "co leci wieczorem" to "co leci wieczorem.",
        "włącz tryb nocny" to "Wącz trytnocny.",
        "wyłącz tryb nocny" to "Wyłącznie trytnocne.",
        "zrób ciszej dialogi" to "zrób ciszej dialogi.",
        "zrób głośniej dialogi" to "zrób gośnie i biologii.",
        "włącz lektora" to "Włącz lektora.",
        "wyłącz lektora" to "Wyłącz lektora.",
        "wróć do ekranu głównego" to "Wróć do ekranu głownego.",
        "otwórz ustawienia" to "otwórzostawienia.",
        "szukaj Wiedźmin" to "Szukaj wieć min",
        "włącz Pana Tadeusza" to "Włącz panę Tadeusza.",
        "pokaż telewizję" to "Pokaż telewizję.",
        "dodaj do ulubionych" to "Dodaj do ulubionych.",
        "usuń z ulubionych" to "Usuncu lubionych.",
        "pokaż ulubione kanały" to "Pukasz ulubiony kanały.",
        "zmień język dźwięku na angielski" to "Zmienię zygdźwienku na angielski.",
        "napisy nie pasują" to "Napisunie pasują.",
        "duże napisy" to "Dużo napisa.",
        "mniejsze napisy" to "Mniejszy napisa.",
        "kontynuuj oglądanie" to "kontynuuj oglądanie.",
        "co teraz leci" to "Suktera sletki.",
        "co będzie po tym programie" to "Co będzie po tym programie?",
        "oglądaj od początku" to "Oglądaj od początku.",
        "wróć na żywo" to "Wróć na żywo.",
        "pomiń reklamy" to "pomim reklamy.",
        "wstecz" to "wstycz",
        "zatrzymaj" to "zatrzymaj.",
        "odtwarzaj" to "odtwarzaj.",
        "włącz coś lekkiego" to "Włącz coś lekko jego.",
        "pokaż perełki" to "Pokaż parę oki.",
        "pokaż filmy dokumentalne" to "Pókaś filmy dokumentalne.",
        "pokaż sport" to "pokaż sport.",
        "szukaj Matrix" to "Szukaj Matrix.",
        "włącz Breaking Bad" to "wątbraking bat.",
        "przypomnij mi o tym programie" to "Przypomnij mi o tym programie.",
        "obserwuj ten serial" to "Okservuj ten serial.",
        "przełącz profil" to "Przełąc profil.",
        "otwórz aplikacje" to "odfusza aplikacja.",
        "pomoc" to "koniec",
        "włącz kanał jeden" to "Włączka na u jeden.",
        "włącz HBO" to "Włącz chabę o.",
        "włącz Canal plus Premium" to "Włącz kanał plus premium.",
        "włącz MiniMini plus" to "Włącz mi niemi nie plus.",
        "w dół" to "Wdol.",
        "w górę" to "Góra.",
        "wybierz trzeci" to "Wybierz trzeci.",
        "odcinek piąty" to "odcinek 5.",
        "zwiastun" to "z wiasą.",
        "kto gra w tym filmie" to "Kto gra w tym filmie?"
    )
    private val parser = RuleParser()

    private fun accuracy(rows: List<Pair<String, String>>): Double {
        val ok = rows.count { (said, heard) ->
            val want = parser.parseNow(said)?.id
            want != null && parser.parseNow(heard)?.id == want
        }
        val total = rows.count { parser.parseNow(it.first) != null }
        return ok.toDouble() / total
    }

    @Test fun smallTrafnoscPolecen() {
        val a = accuracy(small); assertTrue("small: " + a, a >= 0.70)
    }

    @Test fun baseTrafnoscPolecen() {
        val a = accuracy(base); assertTrue("base: " + a, a >= 0.50)
    }
}
