# BLOKADY i zaległe (stan 27.11.2026)

Czytaj, gdy kolejka w `plan/TERAZ.md` jest pusta albo zadanie dotyczy tematu z listy. Pełne wpisy do 26.11 (co próbowano, szczegóły): `plan/archiwum/BLOKADY-do-26.11.md` (tylko `grep`).
Nowy wpis: dopisz na końcu BEZ czytania pliku — `cat >> plan/BLOKADY.md <<'EOF'` z linią `- DD.MM temat: co próbowałem; czego brakuje; co może dać właściciel.` Rozwiązany wpis usuń (jeśli coś postanowiono — jedna linia w `plan/DECYZJE.md`).

## Czeka na właściciela (decyzja, klucz, dane, zgoda)
- Paczka wektorów i model AI (B06b): sekret `TMDB_API_KEY` w ustawieniach repozytorium GitHub (workflow `fork-vectors.yml`) + decyzja o redystrybucji modelu EmbeddingGemma (licencja Gemma). Aplikacja działa na regułach.
- DubLift (B11-3): 5 filmów w dwóch wydaniach (polski lektor/dubbing + oryginał; 2 z różnicą 25/23,976 fps, 1 z innym montażem, 1 z 5.1) — `plan/raporty/dublift.md`.
- Trakt: klucze `TRAKT_CLIENT_ID`/`SECRET` w `local.properties` — do tego czasu testy Trakt pomijane listą wykluczeń.
- Fanart.tv jako zapas obrazków: klucz fanart.tv w `local.properties` (potem zapas w `fork/images/ForkImageQuality`).
- Catch-up dłuższy niż 7 dni (np. TV Team 14): sonda `fork/tv/CatchupDepth` zna prawdziwą długość, ale Live TV Reshaped trzyma najwyżej 7 dni (`LiveTvArchive.MAX_DAYS`, `LiveTvEpg catchupPastMs`, `LiveTvCatchupLinks.isPlayableFrom`) — zmiana w Reshaped + więcej miejsca na przewodnik.
- Lektor na Dysku Google: działa tylko przy zalogowanej synchronizacji Google (zakres drive.file); wspólny folder dla różnych kont = udostępnienie folderu + klucz albo synchronizacja rodzinna. Na JMGO niesprawdzone, czy konto jest zalogowane.
- Emocje lektora: Gemini TTS Tier 2 (formularz niewysłany) / ElevenLabs / zostaje Chirp. ZONOS3 (lokalny głos, `ai-narrator/`, nieskompilowany).
- 4K na ekranie 1080p: `KeRanking.tvCap` celowo odsuwa 4K.
- Kod bazy na wątku głównym: `CollectionsDataStore.parseCollections` (7 %), `UpdateBannerHost` (5 %; aktualizator miał być usunięty).
- W1 (test/limit łącza KE) nierozpoczęty; przenoszenie czegokolwiek z innych forków tylko po potwierdzeniu.
- Macrobenchmark na JMGO: cel `com.nuviodebug.com` jest debuggable, wariant bez debug = druga aplikacja na TV — potrzebna zgoda albo urządzenie bez domowników. Zastępczo `fork-tools/uitest/perf_jmgo.sh` i log `ForkFocus`.
- Serwer logów: wgrać `server/family-sync/panel_api.php` i `panel/app.js` przez DirectAdmin (właściciel musi być zalogowany).
- Angielska nakładka synchronizacji napisów („Syncing subtitles to audio…”, Reshaped `SubtitleSyncStatusOverlay`): ukryć poza diagnostyką? Prawdopodobnie znika po scaleniu aktualizacji bazy (dymki AutoSync usunięte) — sprawdzić po scaleniu.
- `fork-tools/check.sh` potrzebuje klonów `../upstream/*` na SHA z `fork-tools/pins.env`; 27.11 jest tylko `upstream/reshaped` (sklonowanie reszty: kilkaset MB miejsca).
- `.stage/apk/*` starsze niż uxr14 (ok. 2,7 GB) — usunięcie zablokowane uprawnieniami; właściciel może je usunąć.

## Do zrobienia w innym module / wątku
- YouTube w „Oglądaj dalej” nie odtwarza (`youtube:<id>` — moduł YouTube nie ma odtwarzania po samym id; potrzebne np. `playById`).
- Napisy YouTube podwójne (`YouTubeMpdServer`/`YouTubeCaptionTranslation`: każda linia „X\nX”).
- Lektor w odtwarzaczu Niuni: brak wyboru głosu i „Ekspresyjności” (głos tylko w Ustawienia → Moduły → Lektor; `LektorScript` nie ma poziomu emocji).
- Wyszukiwarka Niuni: zakładki YouTube i Osoby bez własnych źródeł (tylko wyniki katalogów albo „Brak wyników”).
- Profile Niuni: długie OK (Edytuj/Usuń) wymaga kolejnych wpięć.
- `ForkLog.say` przy czynnościach, które „nic nie robią”; szum `ForkTv TvStore.kt JsonDecodingException EOF` (pusty plik = pusta lista, bez logu).
- Nieśledzone `plan/raporty/jmgo-*.log` — nie commitować bez przejrzenia (mogą zawierać treści odpowiedzi OkHttp).

## Niesprawdzone na TV / odłożone
- Niesprawdzone: okno napisów na starcie filmu (lektor auto), Baahubali ▲ (rząd kolekcji), Calendar Wstecz, kafle bez ikonki typu, zmiana ścieżki dźwięku YouTube.
- Odłożone (Kicia, głębokie zmiany odtwarzacza): nauka `SmartOutcomeStore`, rozgrzewka `ForgeWarmUp`, bufor `PlayerBufferTuning`, ciche P2P, restart w mpv po błędzie kontenera.
- Stare (17.11, logika + testy JVM są, UI niepodpięte; prawdopodobnie nieaktualne po skórce Niunia): B03 — `ChannelFilters`, `MiniGuideLogic`, `DelayedPlayback`, `ForYouRow`, `ProgramPage`; B04 — `AudioCheck` w odzyskiwaniu źródeł, linia „Gotowe” w szczegółach, „Napisy nie pasują” w menu napisów.
