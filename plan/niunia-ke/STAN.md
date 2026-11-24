# Niunia × Nuvio KE: stan pracy (23.11.2026, ok. 13:00)

## NAJPIERW (gotowy prompt na „kontynuuj”)

> **23.11, 14:30 — aktualne:** zbudowany ke4 (Z-15…Z-23, md5 3d2ddbea…, niezainstalowany). Prompt i pełny stan: `plan/niunia-ke/ODDANIE-ke4.md` — zacznij od niego; tekst niżej jest starszy (ke3).

> Kontynuuj wdrażanie skórki Niunia w forku Nuvio Android TV według przeglądu Nuvio KE. Worktree: `/Users/milajovovich/Desktop/nuvio 2/wt-niunia-ke` (gałąź `niunia-ke` od `skin-niunia` ab84055cf, zmiany NIEZACOMMITOWANE — commit tylko na prośbę właściciela). Zasady: `NuvioFork/ZASADY.md` i `nuvio-tv-interface/CLAUDE.md`. Przeczytaj ten plik do końca. Kolejno: (1) test `ke3` na JMGO ≥ 10 min (zainstalowany, md5 9477cd67…, test dopiero zaczęty), głównie nowy ekran źródeł; (2) build `ke4` z poprawkami po `ke2` (już w kodzie) + kolejne pozycje z listy „Do zrobienia”; (3) pętla: test ≥ 10 min → poprawki → build, raport każdej rundy w `plan/niunia-ke/testy/`.

## Zadanie właściciela (23.11)
1. Przejrzeć Nuvio KE (projektor JMGO i kod). Dla ekranów, które Niunia już ma: spisać pomysły na bardziej funkcjonalne rozwiązania (bez edycji ekranów Niuni), właściciel je zaakceptuje.
2. Dla ekranów KE, których Niunia nie ma: ocenić, czy mają zastosowanie w forku; jeśli tak, narysować odpowiedniki z komponentów design systemu Niuni (bez kopiowania 1:1).
3. Każde wdrożenie testować na JMGO ≥ 10 min na rundę; każdy kolejny build ma zawierać całą kolejkę `ZMIANY-DO-WDROZENIA.md` i nowe ekrany.
4. Ekrany z sekcji Ustawienia są POZA zakresem.

## Co jest zrobione
- KE zaktualizowane do **1.2.2** (19.11): kod `~/Desktop/nuvio 2/upstream/ke-1.2.2` (przypięty `upstream/ke` = 1.1.1 bez zmian), APK `com.nuvio.tv.ke` 1442 na JMGO (zgoda właściciela).
- **Pomysły do akceptacji:** `~/Desktop/nuvio-tv-interface/POMYSLY-Z-KE.md` — P-1…P-36 (Główna, strona tytułu, osoba, Szukaj, Biblioteka, Profile, Live TV, odtwarzacz) + ocena brakujących ekranów. Czeka na decyzję właściciela (nic z P-n nie wdrażać bez `[x]`).
- **19 nowych ekranów makiety** (`nuvio-tv-interface/rebuild/project/`): Streams, StreamsFilters, Discover, CatalogAll, Folder, Studio, LibraryShelves, LibrarySort, LibraryHistory, ProfilePin, TitleActions, EpisodeActions, PlayerNext, PlayerLoading, PlayerError, PlayerMore, PlayerSources, LiveTVOptions, PlayerLiveError. `sync.py`: lint 0; plansze w `canvas.json` (rzędy „Z Nuvio KE”); obrazy spoza makiety w `rebuild/ke-images.json`; generator `tools/ke-ekrany.py`. Wpis **Z-14** w `ZMIANY-DO-WDROZENIA.md`. Nic nie zacommitowane w `nuvio-tv-interface`.
- **Fork (worktree `wt-niunia-ke`)** — wdrożone w kodzie:
  - tokeny v2.1+ (`gen_tokens.py`): Haze*, *Flat, TrackHFocus, Rating*, PagerCardW, AwardsTextW, gradienty detail; Stack1/2 = 20 %;
  - Z-2 (wariant płaski Androida 11): przyciemnienie `scrim-detail-flat` + `scrim-detail-bottom` w hero strony tytułu, przyciski `haze` (`NiuniaButton(haze = true)`); log `effects: flat`. Rozmycie lewej części na API 31+ NIE zrobione (tylko `scrim-detail`);
  - Z-1: kafelek programu (bez kropek i ▶, mini pasek czerwony na żywo / biały obejrzane archiwum, „Tytuł · krótki kanał” 2 linie, `niuniaShortChannel`), komórka EPG (bez ▶, czerwona linia upływu, fokus białe tło);
  - Z-4: archiwum bez przełącznika kanałów, ▲▼ nie zmienia kanału, przyciski od x 40;
  - Z-5: tor paska 8 → 12 dp w fokusie; Z-6: przyciski Dźwięk → Lektor → Napisy → Więcej (bez „UHD”; jakość ręcznie w panelu Więcej — decyzja właściciela 22.11), każdy otwiera swoją zakładkę;
  - Z-7: warstwy stosu 0,7 / 0,4; Z-9: wiersz akcji hero wyrównany do góry; Z-10: `NiuniaRatingAverage` (średnia IMDb+MDBList, kolor OKLab) na Głównej, stronie tytułu, odcinku; wiek i „Oceny” w faktach sekcji Opis;
  - Z-13 / Z-13b: `NiuniaAwardsSummarySection` (kafelki Gale/Kategorie) i `NiuniaAwardsText` (3 linie w hero) — w kodzie, ale fork NIE MA źródła nagród (`NiuniaAwardsSource` = „brak źródła”, wybór źródła należy do właściciela), więc na TV niewidoczne;
  - Z-14: `NiuniaDialog` / `NiuniaActionsDialog`; okna akcji „Oglądaj dalej”, plakatu (wybór list Trakt zostaje oknem bazy) i odcinka; biblioteka z zakładkami, półkami, faktami i panelem „Sortuj i filtruj” (`library/NiuniaLibraryShelves.kt`); ekran źródeł `stream/NiuniaStreams.kt` (trasa skórki „streams”, wpięcie jedną dopisaną linią w `StreamScreen.kt` + wiersz `hooks.tsv`; `check.sh` OK, 63/65).
  - Teksty: `res/values/niunia_ke_strings.xml`.
