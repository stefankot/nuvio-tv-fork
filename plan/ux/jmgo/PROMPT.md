# Prompt: UX, fokus i wydajność Nuvio (fork, skórka Niunia) na JMGO

> Wklej w nowej sesji Claude Code otwartej w `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`.
> Na starcie skopiuj ten plik do `plan/ux/jmgo/PROMPT.md`, żeby „kontynuuj” mógł do niego wrócić.

## Zadanie

Przetestuj działającą na JMGO aplikację pod kątem użyteczności i popraw wszystko, co da się poprawić w naszym kodzie.
Najważniejsze są **przejścia między elementami z fokusem** (pilot) i **czas ładowania**. Pracujesz w pętlach:
testy (≥ 10 min, wszystkie 50 ścieżek) → paczka poprawek → jeden build v7a → instalacja → kolejne testy.
Zostawiasz po sobie: 50 ścieżek jako powtarzalny skrypt, testy jednostkowe (JVM), testy wydajności i raport przed/po.

## Najpierw przeczytaj (tylko to)

1. `ZASADY.md`: obowiązuje w całości (autonomia bez pytań, buildy tylko `fork-tools/build.sh` w zakładce terminala, 1 build naraz, v7a dla JMGO, limit wpięć 65, logi, bezpieczeństwo).
2. `plan/ux/STAN.md` i `plan/ux/RAPORT.md`: poprzednie 5 pętli na emulatorze (22–23.11). Pozycje `[ ]` i `[i]` są punktem wyjścia; nie powtarzaj zrobionego `[B]`.
3. `fork-tools/uitest/README.md`, nagłówki `fork-tools/uitest/sciezki.py` (50 ścieżek z emulatora: `key`, `hold_ok`, `dump`, `settle`, `seek`, `run_path`), `fork-tools/test/{focus,jmgo_adb,jmgo_czekaj_wolny,jmgo_instaluj,jmgo_sesja,klawisz}.sh|py`, `fork-tools/devtest/perf/perf.sh`.
4. `plan/DO_SPRAWDZENIA_NA_TV.md`: sekcje „tv1”, „tv2”, „Czcionki napisów”, szkło (dołącz je do ścieżek, nie rób osobno).
5. `plan/TERAZ.md`: tylko sekcje „NAJPIERW”, żeby wiedzieć, jakie APK jest na JMGO (ostatnio glass7a / subfonts1).

## JMGO: bezpieczeństwo (twarde)

- `adb connect 192.168.31.252:5555`, `export ANDROID_SERIAL=192.168.31.252:5555`. Android 11, armeabi-v7a, 1920×1080 px przy 320 dpi, czyli **1 dp = 2 px** (scena 960×540 dp).
- Projektor należy do domowników. Przed każdą pętlą `bash fork-tools/test/jmgo_czekaj_wolny.sh 10 180`. Przed każdą ścieżką sprawdź, czy na wierzchu jest `com.nuviodebug.com` na oczekiwanym ekranie; dwa razy z rzędu coś innego = ktoś używa TV → przerwij, czekaj na „WOLNY”.
- Tylko `com.nuviodebug.com`. Nigdy `pm clear`, `pm uninstall`, kasowanie danych. Ustawienia systemu wolno zmieniać, gdy przyspiesza to pracę (ZASADY §5, 24.11). Profil właściciela zostaje zalogowany.
- Nie wpisuj PIN-u, kluczy, haseł. Nie uruchamiaj tłumaczenia napisów AI (to płatne). Odtwarzanie najwyżej ok. 10 s na ścieżkę, potem Wstecz.
- Każdą zmianę ustawień w ścieżce cofnij w tej samej ścieżce (przełącznik ×2, ulubione tak → nie). Nie ukrywaj kanałów na stałe.
- Po instalacji: `bash fork-tools/test/jmgo_instaluj.sh <apk>` (md5 + `compile -m speed`).

## Narzędzia: głównie UI Automator

