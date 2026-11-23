# Raport UX/UI — 50 ścieżek użytkownika (22.11, emulator Google TV, skórka Niunia)

Ścieżki: `fork-tools/uitest/sciezki.py` (uiautomator + klawisze pilota). Wyniki pętli: `plan/ux/cykl<N>/sciezki.md`.
Stan pętli i kolejność prac: `plan/ux/STAN.md`.

## Nieoptymalne / błędy (lista zbiorcza; [P] = poprawione w kodzie, [B] = sprawdzone na buildzie)

### Długie naciśnięcie OK (zgłoszenie właściciela)
1. [P] Plakaty Niuni bez długiego OK: Główna (rzędy), Biblioteka, Szukaj (polecane, wyniki), Osoba, „Podobne” na stronie
   tytułu → teraz menu opcji bazy (Szczegóły, Do biblioteki / listy, Obejrzane).
2. [P] „Oglądaj dalej” bez długiego OK → menu bazy (Szczegóły, Od początku, Wybierz źródło, Obejrzane, Usuń).
3. [P] Live TV Niuni: kafelki kanałów/programów i wiersze przewodnika bez długiego OK → ulubione tak/nie z potwierdzeniem.
4. [P] Strona tytułu: długie OK na „Do biblioteki” → wybór list (jak w Nuvio).
5. [P] `niuniaOk` (wspólne OK Niuni): liczył tylko własny zegar 520 ms, ignorował systemową flagę długiego naciśnięcia;
   puszczenie OK po długim OK, które otworzyło okno/ekran, mogło „kliknąć” element w nowym miejscu; po długim OK, które
   przeniosło fokus, następne krótkie OK na tym elemencie ginęło; OK trzymane z poprzedniego ekranu odpalało długie OK
   na nowym. Logika wydzielona do `NiuniaOkGate` (testy JVM `NiuniaOkGateTest`, 8 przypadków).
6. [ ] Profile Niuni: brak długiego OK (Nuvio: Edytuj / Usuń) — wymaga zmiany wpięcia ekranu profili (stan okna jest
   w bazie, deklarowany po wpięciu) — do decyzji/odłożone.

### Fokus i nawigacja
7. [P] Szukaj: po Wstecz ze strony tytułu fokus wracał na klawisz „a” zamiast na otwarty wynik/plakat
   (ścieżka 11) → pamięć ostatnio otwartej pozycji (`NiuniaSearchMemory`).
8. [i] Biblioteka pusta → „Przeglądaj filmy i seriale” otwiera zakładkę Główna; Wstecz → menu (model zakładek menu
   bocznego — zgodne z projektem, nie błąd).
9. [ ] Szukaj Niuni: klawiatura ekranowa 6×N bez skrótu do wyników — z klawisza „a” do Polecanych trzeba wielu ►;
   do oceny w pętli 2.
10. [ ] Kalendarz / Dodatki / Kolejność katalogów: Wstecz z pozycji wraca na „Home” zamiast na element, z którego weszło (ekrany bazy).

### Strona tytułu
11. [B] Karty „Zwiastun N”: każda odtwarzała ten sam zwiastun z TMDB (wybrany film YouTube ignorowany) i przez kilka sekund
    nic nie było widać → odtwarza wybrany zwiastun, od razu komunikat „Wczytuję zwiastun…”.
12. [B] „Więcej” przy opisie: po rozwinięciu nadal „Więcej” → „Mniej” (opis „Zwiń opis”).
13. [B] Pigułki sezonów: długie OK = cały sezon obejrzany / nieobejrzany (jak w Nuvio), z potwierdzeniem.
14. [i] „Odtwórz” bez dodatków ze źródłami: tylko krótki komunikat systemowy (Toast) — działa, ale łatwo przeoczyć.

### Live TV
15. [P] Pusty Live TV odsyłał do „Ustawienia → Telewizja”, której w Niuni nie ma (brak jakiegokolwiek wejścia do źródeł
    Live TV w ustawieniach Niuni na poziomie Prostym) → przycisk „Dodaj listę kanałów” na pustym ekranie i pozycja
    „Telewizja → Źródła kanałów i EPG” w Ustawieniach Niuni (okno źródeł Nuvio z QR).
16. [B] „Szukaj w telewizji” (fork_tv_search): po otwarciu fokus na polu „Szukaj kanału lub programu” (pętla 3).

### YouTube (OneTube — kod obcy, bez zmian wg ZASADY)
17. [i] Bez logowania: ekran „Home” pusty, ▼ z zakładki nie dochodzi do przycisku „Sign in” pod komunikatem; teksty po angielsku.
18. [i] Wczytanie 7–8 s; po otwarciu brak fokusu na treści.

### Wydajność (emulator, pomiar przez uiautomator — orientacyjny)
19. [ ] Czas do stabilnego ekranu Głównej rósł w trakcie sesji: 4,7–5,2 s na początku, 10–12 s po 20 min (PSS aplikacji 396 MB,
    sterta Java 60 MB — bez wycieku; prawdopodobnie przeciążony Mac/emulator) — sprawdzić w pętli 2 od świeżego startu.
20. [i] Wygląd (layout_settings) 7,3 s i brak fokusu po otwarciu; Profile 6,1 s.

Pętla 1: 50 ścieżek, 20,9 min (+ ok. 6 min ręcznie): OK 18, UWAGA 25, BŁĄD 7 (`plan/ux/cykl1/sciezki.md`).
Część BŁĘDÓW to ograniczenia skryptu (Toast niewidoczny dla uiautomatora, opis rozwijany bez zmiany tekstów) — poprawione w skrypcie.