- **Buildy** (wszystkie v7a, `bash fork-tools/build.sh keN :app:assembleFullDebug -PforkAbi=armeabi-v7a` w zakładce terminala; kopie w `wt-niunia-ke/.stage/apk/`): ke1 (runda 1, nie instalowany — JMGO zajęte), ke2 (rundy 1–2, przetestowany 13 min, `testy/ke2.md`), ke3 (+ źródła; ZAINSTALOWANY na JMGO, md5 9477cd67cf4fae1ea998aee46e8d2c5a, test nie wykonany).
- **Poprawki po ke2 już w kodzie (wejdą do ke4):** fokus po zamknięciu panelu odtwarzacza (ponowienie po 450 ms), TMDB w skali 10 w opisie średniej, zakładki biblioteki po polsku („Filmy (108)”), fokus po panelu biblioteki na chipie.

## Do zrobienia (kolejność)
1. Test ke3 ≥ 10 min: źródła (CW → długie OK → „Wybierz źródło”; wybór źródła; filtry szybkie i dodatki; brak źródeł / błąd), regresja ke2, Live TV: mini pasek na żywo (rząd „Dla Ciebie”/„Teraz”), EPG po wczytaniu przewodnika, archiwum (Z-4), panel Więcej w odtwarzaczu (fokus wraca na Więcej).
2. Build ke4 (poprawki + dalsze pozycje), test, pętla.
3. Kolejka: Z-3 (przyciski nad paskiem w odtwarzaczu TV — układ na sztywnych offsetach w `live/NiuniaLivePlayer.kt`), Z-8 (kolorowy rząd ocen + tabela sezon × odcinek z filtrem 20 %), Z-12 (karta tytułu z przewijaniem rzędu, `HorizontalPager`), Z-2 rozmycie na API 31+, Z-11 (w Kotlinie raczej nic).
4. Z-14 do wdrożenia: PlayerLiveError (okno „Kanał nie odpowiada” w stylu Niuni), LiveTVOptions (długie OK na kanale: ulubione, strumień 1 z N, ramówka, ukryj — dziś długie OK tylko przełącza ulubione), PlayerSources / PlayerNext / PlayerLoading / PlayerError, ProfilePin (porównać z istniejącym `NiuniaPinDialog`), LibraryHistory (wymaga danych historii z WatchProgressRepository).
5. **Pomysły P-n przyjęte 23.11** (decyzje i ostateczna postać: `POMYSLY-Z-KE.md` część 4). W makiecie wdrożone jako **Z-15…Z-23** w `ZMIANY-DO-WDROZENIA.md` (zegar z ustawienia Nuvio, Główna, strona tytułu, osoba, Szukaj bez pustych wyników, Live TV, odtwarzacz, **ekran główny YouTube jak w Kicia/Forge**). Wejścia: Discover (chip w Szukaj, gatunki), CatalogAll („Zobacz wszystko”), Studio (chip studia). **Folder odrzucony** (P-6: foldery dostarczają dodatki) — nie wdrażać. Odrzucone też P-8, P-9, P-29, P-30.
6. Po akceptacji właściciela: commit w worktree, scalenie do `skin-niunia` (inne wątki commitują tam równolegle; `git log` przed scaleniem), wpis w `ZMIANY-DO-WDROZENIA.md` przesunięcie punktu „wdrożone”.

## Pułapki
- JMGO (`adb connect 192.168.31.252:5555`): przed instalacją sprawdzić, czy nikt nie ogląda (`dumpsys media_session` bez `state=3` przez kilka minut). Instalacja: md5 lokalnie = md5 z `adb pull`, potem `cmd package compile -m speed -f com.nuviodebug.com`.
- Główny checkout `NuvioFork` ma niezacommitowane, niekompilowane zmiany wątku ustawień — nie budować tam; ten wątek buduje tylko w `wt-niunia-ke` (`build.sh` czeka na każdy Gradle, także z innych katalogów).
- Odtwarzacz: pierwsze ▲ tylko pokazuje nakładkę (fokus na pasku), drugie przechodzi na przyciski. Panele mają `focusProperties { exit = Cancel }` — `requestFocus` poza panel działa dopiero, gdy panel zniknie.
- Pytanie o płatne napisy AI przy odcinkach bez polskich napisów — w testach wybierać „Nie”.
- Narzędzia testowe: `narzedzia/k.sh` (klawisze, `long` = długie OK), `txt.sh` (tekst i fokus z uiautomator), `shot.sh` (zrzut pomniejszony), `str.sh` (teksty KE wg prefiksu); łatki rund 2–3 w `narzedzia/*.py` (już nałożone, tylko do wglądu).
