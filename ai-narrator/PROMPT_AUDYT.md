# Prompt dla kolejnego modelu — niezależny audyt AI Narrator

Repozytorium prywatne: **stefankot/nuvio-tv-fork**, gałąź **feature/ai-narrator** względem **main**. **Nie scalaj do main, nie nadpisuj jego historii, nie publikuj APK ani release.** Zachowaj automatyczne aktualizacje NuvioTV/Reshaped/Echo i minimalną ingerencję w upstream.

## Zakres zaimplementowany

Moduł ai-narrator ma dwa wybieralne backendy:

- **Gemini (chmura)**: Flash/Flash-Lite TTS, style wypowiedzi, klucz szyfrowany Android Keystore, szacowany limit kosztu, buforowanie i odtwarzanie przez oddzielny MediaPlayer. Brak potwierdzenia działania API i zwracanego formatu WAV/PCM.
- **ZONOS3-Mini na Android TV**: nie korzysta z komputera ani serwera LAN. Model GGUF (~1,12 GB) jest pobierany z Hugging Face **wyłącznie po kliknięciu**, z wznowieniem, kontrolą miejsca i SHA-256. Dwa warianty urządzeń TV box 2/3 GB RAM i projektor 4 GB wymagają kontroli niskiej pamięci. Natywny proces C++/ggml ma działać tylko podczas pauzy filmu, pod 127.0.0.1 z sesyjnym tokenem. **Brakuje zbudowanego i zweryfikowanego natywnego wykonywalnego pliku Android ARM64**: synteza lokalna aktualnie BLOCKED, nawet jeśli modele się pobiorą.

Dla ZONOS3 dodano przygotowanie **pierwszej lub drugiej połowy** filmu przed odtwarzaniem, postęp, 15-minutowy budżet na sesję, wznowienie po przerwaniu, trwałe pliki WAV w filesDir i osobną operację usuwania. W trakcie filmu nie wolno syntezować; odtwarzane są wyłącznie zapisane WAV. Program wymaga **pełnej osi czasowej napisów**, której obecny adapter może nie dostarczać dla zwykłego SRT/VTT. Nie ma automatycznego tłumaczenia obcych dialogów na polski.

Pliki: NarratorEngine.kt, NarratorOverlay.kt, NarratorCore.kt, NarratorPreparation.kt, ZonosOnDeviceModels.kt, ZonosMemoryGuard.kt, ZonosNativeServer.kt, LocalZonosTts.kt, GeminiNarratorTts.kt, NarratorPreferences.kt; testy i narzędzia w ai-narrator. Istniejący PlayerScreen.kt zmienia wyłącznie niewielki adapter; inne elementy odtwarzacza nietknięte. Konfiguracja źródeł i opcjonalnych JNI jest w forkowym app/reshaped.gradle.

## Co zwalidować — bez założeń, że kod działa

1. **Kompilacja Android**: ./gradlew :app:compileFullDebugKotlin :app:testFullDebugUnitTest --no-daemon; python3 ai-narrator/tools/check_contracts.py. Wyjaśnij dotychczasowe błędy GitHub Actions (brak logów kroków). Napraw problemy Kotlin/Compose/Gradle, nie udawaj PASS.
2. **Native ZONOS3**: zbuduj binarkę Android NDK arm64 ze źródeł Zyphra; sprawdź jej licencję, zależności, wykonanie ELF/PIE i uruchomienie z APK; zweryfikuj endpointy health/generate, token i format WAV. Bez realnego sukcesu raportuj BLOCKED.
3. **3 urządzenia**: Xiaomi TV Box S 3rd Gen (2 GB), Onn 4K Pro (3 GB), projektor (4 GB). Mierz rzeczywistą wolną RAM, RAM procesu potomnego, czas startu, OOM, przegrzewanie, throttling i czas syntezy 100 kwestii. Sprawdź czy połowa filmu rzeczywiście może powstać poniżej 15 minut; nie obiecuj tego bez benchmarku.
4. **Pobieranie i zapis**: kliknięcie jako jedyny trigger, poprawny pinned model i checksum, wznowienie Range, przekierowania CDN, brak miejsca, przerwanie, restart, quota 2 GiB, trwałość WAV po restarcie, cache hit i deduplikacja, kasowanie wymagające potwierdzenia.
5. **Napisy i seans**: pełne SRT/VTT i napisy osadzone, rozpoznanie niepełnej osi, podział na dwie połowy, pauza/wznowienie/seek, krótkie i zachodzące kwestie; brak aktywnej inferencji podczas ExoPlayer/mpv, brak LAN i cloud fallback, poprawne miksowanie, także ograniczenia passthrough.
6. **Gemini**: zweryfikuj model IDs, schema speechMetadata, WAV/PCM, obsługę błędów i realne koszty.
7. **Upstream**: przetestuj ai-narrator/tools/merge_dry_run.sh z rzeczywistym tagiem i zgodność z mirror-nuviotv-release.yml. Nie zmieniaj automatycznych polityk scalania.

**Raport**: funkcja → PASS/FAIL/BLOCKED → wynik/źródło/log → poprawka. Wymień commity na gałęzi funkcji, krytyczne ryzyka, warunki odbioru. Gdy nie ma wyniku: **BRAK DANYCH**. Zakończ **Założenia i luki**.
