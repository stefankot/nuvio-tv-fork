# Przekazanie — przegląd ekranów (27.11, ~14:55)

Prompt dla nowego wątku: **„Otwórz `NuvioFork/plan/ux/przeglad-ekranow/PRZEKAZANIE.md` i wykonaj.”**

## Zadanie (bez zmian)
Katalog `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`, gałąź `skin-niunia`. Wykonuj `plan/ux/przeglad-ekranow/PROMPT.md` w pętlach: kod → build (w trakcie buildu kod następnej paczki w `.stage/`) → ręczne testy na JMGO ≥ 10 min → znaczniki w `NOTATKA.md` (`✅ TV <build>` / `❌ TV <build>: …`, przy nagłówkach `## NN`) i nowa sekcja pętli w `WYNIKI.md` (1. zdanie: ile poprawek, ile sprawdzono na TV) → SendUserFile → commit + push. Nie pytaj, nie przerywaj. ❌ wraca na początek następnej pętli, po 2 próbach → `plan/BLOKADY.md`.

## Nowe zasady właściciela (27.11, zapisane w ZASADY-build.md / DECYZJE.md)
- **Build: stan co minutę.** Od startu: Monitor na logu (`tail -f /tmp/<log>.log | grep --line-buffered -E "^e: |FAILED|What went wrong|Not enough memory|BUILD"`) + co minutę `bash .stage/bst.sh <log>` (czas, %, zadanie, liczba `e:`). Pierwszy błąd = od razu poprawka i nowy build. To samo przy instalacji (co minutę stan).
- **Pamięć Gradle:** 3 GB to za mało („Not enough memory”). Zawsze:
  `bash fork-tools/build.sh prz0N :app:assembleFullDebug :app:testFullDebugUnitTest --tests 'com.nuvio.tv.fork.tv.*' --continue -PforkAbi=armeabi-v7a "-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8"` w zakładce terminala (run_in_terminal). Build ~10 min; przy 91% trwa kompilacja Kotlin (4–6 min), 94% = kompilacja przeszła, dex/pakowanie. `--continue`: APK powstaje mimo czerwonych testów.
- Instalacja: `fork-tools/test/jmgo_instaluj.sh` przerywa, gdy gra ekran główny JMGO (`com.jmgo.arwen`) — to NIE oglądanie; wtedy ręcznie: `adb install -r`, md5 z `pm path`, `cmd package compile -m speed -f com.nuviodebug.com` (w tle, ~7 min; w tym czasie nie uruchamiaj aplikacji).

## Stan na 14:55
- Wypchnięte commity: paczka 3 (`eadf909d7`), 4 (`d0e9978ab`), 5 (`d0ed39ea7`), 6 (`a378fabb7` + `496ba849b`), wyniki (`7f946e7fa`), zasady (`fb7d632ab`). `plan/DECYZJE.md` ma niezacommitowane 3 nowe wpisy właściciela (napisy wbudowane, „Odkrywaj” przy zegarze, okładki z AIOMetadata) — zacommituj.
- **Build prz06** (paczki 1–6) był na 94% bez błędów o 14:52; w tle zadanie: czeka na APK → `.stage/apk/prz06-v7a.apk` → instalacja na JMGO + md5 + compile. Sprawdź: `ls -la .stage/apk/prz06-v7a.apk`, `adb shell dumpsys package com.nuviodebug.com | grep lastUpdateTime` (JMGO ma czas CST = PL+7 h). Jeśli nie zainstalowane — zainstaluj ręcznie (jak wyżej).
- Na JMGO testowano prz04 (wyniki: `WYNIKI.md`, pętla 1) i częściowo prz05 (zainstalowany przed prz06).
- `.stage/jmgo_zajety.txt` — wpis tego wątku; nadpisz swoim.

