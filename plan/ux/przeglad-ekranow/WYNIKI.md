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

## Pętla 3 (27.11, 15:40–16:05, build prz08; paczka 9 napisana, niezbudowana)
W tej pętli powstały 4 poprawki aplikacji (paczka 8: 3, paczka 9: 1), a na JMGO sprawdzono 9 punktów (prz07 i prz08) — wszystkie działają.

### Działa na TV
- **10 pkt 2 — kafle Live TV przez wspólny komponent kart `NiuniaMediaCard`** (prz08): przewodnik, Szukaj Live TV i Odkrywaj wyglądają jak wcześniej (okładka albo logo, podpis w 2 liniach na rozmyciu, pasek postępu, obwódka fokusu). 10 pkt 1: rząd „Seriale” bez strzępów logo pod kartami.
- **Okładki AIOMetadata**: „Hell's Kitchen” dostaje teraz okładkę SERIALU (wcześniej filmu) — dopasowanie części polskiej nazwy.
- **Okno „Szukaj w YouTube”** trzyma fokus: ▶ z prawego brzegu i ▲ z górnego rzędu zostają w oknie (prz08).
- 05 (prz07): ✓ „Teraz” przy bieżącej jakości (4K); pkt 3 — wybór 1080p = 2 próby w 51 s (pierwsza czekała 40 s na dane), dawniej 6 prób w 63 s.
- 26 (prz07): OK na „Anuluj” na ekranie wczytywania wraca na Główną z fokusem na tej samej karcie.

### Paczka 9 (w kodzie, czeka na build)
CAŁA APLIKACJA krok 2: pasek podpisu zwykłych kart (Oglądaj dalej, odcinki, zwiastuny) na rozmytym dole obrazu zamiast szarego gradientu (ten sam przełącznik co podpisy Live TV).

### Dalej niesprawdzone / do zrobienia
- 22 pkt 2 (puste karty archiwum — AIOMetadata nie zna tytułu), 06 (fokus po dołączeniu polskiej ścieżki), 23 Mix (brak Mixu na ekranie), napisy wbudowane (potrzebny film z wbudowanymi polskimi), „Kropka nad i”.
- 23 pkt 2 Mix „Następne” wymaga zmiany w kopii modułu YouTube z Nuvio One — zapisane w przekazaniu.

### Zmiany na profilu właściciela (JMGO)
- „The Brutalist”: ~2 min (zmiana jakości 4K → 1080p, źródło przełączone na 1080p), jedno wejście anulowane na ekranie wczytywania.
- Szukaj Live TV: wpisane „tvn” i wyczyszczone (historia bez zmian).

## Pętla 4 (27.11, 16:05–16:45, build prz09 = paczki 9–12)
W tej pętli powstały 4 paczki poprawek aplikacji (9–12: 8 miejsc z szarym przyciemnieniem zamienionych na rozmycie + podgląd filmu YouTube z dźwiękiem), a na JMGO sprawdzono 10 z nich — wszystkie działają; jedna uwaga o płynności idzie do następnej paczki.

### Działa na TV (prz09)
- **Szare przyciemnienia → rozmyta kopia tego samego obrazu** (liczona raz, potem z dysku):
  - Główna: lewa strona hero rozmyta (tekst czytelny, bez szarej plamy) — zrzut `p9-glowna-hero.png`;
  - Szczegóły: lewa strona i dół hero na rozmyciu — `p9-szczegoly.png`;
  - karta tytułu (OK na plakacie w rzędzie): dół karty w rozmyciu — `p9-karta-tytulu.png`;
  - strona odcinka serialu: lewa strona na rozmyciu — `p9-strona-odcinka.png`;
  - ekran wczytywania filmu: rozmyty obraz zamiast ostrego pod ciemną warstwą — `p9-wczytywanie.png`;
  - menu boczne: pod panelem rozmyta kopia tła ekranu — `p9-menu.png`;
  - podpisy kart „Oglądaj dalej”, odcinków i zwiastunów na rozmytym dole obrazu — `p9-glowna-cw.png`, `p9-odcinki.png`.
- Przewijanie rzędów Głównej z rozmytymi podpisami: p95 32 ms (budżet ≤ 34 ms; dawny pomiar tej ścieżki p95 93 ms).
- **YouTube 23 pkt 1b**: zaznaczony film po ~3 s sam gra w swojej karcie z dźwiękiem (także pierwszy po wejściu na ekran); przejście na inną kartę i Wstecz od razu wyciszają; podgląd sam gaśnie po 30 s — `p9-yt-podglad.png`.

### Do poprawy (następna paczka)
- Przełączanie tytułu na hero Głównej z rozmyciem: p50 12 ms / p95 40 ms, bez rozmycia 10 / 30 ms (porównanie po świeżym starcie). Poprawka: przejście w rozmycie „wpieczone” w mały rozmyty obraz zamiast osobnej warstwy na cały ekran.
- Szczegóły: rozmycie od dołu zaczyna się wysoko (30 % ekranu) i zmiękcza twarze w środku kadru — przesunę niżej.
- Zostały szare tła: panel menu bocznego i ramka opisu zaznaczonego odcinka.

