# Lektor: głos Google Chirp 3 HD — zasady i działanie (19.11.2026)

## Decyzje właściciela (obowiązujące)
- **Głos lektora = Google Cloud Text-to-Speech, Chirp 3 HD, pl-PL.** Domyślny głos: **Enceladus** (wybrany przez właściciela z 30 próbek).
- Powód zmiany: Gemini TTS przez AI Studio ma na Tier 1 limit **100 zapytań/dzień i 10/min na model** — lektor milkł po ok. 15 min filmu.
  Chirp 3 HD: **200 zapytań/min, bez limitu dziennego**, **1 mln znaków/miesiąc za darmo** (~20 filmów po ~50 tys. znaków), potem 30 USD / 1 mln znaków (~1,50 USD za film).
- **Wysyłanie scenami jest obowiązkowe** (nie kwestia po kwestii) — tak jak przy Gemini.
- Wybór głosu w ustawieniach **z próbkami** (odsłuch przed wyborem).
- Klucz **wpisany na sztywno** (polecenie właściciela): `local.properties` → `BuildConfig.GOOGLE_TTS_API_KEY` (plik poza gitem; klucza nigdy nie commitować ani nie pisać w logach/czacie).
- Gemini i głos systemowy zostają jako silniki do wyboru (zapas).

## Konto i klucz
- Projekt Google: „Gemini Project” (`gen-lang-client-0857437598`, nr 953839486500), konto płatności „apka health track” (kredyt GCP ok. 227 zł — czy pokrywa Cloud TTS, niesprawdzone).
- W projekcie włączone **Cloud Text-to-Speech API**; klucz **„Nuvio lektor Chirp”** ograniczony tylko do tego API.
- Klucz z AI Studio (`GEMINI_API_KEY`, „Gemini API Key”) jest związany z kontem usługi i **Cloud TTS go nie przyjmuje** („API keys are not supported by this API”) — dlatego osobny klucz.
- Uwaga: obchodzenie limitów Gemini przez klucze z innych projektów (np. „Gemini API”) jest sprzeczne z warunkami Google — NIE robimy.

## Jak to działa (kod)
| Element | Plik | Co robi |
|---|---|---|
| Silnik głosu | `ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/ChirpLektor.kt` | implementuje `fork.lektor.Lektor` i `SceneLektor`; REST `texttospeech.googleapis.com/v1/text:synthesize`, nagłówek `x-goog-api-key`, LINEAR16 24 kHz, tempo bazowe 1,1 |
| Wybór silnika | `GeminiLektor.kt` → `LektorVoiceChoice` | `backend()`: chirp (domyślnie) / gemini / system; wybrany bez klucza → następny dostępny; log `głos: wybrany X niedostępny → Y` |
| Wybór głosu | `ai-narrator/.../ChirpVoicePicker.kt` | okno (AlertDialog, pilot): 30 głosów ♂/♀, zaznaczenie odtwarza próbkę z `app/src/main/assets/fork/lektor-chirp/<Głos>.m4a`, „Wybierz” zapisuje |
| Kontroler | `app/.../fork/lektor/LektorController.kt` (bez zmian logiki) | grupuje kwestie w sceny (`Scenes.group`: ≤8 kwestii, ≤600 znaków, przerwa <2,5 s), 2 sceny na obieg, wyprzedzenie 10 min, zapas pojedynczo |
| Hooki | `LektorVoice.usable`, `KeRanking` (lektorVoiceNow) | `cloudVoicePresent` = klucz Chirp ALBO Gemini |
| Klucz | `app/build.gradle.kts` (1 linia) | `buildConfigField GOOGLE_TTS_API_KEY` |

### Sceny (jedno zapytanie na scenę)
1. Kwestie sceny łączone w pole **`markup`** separatorem **` [pause long] `** (znaczniki pauzy działają tylko w `markup`, nie w `text`); nawiasy `[ ]` z napisów zamieniane na `( )`.
2. Limit zapytania 5000 bajtów — scena >4800 bajtów → `null` (kontroler czyta pojedynczo).
3. Nagranie cięte na kwestie przez `SilenceSplit` (okna 20 ms, próg 5% szczytu, pauza ≥200 ms, n−1 najdłuższych cisz).
   Test 19.11 (Enceladus, 5 kwestii): 4 pauzy 1,1–1,5 s, inne ciche miejsca ≤0,3 s → cięcie pewne.
4. Cięcie się nie zgadza → `null` → te kwestie pojedynczo (log `Chirp scena N kwestii (… ms): cięcie po ciszy się nie zgadza`).

### Tempo, pamięć, koszt, logi
- Min. **350 ms** między zapytaniami (≈170/min < limit 200/min; kontroler na starcie potrafi 7/s).
- Pamięć nagrań na dysku: `cacheDir/lektor-chirp` (klucz = głos + tempo + tekst) — ta sama kwestia/scena bez ponownego kosztu.
- Licznik znaków w miesiącu (`SharedPreferences fork_lektor_chirp`): log co ~50 tys. znaków, ostrzeżenie po przekroczeniu 1 mln (dalej płatne).
- Błędy: `ForkLektor` `Chirp TTS <głos>: <typ>: TTS HTTP <kod> (<status Google>)` + komunikat na ekranie „Błąd głosu Google Chirp: …”; bez tekstu kwestii i bez klucza.

## Ustawienia (Moduły → Lektor)
- „Lektor: silnik głosu (Chirp / Gemini / systemowy)” — kolejne naciśnięcia przełączają; domyślnie Chirp.
- „Lektor Chirp: wybierz głos (próbki)” — okno z próbkami.
- Ustawienia Gemini (barwa, model, limit miesięczny) zostają dla silnika Gemini.

## Próbki
- Wspólne zdanie: „Nie mamy czasu. Musimy stąd wyjść, zanim wrócą. Weź klucze i nie oglądaj się za siebie.”, tempo 1,1, AAC 64 kb/s (30 plików, 1,7 MB).
- Strona do odsłuchu na Macu: `plan/probki/glosy.html` (poza gitem). Nowe próbki: skrypt z wątku 19.11 (curl `text:synthesize` → `afconvert -d aac`).

## Stan (19.11 ~16:10)
- Na JMGO zainstalowany build „chirp” (md5 727b10b64d9ef3489bb19dccb2f66ea2). **Niesłyszany jeszcze przez właściciela na TV.**
- Do testu: lektor na filmie z PL napisami (logi `scena OK`, cięcie, `HTTP`), okno wyboru głosu pilotem (czy odtwarza próbki).
- Wniosek o Tier 2 Gemini: formularz wypełniony, niewysłany.
