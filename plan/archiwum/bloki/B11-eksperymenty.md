# B11 Eksperymenty (budżet 4 h)

Cel: sprawdzić trzy rzeczy o niepewnym wyniku. Każde zadanie kończy się raportem w `plan/raporty/`, a kod trafia do aplikacji tylko, gdy raport jest pozytywny. Wszystko domyślnie wyłączone.

## Zadania

- [x] **B11-1 Pomiar jakości mowy po polsku.** Silniki mowy są wdrożone w B07-6. Tutaj tylko pomiar: zestaw 60 nagranych albo zsyntetyzowanych poleceń po polsku przepuść przez Whisper tiny, base i small; zapisz trafność po dopasowaniu encji i czas. Wynik decyduje o progach automatycznego wyboru silnika.
  Ukończone, gdy: raport `raporty/mowa-lokalna.md`; progi w B07-6 poprawione, jeśli pomiar je podważa.

- [x] **B11-2 Lokalny głos neuronowy dla lektora.** Sprawdź, czy biblioteka sherpa w wersji użytej w forku ma syntezę mowy i czy istnieje dla niej polski głos na licencji pozwalającej na użycie. Jeśli tak: druga implementacja `Lektor` z B09, model pobierany na żądanie. Jeśli nie: tylko raport.
  Ukończone, gdy: raport `raporty/glos-neuronowy.md` (dostępne głosy, licencje, rozmiary, szacowana szybkość); kod tylko przy wyniku pozytywnym.

- [x] **B11-3 Próba DubLift.** Tylko analiza i raport, bez integracji (DECYZJE #17). Sklonuj `https://github.com/joojoooo/DubLift` do katalogu poza repozytorium. Opisz w `raporty/dublift.md`:
  1. jak wybierane jest źródło dźwięku i co trzeba zmienić, żeby źródłem było gorsze wydanie z dodatków użytkownika oznaczone jako polskie;
  2. czy dopasowanie (`internal/dublift/alignment.go`) wymaga czystej ścieżki angielskiej po obu stronach, czy zadziała ze ścieżką z lektorem;
  3. czy jest korekta różnicy tempa (25 a 23,976 klatki); jeśli nie, gdzie ją dodać;
  4. co dzieje się z dźwiękiem 5.1;
  5. plan próby na 5 filmach, które musi dostarczyć właściciel (wpis w `BLOKADY.md` z prośbą o pliki).
  Uruchom testy jednostkowe DubLift, jeśli środowisko ma Go.
  Ukończone, gdy: raport istnieje i odpowiada na 5 punktów.

- [x] **B11-4 Tłumaczenie napisów bez chmury.** Sprawdź możliwości lokalne: tłumaczenie na urządzeniu przez bibliotekę systemową albo mały model pobierany na żądanie. Kryterium: polski tekst zrozumiały w dialogach, czas poniżej 2 minut na film na komputerze klasy telewizora. Jeśli żadna droga nie spełnia kryterium, zostaw raport i nie dodawaj kodu.
  Ukończone, gdy: raport `raporty/tlumaczenie-napisow.md`.

## Po bloku

To ostatni blok. Zaktualizuj `STATUS.md` pełną listą wdrożonych modułów, uporządkuj `DO_SPRAWDZENIA_NA_TV.md` według modułów i zapisz w `STAN.md`: „Plan zakończony”. Dopiero wtedy wolno zakończyć pracę.
