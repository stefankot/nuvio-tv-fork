# W5: napisy wbudowane z innego pliku (18.11.2026)

## Czy Kicia to miała?
Nie. Sprawdzone w kodzie Kicii 34c80c6 i całej historii (`git log --all --grep napis`, `git grep` po audiosync/autosync). Najbliższe rzeczy, które Kicia ma (i fork też ma, z Reshaped):
- `SubtitleCandidatePool`: spośród napisów Z DODATKÓW w tym samym języku wybiera plik, który pasuje do dźwięku (inna wersja, inna liczba klatek).
- `EmbeddedSubtitleTimelineLoader`: czyta z pliku MKV/MP4 (zapytaniami o fragmenty) tylko CZASY napisów wbudowanych, jako wzorzec do AutoSync.
- Decyzja właściciela z 13.11 w Kicii: „dopasowanie napisów do wybranego pliku” — wstępne dobranie napisów z dodatków, nie z innych plików.

Pamiętana funkcja to najpewniej połączenie tych dwóch: AutoSync dopasowuje napisy z dodatku do najlepszego pliku.

## Projekt (jeśli właściciel chce prawdziwe przenoszenie)
1. Gdy wybrany plik nie ma polskich napisów wbudowanych, a inne źródło tego tytułu ma je w nazwie (`ReleaseInfo.subtitleLanguages`, KE `ReleaseFacts`) i jest MKV z adresem HTTP:
2. Rozszerzyć `EmbeddedSubtitleTimelineLoader` (dziś czyta Cues → czasy) o odczyt treści: dla każdego wpisu Cues ścieżki napisów pobrać blok (pozycja klastra + pozycja względna) i wyjąć tekst (SRT/ASS) — dziesiątki do setek małych zapytań, w tle, z limitem.
3. Złożyć plik SRT w pamięci i podać odtwarzaczowi jako napisy zewnętrzne (ta sama ścieżka co napisy z dodatków).
4. Uruchomić AutoSync (napisy z innego cięcia/liczby klatek) — już działa dla napisów zewnętrznych.
Koszt: nowy parser bloków Matroska (~300–500 linii), test wymaga prawdziwego pliku z polskimi napisami. Proponuję jako osobny etap po kompilacji.
