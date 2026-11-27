# Przekazanie — przegląd ekranów (27.11, ~16:50)

Prompt dla nowego wątku: **„Otwórz `NuvioFork/plan/ux/przeglad-ekranow/PRZEKAZANIE.md` i wykonaj.”**
Poprzednie przekazania: `.stage/prz10/PRZEKAZANIE-1605.md`, `.stage/prz09/PRZEKAZANIE-1455.md`.

## Zadanie (bez zmian)
Katalog `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`, gałąź `skin-niunia`. Wykonuj `plan/ux/przeglad-ekranow/PROMPT.md` w pętlach: kod → build (w trakcie buildu kod następnej paczki w `.stage/`, nie w `app/`) → ręczne testy na JMGO ≥ 10 min → znaczniki w `NOTATKA.md` (`✅ TV <build>` / `❌ TV <build>: …` na końcu nagłówka `## …`) i nowa sekcja pętli w `WYNIKI.md` (1. zdanie: ile poprawek, ile sprawdzono na TV) → SendUserFile → commit + push. Nie pytaj, nie przerywaj. ❌ wraca na początek następnej pętli, po 2 próbach → `plan/BLOKADY.md`.

## Build i instalacja
- `bash fork-tools/build.sh prz10 :app:assembleFullDebug :app:testFullDebugUnitTest --tests 'com.nuvio.tv.fork.tv.*' --continue -PforkAbi=armeabi-v7a "-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8"` w zakładce terminala (`run_in_terminal`); ~8,5 min. Monitor od startu: `tail -n0 -F /tmp/<log>.log | sed -l 's/\x1b\[[0-9;]*[A-Za-z]//g' | grep --line-buffered -aoE "e: file[^ ]*|[A-Za-z:]+ FAILED|What went wrong|Not enough memory|BUILD (SUCCESSFUL|FAILED)"` (UWAGA: `e:` bywa w środku linii za paskiem postępu — nie kotwicz `^e:`). Stan: `bash .stage/bst.sh <log>`. Błędy: `sed 's/\x1b\[[0-9;]*[A-Za-z]//g' /tmp/<log>.log | tr '\r' '\n' | grep -aoE "e: file[^\n]*" | sort -u`.
- APK → `.stage/apk/prz10-v7a.apk`; instalacja w tle: `bash fork-tools/test/jmgo_instaluj.sh .stage/apk/prz10-v7a.apk > .stage/prz10/inst.log 2>&1` (sprawdza, czy nic nie gra, md5, `compile -m speed`; ~5 min, aplikacji wtedy nie uruchamiaj).
- Logi testu: `bash .stage/prz10/lg9.sh [ile]` (od znacznika `.stage/prz10/mark` = czas JMGO `HH:MM:SS`; ustaw `adb shell "date '+%H:%M:%S'" > .stage/prz10/mark` przed testem). Klawisze z logiem fokusu: `fork-tools/test/kf.sh`; zrzut: `plan/ux/przeglad-ekranow/zrzut.sh NAZWA`. Bufor logcat 32 MB (`adb logcat -G 32M` po restarcie JMGO).

## Stan
- Na JMGO: **prz09** (paczki 9–12, md5 `b58332ee4ebaaed3a413b634abf86934`), sprawdzony — `WYNIKI.md` pętla 4. JMGO wolny (`.stage/jmgo_zajety.txt`).
- **W kodzie, NIEZBUDOWANE — paczka 13** (commit „paczka 13”, sprawdzone tylko `check.sh`, kompilacja niesprawdzona):
  1. `fork/images/AlphaMask.kt` (nowy): przejście ostry obraz → rozmycie wpieczone w kanał alfa małej rozmytej bitmapy (transformacja Coil po `NativeBlur`) zamiast warstwy pozaekranowej z maską `DstIn` na cały ekran. Użyte w: `NiuniaHeroPage.kt` (warstwa zamiast szarego scrimu + statyczne rozmycie z lewej w Szczegółach), `NiuniaOrganisms.kt` (strona odcinka, `NiuniaHeroKind.Immersive`), `nav/NiuniaNav.kt` (pod otwartym menu). Powód: pomiar prz09 — zmiana tytułu na hero Głównej p95 40 ms z rozmyciem, 30 ms bez (`prefs.sh set sp:fork_features niunia_blur_scrims false bool` + `prefs.sh restart`; po pomiarze włącz z powrotem).
  2. Szczegóły: rozmycie od dołu od 45 % (było 30 % — zmiękczało twarze w środku kadru).
