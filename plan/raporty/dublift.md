# B11-3: analiza DubLift (17.11.2026)

Repo: github.com/joojoooo/DubLift (klon płytki w /tmp/dublift, poza repozytorium). Go nie jest zainstalowany w środowisku, więc testów jednostkowych NIE uruchomiono. Analiza = czytanie kodu (README, `internal/dublift/{vix,alignment,engine}.go`); to NIE jest test w działaniu.

## 1. Źródło dźwięku
Dźwięk pochodzi wyłącznie z VixSrc (włoski): `ResolveVix`/`ExtractVixPlaylist` w `vix.go`, wybór ścieżki `preferredVixAudio` (język `it`; brak metadanych = „Unknown · Vixsrc”). Obraz idzie z innych źródeł (Movy, dodatki użytkownika) bez przekodowania; wygenerowany dźwięk to AAC. Aby źródłem był gorszy plik z dodatków oznaczony jako polski, trzeba: (a) zastąpić `ResolveVix` resolverem, który zwraca ścieżkę dźwiękową wskazanego źródła z dodatków (URL + selektor ścieżki), (b) zmienić `vixLanguage`/`preferredVixAudio` na `pl`, (c) zmienić etykiety w UI. Reszta potoku (okno wideo, HLS, ticket) jest niezależna od języka. Wymaga pracy w Go, nie konfiguracji.

## 2. Dopasowanie (`alignment.go`)
Dopasowanie używa ANGIELSKIEJ ścieżki referencyjnej po obu stronach: PCM mono 11025 Hz z obu źródeł (`engine.go:855`), odcisk chromaprint + wynik widmowy (`fingerprint`, `spectral`, `MatchPCM`), kotwice w wielu miejscach filmu (`FindAnchor`, `Anchors`, `Boundaries`), z oceną pewności i flagą `DifferentEdit`. Ścieżka z lektorem (głos nałożony na ściszony oryginał) zadziała tylko tyle, ile oryginał jest słyszalny; sam dubbing bez ścieżki oryginalnej NIE ma odcisku wspólnego z angielskim. Wniosek: potrzebna jest po stronie polskiej ścieżka z oryginałem (lektor na oryginale) albo referencja w tym samym języku; dla dubbingu pełnego dopasowanie przez dźwięk jest niepewne (ryzyko niskiej pewności).

## 3. Różnica tempa (25 vs 23,976)
W kodzie nie znalazłem `atempo` ani przeliczania fps; `Alignment.At(t)` zwraca przesunięcie KROKOWE (stałe do kolejnej granicy `Boundaries`, bez interpolacji), więc dryf liniowy jest śledzony tylko skokami na granicach, nie korektą tempa. Dla PAL speed-up (25 fps, +4,1%) potrzebny byłby filtr `atempo=0.95904` (lub `asetrate`+`aresample`) w `engine.go` w potoku generującym AAC (`engine.go:936`), wybierany gdy wykryte kotwice pokazują stały współczynnik czasu ≠ 1 (regresja liniowa offsetu po czasie).

## 4. Dźwięk 5.1
Wyjście jest zawsze stereo: `-ac 2`, AAC LC 192 kbps (`engine.go:936`). 5.1 z VixSrc jest miksowany do stereo; brak przekazywania kanałów. (Zgodne z B09: lektor i tak wyłącza Dolby.)

## 5. Plan próby na 5 filmach
Potrzebne od właściciela 5 filmów, każdy w dwóch wydaniach: (1) dobre wideo z polskim lektorem/dubbingiem, (2) to samo wideo z oryginalną angielską ścieżką; w tym 2 z różnicą 25/23,976 fps, 1 z innym montażem (reżyserska/kinowa), 1 z 5.1. Dla każdego zmierzyć: pewność kotwic, maks. dryf po 60 minutach, czas przygotowania. Wpis w `plan/BLOKADY.md` z prośbą o pliki dodany.

## Rekomendacja
Nie integrować (DECYZJE #17). Jedyna część warta przeniesienia: algorytm kotwic + korekta tempa, ale wymaga Go/FFmpeg na serwerze — nie działa na telewizorze bez portu.
