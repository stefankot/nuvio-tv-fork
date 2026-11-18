# Prompt dla kolejnego modelu: niezależny audyt i testy AI Narrator

Pracujesz w prywatnym repozytorium GitHub **stefankot/nuvio-tv-fork**, gałąź **feature/ai-narrator**. Punkt odniesienia to **main**. **Nie scalaj, nie wypychaj zmian do main, nie publikuj APK ani release'u bez wyraźnej zgody użytkownika.** Priorytet: aktualizacje upstream NuvioTV/Reshaped/Echo mają nadal synchronizować się przy minimalnej obsłudze ręcznej.

## Co zrobiono

Dodano eksperymentalnego polskiego lektora na podstawie tekstowych napisów, jako moduł w `ai-narrator/`. Dwa wybieralne silniki:

1. **Gemini** — modele `gemini-3.8-flash-tts` i `gemini-3.8-flash-lite-tts`, klucz API szyfrowany Android Keystore, styl per wypowiedź, szacowany limit miesięcznych kosztów.
2. **ZONOS3-Mini lokalny** — NuvioTV łączy się z własnym **serwerem w LAN**, a nie uruchamia model w APK. Adapter `LocalZonosTts.kt` korzysta z `GET /v2/speakers` oraz `POST /v2/generate` (język `pl`, WAV); obsługuje identyfikator głosu, szyfrowany token, ograniczenie do adresów prywatnych. Nie korzysta z demo Hugging Face Spaces i **nie przełącza się samoczynnie na Gemini**.

Dodatkowo: `NarratorEngine.kt` planuje syntezę, prefetch i odtwarzanie przez osobny Android MediaPlayer; `NarratorCore.kt` zawiera typy, planowanie napisów i klucze cache; `NarratorPreferences.kt` zapisuje konfigurację; `NarratorOverlay.kt` jest adapterem/UI. Ograniczono ponowną konwersję list napisów i haszowanie URL, anulowane mają być nieaktualne zadania po seek/zmianie filmu, uporządkowano zwalnianie MediaPlayer i cache WAV.

Zmieniono tylko **+5 wierszy `PlayerScreen.kt`** i **+4 wiersze forkowego `app/reshaped.gradle`**; inne zmiany mieszczą się w `ai-narrator/` i dedykowanym workflow. Dokumentacja: `ai-narrator/README.md`, `ZONOS3_LOCAL.md`, `RAPORTY_I_UTRZYMANIE.md`. Kontrakty sprawdzone statycznie: 13/13, **ale to nie jest dowód kompilacji ani działania**. GitHub Actions `ai-narrator-compatibility.yml` kończył się błędem bez użytecznych logów kroków. Nie wykonano testów na urządzeniu.

## Twoje zadanie — nie ufaj wcześniejszym deklaracjom

1. **Przejrzyj pełny diff `main...feature/ai-narrator` oraz wszystkie pliki modułu.** Znajdź błędy Kotlin/Compose/Gradle, błędy API, wycieki, wyścigi coroutine i niezgodności z Nuvio (ExoPlayer/mpv, napisy, lifecycle). Szczególnie sprawdź dostępność pól `PlayerRuntimeController` i enum `InternalPlayerEngine`.
2. **Uruchom realną kompilację i testy**: `./gradlew :app:compileFullDebugKotlin :app:testFullDebugUnitTest --no-daemon` oraz `python3 ai-narrator/tools/check_contracts.py`. Zdiagnozuj błędy GitHub Actions. Popraw wykryte usterki, dodaj testy regresyjne. Nie raportuj „pass” bez logów.
3. **Zweryfikuj faktyczny protokół dostawców z bieżącą dokumentacją**: czy identyfikatory Gemini istnieją i czy `speechMetadata.style` / struktura `generateContent` są prawidłowe; czy odpowiedź Gemini jest WAV czy surowym PCM i czy potrzeba konwersji. Zweryfikuj rzeczywiste żądania/odpowiedzi ZONOS3-Mini (`/v2/speakers`, `/v2/generate`, głos, `format=wav`, token), a nie tylko nazwy pól.
4. **Testy funkcjonalne i negatywne**: osobno Gemini i LAN ZONOS3; brak klucza, zły token, serwer niedostępny, limit kosztów, przekroczony czas, błędne WAV, cache hit/miss/clear, zmiana silnika, napisy nieobsługiwane, pauza/buforowanie/seek/zmiana filmu, krótkie i nachodzące napisy, kończenie odtwarzania, wznowienie aplikacji. Sprawdź odporność na DNS rebinding/SSRF, prywatność i czy nie ma przypadkowego cloud fallback.
5. **Testy Android TV i wydajności**: fizyczny odbiornik z ExoPlayer oraz mpv; głośność i miks z oryginalnym audio, brak opóźnionej wypowiedzi po seek, CPU/RAM, liczba żądań i koszty, długie sesje i wielokrotny start/stop. Nie pomijaj ograniczeń passthrough/bitstream. Zmierz latencję od pojawienia się napisu do mowy.
6. **Kompatybilność z upstream**: przetestuj `ai-narrator/tools/merge_dry_run.sh <rzeczywisty-tag>` w izolowanym worktree. Potwierdź zgodność z istniejącym `.github/workflows/mirror-nuviotv-release.yml`, w szczególności polityką konfliktów `app/build.gradle.kts`. Nie zmieniaj historii gałęzi `main`.

## Raport końcowy

Dostarcz tabelę **funkcja → PASS/FAIL/BLOCKED → dowód/log → poprawka**, osobno kompilacja, testy automatyczne, oba backendy, Android TV, bezpieczeństwo i upstream. Przy każdej niezweryfikowanej funkcji napisz **BRAK DANYCH**, nie „działa”. Podaj konkretne poprawki i commity na `feature/ai-narrator`; podsumuj ryzyka krytyczne i warunki odbioru. Zakończ sekcją **„Założenia i luki”**.