1. **Ścieżki: `uiautomator dump` przez adb** (jak `sciezki.py`). Nowy skrypt `fork-tools/uitest/sciezki_jmgo.py` (możesz importować z `sciezki.py`): po każdym klawiszu czeka na stabilny fokus i sprawdza regułę F (niżej). Zapisuje każde przejście do `fokus.csv`: ścieżka, krok, klawisz, fokus przed (tekst + bounds), fokus po, czas, reguła, wynik.
   - W pierwszej pętli zmierz czas jednego `dump` na JMGO. Gdy > 1,5 s albo „could not get idle state” (animacje), użyj `uiautomator dump --compressed` i odczytu przez `exec-out`; ruch fokusu potwierdzaj wtedy logiem `ForkFocus` (pkt 3), a `dump` zostaw na punkty kontrolne.
   - Stabilne identyfikatory: sprawdź, czy korzeń skórki Niuni ma `Modifier.semantics { testTagsAsResourceId = true }`. Jeśli nie, dodaj (kod skórki, bez nowego wpięcia) i nadaj `testTag` kluczowym elementom (baner, rząd N, plakat, pozycje menu, chipy, kanał, komórka EPG, przyciski odtwarzacza). Wtedy UI Automator widzi `resource-id`.
   - Przytrzymanie: najpierw spróbuj prawdziwego przytrzymania pilota (`getevent -pl`, potem `sendevent` na urządzeniu wejściowym pilota: wciśnij, odczekaj, puść). Brak uprawnień → seria `input keyevent` w jednym wywołaniu + `--longpress` dla OK, wpis w `plan/BLOKADY.md`.
2. **UI Automator w testach instrumentowanych**: moduł `baselineprofile` (Macrobenchmark + UiAutomator, `NuvioStartupBenchmark`, `NuvioFrameMetricsBenchmark`, podróże HOME_SCROLL, FAST_SCROLL, NAVIGATION, PLAYER, SETTINGS_LIBRARY). Dopisz podróże fokusu: ROW_MEMORY (▼►▼▲), MENU_ROUNDTRIP (◄►), DETAILS_BACK (OK, Wstecz), LIVE_LIST_HOLD, EPG_GRID. Zanim uruchomisz na JMGO, sprawdź w kodzie, że `prepareForJourney` i reszta **nie czyszczą danych** aplikacji. Benchmark na Androidzie 11 bez roota: tylko pomiary (startup, `FrameTimingMetric`), bez generowania profilu bazowego. Po benchmarku ponownie `cmd package compile -m speed -f com.nuviodebug.com`. Jeśli target jest debuggable albo moduł nie da się uruchomić na JMGO, zostaw pomiary z pkt 4 i wpisz przyczynę do `BLOKADY.md`.
3. **Pomiar fokusu w aplikacji: moduł `fork/perf/ForkFocusTrace`** (przełącznik w Narzędziach forka, domyślnie wyłączony; wyłączony nic nie kosztuje). Bez nowych wpięć w bazę: klawisz z istniejącego `ForkHooks.dispatchKeyEvent`, fokus z `niuniaFocusable` / `niuniaFocusLook` (`skin/niunia/components/NiuniaFocus.kt`, `NiuniaAtoms.kt`), ekran z istniejącego `screensWrap`. Log `ForkFocus`: `key=DPAD_RIGHT rep=0 t=…`, `focus=<tag|tekst> bounds=… dtFocus=<ms od zdarzenia do onFocusChanged> dtFrame=<ms do pierwszej klatki po zmianie, withFrameNanos>`, `screen=<trasa> firstFocus=<ms od wejścia>`. Bez tekstów dialogów i danych prywatnych.
4. **Wydajność przez adb** (rozszerz `fork-tools/devtest/perf/perf.sh` albo nowy `fork-tools/uitest/perf_jmgo.sh`):
   - zimny start ×10: `am force-stop`, `am start -W -n com.nuviodebug.com/com.nuvio.tv.launcher.AppIconDefault`, z logcat `Displayed` i `Fully drawn` (Główna ma `ReportDrawnWhen`) oraz `firstFocus` z `ForkFocus`; ciepły start ×10 (HOME systemu, powrót);
   - klatki: `dumpsys gfxinfo com.nuviodebug.com reset` → ścieżka → `dumpsys gfxinfo com.nuviodebug.com` (Janky %, 50/90/95/99 percentyl, slow UI thread, slow bitmap uploads), dla ścieżek 4–8, 17, 27, 39, 42, 49;
   - `JankStats` w logcat pokazuje tylko klatki ≥ 100 ms (filtr `ForkLog.jankWorthLogging`), więc drobniejsze zacięcia licz z gfxinfo;
   - pamięć: `dumpsys meminfo com.nuviodebug.com` (PSS, sterta Java) na starcie, po 50 ścieżkach i po wyjściu z Live TV (JMGO `com.jmgo.hippo` zabija fork w tle przy PSS 480–634 MB);
   - na 3 najwolniejsze przejścia ślad `perfetto` (Android 11 ma `perfetto`; `sched gfx view am wm dalvik`, 10 s), wnioski w raporcie.
