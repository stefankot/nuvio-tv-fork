# AI napisy — stan prac (przerwane na prośbę właściciela, 18.11.2026)

Gałąź: scalone do `hybryda-zrodel` 19.11 (merge 82d132d4f; wcześniej `ai-subtitles-integration`). **Kompiluje się, testy JUnit 31/31 (build96). Nieuruchomione na emulatorze/TV, bez prawdziwego klucza Gemini.**

## Decyzje właściciela (18.11)
- Dostawca: **Google Gemini**, domyślny model `gemini-3.8-flash` (do wyboru: 3.5-flash-lite, 4-hydrogen, 4-argon). Ceny sprawdzone 18.11 na aistudio.google.com/docs/pricing (3.8-flash: $0.75/$3.75 za 1M do 31.12.2026, potem $1.50/$7.50).
- Limity domyślne: **$1 na film/odcinek, $3 dziennie** (zmiana w ustawieniach).
- Zakres tej paczki: **etapy 03+04** (działający przekrój tekstowy). Audio 60 s, tryb ekstremalny, OpenSubtitles: następne paczki.
- API: **Interactions API** (`POST /v1beta/interactions`, `x-goog-api-key`, `store:false`); `generateContent` Google oznacza jako legacy.

## Decyzje właściciela (19.11)
- Dźwięk do AI: **180 s na każdą godzinę filmu** (proporcjonalnie, min. 60 s; fragment ≤20 s) — `OrdinaryAudioBudget.limitForRuntime`, `reserve(..., limitMs)`.
- OpenSubtitles jako źródło: **bez potwierdzenia, niewidoczne dla użytkownika** (pobieranie w tle, gdy brak dobrego oryginału).
- Wysyłanie gotowych polskich napisów AI **DO OpenSubtitles: MUSI BYĆ** (właściciel 19.11, sprostowanie). Te same napisy mają trafiać do innych członków rodziny jako **preferowane** (pierwsze na liście / wybierane automatycznie). DECYZJE 19.11: wysyłanie **automatyczne** po przetłumaczeniu całego filmu (bez pytania); preferowanie dla rodziny **przez panel Rodzina** (lista przetłumaczonych filmów + plik napisów, telewizory rodziny wybierają je pierwsze, także przed publikacją w OpenSubtitles). Do ustalenia: konto OpenSubtitles (wysyłanie wymaga zalogowanego konta + klucza API), oznaczenie „tłumaczenie maszynowe” (wymóg OpenSubtitles), czy wysyłać automatycznie po skończonym tłumaczeniu.
- Klucz Gemini: QR na telefonie + wbudowany klucz (local.properties) jako zapas; zgoda domyślnie włączona.

## Co napisano (nowe pliki, `app/src/main/java/com/nuvio/tv/fork/ai/subtitles/`)
| Plik | Zawartość |
| --- | --- |
| `ModelProvider.kt` | interfejs dostawcy + `GeminiProvider` (OkHttp, schemat JSON, limit 4 MiB odpowiedzi, 429/Retry-After, `incomplete` = obcięta, anulowanie przerywa połączenie) |
| `SpendLedger.kt` | tabela cen z datami, rezerwacja najgorszego kosztu przed żądaniem, rozliczenie z usage (w tym cache), trwały rejestr, fail closed |
| `AiSubtitleDocument.kt` | dokument (kwestie, pomocnicze, bloki z rewizjami, słownik, pochodzenie, status), klucz przekładu (tylko tekst źródeł + model + wersja promptu), magazyn w `filesDir` (nie cache) |
| `TranslationPrompt.kt` | podział na bloki (≤120 kwestii / 5 min, cięcie na pauzie), priorytet od pozycji, prompty v1 (tłumacz, recenzent, naprawa), schematy JSON, ścisły parser, manifest eksportu |
| `TranslationEngine.kt` | blok: tłumaczenie → walidacja → 1 naprawa → recenzja → walidacja; ponowienia ≤2; ETA z pomiarów |
| `AiSubtitleSession.kt` | `PlayerPort`, wybór źródła i pomocniczych (do 3 na kategorię, weryfikacja pokrycia czasu ≥60%), kolejka 2 równoległe, publikacja blok po bloku (WATCH) albo całość (PREPARE_ALL), linia na ekranie nie zmienia się w trakcie, etykiety powiadomienia |
| `ExoPlayerPort.kt` | adapter ExoPlayer przez sidecar (`startSidecarAddonSubtitle` z tekstem w pamięci, potem `commitPreparedSidecarSubtitle` z kluczem `ai://…` i generacją; bez `setMediaSource`) |
| `AiSubtitles.kt` | ustawienia, klucz w Keystore (`ProtectedProfilePreferences`), start/stop/anuluj/oryginał/zapisane |
| `AiSubtitlesModule.kt` | moduł forka, ekran ustawień (klucz, zgoda, model, limity, lista zapisanych, eksport SRT/VTT/manifest przez SAF + zapas bez menedżera plików), powiadomienie w rogu (Forge), wiersze do okna „Więcej” |

## NASTĘPNE KROKI (dokładnie)
1. ZROBIONE 18.11 (sesja 2). Hooki (jednoliniowe, wpisać do `fork-tools/hooks.tsv`):
   - `ui/screens/player/PlayerViewModel.kt` w `bindExoSubtitleView`: `com.nuvio.tv.fork.ai.subtitles.AiSubtitles.bind(controller) // Fork hook: ai-subtitles`; w `onCleared` przed `controller.onCleared()`: `...AiSubtitles.unbind(controller) // Fork hook: ai-subtitles`.
   - `skin/kicia/player/ForgePlayerControls.kt`, okno „Więcej” po wierszu „Lektor”: `com.nuvio.tv.fork.ai.subtitles.AiSubtitlesMenuRows { moreOpen = false }` (UWAGA: inna sesja ma tu niezacommitowane zmiany — scalać ostrożnie).
   - `fork/registry/ForkModules.kt`: dopisać `com.nuvio.tv.fork.ai.subtitles.AiSubtitlesModule`.
