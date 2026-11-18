# Zadanie dla niezależnego modelu: audyt AI Subtitles

Repozytorium: `stefankot/nuvio-tv-fork` (prywatne).
Gałąź do oceny: `feature/ai-subtitles/review-464b04df-20261118-2014`; baza: `final` @ `464b04df900738a24733a274c9c0daa601fc51cb`. **Nie modyfikuj `main`, `final` ani prac innych agentów; nie wykonuj merge.**

Na gałęzi umieszczono **izolowany prototyp rdzenia**, a nie gotową funkcję Android TV. Własny pakiet `app/src/main/java/com/nuvio/tv/fork/ai/subtitles/` zawiera: politykę wyboru pomocy słowiańskiej i zachodniej dla PL, walidację gotowych bloków, trwały limit zwykłego audio 60 s oraz eksport SRT/WebVTT. W `tools/ai-subtitles-tests/` są samodzielne testy Kotlin, a w `app/src/test/.../AiSubtitleContractsTest.kt` testy JUnit. Są też `scripts/ai-subtitles/`, `UPSTREAM_LOCK.json`, `STATE.md` i raport optymalizacji. **Nie podłączono** API modeli, odtwarzaczy Exo/MPV, interfejsu, kolejki zadań, pobierania napisów/audio ani publikacji OpenSubtitles. Nie skopiowano kodu z obcych forków.

Przy przygotowaniu paczki samodzielne testy JVM zakończyły się wynikiem **16/16**, ale to tylko deklaracja poprzedniej implementacji do ponownej weryfikacji. Mikrobenchmark 6000 kwestii również nie dowodzi wydajności na TV.

**Wykonaj niezależny audyt i próby:**
1. Sprawdź aktualne `AGENTS.md`, HEAD, worktree, pliki i zależności; pracuj wyłącznie we własnym katalogu/gałęzi. Sprawdź `git diff --check`, zakres zmian wobec bazy i zgodność z istniejącymi interfejsami Nuvio. Wypunktuj martwy kod, niepotrzebne abstrakcje i słabe kontrakty.
2. Uruchom `./tools/ai-subtitles-tests/run.sh`, `./scripts/ai-subtitles/check-upstreams --offline`, `./scripts/ai-subtitles/check-ai-subtitles-hooks`, następnie dobierz rzeczywisty wariant Gradle (`./gradlew :app:tasks --all`) i uruchom jego testy jednostkowe oraz build. Zapisz polecenia, kody wyjścia i błędy; nie traktuj braku testu jako sukcesu.
3. Sprawdź rygorystycznie: RU→alternatywny słowiański, wybór zachodniego tylko z listy, język oryginału i PL wykluczone, brak plików pomocniczych, błędne identyfikatory i odpowiedzi AI, Unicode i formaty SRT/VTT, stabilność timingów, dużą liczbę kwestii.
4. Testuj limit audio: równoległe procesy/wątki, restart, błędy zapisu, uszkodzony plik, niepewne i ponowione wysyłki, duplikaty ID oraz odporność na wyczerpanie miejsca. **Pilne:** `app/build.gradle.kts` ma `minSdk=24`, a `OrdinaryAudioBudget` używa `java.nio.file.Files.move(..., ATOMIC_MOVE)` — sprawdź działanie na API 24–25 i trwałość po awarii.
5. Sprawdź prawdziwy tor wykonania na emulatorze/TV: brak automatycznej integracji. Zbadaj `commitPreparedSidecarSubtitle`, generacje sesji, przewijanie, opóźnione wyniki i bufor Exo; MPV oceniaj osobno. Nie deklaruj tych funkcji działającymi bez wdrożenia i testów.

**Raport:** tabela „wymaganie → istnieje / częściowo / brak → dowód/test”, lista błędów z priorytetem i plik:linia, wyniki builda, regresje, ryzyka kosztu/prywatności oraz minimalny plan poprawek. Uczciwie oddziel przetestowane od niezweryfikowanych. Nie publikuj napisów, nie uruchamiaj płatnych wywołań AI i niczego nie scalaj.
