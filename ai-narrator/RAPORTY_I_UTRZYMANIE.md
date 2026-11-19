| **Łącznie** | **33** | **3429** | **0** || Nowe pliki modułu i workflow | 29 | 3407 | 0 |# Raporty lektora AI — 18.11.2026

**Stan:** prototyp integracyjny, nieodebrany produkcyjnie. Gałąź `feature/ai-narrator`, baza `main` = `01ea2ed1860616d758d575154ee08cbad7b43520`. Brak merge do `main` i brak publikacji aplikacji.

## 1. Upstream modification report

Porównanie gałęzi z bazowym `main` za pomocą GitHub Compare:

| Klasa | Pliki | Dodane wiersze | Usunięte |
|---|---:|---:|---:|
| Pliki upstream zmienione | 3 | 12 | 0 |
| Pliki forka zmienione | 1 | 10 | 0 |
| Nowe pliki modułu i workflow | 21 | 2437 | 0 |
| **Łącznie** | **23** | **2450** | **0** |

Jedyna zmiana istniejącej klasy odtwarzacza: `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt` — wywołanie `NarratorOverlay` (5 wierszy). Własny `app/reshaped.gradle` otrzymał deklaracje źródeł oraz konfigurację weryfikowanego APK ARM32 (+10 wierszy, bez dołączania binarki). Manifest Androida otrzymał +3 linie, a lista ścieżek FileProvider +4. **Bez zmian** w `app/build.gradle.kts`, `PlayerRuntimeController*`, mpv, ExoPlayer i obecnej synchronizacji.

## 2. Integration boundary report

- **Wejście:** `PlayerScreen.kt` → `NarratorOverlay.kt`.
- **Odczyty adaptera:** `PlayerRuntimeController.uiState`, `playbackTimeline`, `subtitleCueTracker.state`, opcjonalna `subtitleAutoSyncCues`, publiczna właściwość mpv `sub-text`.
- **Osobny model:** `NarratorFrame` i `NarratorLine` nie importują typów NuvioTV.
- **Wyjście:** oddzielny Android `MediaPlayer`. Brak zmian w dekoderze i ścieżce dźwiękowej filmu.
- **Konfiguracja:** `app/reshaped.gradle` wskazuje katalog `ai-narrator`; hasło API użytkownika chronione Android Keystore.

## 3. Upstream compatibility test

**Wykonano:** odczyt i porównanie struktury GitHub, weryfikację obecności 9 wymaganych kontraktów w 6 plikach integracyjnych — **9/9 zgodnych**. Potwierdzono także schemat `speechMetadata` i `prebuiltVoiceConfig.voiceName` w kodzie klienta.

**Aktualizacja 18.11.2026:** backend ZONOS3-Mini dodano w katalogu `ai-narrator` bez dalszej ingerencji w upstream. Początkowo używał LAN; tryb LAN został zastąpiony przygotowaniem WAV bezpośrednio na Android TV. Obejmuje pobieranie modelu na żądanie, zarządzanie RAM i trwałe audio. Brak skompilowanego silnika natywnego ARM64: BLOCKED. Szczegóły: [ZONOS3_LOCAL.md](ZONOS3_LOCAL.md). Stan: **nieprzetestowany na urządzeniu**.