## Pętla 2 (22:15–, APK ux2 = commit 5ded5239f, arm64)
- Ręcznie: pusty Live TV → „Dodaj listę kanałów” (fokus od razu na przycisku) → okno źródeł → M3U
  `https://iptv-org.github.io/iptv/countries/pl.m3u` (darmowa lista iptv-org) → 188 kanałów; długie OK na wierszu kanału →
  „ulubione dodane: 4 Fun Dance” + komunikat, puszczenie nie włącza kanału; filtr „Ulubione” pokazuje kanał; OK → odtwarzanie (PLAYING).
- 21. [P] Lista bez przewodnika: nad kanałami „Nic tu jeszcze nie ma.” → podpowiedź, że kanały nie mają programu i gdzie dodać EPG.
- 22. [i] Okno źródeł Live TV (Reshaped) po angielsku — kod obcy, bez tłumaczeń (ZASADY).
- 23. [i] Mac przeciążony w czasie pętli 2 (średnie obciążenie 8–9, systemowy `suggestd` 93% CPU) — czasy wczytania YouTube
  (11–39 s) i Kalendarza nie są miarodajne.
- Automatycznie: 50 ścieżek, 44,3 min (Mac przeciążony — ścieżki YouTube po 3–10 min): OK 23, UWAGA 25, BŁĄD 2
  (pętla 1: OK 18 / BŁĄD 7). Szukaj: powrót fokusu na wynik i długie OK — OK (ścieżki 11–13). Live TV: OK, długie OK, przewijanie — OK.
- Ręcznie potwierdzone [B]: menu „Akcje tytułu” na plakacie Głównej i w Bibliotece (stan „Dodaj/Usuń z biblioteki” poprawny),
  puszczenie OK po przytrzymaniu nie wybiera opcji, Wstecz z menu → fokus na tym samym plakacie, następne krótkie OK od razu działa;
  dodanie do Biblioteki z menu; gatunki po polsku („Biograficzny”, „Dramat”); odtwarzacz Live TV zwalniany po Wstecz (ExoPlayer Init → Release).
- 24. [P] Live TV: po Wstecz z kanału log „fokus wraca na kanał”, ale chwilę później fokus na „Szukaj” u góry → ponowienie
  fokusu po 0,6 i 1,5 s, jeśli użytkownik nic nie nacisnął.
- 25. [i] „Odśwież dodatki”, „Zmień kolejność” (ekrany bazy): brak widocznej zmiany po OK w odczycie uiautomatora — do sprawdzenia na TV.
- 26. [i] Ekran błędu kanału („Kanał nie odpowiada”, „Spróbuj ponownie” / „Następny kanał”) — czytelny; Wstecz wychodzi
  (przy przeciążeniu emulatora z opóźnieniem ~1 s, JankStats 1,2 s).

## Pętla 3 (22.11 23:25 – 23:51, APK ux3, arm64)
- Ręcznie [B]: podpowiedź EPG, powrót fokusu na kanał po Wstecz, „Źródła kanałów i EPG” i „Szukaj w telewizji” w Ustawieniach Niuni;
  Lupin: karta „Zwiastun 1” → komunikat, zwiastun gra po ~3 s (log `zwiastun znaleziony (wybrany)`), Wstecz → fokus na karcie;
  „Więcej” → „Mniej” („Zwiń opis”) → „Więcej”; długie OK na „Sezon 1” → „sezon 1 obejrzany (5 odc.)”, drugie → „nieobejrzany”.
- Automatycznie (`cykl3/sciezki.md`, 18 ścieżek, 15,4 min): OK 0 / UWAGA 18 / BŁĄD 0 — prawie wszystkie UWAGI to czas wczytania
  na emulatorze (zimny start 30 s, Główna 10–21 s, Live TV 8–12 s; Mac obciążony 5–6) — niemiarodajne, porównać na JMGO.
- 27. [P] Okno „Kanał nie odpowiada”: pierwszy Wstecz nic nie robił (klawisze najpierw dostaje odtwarzacz, okno nie widziało
  Wstecz) → Wstecz od razu zamyka okno i wraca do listy kanałów; OK i strzałki działają na przyciskach okna (`ForgeChannelFailed.onKey`).
- 28. [P] Strona tytułu (ścieżka 19): po Wstecz ze strony osoby (Obsada) albo tytułu z „Podobne” fokus wracał na „Odtwórz” →
  wraca na tę samą osobę/plakat.
- 29. [P] Długie OK na odcinku od razu odtwarzało → okno opcji odcinka z Nuvio (Odtwórz, obejrzany/nieobejrzany, cały sezon,
  poprzednie odcinki); po zamknięciu fokus wraca na odcinek.
- 30. [P] Live TV (ścieżka 29): ▲ z pierwszego kanału trafiał w kategorię najbliżej środka („Informacje”) → na ostatnio
  wybraną pigułkę (na początku „Wszystkie”).
- 31. [i] Odcinek obejrzany = pełny pasek postępu na kadrze (bez napisu) — czytelne na TV; uiautomator tego nie widzi.
- 32. [i] Ścieżki 13, 20, 21: ograniczenia skryptu (klawisz „c” zamiast plakatu, tytuł bez „Podobne”) — nie błędy aplikacji.
