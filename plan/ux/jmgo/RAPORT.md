# UX JMGO — raport ustaleń (fokus i wydajność, skórka Niunia)

Stan: po pętli 0 (APK `tv2e`, 24.11) i analizie E2. Oznaczenia: `[P]` poprawione w kodzie (jeszcze niezbudowane), `[B]` sprawdzone na buildzie,
`[ ]` otwarte, `[i]` cudze lub zgodne z projektem, `[makieta]` decyzja właściciela / zmiana wyglądu. Liczby ścieżek: `cykl0/sciezki.md`, budżety: `cykl0/wydajnosc.md`.

Pętla 0: **15 OK / 17 UWAGA / 15 BŁĄD / 3 pominięte** z 50. Po rozdzieleniu 15 BŁĘDÓW: 5 to błędy aplikacji (19, 21, 22, 41, 49 — poprawione w kodzie `[P]`), 4 to samo GPU (4, 7, 8, 39 — E1 / O1–O2),
5 to błędy oceny w skrypcie albo kod obcy (11, 12, 42, 43, 47), 1 do wyjaśnienia (33, O6). Osobno przekroczona pamięć (O3, budżet, nie ścieżka).

## [P] Poprawione w kodzie (E2) — wejdą do builda E3, test na JMGO w pętli 1

| # | Ścieżka | Reguła | Objaw na JMGO | Poprawka (plik) | Test JVM |
|---|---|---|---|---|---|
| P1 | 21 Biblioteka | F3 | ▲ z plakatów trafiał w chip „Seriale”, choć ostatnio zaznaczony był „Filmy” | `focusRestorer()` na pasku zakładek (`NiuniaLibraryShelves.kt`) | konwencja F3 (framework); logika pamięci: `NiuniaFocusMemoryTest` |
| P2 | 22 Biblioteka (siatka, półki, Historia) | F5 | Wstecz ze strony tytułu: fokus na „Wszystko”, nie na otwartym plakacie | `NiuniaFocusMemory` + `rememberNiuniaFocusRestore`: ten sam plakat i to samo przewinięcie (`NiuniaLibraryShelves.kt`, `NiuniaLibraryHistory.kt`) | `NiuniaFocusMemoryTest` (klucz, dociągnięcie, usunięty element, inny układ, przewinięcie) |
| P3 | 49 Odkrywaj | F5 | Wstecz: fokus na pierwszym chipie, siatka od góry | j.w. (`NiuniaDiscover.kt`); fokus startowy na chipie tylko przy pierwszym wejściu | `NiuniaFocusMemoryTest` |
| P4 | 19 Szukaj → Odkrywaj | F5 | Wstecz z Odkrywaj: fokus na klawiszu „a”, nie na chipie „Odkrywaj” | `NiuniaFocusKeys.searchReturn` + znacznik `OPENER_DISCOVER` (`NiuniaSearch.kt`) | `NiuniaFocusKeysTest.searchReturnsToOpenerOfTheSameQuery`, `…IgnoresMemoryOfAnotherQuery` |
| P5 | 41 Live TV, Opcje kanału (i 4 inne panele) | F4 | ▼ w otwartym panelu przeskakiwało po kanałach pod spodem (w logu „podgląd → kolejny kanał” przy otwartym panelu); Wstecz kończył na innym kanale | `niuniaPanelTrap` = pułapka strzałek + `exit = Cancel` + `focusGroup` — Opcje kanału, Kraj i język, filtry źródeł, panel Biblioteki, zmiana źródła w odtwarzaczu (`NiuniaPanelTrap.kt`) | zachowanie frameworka (brak Robolectric); sprawdzić na JMGO w pętli 1 |
| P6 | log z pętli 0 (ZASADY §3) | — | log `ForkNiunia` drukował pełny adres strumienia IPTV (login i hasło dostawcy w ścieżce) przy wejściu do odtwarzacza z Live TV | `NiuniaLiveReturn.logLabel` — etykieta zamiast adresu, także w `toString` wpisu powrotu; `NiuniaLiveTv.kt`; zapisany `cykl0/logcat.txt` oczyszczony | `NiuniaLiveReturnTest` |

Skrypt `fork-tools/uitest/sciezki_jmgo.py` — poprawiona ocena (te „błędy” nie były błędami aplikacji):
- ścieżki 11–13, 15: menu Niuni otwiera **▲ z góry ekranu i Wstecz, nie ◄** (`NiuniaNav.kt`); skrypt wchodzi ▲ i porównuje z elementem, z którego wyszedł (wcześniej: pierwszy plakat ze startu ścieżki);
- ścieżka 7: ▲ trzymane 3 s kończy na „Zmień profil” (najwyższy element; ▲ z banera otwiera menu) — poprawne; długie OK na pozycji menu bez własnej akcji = zwykłe OK po puszczeniu — poprawne (F10);
- ścieżka 19: „polecany plakat” był chipem „Odkrywaj” (szeroki, nie plakat);
- ścieżki 39 i 49: wiersz na całą szerokość ekranu ma krawędzie boczne z projektu (F7 sprawdza tylko pion); położenie po Wstecz z tolerancją 4 px (ścieżka 50: 1 px);
- ścieżka 43: pole „Szukaj kanału lub programu” (560 px) uznawane za menu — menu ma ≤ 450 px; ścieżka 47: YouTube ma własny pasek „Główna” (kod obcy);
- ścieżka 42: przewodnik Niuni to wiersze kanałów na całą szerokość, ► nic nie robi.

