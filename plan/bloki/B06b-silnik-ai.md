# B06b Lokalny silnik znaczeń (budżet 4 h)

Cel: polecenia filmów i rozumienie mowy działają u każdego użytkownika od razu, bez klucza do żadnej usługi. Podstawą jest mały model osadzeń (embedding) uruchamiany na urządzeniu oraz paczka wektorów liczona poza urządzeniem. Model przez API pozostaje opcjonalnym ulepszeniem (DECYZJE #14).

Moduł: `J/fork/ai` (`J` = `app/src/main/java/com/nuvio/tv`). Używają go B07 (głos) i B10 (polecenia).

Założenia:

- Model: EmbeddingGemma 300M w wersji skwantyzowanej (wielojęzyczny, ponad 100 języków, poniżej 200 MB RAM według Google; wymiar 768 z możliwością obcięcia do 128). Tekst zapytania i teksty filmów muszą być liczone tym samym modelem.
- Najsłabsze urządzenie ma 2 GB RAM. Model nie jest ładowany podczas odtwarzania. Ładuje się na żądanie, liczy i jest zwalniany.
- Wektory filmów nie są liczone na telewizorze. Przychodzą jako gotowa paczka.
- Modelu nie dołączamy do APK. Pobieranie przy pierwszym użyciu, z repozytorium właściciela, tak jak modele mowy. Przed publikacją paczki sprawdź warunki licencji Gemma dotyczące redystrybucji i zapisz wynik w `plan/raporty/licencja-modelu.md`.

## Zadania

- [x] **B06b-1 Interfejs i matematyka.** `Embedder { suspend fun embed(teksty: List<String>): List<FloatArray> }`, obcięcie do 128 wymiarów z normalizacją, kwantyzacja wektorów do 8 bitów, podobieństwo kosinusowe, wyszukiwanie najbliższych w tablicy (liniowe; 50 000 wektorów po 128 bajtów). Czysty Kotlin.
  Ukończone, gdy: testy JVM: normalizacja, zgodność podobieństwa przed i po kwantyzacji w granicach 0,02, czas przeszukania 50 000 wektorów poniżej 50 ms w teście.

- [ ] **B06b-2 Uruchomienie modelu.** Najpierw sprawdź, czy biblioteka sherpa w forku udostępnia środowisko ONNX Runtime z poziomu Kotlina. Jeśli tak: wersja ONNX modelu. Jeśli nie: LiteRT (wariant `litert-community/embeddinggemma-300m`) jako jedna nowa zależność. Tokenizer z plików modelu. Ładowanie na żądanie, zwolnienie po 60 s bezczynności, blokada ładowania podczas odtwarzania i gdy wolnej pamięci jest mniej niż 400 MB. Gdy model jest niedostępny, `Embedder` zgłasza „niedostępne”, a moduły wracają do reguł.
  Ukończone, gdy: test JVM zarządcy cyklu życia na atrapie; pozycje na liście TV („czas pierwszego ładowania i czas jednego zapytania” na każdym z trzech urządzeń).

- [x] **B06b-3 Mniejszy model zapasowy.** Zarządca wybiera model według pamięci: przy 2 GB RAM mniejszy wielojęzyczny model osadzeń (kandydat: multilingual-e5-small skwantyzowany), ale tylko dla rozumienia mowy, gdzie wszystkie wektory liczone są na urządzeniu. Polecenia filmów zawsze używają modelu zgodnego z paczką.
  Ukończone, gdy: test JVM wyboru modelu; raport z rozmiarami w `plan/raporty/modele-osadzen.md`.

- [x] **B06b-4 Format paczki wektorów.** Plik binarny: nagłówek (nazwa i wersja modelu, wymiar, liczba pozycji), tablica identyfikatorów TMDB z typem, tablica wektorów 8-bitowych, tablica cech (rok, gatunki jako maska bitowa, ocena, liczba głosów, czas trwania). Odczyt przez mapowanie pliku w pamięć. Paczka z innym modelem niż zainstalowany jest odrzucana.
  Ukończone, gdy: testy JVM zapisu, odczytu i odrzucenia niezgodnej paczki.

- [x] **B06b-5 Budowanie paczki poza urządzeniem.** Skrypt `scripts/fork/build_vectors.py` i workflow `fork-vectors.yml` (ręczne uruchomienie i raz w tygodniu): pobiera z TMDB ok. 40 000 filmów i seriali (popularne, najwyżej oceniane, oraz mało znane z oceną co najmniej 6,5), składa tekst (tytuł, opis po polsku i angielsku, gatunki, słowa kluczowe), liczy wektory tym samym modelem, zapisuje paczkę i wektory nastrojów (B10) jako plik wydania. Klucz TMDB tylko jako sekret repozytorium. Aplikacja pobiera paczkę jak katalog KE (`J/livetv/source/PackStore.kt` jako wzór), najwyżej raz na tydzień.
  Ukończone, gdy: skrypt przechodzi test na 20 pozycjach z atrapą API; workflow jest poprawnym YAML-em. Brak sekretu TMDB w repozytorium: wpis w `BLOKADY.md`, a aplikacja działa bez paczki na regułach.

- [ ] **B06b-6 Rejestracja.** `AiModule : ForkModule` z wyłącznikiem. Ustawienia (ADVANCED): stan modelu i paczki, pobierz teraz, usuń. W poziomie Prostym nic nie widać; wszystko dzieje się w tle na Wi-Fi.
  Ukończone, gdy: wyłączony moduł niczego nie pobiera (test JVM).
