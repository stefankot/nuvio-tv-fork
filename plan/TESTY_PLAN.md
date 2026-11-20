# Plan testów (20.11.2026)

Cel: najmniej pracy, najwięcej wykrytych błędów. Dwa poziomy, oba uruchamiane w JEDNYM buildzie paczki:

1. **Testy jednostkowe (JVM, `:app:testFullDebugUnitTest`)** — czysta logika każdego modułu; sekundy na test, bez urządzenia.
2. **Test dymny na emulatorze (`fork-tools/test/smoke.sh`)** — apka startuje w każdej skórce, przejście pilotem po każdej zakładce menu,
   wejście w pierwszą pozycję i Wstecz; wynik = brak crasha (bufor `crash`), fokus nie ginie, log `Fork*` bez wyjątków.
   Emulator nigdy razem z Gradle (8 GB RAM). JMGO tylko do oglądania przez właściciela, nie do testów.

Kolejność w paczce: commit poprawek → `build.sh <nazwa> :app:testFullDebugUnitTest :app:assembleFullDebug -PforkAbi=arm64-v8a`
→ emulator → `smoke.sh` → poprawki do następnej paczki.

## Pokrycie testami jednostkowymi (moduł → co sprawdzamy)
| Moduł | Testy | Stan |
|---|---|---|
| fork/tv (EPG, kanały, przewodnik, alerty, historia) | 22 pliki | było |
| fork/sources (ranking źródeł, język, napisy, presety) | 5 | było |
| fork/lektor (silnik, mikser, cięcie ciszy) | 3 | było |
| fork/voice (polecenia głosowe, czas) | 2 | było |
| fork/ai, discover, launcher, night, picks, registry, settings/nav | po 1–2 | było |
| skin/api: SkinId z klucza | NiuniaLogicTest | NOWE 20.11 |
| skin/niunia: etykiety rankingu, linia „gatunek · ocena”, plakat pionowy ze wzoru + zapas | NiuniaLogicTest | NOWE 20.11 |
| fork/buildage: „built-1h” (minuty/godziny/dni) | BuildAgeTest | NOWE 20.11 |
| fork/subs: MPL2 i MicroDVD → SRT (kursywa, nowa linia, brak końca, FPS) | TextSubtitleFormatsTest | NOWE 20.11 |
| fork/yt: id filmu z lokalnego MPD | YouTubeTextSourceTest | NOWE 20.11 |
| fork/echo, family, net, tvdb, skin/kicia (UI) | — | tylko test dymny (głównie Compose/sieć); dopisywać, gdy padnie błąd |

## Test dymny (emulator) — scenariusze
Dla skórek: niunia, kicia, nuvio (ustawiane `prefs.sh set sp:fork_skin skin <id>` + restart):
1. Start (`am start`), 20 s → proces żyje, brak `FATAL` w buforze crash.
2. Menu: każda zakładka po kolei (OK) → ekran się otwiera, proces żyje, fokus wraca do treści (▶ / ▼).
3. W treści: OK na pierwszej pozycji → szczegóły → Wstecz.
4. Zbiorczo: lista crashy + `W/E Fork*` z wyjątkami do `.stage/smoke/<data>.txt`.