5. **Zrzuty ekranu tylko do wyglądu fokusu**: raz na typ elementu i po każdej zmianie wyglądu, zmniejszone `sips -Z 960`. Reszta tekstowo.

## Reguły fokusu (każdy krok ścieżki sprawdza co najmniej jedną)

- **F1 Start ekranu**: po otwarciu dokładnie jeden element z fokusem, na głównej akcji (nie w menu, nie poza ekranem), w budżecie czasu.
- **F2 Kierunek**: nowy fokus leży w stronę strzałki i jest najbliższym sąsiadem w tej osi; w siatkach i EPG ▼▲ zachowuje kolumnę lub czas.
- **F3 Pamięć**: powrót do rzędu, listy albo paska chipów przywraca ostatni lub wybrany element, nie pierwszy. Jedna konwencja w całej aplikacji; zapisz ją w `plan/DECYZJE.md`.
- **F4 Bez zawijania**: na krawędzi strzałka nic nie robi; fokus nie skacze do następnego rzędu ani na drugi koniec.
- **F5 Powrót**: Wstecz albo zamknięcie okna przywraca fokus na element, który je otworzył, z tym samym przewinięciem.
- **F6 Stabilność**: dociąganie danych, zmiana tekstu i przebudowa listy nie przenoszą fokusu (fokus trzymany po kluczu/id, nie po indeksie; usunięty element → najbliższy sąsiad).
- **F7 Widoczność**: element z fokusem w całości na ekranie, ≥ 8 px (4 dp) od krawędzi i od sąsiadów po powiększeniu, nic go nie zasłania.
- **F8 Długie OK**: 520 ms otwiera akcję elementu; puszczenie OK po długim OK niczego nie klika; następne krótkie OK działa (`NiuniaOkGate`).
- **F9 Przytrzymanie**: fokus nadąża; po puszczeniu ruch kończy się najwyżej 1 element dalej; żadnej klatki ≥ 100 ms.
- **F10 Martwe miejsca**: element nieaktywny lub bez akcji nie robi pułapki; długie OK bez akcji nic nie psuje.
- **F11 Menu boczne**: ◄ z pierwszej kolumny otwiera menu z fokusem na bieżącym ekranie; ► wraca do treści na zapamiętany element.
- **F12 Wstecz**: zawsze widoczny skutek w ≤ 300 ms, nigdy wyjście z aplikacji bez pytania.
- **Wygląd fokusu** (zrzut, raz na typ): karty z obrazem uniesienie 4 dp, skala 1,1, biała obwódka 3 dp; przyciski białe, skala 1,08; wiersze, chipy, klawisze, komórki EPG białe bez skali. Animacja `tween(180)` w `NiuniaFocus.kt` nie może zacinać na JMGO.

## Budżety czasu (propozycja; zapisz w `plan/DECYZJE.md`, mierz przed i po)

