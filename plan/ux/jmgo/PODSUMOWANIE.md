# UX na projektorze JMGO — podsumowanie dla właściciela (24.11.2026)

**W jednym zdaniu:** poprawiłem rysowanie Głównej (mniej zacięć), naprawiłem kilka błędów powrotu fokusu i bardzo przyspieszyłem same testy, ale
**pełnej, wiarygodnej pętli testów nie ma**, a pamięć aplikacji wciąż jest za duża. Pętli 2 nie robiłem — zapisano, że zakazałeś kolejnych przebiegów.

## Trzy stany (napisane → zbudowane → sprawdzone na JMGO)
| Co | Napisane | Zbudowane | Sprawdzone na JMGO |
|---|---|---|---|
| Główna: lżejsze rysowanie tła i plakatów (zacięcia) | tak | tak (`uxj1`) | **tak** — liczby niżej |
| Powrót fokusu w Bibliotece, Odkrywaj, Szukaj (Wstecz wraca na ten sam plakat) | tak | tak (`uxj1`) | **nie** (ścieżki 22 i 38 test pomija, 19 niejednoznaczna) |
| Panele (Opcje kanału, Kraj i język…) nie wypuszczają fokusu na ekran pod spodem | tak | tak (`uxj1`) | **nie** (ścieżka 41 niejednoznaczna) |
| Biblioteka: ▲ z treści wraca na ostatnio wybraną zakładkę („Filmy”, nie „Seriale”) | tak (druga wersja) | tak (`uxj3`) | **nie** — pierwsza wersja nie zadziałała w 3 przebiegach, druga nie była testowana |
| Logi nie pokazują adresu IPTV (login i hasło dostawcy) | tak | tak | tylko test JVM |
| Szybszy czytnik ekranu do testów (`fastdump`) | tak | — | **tak** — pętla 40,6 → ok. 12 min |

Na JMGO jest teraz `uxj3` (md5 `6deb562d…`), zainstalowany, `compile -m speed` zrobione.

## Co było wolne albo mylące, a co się zmieniło
| Sytuacja | Przed (`tv2e`) | Po (`uxj1`/`uxj3`) | Cel |
|---|---|---|---|
| Główna, przytrzymane ► — zacięte klatki | 98 % (typowa klatka 69 ms) | 22–50 % (14–17 ms) | ≤ 5 % — **jeszcze nie** |
| Główna, ▼ z banera — zacięte klatki | 93 % | 45 % (jednorazowo 3 %) | ≤ 5 % — **jeszcze nie** |
| Główna, przytrzymane ▼ — zacięte klatki | 93 % | 78 % | ≤ 5 % — **jeszcze nie** |
| Szukaj — klawiatura → wyniki | 8,8 % | 0 % | ≤ 5 % — OK |
| Live TV, lista kanałów | 71 % | 1,4 % | ≤ 5 % — OK (nie ruszałem; różnica z innego stanu kanałów) |
| Odkrywaj, siatka | 58 % | 46 % | ≤ 5 % — **jeszcze nie** |
| Start aplikacji od zera (Główna narysowana) | 3,37 s | 3,37 s | ≤ 3,5 s — OK, bez zmian |
| Ponowne wejście do aplikacji | 0,23 s | 0,25 s | ≤ 1 s — OK |
| Pierwszy fokus po starcie | nie mierzono | ok. 2 s | ≤ 3,5 s — OK |
| **Pamięć po 50 ścieżkach** | 884 MB | **1 009 MB** (w Live TV 1 172 MB) | ≤ 400 MB — **BŁĄD, gorzej niż przed** |
| Czas jednej pętli testów (50 ścieżek) | 40,6 min | 11,9 min | — |

Dlaczego jeszcze zacina: po poprawce GPU mieści się w klatce, zostaje praca aplikacji przy ruchu fokusu (odświeżanie widoku, układ nowych plakatów,
sprzątanie pamięci). To wymaga osobnej poprawki, nie dalszego odciążania grafiki.

## Dlaczego nie ma wyniku „2 pętle bez błędu”
- Przebieg pętli 1 (`uxj1`) dał 11 OK / 14 UWAGA / **21 BŁĄD** / 4 pominięte, ale 21 błędów to głównie błąd **testera** (odczyt fokusu spóźniony o jeden
  klawisz). Poprawiłem go po przebiegu, lecz kolejny pełny przebieg na `uxj3` przerwano na Twoje polecenie.
- Liczby sprzętowe (starty, pamięć, zacięcia) od czytnika nie zależą i są wiarygodne; oceny „fokus OK/BŁĄD” z tego przebiegu — nie.
- Kryterium końca (dwie pętle z rzędu: 0 błędów i ≥ 48/50 OK) **nie jest spełnione**. Pętli 2 nie uruchamiałem.

## Co zostało (kolejność wg ważności) — bez poprawek
1. **Pamięć** (1 GB zamiast 400 MB), przyczyna nieznana; zacząć od pamięci obrazów i odtwarzaczy Live TV.
2. **Zacięcia przy przytrzymaniu na Głównej** — wątek aplikacji (rekompozycja, układ, sprzątanie pamięci).
3. **Wstecz ze strony tytułu** wraca na ten sam element, ale 49–125 px niżej (ścieżki 28, 30).
4. **Szukaj ◄ z wyników** ląduje na „Sterowanie głosem”, nie na klawiszu (17) — możliwy artefakt testu.
5. **Live TV**: brak fokusu po otwarciu, długie OK nie otwiera „Opcji kanału” (40–42).
6. Test JVM `SettingsTreeSpecTest` (tryb prosty ≤ 20 pozycji) pada od `uxj1` — cudzy kod ustawień, nie ruszany.

## Co dalej (do Twojej decyzji)
Osobne zadanie „poprawki aplikacji”: najpierw ustalić przyczynę pamięci, potem punkty 3–5; jeden build i sprawdzenie tylko dotkniętych ścieżek
(nie całej pięćdziesiątki). Szczegóły i dowody: `PRZEKAZANIE.md`, `RAPORT.md`, `etap1-gpu.md`, `cykl1/wydajnosc.md`.
