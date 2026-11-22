# Polski dźwięk z innego wydania w Nuvio

21 listopada 2026. Aplikacja zachowuje poprawkę automatycznej zmiany jakości po zmianie toru dźwięku. Opis tamtego przypadku jest w pliku `AUTOJAKOSC_PO_LEKTORZE.md`.

## Dobór i odtwarzanie

- Opcja „Polski dźwięk z innego wydania” jest domyślnie włączona w ustawieniach odtwarzacza. Automatyczny wybór obrazu przestaje obniżać pozycję wydania tylko dlatego, że ma słabsze lub niepolskie audio. Zachowuje ustawione filtry, ograniczenia urządzenia i kryteria gotowości źródła.
- Sonda na szczegółach zapisuje język, kolejność i dostępne parametry ścieżek, czas pliku, tempo obrazu i możliwość odczytu zakresów. Sprawdza do 18 źródeł. Opisy pomagają wskazać pliki do odczytu, a o języku i jakości ścieżki decydują odczytane metadane. Kolejność uwzględnia obsługę kodeka i toru wyjściowego, liczbę kanałów, klasę kodeka i dostępne wartości bitrate'u oraz próbkowania. Do automatu trafiają ścieżki głównego programu.
- Film może zacząć się z własnym dźwiękiem. Analiza rusza przy co najmniej 4 s obrazu i dźwięku w buforze. Obejmuje do trzech polskich ścieżek z tego samego tytułu lub odcinka. Czas wydań może się różnić o najwyżej 4 s. Znane oznaczenie montażu i tempo obrazu muszą być zgodne. Dublagem porównuje dźwięk z dwóch oddalonych od siebie, 15-sekundowych fragmentów. Połączenie następuje przy wysokiej pewności obu wyników, przesunięciu do 3 s i zgodności wyników do 120 ms.
- ExoPlayer pobiera obraz i napisy z pierwszego pliku, a dźwięk z drugiego. Wyliczone przesunięcie działa na wspólnym zegarze również podczas przewijania. Gdy zabraknie zgodnej ścieżki lub plik dźwięku przestaje działać, odtwarzacz może dalej użyć dźwięku z bieżącego obrazu. Przy powrocie zachowuje pozycję. Wybór ścieżki przez widza zatrzymuje automatyczne szukanie.
- Synchronizacja w tle dotyczy filmów i odcinków w plikach HTTP(S) odtwarzanych wewnętrznym ExoPlayerem. Drugi plik musi obsługiwać żądania zakresowe. Próba dwóch fragmentów chroni przed typowymi różnicami montażu i tempa. Może odrzucić poprawny dubbing, jeśli akustycznie nie ma dość wspólnego materiału do pomiaru.

## Kod z Dublagem i przyszłe aktualizacje

Punktem odniesienia jest commit `c683a2b6a84bf4af0b4b9b08d9faf1d7c0d33447`. Commit importu `dcaf5936d` przechowuje oryginalne wersje analizatora, demultipleksera, FFT, dekodera PCM, korekty zegara i dostępu do zakresów oraz testów. Ścieżki i pakiety odpowiadają oryginałowi. [Kod źródłowy Dublagem](https://github.com/SaladinoGW/NuvioTV-Dublagem/tree/c683a2b6a84bf4af0b4b9b08d9faf1d7c0d33447).

Lokalne zmiany w dwóch plikach analizatora są zapisane jako `fork-tools/patches/dublagem/01-background-analysis.patch`. Łatka `02-timing-tests.patch` ogranicza importowane testy do używanych funkcji zegara. Automatyczny wybór, integracja z ustawieniami i cykl życia sesji są w `fork/dublagem/`, `TrackScout.kt` oraz w wpięciach odnotowanych w `fork-tools/hooks.tsv`. Przy następnej aktualizacji można wymienić import według przypiętej wersji i ponownie nałożyć te łatki. Do kompozycji użyto Media3 `MergingMediaSource`. [Dokumentacja Media3](https://developer.android.com/reference/androidx/media3/exoplayer/source/MergingMediaSource).

## Sprawdzenie na fizycznym JMGO

| Próba | Wynik |
| --- | --- |
| 4K z dźwiękiem angielskim, dwa pliki z polskim dźwiękiem AAC stereo i AC3 5.1 | Wideo 3840 × 2160 wystartowało pierwsze. Użyto ścieżki polskiej AC3 5.1 z drugiego pliku. W tym samym pliku była też ścieżka angielska. Oba fragmenty dały przesunięcie +600 ms przy 620 ms dodanych podczas przygotowania klipów. |
| Przewinięcie w trakcie odtwarzania z polskim audio | Po przewinięciu do 45,088 s odtwarzacz wrócił do stanu odtwarzania i kontynuował od około 46 s. Ścieżka z drugiego pliku została zachowana. |
| Polski plik o podobnym czasie trwania i innym sygnale audio | Dublagem odrzuciło ścieżkę po niepewnym dopasowaniu. Odtwarzacz kontynuował z oryginalnym dźwiękiem i obrazem 3840 × 2160. |
| Utrata dostępu do pliku z polską ścieżką podczas przewijania | Po odpowiedzi HTTP 410 dla drugiego pliku odtwarzacz przywrócił oryginalny dźwięk na pozycji 82,000 s. Potwierdzono dalej obraz 3840 × 2160 i odtwarzanie przy 82,038 s. |

Próby wykonano na kontrolowanych klipach z ExoPlayerem. Autoodtwarzanie pozostało włączone. Wyłączono osobny mechanizm awaryjnej zmiany źródła, żeby sprawdzić, czy brak polskiej ścieżki w pierwszym pliku nie odrzuca poprawnego obrazu. Lektor AI był wyłączony.

Lokalny build ARM32 z commitu `21087fcbc` zakończył się w 11 min 46 s z wynikiem 98 testów zaliczonych i `fork-tools/check.sh` OK (65 plików). W końcowej kompilacji pominięto zadanie `lintVitalAnalyzeFullDebug`. APK wgrany na JMGO miał tę samą zawartość co lokalny plik, SHA-256 `80fdcdbb8be62ef045101f3cabb2b7dce6538283eda95802adb0541038baecc4`.

Kod w gałęzi `claude/paczka-oczywiste` w katalogu `NuvioFork/.claude/worktrees/paczka-oczywiste` pochodzi ze sprawdzonej wersji `21087fcbc` (scalenie `a5324c010`).