| Pomiar (JMGO, po `compile -m speed`) | Cel | Błąd |
|---|---|---|
| Zimny start → `Fully drawn` Głównej i fokus na banerze | ≤ 3,5 s | > 5 s |
| Ciepły start | ≤ 1,0 s | > 2 s |
| OK → pierwszy fokus na nowym ekranie | ≤ 600 ms | > 1,5 s |
| Pełne dane ekranu z pamięci/dysku (z sieci) | ≤ 1,5 s (≤ 3 s), szkielet od razu | > 5 s |
| Wstecz → fokus na elemencie źródłowym | ≤ 300 ms | > 800 ms |
| Strzałka → nowy fokus (`dtFrame`), mediana / p95 | ≤ 50 / ≤ 100 ms | p95 > 200 ms |
| Klatki w nawigacji pilotem (gfxinfo): Janky / p95 | ≤ 5 % / ≤ 34 ms | > 15 % |
| Obraz plakatu/hero po fokusie z pamięci (z sieci) | ≤ 300 ms (≤ 1 s) | > 2 s |
| Live TV: lista kanałów z dysku | ≤ 1 s | > 3 s |
| OK „Odtwórz” → pierwsza klatka (mierz, źródła są cudze) | ≤ 3 s | > 8 s |
| PSS po 50 ścieżkach | ≤ 400 MB | > 480 MB |
| Awarie, ANR, nieobsłużone błędy `Fork*` | 0 | ≥ 1 |

## 50 ścieżek (każda: start z ustalonego ekranu, kroki, reguły)

Nazwy ekranów i przycisków bierz z bieżącego buildu (stare ścieżki w `sciezki.py` mogą być nieaktualne po tv1/tv2). Ekranu brak → ścieżka „POMINIĘTA” z powodem, nie wymyślaj zastępczej.

**A. Start i Główna**
1. Zimny start: fokus na „Odtwórz” w banerze; po dociągnięciu rzędów fokus nie skacze. [F1 F6, czas]
2. Ciepły start (HOME systemu → ikona): fokus tam, gdzie był, albo na banerze (zapisz które). [F1]
3. Baner: ◄► po przyciskach, na skrajnych bez zawijania, ▲ nie gubi fokusu. [F4 F10]
4. Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero zmienia się bez skoku. [F2 F7, gfxinfo]
5. Rząd ►×6 → ▼ → ▲: ten sam plakat. [F3]
6. Rząd 1, 4. plakat ▼ → rząd 2: plakat zgodny z konwencją F3/F2. [F2 F3]
7. Przytrzymana ▼ 3 s przez wszystkie rzędy i ▲ do banera. [F9, gfxinfo]
8. Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku. [F4 F7 F9, gfxinfo]
9. Plakat OK → strona tytułu → Wstecz: ten sam plakat, to samo przewinięcie. [F5 F12, czas]
10. Długie OK na plakacie → menu opcji → Wstecz: fokus na plakacie, puszczenie OK nic nie klika. [F8 F5]

**B. Menu boczne**
11. ◄ z pierwszego plakatu → menu z fokusem na „Główna”. [F11]
12. Menu ▼ do końca i ▲ (bez zawijania), ► → element treści, z którego wyszło. [F4 F11]
13. Menu → Szukaj → Biblioteka → Live TV → Główna: na każdym pierwszy fokus w treści; Główna przywraca fokus. [F1 F5, czas]
14. Wstecz z głębi rzędu na Głównej: baner/menu, drugi Wstecz nie zamyka bez pytania. [F12]
15. Długie OK na pozycji menu: nic się nie psuje. [F10]

**C. Szukaj**
16. Wpis „bat” klawiaturą ekranową: fokus zostaje na klawiszu, wyniki dochodzą bez kradzieży fokusu. [F1 F6]
17. Z klawiatury do wyników: policz ► (RAPORT pkt 9; > 6 = propozycja skrótu), ◄ wraca na ten sam klawisz. [F2 F3, gfxinfo]
18. Wynik OK → tytuł → Wstecz: fokus na wyniku. [F5]
19. Polecane: długie OK → menu → Wstecz. [F8 F5]
20. Kasowanie liter do pustego pola: wyniki znikają, fokus nie spada w niewidoczne miejsce. [F6 F7]

