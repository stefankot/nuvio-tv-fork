# ZASADA STAŁA (19.11.2026, właściciel): ostry limit buildów

1. **Najwyżej 1 build równocześnie** (kompilacja, APK, testy Gradle — każde wywołanie `./gradlew`).
2. **Najwyżej 1 build w kolejce.**
3. **Przed każdym buildem wyzerować zadania w kolejce:** nowe zlecenie anuluje czekające i samo zajmuje jego miejsce.

## Jak (obowiązkowo, dla każdego wątku/agenta)
- Gradle uruchamiamy WYŁĄCZNIE przez `bash fork-tools/build.sh <nazwa-logu> <zadania...>`, w zakładce terminala aplikacji
  (skrypt sam dodaje `--console=rich` i zapisuje `/tmp/<nazwa-logu>.log`). Bezpośrednie `./gradlew` jest zakazane.
- Skrypt pilnuje zasad 1–3 blokadami w `.build-lock/` (poza gitem) i czeka także na Gradle uruchomiony poza nim.
- Kod wyjścia 75 = zlecenie anulowane w kolejce (zastąpione nowszym) — to nie błąd kodu.
- Nie zatrzymuj demona Gradle (`--stop`, `pkill`) ani cudzego buildu, jeśli nie Ty go uruchomiłeś.

## Dlaczego
19.11: dwa wątki uruchomiły kompilacje naraz w tym samym katalogu (wspólny plik logu, jeden zatrzymał demona drugiego,
build przerwany). Mac ma 8 GB RAM — dwa buildy naraz to swap i wielokrotnie dłuższy czas.