### Niesprawdzone / zablokowane
- Ekran źródeł (pod „Inne źródło” film ruszył szybciej, niż zdążyłem wybrać), okno Niuni (okno akcji z „Więcej podobnych” pochodzi z bazy NuvioTV — tła nie zmieniam), kafle archiwum pod odtwarzaczem IPTV (rząd kanałów nie ma obrazów).
- **23 pkt 2 „Następne” z Mixu** → `plan/BLOKADY.md`: wymaga zmiany kopii modułu YouTube z Nuvio One i nowej funkcji „następny film” w odtwarzaczu (YouTube w forku po końcu filmu nigdzie nie przechodzi). OK na Mixie odtwarza pierwszy film.
- Bez zmian: 22 pkt 2 i okładki AIOMetadata czekają na sprawdzenie konfiguracji wtyczki przez właściciela.

### Zmiany na profilu właściciela (JMGO)
- „Guarding Stars”: wznowione na ~30 s (zostało 94 → 93 min).
- Live TV: krótko 13 Ulica i Active Family (mogą być w „Oglądaj dalej” Live TV).
- YouTube: dwa podglądy w kartach na Głównej YouTube (bez otwierania filmów).
- Ustawienie forka `niunia_blur_scrims` na chwilę wyłączone do pomiaru i włączone z powrotem.


## Pętla 5 (27.11, 16:50–17:40, build prz10 = paczki 13–14)
W tej pętli powstały 3 paczki poprawek aplikacji (13–15: rozmycia tylko pod drobnym tekstem w 9 miejscach, płynniejsze przejścia, pasek postępu na kartach IPTV), a na JMGO sprawdzono 9 miejsc z paczek 13–14 — wszystkie działają; paczka 15 (pasek postępu) jest w buildzie prz11, sprawdzę ją w pętli 6.

Twoje polecenie (16:47): „Rozmycie po lewej na hero na home usuń. Ma być zwykłe zdjęcie hero pod spodem bez żadnych efektów. Rozmycia przykrywają za dużo powierzchni zdjęć wszędzie. Ustaw absolutnie minimalne powierzchnie niezbędne dla czytelności małych napisów.”

### Działa na TV (prz10)
- **Główna:** hero to zwykłe zdjęcie — bez rozmycia i bez przyciemnienia — `p10-glowna-hero.png`.
- **Rozmycie tylko „łatą” pod blokiem drobnego tekstu**, reszta zdjęcia ostra:
  - karta tytułu (OK na plakacie): łata pod gatunkami, opisem i datą — `p10-karta-tytulu.png`;
  - Szczegóły: bez rozmycia z lewej i od dołu; łata pod gatunkami, opisem i datą oraz pod obsadą po prawej — `p10-szczegoly.png`;
  - strona odcinka: zwykłe zdjęcie, łata pod gatunkami, opisem, datą i oceną — `p10-strona-odcinka.png`;
  - ekran wczytywania filmu: ostre zdjęcie, łata tylko pod „Buforuję…” i linijkami o pliku — `p10-wczytywanie.png`.
- **Podpisy kart:** rozmyty jest tylko wąski pas pod samym tekstem (było: dolne 45–55 % karty) — „Oglądaj dalej” (`p10-cw-zoom.png`), odcinki serialu, kafle Live TV (`p10-livetv-zoom.png`: twarze nad podpisem ostre).
- **Menu boczne:** rozmyty i przyciemniony jest tylko sam panel (koniec szarego tła panelu); reszta ekranu ostra, bez rozmycia lewej połowy — `p10-menu.png`.
- **Szczegóły serialu:** pod opisem zaznaczonego odcinka nie ma już szarego pola — `p10-odcinki.png`.
- Płynność: przełączanie tytułu na hero Głównej p50 22 ms / p95 34 ms (budżet ≤ 34 ms; w prz09 z rozmyciem p95 40 ms).

### Do poprawy / w buildzie prz11
- **Pasek postępu na kartach IPTV** (Twoja uwaga ~17:20): wraca mały pasek (jak dawniej), z lewej przy linii tekstu, ale nad tekstem, nie obok niego. To samo na kaflach archiwum pod odtwarzaczem IPTV (tam pasek był jeszcze w jednym wierszu z podpisem).

### Do Twojej oceny
- Na jasnych zdjęciach hero Głównej (np. śnieg w „Guarding Stars”) opis i tytuł rzędu „Oglądaj dalej” są słabo czytelne — tak jak kazałeś, na Głównej nie ma żadnego efektu. Jeśli chcesz, mogę dać tam taką samą małą łatę pod opisem jak w Szczegółach.
- Świadomie bez zmian: tło strony pod hero (rzędy), ekran źródeł i okna — tam całe tło jest tłem listy lub okna, a nie zdjęciem do oglądania.

### Niesprawdzone / zablokowane
- Ekran błędu odtwarzacza (łata jak przy wczytywaniu) i plakaty z logo (rozmycie od 76 % zamiast od 66 %) — nie trafiły się w teście.
- Bez zmian z pętli 4: ekran źródeł, okno Niuni, kafle archiwum pod odtwarzaczem IPTV; 23 pkt 2 w `plan/BLOKADY.md`.
- AIOMetadata (mało okładek): osobny wątek sprawdza teraz, co jest źle ustawione we wtyczce — raport w `plan/raporty/aiometadata.md`.

### Zmiany na profilu właściciela (JMGO)
- „Ciało” S1 E1: wejście w odtwarzanie anulowane na ekranie wczytywania po ~2,5 s (pozycja bez zmian: zostało 38 min).
- Menu boczne otwierane i zamykane kilka razy; na Głównej przełączone 10 wyróżnionych tytułów.
