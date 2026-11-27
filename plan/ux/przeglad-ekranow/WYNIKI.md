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

## Pętla 2 (27.11, 15:00–15:40, buildy prz06 i prz07; paczka 8 napisana, niezbudowana)
W tej pętli powstało 8 poprawek aplikacji (paczka 7: 4, paczka 8: 4), a na JMGO sprawdzono 11 punktów: 9 działa, 2 nie (22 pkt 2, nowy błąd okna „Szukaj w YouTube” — poprawiony w paczce 8).

### Nowe od właściciela (paczka 7, build prz07)
- **„Odkrywaj” u góry obok zegara** na ekranie Szukaj — ✅ TV: ▲ z „abc” wchodzi na przycisk, OK otwiera Odkrywaj, Wstecz wraca na przycisk.
- **Napisy wbudowane przed plikami z dodatków** (w każdym języku; gdy wbudowanych polskich brak — nadal polskie z dodatku). Niesprawdzone na TV: „The Brutalist” nie ma wbudowanych polskich.
- **Okładki Live TV z wtyczki AIOMetadata** (ElfHosted z profilu) zamiast TMDB — działają, ale AIOMetadata zna mniej polskich tytułów: 35 okładek na 106 tytułów (33 %), wcześniej TMDB 66 na 127 (52 %). Np. brak „Piotruś Królik”, „Gry małżeńskie”, „Po sąsiedzku”. „Hell's Kitchen” dostawał okładkę FILMU (wtyczka podaje tylko polską nazwę serialu) — poprawione w paczce 8.

### Działa na TV
02 nagrody w lewej kolumnie (prz06) · plakaty: rozmycia wczytywane z dysku po restarcie (prz06) · 11 podgląd kanału 520×288 bez paska obrazu nad osią godzin · 21 różne numery kanałów w filtrze „Hiszpański” · 24 fokus na 1. wyniku po wyszukaniu · CAŁA APLIKACJA krok 1: podpisy kafli Live TV na rozmytym dole obrazu · 23 pkt 1a fokus na 1. filmie YouTube.

### Nie działa / niesprawdzone
- ❌ 22 pkt 2: karty „Poprzednie odcinki” w odtwarzaczu IPTV puste — przyczyną jest brak tytułu w AIOMetadata (kod pyta poprawnie).
- ❌ nowy: w oknie „Szukaj w YouTube” ▶ z „f” (prawy brzeg klawiatury) przenosił fokus na ekran POD oknem — okno trzyma teraz fokus (paczka 8).
- 06 (fokus po dołączeniu polskiego dźwięku przy otwartym panelu): niesprawdzone — „The Social Reckoning” raz dołączył dźwięk sam przy schowanym panelu, potem 2× „synchronizacja niepotwierdzona” i przełączenie na inne wydanie.
- Dalej niesprawdzone: 05 ✓ jakości, 23 Mix, pusta playlista YouTube, OK na „Anuluj”, stos serialu, „Kropka nad i”.

### Paczka 8 (w kodzie, czeka na build)
10 pkt 2 kafle Live TV przez wspólny komponent `NiuniaMediaCard` (NowCard / ChannelCard) · okładki AIOMetadata: dopasowanie części polskiej nazwy (+ test JVM) · okno „Szukaj w YouTube” trzyma fokus.

### Narzędzia
- Logi forka znikały z logcat: pełny bufor JMGO (16 MB) przycina najbardziej gadatliwą aplikację. Bufor powiększony do 32 MB; biała lista logd wyłączała logi zamiast je chronić — cofnięta (DECYZJE).

### Zmiany na profilu właściciela (JMGO)
- Obejrzane fragmenty: „The Social Reckoning” (~7 min w dwóch podejściach, raz z polskim dźwiękiem z innego wydania), „The Brutalist” (~1 min), kanał BBC CBeebies (archiwum, pauza).
- „Kraj i język” na chwilę „Hiszpański”, przywrócony na „Polska”. Wyszukanie „jazz” z historii YouTube (bez nowych wpisów).
