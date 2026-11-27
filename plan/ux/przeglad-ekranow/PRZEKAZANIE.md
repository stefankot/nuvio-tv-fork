# Przekazanie — przegląd ekranów (27.11, ~13:20)

Prompt dla nowego wątku: **„Otwórz `NuvioFork/plan/ux/przeglad-ekranow/PRZEKAZANIE.md` i wykonaj.”**

## Zadanie (bez zmian)
Katalog `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`, gałąź `skin-niunia`. Wykonuj `plan/ux/przeglad-ekranow/PROMPT.md` w pętlach: kod ≥ 10 min → build (w trakcie buildu kod następnej paczki) → ręczne testy na JMGO ≥ 10 min (nie krócej niż kod) → wyniki w NOTATKA i WYNIKI → commit + push. Nie pytaj, nie przerywaj. Lista: `NOTATKA.md` (czytaj sekcjami `## …`, grep + `sed -n`). Gotowe = każdy punkt NOTATKA ma `✅ TV <build>` / `❌ TV <build>: …` albo wpis w `plan/BLOKADY.md`. Po pętli: `WYNIKI.md` (1. zdanie: ile poprawek, ile sprawdzono na TV) → SendUserFile → commit + push. ❌ na początek następnej pętli, po 2 próbach → BLOKADY. Punkty „do decyzji właściciela”: wariant łatwiejszy do cofnięcia + zdanie w `plan/DECYZJE.md`.

## Stan
- Paczka 1 (03, 26, 04, 05 pkt 3, 22): commit 4617f541a (wypchnięty).
- Paczka 2 + reszta (skrypty `.stage/prz02/apply_*.py` — JUŻ ZASTOSOWANE, nie uruchamiaj ponownie): 15/16 (`IptvPrefixes`, pseudo-regiony), 14 (kolejka 3-stopniowa, kategorie wg kraju), 12 (`ChannelNames.normalCase`, pełny podpis), 20, 21 (numer po źródło|id), 01 (karta tytułu: 1 przycisk), 08 („O serialu” przewija), **Dublagem „Polski” z menu dźwięku** (`PolishDubbing.onAudioMenuLanguage`, `onSwitchTracks`; wpięcie w `PlayerRuntimeControllerPlaybackEvents.kt`), 17 (historia szukania Live TV + filmów, długie OK = usuń; zawsze siatka), 18 (rząd IPTV = tylko archiwum), 19 (logo na szerokość kafla), 23 pkt 1a (fokus na 1. film) i pkt 2 (Mix = 1. film), 24 (własna wyszukiwarka YouTube `NiuniaYouTubeSearch.kt`), 27 (bez szarej mgły od lewej, statyczne rozmycie z maską), 25 („Przeglądaj” = pigułki), „Gotowe” w rogu (`SourcesModule.rootOverlays` = pusto). Decyzje dopisane w `plan/DECYZJE.md` (27.11).
- **Build prz02 (paczki 1+2) był W TOKU przy przekazaniu** (13:20, zakładka terminala „build prz01”, log `/tmp/prz02.log`, kompilacja Kotlin bez błędów do 17. min). NIE uruchamiaj nowego buildu, dopóki ten trwa (`grep -aE 'BUILD|^e: ' /tmp/prz02.log`). Sukces → `cp app/build/outputs/apk/full/debug/app-full-armeabi-v7a-debug.apk .stage/apk/prz02-v7a.apk`, instalacja `fork-tools/test/jmgo_instaluj.sh .stage/apk/prz02-v7a.apk`. Błąd `e:` → popraw (kod paczki 2 jest w commicie „przegląd ekranów: paczka 2 …”), zbuduj ponownie. prz01 padł na pakowaniu (demon Gradle zatrzymany z zewnątrz) — sam kod paczki 1 skompilował się bez błędów.
- Gradle: `~/.gradle/gradle.properties` miało `-Xmx5g` → build 40 min; zmienione na 3072m (zgoda właściciela), kopia `.przed-27-11`.

## Najpierw: testy na JMGO (wpisz się do `.stage/jmgo_zajety.txt`, sprawdź, czy nikt nie ogląda; OK tylko z fokusem na celu — `fork-tools/test/fx2.sh`, klawisze `kf.sh`)
1. **„The Social Reckoning” (najważniejsze, właściciel: „sprawdzić dokładnie”)**: film z Popular/Szukaj, źródło 4K bez PL (Flix-Streams Usenet Vault), menu dźwięku → „Polski”. Oczekiwane: BEZ przełączania źródeł; logi `ForkDublagem`: „menu: Polish asked → donor search”, „donors with Polish audio: N”, „donor shortlist=… skipped={…}”, potem dołączona ścieżka „1:” i polski dźwięk na tym samym obrazie 4K. Gdy się nie uda: komunikat + przełączenie źródła tylko na plik z PL („source switch: new file has no pl track → next source”). Przyczyny odrzuceń dawców z logu → poprawka.
2. 26: ekran wczytywania — ◀▶ zmienia fokus „Inne źródło”/„Anuluj”, OK/Wstecz; linia parametrów pliku.
3. 04: Napisy — ▼ do końca długiej listy, „Inne języki”. 03: log `ForkDurationGuard` (np. „Ciało”). 22: archiwum — ◀▶ przewija, ▲▼ bez zmiany kanału, brak „Na żywo”.
4. Live TV: 14, 15, 16 (Hiszpański), 12, 20, 21, 17, 18 (log „iptv: rząd — …”), 19. Potem 01, 08, 27, 25, 23, 24, „Gotowe”.
Wyniki → znaczniki w NOTATKA (`✅ TV prz02` / `❌ TV prz02: …`), `WYNIKI.md` (jeszcze nie istnieje).

## Zostało (kolejne pętle)
23 pkt 1b (podgląd filmu z dźwiękiem w karcie), 23: pusta playlista = komunikat + fokus na karcie, Mix „Następne” z listy Mixu; 24 pkt 2 (puste kafle bez obrazków); 16 pkt 2 (duplikaty); 17 pkt 3 (okładki zamiast logo); 05 pkt 1-2, 06, 07, 02, 10, 11, 13; CAŁA APLIKACJA: szare gradienty → statyczne rozmycie; plakaty (Tint bez czarnego gradientu, rozmycia na dysku). Testy JVM `--tests 'com.nuvio.tv.fork.tv.*'` (ChannelNames po zmianie normalCase) — nie uruchomione.

## Pułapki
Test skryptów: kopiuj tylko edytowane pliki do scratchpada (nie cały `app/`). Nie edytuj `app/` w trakcie buildu. Build tylko `bash fork-tools/build.sh <log> :app:assembleFullDebug -PforkAbi=armeabi-v7a` w zakładce terminala; APK → `.stage/apk/<nazwa>-v7a.apk`, instalacja `fork-tools/test/jmgo_instaluj.sh <apk>`.
