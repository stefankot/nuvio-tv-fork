# Ocena modułu Scout z Nuvio Arc (B10-0)

Źródło: `upstream/arc` (commit przypięty w `fork-tools/pins.env`, ARC=343b275). Pakiet jest kompletny w klonie (nie tylko odwołania z ustawień).

## Rozmiar i sprzężenie
- 71 plików Kotlin, ok. 23 000 linii: `core/scout` (logika, `ask/`, `sync/`), `ui/screens/scout` (ekrany, `ask/`), `domain/model/Scout*.kt`, `data/local/Scout*Store.kt`, `ui/screens/settings/ScoutSettingsContent.kt`, `core/player/ScoutMixSession.kt`.
- Silnie związany z modelami aplikacji (`WatchedItem`, magazyny DataStore profilu, nawigacja, Hilt, ustawienia). Ponad 100 plików bazy ma odwołania do Scout w menu, nawigacji i ustawieniach.
- Przeniesienie 1:1 oznaczałoby kilkaset błędów kompilacji i kilkanaście punktów wpięcia w rdzeniu, a limit forka to 35 plików.

## Jak powstaje profil gustu (`ScoutTaste.kt`)
- Z historii oglądania i ocen: waga tytułu = waga oceny × (1 + ln(liczba obejrzeń)); fakty o tytułach (gatunki, słowa kluczowe TMDB, obsada, ekipa, kraje) pobierane z TMDB i trzymane na dysku na profil (`files/scout/taste_<profil>.json`).
- Z tego paski gatunków/słów/osób/krajów oraz „co ostatnio zyskuje”.

## Kandydaci i model
- Kandydaci z TMDB (`ScoutEngine`, `ScoutCatalogProvider`), rzędy: wybrane, gatunek, „bo obejrzałeś”, perełki, kafle, miksy.
- Model językowy tylko przez API w trybie „zapytaj” (`ask/AskConnectors.kt`: OpenAI, Claude, Gemini, Groq, inne; klucz w `AskKeyVault`). Brak lokalnego modelu osadzeń; bez klucza działa część regułowa.
- Perełki dla „kino niezależne”: reguły oparte na ocenie i liczbie głosów, bez wektorów; jakość zależy od słów kluczowych TMDB.

## Decyzja
Nie przenosić Scout. Budować własny moduł `fork/picks` (B10-1..8) z tym samym pomysłem profilu gustu, ale małym i wyłączalnym, z lokalnymi wektorami (B06b) jako podstawą i modelem przez API (ten sam `LlmClient` co w B07) jako ulepszeniem. Z Scout bierzemy pomysły: wagę obejrzeń × ocena (logarytm), pamięć faktów o tytułach na dysku, filtr „nigdy”. Licencja Arc do sprawdzenia przy ewentualnym kopiowaniu fragmentów (w tym module kodu nie kopiujemy).
