# B10 Polecenia według nastroju (budżet 4 h + 4 h)

Cel: rzędy „Lekkie”, „Wymagające”, „Nieodkryte perełki” dopasowane do profilu. Priorytetem jest jakość (DECYZJE #14, #24): baza na regułach działa zawsze, a model po API poprawia ranking, gdy użytkownik podał klucz.

Moduł: `J/fork/picks` (`J` = `app/src/main/java/com/nuvio/tv`). Rzędy przez `homeRows()` rejestru.

## Zadania

- [ ] **B10-0 Ocena modułu Scout z Nuvio Arc.** Arc ma moduł poleceń „Scout”: profil gustu („your taste”), wybory szybkie i słowne, rzędy (wybrane, gatunek, „bo obejrzałeś”, perełki, kafle, miksy) oraz pytanie głosem lub tekstem (`ui/screens/scout/ask`: `MicButton`, `QuestionField`, `rememberAskVoice`). Znajdź źródła: `../upstream/arc`, a gdy brak, gałąź `wip-all` (uwaga: w archiwum są tylko odwołania z ustawień, sam pakiet `scout` może nie być skopiowany). Napisz `plan/raporty/scout.md`: jak powstaje profil gustu, skąd kandydaci, czy i jaki model jest używany (lokalny czy API), ile plików rdzenia dotyka, jakość rzędu perełek dla profilu „kino niezależne”. Decyzja: przenieść Scout jako moduł (z adapterami, wyłączalny) i pominąć zadania B10-1 do B10-4 w częściach, które pokrywa, albo budować własny moduł. Gdy źródeł Scout nie ma w środowisku: wpis w `BLOKADY.md` i budowa własnego modułu od B10-1.
  Ukończone, gdy: raport i decyzja zapisana w `DECYZJE.md`.

- [ ] **B10-1 Profil gustu.** Czysta logika, osobno dla profilu. Wejście: historia oglądania aplikacji (obejrzane do końca = plus; porzucone przed 15 minutami = minus), kciuki, ankieta. Wyjście: wagi gatunków i słów kluczowych TMDB, preferowane dekady, trzy suwaki (znane ↔ nieodkryte, lekkie ↔ wymagające, nowe ↔ klasyka), lista „nigdy”.
  Ukończone, gdy: testy JVM: dwie różne historie dają różne wagi; porzucenie obniża wagę gatunku.

- [ ] **B10-2 Kandydaci.** Z TMDB: `discover` po gatunkach i słowach kluczowych profilu, „podobne” do 10 najlepiej ocenionych przez profil tytułów. Do 400 kandydatów, pamięć podręczna 24 h. Odsiew: już obejrzane, lista „nigdy”, wiek profilu.
  Ukończone, gdy: test JVM odsiewu na atrapie API.

- [ ] **B10-3 Punktacja i nastroje.** Punkt = podobieństwo do profilu × waga nastroju. Nastroje jako zestawy wag w `assets/fork/moods.json`:
  - Lekkie na dziś: komedia, familijny, przygodowy; czas do 110 min; bez ciężkich słów kluczowych
  - Wymagające: dramat, dokument, kino autorskie; wysoka ocena; dowolna długość
  - Nieodkryte perełki: ocena co najmniej 7,0 przy liczbie głosów w przedziale 150–5000; premia za kino niezależne i festiwalowe (słowa kluczowe), kara za popularność
  - Trzyma w napięciu: kryminał, thriller, sensacja
  - Na 90 minut: czas 75–100 min
  Suwak „znane ↔ nieodkryte” przesuwa limit popularności we wszystkich rzędach.
  Ukończone, gdy: testy JVM: film masowy nie trafia do „perełek”; profil „łatwe kryminały” i profil „kino niezależne” dostają rozłączne pierwsze dziesiątki na tej samej puli kandydatów.

- [ ] **B10-3b Punktacja na wektorach (domyślna, gdy jest paczka z B06b).** Wektor gustu profilu = średnia wektorów tytułów obejrzanych do końca i ocenionych kciukiem w górę minus 0,5 średniej porzuconych. Jest to arytmetyka na gotowej paczce, bez uruchamiania modelu. Wektory nastrojów przychodzą w paczce (opisy nastrojów po polsku, liczone poza urządzeniem). Punkt tytułu = podobieństwo do gustu × podobieństwo do nastroju × waga cech z reguł (limit popularności dla „perełek”, czas trwania, lista „nigdy”). Różnorodność: nie więcej niż 3 tytuły o podobieństwie wzajemnym powyżej 0,9 w jednym rzędzie. Prośba swobodna („coś jak Fargo, ale lżejsze”) to jedyny przypadek, w którym model liczy wektor na urządzeniu. Bez paczki działa punktacja z B10-3.
  Ukończone, gdy: testy JVM na małej sztucznej paczce: profil „kino niezależne” i profil „łatwe kryminały” dostają rozłączne pierwsze dziesiątki; film masowy nie trafia do „perełek”; reguła różnorodności działa.

- [ ] **B10-4 Rzędy na ekranie głównym.** Pięć rzędów nastrojów, po 20 tytułów, odświeżane raz na dobę w tle (poza odtwarzaniem). Pod kartą krótki powód z danych, bez modelu: „bo obejrzałeś X” albo „mało znany, ocena 7,8”.
  Ukończone, gdy: test JVM generowania powodu; rzędy zgłoszone w rejestrze.

- [ ] **B10-5 Filtr „zagra po polsku”.** Dla profilu „Zawsze po polsku” (B04): dla pierwszych 30 tytułów każdego rzędu sprawdź w tle, czy dodatki zwracają źródło pasujące do reguł polskiego dźwięku; tytuły bez takiego źródła spadają na koniec. Najwyżej 60 sprawdzeń na dobę, wynik pamiętany 3 dni.
  Ukończone, gdy: test JVM kolejności po filtrze i limitu sprawdzeń.

- [ ] **B10-6 Ankieta i „Na co masz ochotę?”.** Ankieta (raz, do pominięcia): wybierz 5 z 20 plakatów, trzy suwaki, lista „nigdy”. Ekran „Na co masz ochotę?”: sześć kafli nastrojów i wynik jako jedna lista. Polecenie głosowe „włącz coś lekkiego” (jeśli B07 jest) otwiera ten ekran z wybranym nastrojem.
  Ukończone, gdy: test JVM zapisu ankiety do profilu gustu.

- [ ] **B10-8 Ranking przez model po API.** Gdy użytkownik podał klucz (ten sam `LlmClient` co w B07-8): model dostaje 60 najlepszych kandydatów z punktacji regułowej, opis profilu gustu (wagi i suwaki, 15 tytułów lubianych i 5 porzuconych) oraz nastrój, i zwraca kolejność 20 tytułów z jednym zdaniem „dlaczego dla Ciebie”. Każdy zwrócony tytuł musi mieć identyfikator z listy kandydatów; inne są odrzucane. Raz na dobę na profil i nastrój, wynik w pamięci podręcznej. Bez klucza rzędy zostają na regułach. W ustawieniach jedno zdanie o tym, co jest wysyłane.
  Ukończone, gdy: test JVM na atrapie klienta (odrzucenie tytułu spoza listy, powrót do reguł przy błędzie).

- [ ] **B10-7 Ustawienia.** ESSENTIAL: które rzędy pokazywać, suwaki, powtórz ankietę. ADVANCED: wyczyść profil gustu.
  Ukończone, gdy: kompiluje się.