**D. Biblioteka**
21. Chipy/listy ▼ do treści, ▲ na wybrany chip. [F3]
22. Historia (Z-14): OK → tytuł → Wstecz; długie OK na pozycji. [F5 F8]
23. Pusta lista: fokus na przycisku stanu pustego. [F1 F10]

**E. Strona tytułu**
24. Film: fokus na „Odtwórz” w budżecie; ◄► po przyciskach bez zawijania. [F1 F4, czas]
25. „Więcej” → „Mniej”: fokus zostaje na przycisku po zmianie układu. [F6]
26. Serial: ▼ pigułki sezonów, ► zmiana sezonu, ▼ odcinki, ▲ na wybraną pigułkę. [F3 F6]
27. Tabela sezon × odcinek (Z-8): ◄►▲▼ po komórkach, przewijanie trzyma fokus. [F2 F7, gfxinfo]
28. Odcinek OK → wybór źródła → Wstecz: ten odcinek; długie OK = opcje odcinka. [F5 F8]
29. Obsada → Osoba → plakat → tytuł → Wstecz ×3: za każdym razem element źródłowy. [F5]
30. Zwiastun: OK → od razu „Wczytuję zwiastun…” → Wstecz na kartę zwiastunu. [F5 F12, czas]
31. Wstecz z głębi strony (▼×6): wraca na górę lub wychodzi, fokus zawsze widoczny. [F7 F12]

**F. Źródła i odtwarzacz**
32. Wybór źródła: pierwszy fokus na pierwszym źródle; filtry ▲▼ z powrotem na wybrany filtr. [F1 F3]
33. Odtwarzacz: OK pokazuje nakładkę, ▲ do przycisków nad paskiem (Z-3), ◄►, ▼ na pasek. [F3 F4]
34. Przewijanie ◄► pojedyncze i przytrzymane; po schowaniu nakładki następny klawisz nie gubi fokusu. [F9 F6]
35. „Zmień źródło” (Z-14) → lista → Wstecz na ten przycisk. [F5]
36. Wstecz z odtwarzacza → strona tytułu, fokus na „Odtwórz/Wznów”; czas pierwszej klatki. [F5 F12, czas]

**G. Live TV**
37. Wejście: fokus na ostatnim lub pierwszym kanale; czas listy z dysku. [F1, czas]
38. Chip „Kraj i język” → panel Kraj / Język → wybór → Wstecz: fokus na chipie, filtr w tle bez kradzieży fokusu; przywróć poprzedni wybór. [F5 F6]
39. Lista kanałów: przytrzymana ▼ 5 s i ▲. [F9, gfxinfo]
40. Kanał OK → odtwarzanie → Wstecz: ten sam kanał; przy „… nie odpowiada”: przyciski Następny kanał · Spróbuj ponownie · Lista kanałów i Wstecz. [F5 F1 F12]
41. Długie OK na kanale → „Opcje kanału” → ▼ po opcjach → Wstecz na kanał (ulubione tak → nie). [F8 F5]
42. Przewodnik EPG: ►◄ w czasie, ▼▲ między kanałami trzyma czas; podgląd na żywo (P-32) nie kradnie fokusu i nie zacina. [F2 F6, gfxinfo]
43. „Szukaj w telewizji”: fokus w polu, wynik OK → Wstecz. [F1 F5]