## Najpierw (pętla 2)
1. **Nałóż `.stage/prz07/NiuniaLivePreview.kt`** (gotowy plik: powrót do podglądu 520×288 z gradientami + pasek tła na dolnej krawędzi = 11 pkt 1): `cp .stage/prz07/NiuniaLivePreview.kt app/src/main/java/com/nuvio/tv/skin/niunia/livetv/NiuniaLivePreview.kt` (tylko gdy żaden build nie trwa). 11 pkt 2 (większy podgląd) jest w BLOKADY (2 próby: gradient nie przykrywał TextureView przy polu 640×360).
2. **Nowe od właściciela (DECYZJE 27.11):**
   - napisy: domyślnie wbudowane, nie zewnętrzne (automatyczny wybór przy starcie + kolejność w zakładce Napisy: `skin/niunia/player/NiuniaPlayerChrome.kt` `NiuniaSubtitlesTab`; wybór automatyczny szukaj: `grep -rn "addonSubtitles\|PolishFirst" app/src/main/java/com/nuvio/tv/fork`);
   - Szukaj (główny): „Odkrywaj” obok zegara u góry (`skin/niunia/search/NiuniaSearch.kt`, grep `Odkrywaj`/`niunia_search_discover`);
   - okładki Live TV: `fork/tv/covers/TvCovers.kt` przepisać na dane wtyczki AIOMetadata (skonsolidowane z wielu serwisów) zamiast bezpośredniego TMDB `search/multi` — zachować pamięć RAM+dysk, limit 2 zapytań, regułę „film tylko dla programów ≥ 70 min”; jak Nuvio pyta dodatki o metadane: grep `AIOMetadata\|aiometadata\|addonMeta\|MetaRepository` w `app/src/main/java/com/nuvio/tv`.
3. Testy prz06 na JMGO (sprawdzić poprawki z prz05/prz06): 06 fokus w panelu Dźwięk po dołączeniu polskiej ścieżki (Dublagem dołącza sam po ~30 s od startu „The Social Reckoning” z „Oglądaj dalej”), 05 ✓ przy bieżącej jakości i źródle, 21 numery w filtrze „Hiszpański” (panel „Polska” na końcu rzędu pigułek → Język → Hiszpański; **potem przywróć „Polska”**), 22 pkt 2 okładki w rzędzie archiwum IPTV, 24 fokus na 1. wyniku po „Szukaj w YouTube”, 02 nagrody po lewej (film z nagrodami, np. tt8999762), plakaty i hero (rozmycia z dysku: log `ForkImages: rozmycie z dysku`), podpisy kafli Live TV na rozmyciu (CAŁA APLIKACJA krok 1, klucz `niunia_blur_captions`), „Kropka nad i” bez plakatu filmu. Niesprawdzone dotąd: 23 Mix, pusta playlista YouTube, OK na „Anuluj” przy wczytywaniu, 05 pkt 3, stos serialu (10 pkt 1).
4. Dalej z NOTATKI: 10 pkt 2 (kafle Live TV na `NiuniaMediaCard`), CAŁA APLIKACJA (kolejne szare gradienty → rozmycie), 23 pkt 1b (podgląd z dźwiękiem), Mix „Następne” z listy Mixu, 16/22 drobne.

## Pułapki z testów
- Nakładka odtwarzacza Niuni chowa się po ~4 s: sekwencje klawiszy w jednym poleceniu (▼ pokazuje, ▲ = „Dźwięk”, ▶▶ = „Napisy”, ▶▶▶ = „Więcej”); fokus w odtwarzaczu widać tylko na zrzucie (`adb shell screencap`), uiautomator go nie widzi. OK przy schowanej nakładce na pasku = pauza.
- IPTV na żywo: ▲▼ przy schowanej nakładce przełącza kanał; nakładkę z przyciskami otwiera OK (pauza), potem ▼ = rząd archiwum.
- Okna nad ekranem (Szukaj w YouTube, panele) uiautomator nie widzi — zrzut ekranu; fokus z logu `ForkFocus`.
- Pomocnicze: `.stage/prz03/lg.sh [--mark|wzorzec]` (logi Fork od znacznika, bez adresów), `.stage/bst.sh <log>` (stan buildu), `fork-tools/test/type.sh słowo` (klawiatura 6 kolumn; zaczyna od litery).
- Zmiany profilu właściciela dotąd: obejrzane fragmenty „The Social Reckoning”, „Odyseja”, kanały Live TV; filtr „Kraj i język” przywrócony na „Polska”.
