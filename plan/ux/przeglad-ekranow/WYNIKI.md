# Wyniki — przegląd ekranów 27.11 (pętle kod → build → testy na JMGO)

## Pętla 1 (27.11, 13:20–14:40): buildy prz03, prz04 (testy), prz05 (w toku)

W tej pętli powstało 18 poprawek aplikacji (paczki 3–5), a na JMGO sprawdzono 27 punktów przeglądu (paczki 1–4): 23 działają, 4 wymagały poprawki — poprawki są w buildzie prz05.

### Najważniejsze: „The Social Reckoning” i polski dźwięk (Dublagem)
- Plik Comet 1080p (TorBox) miał tylko ścieżkę angielską (opis źródła mylnie mówił „polski dźwięk”).
- Dublagem sam znalazł wydanie z polskim dźwiękiem (18 kandydatów, 1 sprawdzony, 17 pominiętych jako „not_probed”), zsynchronizował dźwięk (przesunięcie 0 ms) i dołączył go do TEGO SAMEGO obrazu — bez przełączania źródła. Na ekranie: „Dołączono polski dźwięk z innego wydania”. W panelu „Dźwięk”: „polski 5.1 ✓”, „angielski 5.1”.
- Błąd znaleziony przy okazji: po dołączeniu ścieżki przy otwartym panelu znikał fokus (pilot nic nie zaznaczał, tylko Wstecz) — poprawione w prz05.

### Działa na TV (prz04)
01 karta tytułu · 03 kontrola długości pliku (log) · 04 lista napisów (polskie na górze, „Inne języki”, ▼ do końca) · 05 panel „Jakość” Niuni + długie OK = źródła · 06 panel „Dźwięk” (prosta lista) · 07 zwiastuny jak karty Głównej · 08 „O filmie” · 10 okładki programów z TMDB · 12 czyste nazwy kanałów · 13 pasek postępu nad podpisem · 14 kategorie bez OK, polskie pierwsze · 15/16 kraje i języki (arabskie ≠ Argentyna, złączone kopie jakości) · 17 Szukaj Live TV (siatka, historia, usuwanie długim OK) · 18 rząd archiwum · 19 logo kanału · 20 Ulubione/Oglądaj dalej bez filtra · 22 archiwum (bez „Na żywo”, bez zmiany kanału, ◀▶ przewija) · 23 fokus na 1. filmie YouTube · 24 wyszukiwarka YouTube · 25 „Przeglądaj” = pigułki · 26 ekran wczytywania (◀▶, parametry pliku) · 27 hero bez szarego gradientu · plakaty bez czarnego gradientu · brak „Gotowe”.

### Wymagały poprawki (w prz05)
- 11 podgląd kanału: po powiększeniu obraz rysował się nad gradientem (przycięcie pola) — bez przycinania, wideo 16:9 kończy się w strefie pełnego tła.
- 21 numery kanałów: w filtrze „Hiszpański” wszystkie kanały miały „42” — klucz numeru uzupełniony o adres.
- 22 pkt 2: puste karty odcinków w rzędzie archiwum — okładki TMDB.
- 24: po wyszukaniu fokus na szynie zamiast na pierwszym wyniku (efekt anulował sam siebie).
- Też: panel „Jakość” nie wiedział, co gra (brak ✓ przy 1080p) — teraz z nazwy źródła; okładka filmu dla programu informacyjnego („Kropka nad i”) — filmy tylko dla programów ≥ 70 min.

### Niesprawdzone / do zrobienia
- Niesprawdzone: 23 Mix (brak Mixu na ekranie), pusta playlista YouTube, OK na „Anuluj” przy wczytywaniu, 05 pkt 3, stos serialu (10 pkt 1).
- Nie robione: 02 (nagrody po lewej — potrzebny projekt), 10 pkt 2 (kafle na MediaCard), CAŁA APLIKACJA (szare gradienty → rozmycie), plakaty pkt b (rozmycia na dysku), 23 pkt 1b (podgląd z dźwiękiem), 16/22 drobne.
- Obserwacje: wyniki wyszukiwarki filmów bez miniatur i z mało trafnymi wynikami („reckoning”); komunikat „Source: …” po angielsku w odtwarzaczu IPTV; nagłówek „Results for …” w YouTube po angielsku.

### Build i narzędzia
- prz02b padł na braku pamięci kompilatora (3 GB); od prz03 build z 4 GB (`-Dorg.gradle.jvmargs`), APK w ~9–10 min. Zasada właściciela: stan buildu co minutę, pierwszy błąd = od razu poprawka (ZASADY-build).
- Testy JVM `com.nuvio.tv.fork.tv.*`: 158, wszystkie zielone w prz04 (poprawione 4 przestarzałe testy i błąd „TVNHD”).

### Zmiany na profilu właściciela (JMGO)
- Obejrzane fragmenty: „The Social Reckoning” (~8 min, z „Oglądaj dalej”), „Odyseja” (wznowienie ~1 min), kanały Live TV (Adventure, Ale Kino+, Animal Planet — archiwum).
- Wybór „Kraj i język” na chwilę „Hiszpański”, przywrócony na „Polska”.
- Historia Szukaj Live TV: dodane i usunięte „kuchnia” (bez zmian netto). Okładki programów zapisują się w `files/fork_tv_covers.json`.