**H. Ustawienia, YouTube i ekrany bazy**
44. Ustawienia: ▼ kategorie, ► treść, ◄ ta sama kategoria; Wygląd → „Szkło”: „Niedostępne na tym telewizorze”, bez pułapki. [F3 F10]
45. Przełącznik OK ×2: stan wraca, fokus zostaje. [F6]
46. Napisy → wygląd: Atkinson Hyperlegible domyślna, na liście Tiresias; lista → Wstecz bez zmiany. [F1 F5]
47. YouTube: czas do fokusu w treści (RAPORT pkt 18), OK → odtwarzacz → Wstecz. Kod obcy: tylko pomiar i raport. [F1 F5, czas]
48. Profile: ◄► po profilach, wejście w PIN i Wstecz (bez wpisywania). [F1 F5]
49. Discover / CatalogAll / Studio: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek bez przewinięcia na początek. [F2 F5 F7 F9, gfxinfo]
50. Kalendarz, Dodatki, Kolejność katalogów, Kolekcje: pierwszy fokus, Wstecz z pozycji na element źródłowy (RAPORT pkt 10). [F1 F5]

Wynik ścieżki: OK (wszystkie reguły i budżety), UWAGA (budżet „cel” przekroczony, reguła spełniona), BŁĄD (reguła złamana, budżet „błąd”, awaria). Każda UWAGA i BŁĄD → pozycja w `RAPORT.md` z numerem ścieżki, regułą i miarą.

## Testy jednostkowe (JVM)

- Katalog `app/src/test/java/com/nuvio/tv/skin/niunia/` (wzór: `NiuniaOkGateTest`), uruchamianie przez `bash fork-tools/build.sh <log> :app:testFullDebugUnitTest --tests 'com.nuvio.tv.skin.niunia.*' --tests 'com.nuvio.tv.fork.perf.*'`, wyniki `fork-tools/test/junit_wyniki.py`.
- Zasada: błąd fokusu znaleziony na TV → najpierw wydziel logikę do czystej funkcji i napisz test, który go odtwarza; dopiero potem poprawka.
- Obowiązkowe zestawy:
  1. `NiuniaOkGate`: dopisz przypadki z pętli (przytrzymanie z poprzedniego ekranu, flaga systemowa, otwarcie okna przy 520 ms).
  2. Pamięć fokusu (rząd, Szukaj, powrót do otwierającego): po kluczu; przetrwa dociągnięcie, zmianę kolejności i usunięcie elementu.
  3. Pierwszy fokus ekranu jako funkcja stanu (wczytywanie / pusty / błąd / dane) dla Głównej, tytułu, Live TV, Szukaj, Biblioteki.
  4. Krawędzie bez zawijania i wybór sąsiada w siatce (kolumna) oraz w EPG (komórka nachodząca na kursor czasu).
  5. Przytrzymanie: przy `repeatCount > 0` pomijamy kosztowną pracę (hero, podgląd), ruch nie przeskakuje.
  6. Obciążenie: filtr „Kraj i język”, kolejność listy, wyszukiwanie na 20 tys. pozycji poniżej progu z zapasem (stabilne, nie migające), poza wątkiem UI.
- Jeśli w `app/build.gradle.kts` są już Robolectric i `ui-test-junit4`, dodaj testy Compose fokusu dla komponentów Niuni (`performKeyInput { pressKey(Key.DirectionRight) }`, `assertIsFocused`). Jeśli nie ma, nie dodawaj zależności; zostań przy czystej logice i zapisz decyzję w `DECYZJE.md`.

## Co poprawiać (najczęstsze przyczyny wolnych przejść na słabym TV)

- Rekompozycja całego rzędu/ekranu przy każdej zmianie fokusu (stan fokusu czytany za wysoko) → `derivedStateOf`, stabilne lambdy, `key = id` i `contentType` w Lazy, sprawdź `compose_stability_config.conf`.
- Hero i obraz tła przeładowywane przy każdym ruchu → aktualizacja dopiero po ok. 250 ms bez ruchu i nigdy w trakcie przytrzymania; obrazy w rozmiarze wyświetlania (Coil `size`), pamięć podręczna, wczytanie sąsiadów z wyprzedzeniem.
- Animacja fokusu przez `graphicsLayer` (skala, uniesienie), nie przez zmianę rozmiaru lub `padding` (relayout); obwódka w `drawBehind`.
- Przewijanie do fokusu: jedno przewinięcie na ruch, bez podwójnego `bringIntoView`; w trakcie przytrzymania przewijanie natychmiastowe zamiast animowanego.
- Android 11: `effects="flat"`, bez rozmycia na żywo (ZASADY §2); tylko jednorazowe rozmycie obrazów (`fork/images/NativeBlur`).
- Start: nic ciężkiego w `Application.onCreate` i na pierwszej klatce; moduły forka leniwie; ciężkie obliczenia w tle, przyrostowo, z dysku (ZASADY §2).
- Logi: żadnego logowania w każdej klatce; `ForkFocusTrace` domyślnie wyłączony.

