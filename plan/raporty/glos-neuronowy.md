# B11-2: lokalny głos neuronowy dla lektora (17.11.2026)

**Wynik: częściowo pozytywny. Kod NIE dodany (licencja do rozstrzygnięcia).**

## Biblioteka
sherpa-onnx 1.13.8 (ta w forku) zawiera syntezę mowy: klasy `OfflineTts`, `OfflineTtsConfig` oraz modele VITS (Piper), Matcha, Kokoro, Kitten, Pocket, Supertonic (zweryfikowane na `classes.jar` z AAR; natywne biblioteki arm64 są w paczce).

## Polskie głosy (Piper, repo `rhasspy/piper-voices`, sherpa: konwersja `csukuangfj/vits-piper-pl_PL-*`)
- `pl_PL-gosia-medium`, `pl_PL-darkman-medium`, `pl_PL-mc_speech-medium` (22,05 kHz, 1 mówca).
- Zbiory danych: CC0 (darkman: NabuCasa voice-datasets; mc_speech: Kaggle; gosia: „Gosia 1.0”, CC0-1.0).
- Repozytorium głosów: MIT.
- **Niejasność:** karty modeli mówią, że głosy są dotrenowane (finetune) z angielskiego `lessac`, którego korpus ma warunki Blizzard Challenge (możliwe ograniczenie do użycia niekomercyjnego). Do rozstrzygnięcia przez właściciela przed wdrożeniem (aplikacja prywatna/niekomercyjna: prawdopodobnie bez przeszkód).
- Szacunek rozmiaru: modele medium ok. 60–75 MB; szybkość: Piper medium na CPU arm64 zwykle kilka razy szybciej niż czas rzeczywisty (NIE zmierzone tutaj).

## Dalszy krok (gdy właściciel zaakceptuje licencję)
Druga implementacja `Lektor` (`SherpaLektor`) obok `SystemLektor`: model pobierany na żądanie do `files/fork_lektor_model/`, `OfflineTtsVitsModelConfig(model, tokens, dataDir=espeak-ng-data)`; `generate(text, speed)` zwraca float PCM → konwersja do `Pcm`. Wymaga paczki `espeak-ng-data` (w archiwum modelu sherpa).
Źródła: https://huggingface.co/rhasspy/piper-voices/tree/main/pl/pl_PL , https://huggingface.co/rhasspy/piper-voices/discussions/94