## [ ] Otwarte (do pętli 1 na `uxj2`)

| # | Sprawa | Dane z pętli 0 | Co dalej |
|---|---|---|---|
| O1 | Zacięcia GPU na Głównej (ścieżki 4, 7, 8) | Janky 93–98 %, p95 65–150 ms; Slow UI thread 0, Slow issue draw commands 0 | E1 (`NiuniaHeroPage`, `NiuniaPoster`); pomiar po `uxj1` |
| O2 | Zacięcia GPU poza Główną: lista kanałów (39), siatka Odkrywaj (49), Szukaj (17) | Janky 71 % / 58 % / 8,8 % | zmierzyć po `uxj1`/`uxj2`; jeśli nadal > 15 % — poprawka warstw plakatu w tych ekranach |
| O3 | Pamięć: PSS po 50 ścieżkach **884 MB** (limit błędu 480), w Live TV 1 054 MB | natywna 43 → 384 MB, Java 65 → 116 MB (w Live TV 303 MB); nie wraca po wyjściu | E3: PSS po grupach ścieżek, ustalić przyczynę (odtwarzacz? cache obrazów? lista kanałów) |
| O4 | Pomiar czasów fokusu | tv2e bez `ForkFocusTrace` — tylko odczyt co ~2,2 s | pętla 1: `dtFrame`, „OK → fokus”, Wstecz w ms z logu `ForkFocus` |
| O5 | Ścieżka 28: Wstecz z odcinka → ten sam element, ale o 28 px niżej | `[80,416,490,955]` zamiast `[80,388,490,927]` | F5 „to samo przewinięcie”; sprawdzić po P2 (inny ekran, ta sama klasa) |
| O6 | Ścieżka 33: ◄ z przycisku „Dźwięk” w odtwarzaczu → „fokus zgubiony” | po 4,4 s odczytu | hipoteza: nakładka chowa się po ~4 s bezczynności, a odczyt trwa 2,2 s — sprawdzić z `ForkFocus` |
| O7 | Ścieżki, które nie doszły do celu (skrypt nie znalazł elementu): 20 (⌫), 23, 26, 27, 29, 32, 35, 38, 43 (klawisz „t”), 44 („Szkło”), 46 („Czcionka”) | 3 pominięte + 8 UWAGA | E3: poprawić nawigację skryptu w tych ścieżkach (elementy istnieją w stringach aplikacji) |
| O8 | Czas „OK → fokus” na Głównej 3,1–5,1 s w odczycie | `Fully drawn` mediana 3,37 s (OK) | po `ForkFocus` rozstrzygnąć, czy to granica odczytu |

## [i] Cudze lub zgodne z projektem

- Odtwarzanie (ścieżki 33, 36, 40): pierwszy fokus 5,6–5,8 s, całość kroku 12–16 s — źródła i odtwarzacz cudze; tylko pomiar.
- YouTube (47): własny pasek „Główna / …”, czasy kodu obcego.
- Awaria `com.android.inputmethod.latin` (NPE `ExtractedText`, 21:26:32) — systemowa klawiatura JMGO, nie aplikacja.
- Ostrzeżenia sieciowe w `ForkNiunia`: logo kanałów Strong8K HTTP 404, jeden obraz HTTP 410 — cudze serwery (aplikacja przechodzi do następnego źródła logo).
- Przewodnik Niuni nie ma osi czasu (wiersz = kanał + bieżący program): reguła F2 „▼▲ trzyma czas” go nie dotyczy.

## [makieta] Decyzje właściciela

- **Czy ◄ z pierwszej kolumny ma otwierać menu?** Dziś menu otwiera ▲ z góry ekranu i Wstecz (projekt Niuni, decyzja 20.11). Reguła F11 z promptu mówi ◄. Zmiana wymaga ruszenia `PillNavScaffold` (kod bazy) — zostawiono bez zmiany, zapisano w `plan/DECYZJE.md`.
- Przewodnik EPG z osią czasu (komórki programów w poziomie) — dziś go nie ma; to zmiana wyglądu.

## Aktualizacja E3 (24.11, koniec dnia)
- `[B]` P1 (ścieżka 21, ▲ na chip Biblioteki): **nie zadziałała** — `focusRestorer()` na rzędzie zakładek dał ten sam błąd („Seriale” zamiast „Filmy”) w 3 przebiegach na `uxj1`. `[P]` P7 (E3): jawne `exit` grupy treści → ostatnio zaznaczony chip (`NiuniaLibraryShelves.kt`, `uxj3`), niesprawdzona.
- P2–P5: nie potwierdzone ani obalone — ścieżki 22 i 38 test pomija (brak zakładki „Historia” / chipa „Kraj i język”), 19, 41, 42 niejednoznaczne.
- O3 pamięć: **gorzej** niż w pętli 0 (884 → 1 009 MB po ścieżkach; Java 187, natywna 400 MB), bez znanej przyczyny.
- O4: `firstFocus` zimny start mediana 1 994 ms (pierwszy pomiar). O6/O8 (odczyt 2,2 s) rozwiązane przez szybki czytnik.
- Nowe, niepoprawione: Wstecz ze strony tytułu o 49–125 px niżej (28, 30); Szukaj ◄ z wyników na „Sterowanie głosem” (17); Live TV bez fokusu po otwarciu i długie OK bez opcji kanału (40–42).
