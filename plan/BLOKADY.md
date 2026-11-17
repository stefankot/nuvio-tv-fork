# BLOKADY

Zadania, których nie dało się dokończyć. Przeglądaj na początku każdego bloku.

Format wpisu:

```
## B03-4 (data)
Co próbowałem: ...
Czego brakuje: ...
Co może dostarczyć właściciel: ...
```

## B03-3/6/7/9 (część UI)
Co próbowałem: logika i testy JVM gotowe (ChannelFilters, MiniGuideLogic, DelayedPlayback, ForYouRow, ProgramPage, CellMark, PendingPlay). Rysowanie panelu grup, MiniGuide w LiveTvPlayerOverlay, automat „z opóźnieniem” w odtwarzaczu i wiersze modułów w HomeScreen wymagają zmian w dużych plikach Reshaped/rdzenia bez możliwości sprawdzenia wyglądu.
Czego brakuje: sprawdzenia na telewizorze/emulatorze (po zakończeniu kompilacji), decyzji o wyglądzie (skórka).
Co może dostarczyć właściciel: opinię po zobaczeniu rzędu „Dla Ciebie” i strony programu.


## B04-4 / B04-6 / B04-8 (część w odtwarzaczu i szczegółach)
Co próbowałem: logika i testy (AudioCheck, SourceLanguage.readyLine, ReadyPick, SubtitleQueue). Podpięcie sprawdzenia ścieżek do mechanizmu odzyskiwania źródeł, linia „Gotowe” w ekranie szczegółów i pozycja „Napisy nie pasują” w menu napisów wymagają zmian w dużych plikach odtwarzacza/szczegółów bez wglądu w wygląd.
Czego brakuje: weryfikacji na emulatorze i decyzji o wyglądzie linii.
Co może dostarczyć właściciel: opinię po teście na telewizorze.

## B06b-5 / B06b-2 (paczka wektorów i model)
Co próbowałem: skrypt `scripts/fork/build_vectors.py` z testem na atrapie API (zielony), workflow `fork-vectors.yml` (poprawny YAML), format paczki i matematyka z testami JVM.
Czego brakuje: sekretu `TMDB_API_KEY` w repozytorium (bez niego paczka nie powstaje; aplikacja działa na regułach) oraz uruchomienia modelu na urządzeniu (EmbeddingGemma/LiteRT) — wymaga pobrania modelu i sprawdzenia licencji Gemma (`plan/raporty/licencja-modelu.md`).
Co może dostarczyć właściciel: sekret TMDB w ustawieniach repozytorium; decyzję o redystrybucji modelu.

- B11-3 DubLift: potrzebne od właściciela 5 filmów w dwóch wydaniach (polski lektor/dubbing + oryginał angielski; w tym 2 z różnicą 25/23,976 fps, 1 z innym montażem, 1 z 5.1) do próby dopasowania. Raport: plan/raporty/dublift.md