**GitHub Actions:** przebieg [AI narrator compatibility #44382299088](https://github.com/stefankot/nuvio-tv-fork/actions/runs/44382299088) zakończył się `failure` w obu próbach; GitHub nie zwrócił kroków ani logów zadania. Przyczyna nieznana, **brak potwierdzonej kompilacji lub testów**. Nie jest to wynik pozytywny.

**Nie wykonano:** kompilacji Gradle, testów jednostkowych JVM na rzeczywistej gałęzi, testowego scalania oficjalnego tagu oraz testów ExoPlayer/mpv na urządzeniu. Środowisko sesji nie ma dostępu Git do prywatnego klonu, więc nie powstał lokalny worktree. **Liczba faktycznych konfliktów z nową aktualizacją: brak danych, nie 0.**

Przygotowano `ai-narrator/tools/check_contracts.py` i `merge_dry_run.sh` oraz osobny workflow `.github/workflows/ai-narrator-compatibility.yml`. Test scalania jest niekomitującym `git worktree add --detach`; po sprawdzeniu wykonuje `merge --abort` i usuwa worktree. Nie wypycha commitów ani nie uruchamia publikowania APK.

## 4. Merge-conflict risk report

| Ryzyko | Plik | Zalecenie |
|---|---|---|
| Wysokie | `PlayerScreen.kt` | Zachować tylko lokalny blok integracji; rozwiązywać konflikt ręcznie |
| Krytyczne przy edycji | `app/build.gradle.kts` | Nie wprowadzać zmian: istniejący AutoSync zastępuje plik wersją upstream |
| Średnie | `app/reshaped.gradle` | Kontrolować deklaracje katalogów po zmianie AGP |
| Średnie — zmiana API | adapter dla `SubtitleCueTracker`, mpv | Aktualizować tylko `NarratorOverlay.kt`, z testem kontraktu |

## 5. Maintenance guide

1. Uruchomić oddzielny lokalny worktree dla gałęzi funkcji; nie edytować żadnego współdzielonego katalogu ani `main`.
2. Pozostawić istniejący workflow AutoSync NuvioTV bez zmian. Nie nadpisywać procedur synchronizacji Reshaped ani Cherry-picków Echo.
3. Na gałęzi `feature/ai-narrator` uruchomić **AI narrator compatibility** dla dokładnego nowego tagu NuvioTV. Raport zawiera ścieżki konfliktów. Dla źródeł Echo/Reshaped najpierw określić rzeczywisty fork i rewizję.
4. Wymagać kompilacji `compileFullDebugKotlin`, testów `testFullDebugUnitTest`, kontroli kontraktów oraz ręcznej weryfikacji obydwu silników.
5. Przy konflikcie nie wykonywać automatycznie `ours`/`theirs` na `PlayerScreen.kt`. Przenieść pojedyncze wywołanie adaptera do nowego miejsca i uruchomić testy ponownie.
6. Dopiero po dodatnim raporcie kompatybilności, testach odsłuchowych, pomiarach opóźnień i kontroli kosztów rozważać ręczną integrację. **Nigdy nie włączać automatycznego merge do `main`.**

## Ograniczenia odbioru

- Brak niezawodnego, ogólnego strumienia napisów z przyszłymi czasami dla obydwu silników. Synteza na podstawie aktualnego napisu może się spóźniać; krótkie kwestie są pomijane.
- Miks systemowy nie gwarantuje współpracy z passthrough Dolby/DTS; tłumienie ścieżki oryginalnej nie jest wdrożone.
- Limit oparty na szacowanych tokenach nie jest twardym ograniczeniem faktury dostawcy.
- Brak wyników realnych odsłuchów, połączenia z API i kompilacji. **Wymóg pełnowartościowego lektora nie jest jeszcze spełniony.**

Referencje API: https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/text-to-speech/overview oraz https://cloud.google.com/model-garden/pricing .


### Aktualny przebieg po dodaniu ZONOS3
[AI narrator compatibility #44386680927](https://github.com/stefankot/nuvio-tv-fork/actions/runs/44386680927): `failure` bez zwróconych kroków/logów. Nie może być traktowany jako wynik kompilacji ani testu poprawności kodu.

### Dodatkowa granica integracyjna
`ZonosNativeServer.kt` uruchamia serwer natywny wyłącznie na 127.0.0.1 danego Android TV; `LocalZonosTts.kt` jest transportem do tego procesu, nie do komputera LAN. Nie wymaga modyfikacji `PlayerRuntimeController` ani silników wideo. Wybór dostawcy i cache są odizolowane wewnątrz modułu.

### Aktualizacja: urządzenia 2/3/4 GB i pre-rendering

Dodano pobieranie modeli GGUF (~1,12 GB) po kliknięciu, SHA-256, wznowienie, kontrolę miejsca, planowanie pierwszej lub drugiej połowy filmu, pasek postępu, zatrzymanie sesji po 15 minutach oraz trwały cache WAV do 2 GiB. Synteza ZONOS3 podczas filmu została wyłączona. Dla Xiaomi TV Box S 3rd Gen (2 GB), Onn 4K Pro (3 GB) i projektora (4 GB) wprowadzono konserwatywne progi wolnej RAM (1250/1500/1750 MiB przy starcie). **Nie przetestowano ich na żadnym urządzeniu.**

**Blokery odbioru:** brak skompilowanego i uruchomionego na Androidzie natywnego silnika ZONOS3 ARM64; brak potwierdzenia, że zwykłe napisy SRT/VTT są dostępne jako kompletna lista czasowa; brak benchmarku, czy połowa filmu powstaje w 15 minut; brak udanej kompilacji Gradle. Dodany eksperymentalny skrypt NDK nie jest dowodem działania. Wynik GitHub Actions nadal failure bez szczegółowych kroków: https://github.com/stefankot/nuvio-tv-fork/actions/runs/44395987767 .

Przygotowanie dotyczy syntezy polskich napisów, **nie ich tłumaczenia z innych języków**. Pliki zostają lokalnie po zamknięciu aplikacji, ale zachowanie w praktyce nie zostało zweryfikowane na Android TV.

### Aktualny wynik CI (18.11.2026)
[Uruchomienie #44396760322](https://github.com/stefankot/nuvio-tv-fork/actions/runs/44396760322): failure. Nie uzyskano prawidłowego raportu kompilacji ani wykonanych testów z tego workflow.

**Luka sprzętowa:** tryb przygotowania wymaga otwartego odtwarzacza z dostępnymi napisami. Samo wstrzymanie filmu może pozostawić w pamięci dekoder wideo. Na 2 GB RAM w dalszym ciągu może wystąpić blokada uruchomienia natywnego ZONOS3; optymalizacja „całkowicie przed inicjalizacją filmu” wymaga nowej ścieżki pozyskiwania pełnych napisów bez tworzenia odtwarzacza.

### Aktualizacja: 19.11.2026 — osobny ARM32 APK z GitHub

Usunięto wcześniejszą próbę dołączania natywnej binarki ARM64 do NuvioTV. W katalogu `ai-narrator/worker-arm32/` dodano osobny projekt Android `armeabi-v7a`, usługę dostępną wyłącznie dla aplikacji z tym samym podpisem, przekazywanie GGUF jako URI z prawem odczytu, walidację SHA i uruchamianie serwera wyłącznie na 127.0.0.1. W NuvioTV dodano `ZonosArm32Installer`, który automatycznie pobiera podpisany pakiet po aktywacji funkcji, gdy skonfigurowano publiczne GitHub Release z konta `stefankot`. Instalacja wymaga zgody Androida.

**Stan rzeczywisty:** nie powstał sprawdzony plik APK ARM32; `stefankot/nuvio-tv-fork` jest prywatne i release assets nie są publicznie dostępne. Wymagany jest podpisany testowany plik z osobnego publicznego repozytorium właściciela, ustalenie dokładnego SHA-256 i skonfigurowanie URL przed budową NuvioTV. Rewizję kodu natywnego przypięto, ale CMake/NDK i obsługa na ARM32 są nieprzetestowane.

Dodatkowy minimalny koszt integracji: oprócz PlayerScreen.kt (+5) pojawiły się wpisy w AndroidManifest.xml (+3) i file_paths.xml (+4) do widoczności pakietu, kontroli podpisu i bezpiecznego przekazania modeli. **Brak dowodu kompilacji i działania.** Liczby w pierwotnej tabeli u góry dokumentu są historyczne; aktualne należy ustalać z GitHub Compare.
