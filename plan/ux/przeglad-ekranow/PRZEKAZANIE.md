# Przekazanie — przegląd ekranów (27.11, ~16:05)

Prompt dla nowego wątku: **„Otwórz `NuvioFork/plan/ux/przeglad-ekranow/PRZEKAZANIE.md` i wykonaj.”**
Poprzednie przekazanie (14:55): `.stage/prz09/PRZEKAZANIE-1455.md`.

## Zadanie (bez zmian)
Katalog `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`, gałąź `skin-niunia`. Wykonuj `plan/ux/przeglad-ekranow/PROMPT.md` w pętlach: kod → build (w trakcie buildu kod następnej paczki w `.stage/`) → ręczne testy na JMGO ≥ 10 min → znaczniki w `NOTATKA.md` (`✅ TV <build>` / `❌ TV <build>: …` na końcu nagłówka `## NN`) i nowa sekcja pętli w `WYNIKI.md` (1. zdanie: ile poprawek, ile sprawdzono na TV) → SendUserFile → commit + push. Nie pytaj, nie przerywaj. ❌ wraca na początek następnej pętli, po 2 próbach → `plan/BLOKADY.md`.

## Build i instalacja (zasady właściciela 27.11 w ZASADY-build.md)
- `bash fork-tools/build.sh prz0N :app:assembleFullDebug :app:testFullDebugUnitTest --tests 'com.nuvio.tv.fork.tv.*' --continue -PforkAbi=armeabi-v7a "-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8"` w zakładce terminala; ~10 min. Od startu Monitor: `tail -n0 -F /tmp/<log>.log | sed -l 's/\x1b\[[0-9;]*[A-Za-z]//g' | grep --line-buffered -aE "^e: |FAILED|What went wrong|Not enough memory|BUILD (SUCCESSFUL|FAILED)"` + co minutę `bash .stage/bst.sh <log>`.
- APK → `.stage/apk/prz0N-v7a.apk`; `adb install -r`, md5 z `pm path` = md5 pliku, `adb shell cmd package compile -m speed -f com.nuviodebug.com` w tle (~7 min, aplikacji wtedy nie uruchamiaj; koniec = brak procesu `dex2oat`, `dumpsys package` → `status=speed`).

## Logi na JMGO (ważne)
- Bufor logcat pełny przycina logi forka (najbardziej gadatliwa aplikacja) — przed testem `adb logcat -G 32M` (ustawione 27.11, po restarcie JMGO powtórz) i ewentualnie `adb logcat -c`. NIE ustawiaj białej listy logd (`logcat -P '… 10123'`) — wyłączała logi forka.
- `logcat -T` bywa pusty: czytaj `adb logcat -d | grep -a "ForkFocus: focus=" | tail` albo `.stage/prz07/lg2.sh [wzorzec]` (od znacznika `.stage/prz03/mark`, sortowane po czasie).
- `fork-tools/test/type.sh` nie działa w oknie „Szukaj w YouTube” (nie odczytuje fokusu) — wpisuj strzałkami albo wybierz wpis z „Ostatnio szukane”.

## Stan
- Buildy: prz06 (paczki 1–6), prz07 (paczka 7: podgląd 520×288, napisy wbudowane, „Odkrywaj” przy zegarze, okładki AIOMetadata), prz08 (paczka 8: kafle Live TV przez `NiuniaMediaCard`, dopasowanie nazw AIOMetadata + test JVM, okno „Szukaj w YouTube” trzyma fokus). Na JMGO: prz08 (md5 `5b54f0972a825c59757d70b5daf9cd2f`).
- W kodzie, niezbudowane: paczka 9 (`9bc44a595`) — pasek podpisu zwykłych kart MediaCard (Oglądaj dalej, odcinki, zwiastuny) na rozmytym dole obrazu (CAŁA APLIKACJA krok 2).
- Wyniki: `WYNIKI.md` pętle 1–3; znaczniki w `NOTATKA.md`.
- `.stage/jmgo_zajety.txt` — wpis tego wątku; nadpisz swoim.

## Najpierw
1. Na JMGO prz08 (sprawdzony, `WYNIKI.md` pętla 3). Paczka 9 (pasek podpisu kart na rozmyciu) jest w kodzie — to za mało na build: najpierw dopisz kolejne punkty (niżej), potem build prz09 z całością.
2. Po instalacji prz09 sprawdź: rząd „Oglądaj dalej” na Głównej i karty odcinków/zwiastunów w Szczegółach — podpis na rozmytym dole obrazu, czytelny, przewijanie płynne (budżety JMGO w `plan/DECYZJE.md`, „Wydajność”).
3. Wyniki okładek AIOMetadata (33 % trafień, TMDB 52 %) są w `WYNIKI.md` (pętla 2) — jeśli właściciel odpowie, zastosuj jego decyzję (np. TMDB awaryjnie po braku w AIOMetadata: `TvCovers.fetchAio` → `fetch`).

## Dalej (kolejka z NOTATKI)
- 22 pkt 2: karty „Poprzednie odcinki” IPTV puste, bo AIOMetadata nie zna tytułu (np. „Piotruś Królik”). Kod pyta poprawnie. Ostatnia deska z NOTATKI: kadr z samego nagrania archiwum. Druga nieudana próba = BLOKADY (pierwsza: TMDB w prz05 nietestowane na TV, druga: AIOMetadata prz07).
- Okładki AIOMetadata: 33 % trafień (TMDB miał 52 %) — wynik zgłoszony właścicielowi w WYNIKI; czekać na jego reakcję, nie wracać samemu do TMDB (decyzja 27.11).
- 23 pkt 2 Mix „Następne”: wymaga zmiany w `youtube/YouTubeViewModel.kt` (kopia z Nuvio One — `queue` prywatne; Mix to playlista „RD…”: po starcie pierwszego filmu w tle `content.getPlaylist(id)` i podmiana `queue`). Zmiana w kopii z forka — sprawdź ZASADY-kod („Kopie z forków”) i zrób przez adapter/osobny plik, jeśli się da.
- CAŁA APLIKACJA dalej: szare gradienty hero/szczegółów/paneli → rozmycie (lista plików w NOTATCE, sekcja „CAŁA APLIKACJA”).
- 23 pkt 1b (podgląd filmu YouTube z dźwiękiem w karcie) — duże; sprawdzić pamięć JMGO.
- Niesprawdzone: 06 fokus po dołączeniu polskiej ścieżki przy otwartym panelu (Dublagem często nie potwierdza synchronizacji), 23 Mix, pusta playlista YouTube, stos serialu (10 pkt 1), „Kropka nad i” bez plakatu filmu (TVN24 wieczorem), napisy wbudowane (potrzebny film z wbudowanymi polskimi napisami i wybranymi z dodatku — log `ForkSubs: napisy wbudowane:`).

## Pułapki z testów (dalej aktualne)
- Nakładka odtwarzacza chowa się po ~4 s: sekwencje klawiszy w jednym `adb shell "…"` (▼ pokazuje, ▲ = Dźwięk, ▶▶▶ = Więcej; Więcej: Źródła, Jakość, …). Fokus w odtwarzaczu widać tylko na zrzucie.
- Live TV: Wstecz z przewodnika przenosi fokus na pigułki, drugie Wstecz z pigułek = Główna. Panel „Kraj i język”: zakładkę zmienia OK. Po teście przywróć „Polska”.
- Okna nad ekranem (Szukaj w YouTube, panele) — uiautomator nie widzi; fokus z logu `ForkFocus` (okno YouTube: fx2.sh widzi klawisze).
