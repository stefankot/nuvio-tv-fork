---
name: buildy-zbiorczo-bez-bezczynnosci
description: "Zakaz builda dla pojedynczej poprawki i zakaz bezczynnego czekania; buduj zbiorczo, w czasie builda pisz kolejne funkcje"
metadata:
  node_type: memory
  type: feedback
  originSessionId: ae9b315d-81e3-45e2-a141-2359ada5fa53
  modified: 2026-11-17T11:37:30.419Z
---

Nie zlecaj kompilacji/testów dla jednej poprawki. Zbierz WSZYSTKIE aktualnie możliwe poprawki i funkcje, dopiero potem jeden build. Podczas builda i testów NIE czekaj bezczynnie: pisz kolejne funkcje i poprawki (do katalogu pośredniego `.stage/`, żeby nie zmieniać plików, które Gradle właśnie czyta), po builddzie wdrażaj je. Nie kończ tury, gdy w kolejce coś jest; kompilacja >10 min = błąd; raportuj co ok. 15 min, emulator tylko `-no-window` i testy tekstowe (uiautomator).

**Why:** użytkownik zauważył serię małych buildów (build dla jednego błędu) i długie bezczynne czekanie; wcześniej też przerwałem pracę mimo niepustej kolejki.

**How to apply:** przed każdym buildem zrób listę oczekujących poprawek i wprowadź wszystkie; build w tle + pętla pomiaru czasu z limitem 10 min; w tym czasie pisz kod w `.stage/` i krótkie raporty statusu. Powiązane: [[feedback_autonomia]], [[feedback_emulator_i_aplikacje]].