## Granice zmian

- Poprawiasz zachowanie i wydajność w skórce Niunia (`skin/niunia/**`) i modułach forka (`fork/**`). Kod bazy i obcy (OneTube/YouTube, Reshaped, KE) tylko przez istniejące wpięcia; limit wpięć 65 jest wyczerpany, więc nowych nie dodawaj. Czego nie da się zrobić bez zmiany bazy → `RAPORT.md` jako `[i]`.
- **Wygląd** (rozmiary, odstępy, kolejność, teksty, styl fokusu) nie zmieniasz w Kotlinie: ustala go makieta (`~/Desktop/nuvio-tv-interface`). Rozbieżność z makietą albo propozycja zmiany wyglądu → `RAPORT.md` jako `[makieta]` dla właściciela.
- Każda nowa funkcja: osobny moduł, przełącznik w ustawieniach forka, logi `Fork<Moduł>`, każdy błąd zalogowany (`${e.javaClass.simpleName}: ${e.message}`).
- Przed commitem: `git log` / `git pull` (inne wątki), `bash fork-tools/check.sh`. Gałąź `skin-niunia`. Build tylko z dużej paczki poprawek, jeden na pętlę: `bash fork-tools/build.sh uxj<N> :app:assembleFullDebug -PforkAbi=armeabi-v7a` (+ testy JVM w tym samym wywołaniu albo zaraz po), w zakładce terminala. Emulator wyłączony.

## Pętle i zapis

- **Pętla 0 (przed zmianami)**: na APK, które jest teraz na JMGO, uruchom 50 ścieżek i pomiary wydajności. To jest punkt odniesienia „przed”. Wszystkie pomiary z pętli 0 wpisz do tabeli budżetów.
- Każda pętla: ≥ 10 min testów → poprawki → build → `jmgo_instaluj.sh` → testy. Po każdej pętli zapisz stan (ZASADY §4) w `plan/ux/jmgo/`:
  - `STAN.md`: pętla, APK (commit, md5), czasy, liczba OK / UWAGA / BŁĄD, co dalej;
  - `RAPORT.md`: ustalenia `[P]` poprawione w kodzie, `[B]` sprawdzone na buildzie, `[ ]` otwarte, `[i]` cudze lub zgodne z projektem, `[makieta]`;
  - `cykl<N>/sciezki.md` (tabela 50 ścieżek), `cykl<N>/fokus.csv`, `cykl<N>/wydajnosc.md` (budżety: pętla 0, poprzednia, bieżąca).
- Koniec, gdy dwie pętle z rzędu dają 0 BŁĄD, ≥ 48/50 OK, a każdy przekroczony budżet ma w raporcie przyczynę (kod obcy, sieć, sprzęt). Najwyżej 6 pętli; potem podsumowanie i stop.
- Na końcu `plan/ux/jmgo/PODSUMOWANIE.md` dla właściciela, prostym językiem: co było wolne lub mylące, co poprawiono, czasy przed i po, co zostało i dlaczego. Trzy stany zawsze rozdzielone: napisane / zbudowane / sprawdzone na JMGO.
- Przy ok. 95 % kontekstu: zgodnie z ZASADY §1 (stan i dokładne następne kroki w `plan/TERAZ.md`, sekcja „NAJPIERW” z odesłaniem do `plan/ux/jmgo/PROMPT.md`).