- Do sprawdzenia po prz10: wygląd bez zmian względem prz09 (zrzuty `p9-*.png` w tym katalogu), log `ForkImages: maska rozmycia w obrazie`, pomiar A/B jak wyżej (cel: z rozmyciem ≈ bez), Szczegóły — dół rozmyty dopiero pod twarzami.

## Dalej (kolejka)
- **Szare panele** (CAŁA APLIKACJA): panel menu bocznego — `components/NiuniaOrganisms.kt` `NiuniaSidebarPanel` (`forkGlass(Role.Nav, fallback = NiuniaColor.Panel)`): gdy pod spodem jest rozmyta kopia tła (`NiuniaBackdropNow.current != null`), tło panelu półprzezroczyste ciemne zamiast szarego; ramka opisu zaznaczonego odcinka w Szczegółach (szare zaokrąglone pole pod kartą odcinka, zrzut `p9-odcinki.png`) — nie znalazłem jeszcze miejsca w kodzie (`detail/NiuniaDetail.kt` ok. 839–870 to karta bez tła; szukaj komponentu odcinka z opisem 5 linii i datą „11 lis 2026 · 58 min”).
- Niesprawdzone z prz09: ekran źródeł (`stream/NiuniaStreams.kt`, `NiuniaHeroKind.Blurred`; „Inne źródło” na ekranie wczytywania — film potrafi ruszyć w 1–2 s, naciśnij od razu), okno Niuni (`NiuniaDialog` — okno akcji z „Więcej podobnych” jest z bazy NuvioTV, nie Niuni), kafle archiwum IPTV pod odtwarzaczem (`NiuniaWideTile`, paczka 12; potrzebny kanał z archiwum i okładkami; ▼ w odtwarzaczu Live TV zmienia kanał).
- 23 pkt 2 „Następne” z Mixu → już w `plan/BLOKADY.md` (zmiana kopii Nuvio One + brak „następnego filmu” YouTube w odtwarzaczu). Nie ruszaj bez decyzji właściciela.
- **Właściciel (27.11, ~16:10): „AIOMetadata musi być źle skonfigurowane, sprawdzę to potem”.** Nie zmieniaj źródła okładek ani dopasowania (33 % trafień), nie wracaj do TMDB. 22 pkt 2 (puste karty archiwum) czeka na właściciela — nie do BLOKADY. Po jego zmianie: wyczyść `files/fork_tv_covers_aio2.json` i porównaj trafienia w logu `ForkTvCovers`.
- Dalej niesprawdzone (stare): 06 fokus po dołączeniu polskiej ścieżki, Mix (OK = 1. film), pusta playlista YouTube, stos serialu (10 pkt 1), „Kropka nad i” bez plakatu filmu, napisy wbudowane.

## Pułapki z testów
- OK na „Wznów”/„Odtwórz” uruchamia film na profilu właściciela od razu (wczytywanie trwa 1–2 s) — wyjdź Wstecz i wpisz zmianę w WYNIKI.
- Nakładka odtwarzacza chowa się po ~4 s: klawisze w jednym `adb shell "…"`. W odtwarzaczu Live TV ▼ = następny kanał.
- `logcat -d | grep ForkFocus` bywa pusty po chwili — `kf.sh` (logcat `-T`) albo `fx2.sh` (uiautomator).
- Live TV: ◀ z rzędu kart nie otwiera menu; Wstecz z pigułek = Główna; trzeci Wstecz na Głównej otwiera menu.
- Okna nad ekranem (Szukaj w YouTube, panele) — uiautomator nie widzi; fokus z logu `ForkFocus`.
- Podgląd YouTube w karcie gra z dźwiękiem — po teście wyjdź z YouTube i sprawdź ciszę: `adb shell dumpsys media.audio_flinger | grep -c '^  Standby: no'` = 0 (kilka sekund po zatrzymaniu).
