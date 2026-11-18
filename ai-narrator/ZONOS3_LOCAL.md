# Lokalny ZONOS3-Mini w NuvioTV

Stan: backend HTTP zintegrowany na gałęzi `feature/ai-narrator`; brak potwierdzonych odsłuchów na fizycznym Android TV.

## Co jest lokalne

Wagi i inferencja uruchamiane są na komputerze / serwerze w sieci domowej. NuvioTV łączy się z lokalnym `/v2/generate`, pobiera gotowy WAV i odtwarza go obok dźwięku filmu. Dla tego dostawcy **nie jest wykonywane żadne żądanie do Gemini, Hugging Face Spaces ani Zyphra Cloud**, także po błędzie. Samo otwarcie demo Hugging Face nie wystarcza do uruchomienia serwera; demo nie służy jako endpoint aplikacji.

Androidowy port samego silnika GGUF **nie został** dołączony do APK. Nie należy przedstawiać LAN-serwera jako inferencji bezpośrednio na Android TV.

## Uruchomienie serwera

Oficjalne źródła i wersje:
- https://github.com/Zyphra/ZONOS3-Mini
- https://github.com/Zyphra/ZONOS3-Mini/blob/main/servers/ggml/README.md
- https://huggingface.co/Zyphra/ZONOS3-Mini
- https://github.com/Zyphra/ZONOS3-Mini/blob/main/docs/api.md

Na komputerze Linux lub macOS z CMake, Rust i zgodnym kompilatorem:

```bash
git clone --recurse-submodules https://github.com/Zyphra/ZONOS3-Mini.git
cd ZONOS3-Mini/servers/ggml
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build -j
hf download Zyphra/ZONOS3-Mini gguf/backbone-ptq.gguf gguf/zac-keep2.gguf gguf/spk-encoder.gguf --local-dir .
./build/zonos3-server gguf/backbone-ptq.gguf --tts-default-voices-dir ../../assets/default_voices --port 1919
```

To polecenie stanowi przykład z dokumentacji Zyphra; sposób nasłuchiwania na interfejsie LAN zależy od aktualnych opcji uruchomieniowych serwera. **Zweryfikuj `./build/zonos3-server --help` i ogranicz nasłuch do adresu interfejsu sieci domowej**. Domyślny `127.0.0.1` jest dostępny tylko na komputerze serwera, nie na telewizorze. W przypadku dostępu przez LAN włącz `ZONOS3_ACCESS_TOKEN` i regułę zapory dopuszczającą wyłącznie telewizor. Zwykły HTTP przesyła napisy oraz ewentualny token w sieci bez szyfrowania; bezpieczniejszym rozwiązaniem jest lokalny reverse proxy HTTPS z certyfikatem zaufanym przez Android TV. Nie udostępniaj portu do Internetu.

Na komputerze sprawdź:

```bash
curl --fail --silent http://127.0.0.1:1919/v2/health
curl --fail --silent http://127.0.0.1:1919/v2/speakers
```

Drugi endpoint musi zawierać przynajmniej jeden głos. Gdy brak głosu, sprawdź `--tts-default-voices-dir` i zasoby domyślnych głosów z oficjalnego repozytorium.

## Ustawienia w aplikacji

W panelu **Lektor AI** przełącz silnik na **ZONOS3 (lokalnie)**, wpisz adres bazowy serwera w sieci domowej (z portem), opcjonalny identyfikator głosu oraz token serwera. Po zapisaniu włącz lektora. Pole głosu można pozostawić puste — adapter użyje pierwszego głosu z `GET /v2/speakers` i zapamięta go na czas działania aplikacji.

Backend wysyła wyłącznie tekst dialogu z `language=pl`, `stream=false`, `format=wav`, `text_normalization=true`. Żądania są ściśle ograniczone do adresów lokalnych, połączenie nie podąża za przekierowaniami. Lokalne tokeny przechowywane są w Android Keystore, nie w kodzie APK.

### Ograniczenia

- Obecny adapter wykorzystuje **WAV po zakończeniu syntezy**, nie ciągłe strumieniowanie PCM. Gdy synteza jest wolniejsza od napisów, lektor pominie spóźnioną kwestię.
- ZONOS3 pozwala na kontrolę barwy i emocji przez dodatkowy zestaw kierunków `steer.gguf`, ale zależy to od konfiguracji serwera; bieżący adapter **nie wysyła** specyficznych parametrów sterowania emocjami. Styl Gemini i lokalna synteza nie są równoważne.
- Weryfikacja odsłuchowa polskiej wymowy, synchronizacji i obsługi sprzętu audio nadal jest konieczna.
- Połączenie HTTP do hosta publicznego lub adres serwera z danymi logowania, ścieżką i parametrami jest odrzucane.
- Gdy `ZONOS3` nie odpowie, aplikacja nie wywoła Gemini jako zamiennika. Wybór źródła musi być świadomy.

## Kompatybilność upstream

Pliki `LocalZonosTts.kt`, `NarratorEngine.kt`, `NarratorOverlay.kt` i ustawienia należą do katalogu `ai-narrator/`. Zmiana nie wymaga edycji `PlayerScreen.kt`, `PlayerRuntimeController`, `app/build.gradle.kts`, mpv ani ExoPlayer. Główny adapter pozostaje jedynym miejscem styku.