2. ZROBIONE: Po scaleniu z pracą drugiej sesji: w `ExoPlayerPort.loadCues` przepuścić tekst przez `com.nuvio.tv.fork.subs.TextSubtitleFormats.toSrt` (MPL2/MicroDVD), jak w jej hooku w `startSidecarAddonSubtitle`.
3. ZROBIONE 18.11: Testy JUnit (`app/src/test/.../fork/ai/subtitles/`): BlockPlanner, ReplyParser, SpendLedger (limit, release, uszkodzony plik), ModelPricing (daty), GeminiProvider z MockWebServer (kształt żądania, 429, `incomplete`, duża odpowiedź), TranslationEngine z fałszywym dostawcą (naprawa, recenzja, limit kosztów), AiSubtitleSession z fałszywym PlayerPort (kolejność od pozycji, publikacja, USER_SWITCHED, wznowienie bez płacenia).
4. ZROBIONE (build96 OK): Kompilacja: `./gradlew :app:testFullDebugUnitTest --tests 'com.nuvio.tv.fork.ai.subtitles.*' -PforkAbi=arm64-v8a --console=rich | tee /tmp/buildN.log` w zakładce terminala, tylko gdy inna sesja nie buduje. Spodziewane poprawki: importy, `internal` członkowie kontrolera, `String.format(Locale,…)`.
5. Test emulatora / TV wymaga klucza Gemini od właściciela (wpisuje sam w Ustawienia → Moduły → AI napisy) i zgody; pierwsze płatne żądanie dopiero po tym.
6. Potem: scalenie do `hybryda-zrodel`, wpis do `plan/TERAZ.md`, następna paczka (audio 60 s, MPV, OpenSubtitles).


## WYNIKI 18.11 (sesja 2)
- Scalono `hybryda-zrodel` (b663e71cb) do gałęzi. Hooki dodane (commit 6ce047c56): `PlayerViewModel.bindExoSubtitleView` → `AiSubtitles.bind`, `onCleared` → `unbind` (wpisy w `fork-tools/hooks.tsv`), okno „Więcej” w `ForgePlayerControls` → `AiSubtitlesMenuRows`, `ForkModules` → `AiSubtitlesModule`. Źródła przez `TextSubtitleFormats.toSrt` (MPL2/MicroDVD).
- `bash fork-tools/check.sh` → OK (hand-maintained: 54); `scripts/ai-subtitles/check-ai-subtitles-hooks` → 0.
- build96 (worktree, `./gradlew :app:testFullDebugUnitTest --tests 'com.nuvio.tv.fork.ai.subtitles.*' -PforkAbi=arm64-v8a --console=rich | tee /tmp/build96.log`): BUILD SUCCESSFUL, 5 min 38 s. AiSubtitlePipelineTest 15/15, AiSubtitleAuditTest 7/7, AiSubtitleContractsTest 9/9; 0 błędów. Kompilacja kodu aplikacji przeszła (wymagana przez testy). APK nie budowano (zasada paczek).
- Testy JUnit potoku sprawdzają: podział bloków i priorytet od pozycji, parser (odrzuca złe JSON), ceny z datą i cache, limity kosztów (title/dzień, release, uszkodzony rejestr = blokada), kształt żądania Gemini (klucz tylko w nagłówku, `store=false`, kontekst przed zadaniem), 429/Retry-After, 400 bez opłaty, `incomplete`, naprawa + recenzja, limit kosztów, sesję (publikacja blok po bloku, pełny kontekst w każdym żądaniu, wznowienie bez płacenia, stop po wyborze innych napisów, PREPARE_ALL tylko całość), wybór pomocniczych (bez drugiego EN, PT tylko jako zachodni, jawny brak słowiańskiego), ETA tylko z pomiarów, klucz niezależny od przesunięcia czasu, manifest.
- Ostrzeżenia kompilatora (do sprzątnięcia, nie błędy): `ExoPlayerPort.kt:1` zbędny `@OptIn`, `ModelProvider.kt:111` zbędne `?.`, `TranslationEngine.kt:91` zbędne `!!`.
- NASTĘPNY KROK: punkt 4 sekcji „NAJPIERW: AI napisy” w `plan/TERAZ.md` (scalenie do `hybryda-zrodel`, potem test na emulatorze w dużej paczce, potem test z kluczem właściciela).

## Znane braki / ryzyka w napisanym kodzie
- Brak końcowego przeglądu spójności nazw po całym filmie (jest tylko słownik przekazywany kolejnym blokom).
- Czas: dokument trzyma czasy z chwili tłumaczenia (z AutoSync, jeśli źródło było wyświetlane); osobnego przekształcenia na oś filmu jeszcze nie ma.
- Kontekst: pełny oryginał + oba pomocnicze w każdym żądaniu (wspólny prefiks dla cache Google). Bez trafień cache film ~1500 kwestii może przekroczyć $1 → praca zatrzyma się z jawnym komunikatem.
- Kształt odpowiedzi Interactions API wzięty z dokumentacji, niezweryfikowany prawdziwym kluczem.
- MPV: jawny komunikat „jeszcze nieobsługiwany”.

## Wcześniejszy audyt rdzenia
Patrz `AUDIT_REPORT.md` (w głównym katalogu, ignorowany przez git) i commit 674dc311b.
