# PROMPT: audyt wydajności forka Nuvio (zacinanie, szczególnie telewizja) i przeniesienie rozwiązań z Kicii

Wklej całość do nowej sesji Claude Code uruchomionej w `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`. Pracuj po polsku, autonomicznie, bez pytań i bez zatrzymywania się, dopóki kolejka nie jest pusta. Przeczytaj najpierw `../CLAUDE.md`, `plan/STAN.md` i `plan/KONTYNUUJ.md` (zasady pracy), potem ten plik.

## 0. Po co

Właściciel zgłasza, że aplikacja na słabych telewizorach „zacina się”, a pilot/klawisze reagują z opóźnieniem, szczególnie w telewizji (Live TV, przewodnik EPG, zmiana kanału). Podejrzenie: to wydajność aplikacji, nie pilot. W Kicii (`../nuvio-tv-kicia`, tylko odczyt) jest audyt wydajności tego samego rodzaju aplikacji, zmierzony na JMGO H931. Zadanie ma trzy części:
1. **Zmierzyć** fork tą samą metodą co Kicia i na tych samych urządzeniach.
2. **Ustalić, co z audytu Kicii jest naprawdę rozwiązane w jej kodzie**, a co tylko opisane (patrz uwaga niżej).
3. **Wdrożyć poprawki** (przeniesione z Kicii albo napisane według jej audytu) tak, żeby po ponownym pomiarze było wyraźnie szybciej, bez utraty funkcji.

### Uwaga o Kicii (zweryfikowane 17.11.2026, nie zakładaj, sprawdź)
Właściciel sądzi, że Kicia ma te problemy rozwiązane. Wstępne sprawdzenie temu przeczy: repozytorium Kicii ma jeden commit; `EAGER_BACKGROUND_LOAD = true` w `ui/forge/ForgeUi.kt:55` (to jest znalezisko WY-307, które audyt każe wyłączyć) nadal jest włączone; `docs/makieta-tv/v7/wydajnosc.md` i `pomiary/wydajnosc-ranking.md` to **audyt z pomiarami i zaleceniami** (znaleziska WY-101 … WY-406), a etap Kotlin jest dopiero zapowiedziany (`PROMPT-WDROZENIE-KOTLIN.md`). Istnieją natomiast gotowe elementy w kodzie: moduł `baselineprofile/` (makrobenchmarki startu i klatek), `ui/screens/settings/MemoryBudget.kt` (progi pamięci dla słabych urządzeń), tryb Essential, `ui/forge/home/ForgeBackgroundPreload.kt`. Dla każdego znaleziska z rankingu ustal więc tabelą: **wdrożone w kodzie Kicii (ścieżka i linia) / tylko zalecenie / nie dotyczy forka**. Przenosisz kod tylko tam, gdzie istnieje; resztę piszesz według zalecenia z audytu, jako nowy kod forka.

## 1. Urządzenia (testy TYLKO na prawdziwych telewizorach; emulator tylko `-no-window`, nigdy razem z Gradle)

| Urządzenie | Adres | Uwagi |
|---|---|---|
| Xiaomi TV Box 3rd gen `MiTV-AFMU0` | `192.168.31.183:35313` (port dynamiczny; gdy nie działa, sprawdź `adb mdns services` albo `nc -z 192.168.31.183 5555`) | Android 14, **tylko armeabi-v7a**, 2 GB RAM, ok. 500 MB swapu w użyciu: najsłabsze, najlepsze do wykrywania zacinania; APK: `app-full-armeabi-v7a-debug.apk` |
| JMGO H931 | `192.168.31.252:5555` | to urządzenie, na którym Kicia robiła pomiary (Android 11, armeabi-v7a, 4 rdzenie, 3,8 GB RAM): daje wprost porównywalne liczby; nic na nim nie ruszaj poza instalacją tej aplikacji i pomiarami |

Połączenie z macOS: gdy `adb connect` zwraca „No route to host”, a `nc -z IP PORT` się łączy, zrób `adb kill-server; adb start-server` i połącz ponownie. Instalacja: `fork-tools/devtest/install_device.sh <ip:port>` (wybiera APK wg ABI, sprawdza sumę kontrolną). Narzędzia testowe i skrót nawigacji bez klikania (`am start -a com.nuvio.fork.OPEN --es route …`, tylko debug) opisuje `fork-tools/devtest/README.md`. `adb input tap` nie działa na przyciskach TV; używaj klawiszy DPAD, a ich sterowanie samo obciąża słaby procesor (liczby to górna granica, jak w pomiarach Kicii).

## 2. Faza A: pomiar bazowy forka (przed jakąkolwiek zmianą)

Użyj metody i skryptów Kicii: `../nuvio-tv-kicia/docs/makieta-tv/v7/pomiary/pomiar.sh` i `pomiar2.sh` (kroki: starts, starts12, screens, tabs, mem, lib_redo, build), `../audyt/lib.sh`, `zloz-wydajnosc.sh`. Dostosuj nazwę pakietu (`com.nuviodebug.com`) i adres urządzenia do forka; skopiuj skrypty do `fork-tools/devtest/perf/`, nie edytuj Kicii. Dla **obu** urządzeń zmierz i zapisz w `plan/raporty/wydajnosc-baza.md` (tabela: Kicia vs fork, to samo urządzenie tam, gdzie się da; surowe dane do `plan/raporty/wydajnosc-surowe/`):

1. Zimny start (`am start -W`), czas do ekranu profilu, do okładek i hero po wyborze profilu; ciepły start.
2. Procesor i wątki przez pierwsze 60 s po starcie (`top -H`, `/proc/<pid>/stat`); GC; pamięć PSS/RSS po starcie i po przejściu wszystkich ekranów; swap.
3. Klatki po terminie i opóźnienie wejścia (`dumpsys gfxinfo <pkg> framestats`, reset przed każdym ekranem; p50/p90/p99) dla: Home (Modern i Classic), zakładki paska, Szczegóły, Biblioteka, Szukaj, Ustawienia, **Narzędzia, Odkrywaj telewizję, Na co masz ochotę?, Szukaj w telewizji**, Live TV (lista kanałów, przewodnik, przewijanie siatki EPG w poziomie i w pionie, otwarcie programu).
4. **Telewizja, szczegółowo**: (a) czas od naciśnięcia OK na kanale do pierwszej klatki (logcat: `adb logcat -s ForkPerf` daje `first_play start_to_play_ms=… key_to_play_ms=…` z `fork/sources/PlayerBridge.kt`; start_to_play liczy się od ustawienia adresu strumienia do pierwszego odtwarzania bez nakładki ładowania; wstępne wartości na Xiaomi: 550-707 ms, patrz `plan/raporty/wydajnosc-baza.md`), 10 kanałów, zimno i ciepło; (b) zmiana kanału ▲▼ w odtwarzaczu (czas do nowego obrazu), 20 zmian pod rząd; (c) otwarcie i przewijanie przewodnika, czas do pierwszych programów; (d) obciążenie procesora i pamięci podczas 5 minut oglądania kanału na żywo i podczas przewijania przewodnika; (e) bezczynność na Home i na Live TV przez 120 s (klatki/s, CPU), bo „bezczynność nie jest bezczynna” (WY-406 w Kicii).
5. Sieć po starcie i po wyborze profilu (`/proc/net/dev` wlan0), szum w logu (liczba linii OkHttp, błędów TMDB 401, `ImdbEpisodeRatingsRepo`, `sponsor.ajay.app`).
6. Różnica debug/release: zbuduj wariant wydaniowy (R8, bez logowania OkHttp), jeśli konfiguracja budowania na to pozwala, zmierz drugi raz i opisz różnicę. Pomiary debug są zawyżone.

Playlista do testów Live TV (podał właściciel): `http://hls.gd/pl/41/4yk3l7ulv116k/playlist.m3u8`, EPG: `https://epg.ovh/pl.gz` (`app/src/main/assets/fork/epg_sources.json`). Dodaj ją w Live TV przez serwer konfiguracji (`reshaped/livetv/LiveTvSetupServer.kt`: `POST /<token>/api/source` z JSON `{"type":"m3u","url":…,"epg":…,"logo":""}`; token i adres widać na ekranie dodawania listy, odczytaj tekstem) albo z ekranu dodawania listy. Przełącznik Live TV: Ustawienia → Nuvio Reshaped → „Live TV”.

## 3. Faza B: co z naszych dodatków obciąża start i telewizję

Fork dodał moduły (rejestr `fork/registry/ForkModules.kt`). Rozdziel wpływ **każdego** na pomiary z fazy A, wyłączając je pojedynczo (Ustawienia → Moduły; klucz `module_<id>` w prefs `fork_features`) i mierząc różnicę: `tv` (rozgrzewka przewodnika `LiveTvRepository.warmUp` w `rootOverlays`, czytanie dziennika, zbieranie emisji, drugi przewodnik, logo), `sources`, `night` (nakładka na całe okno nad treścią: `zIndex(100)` w `ForkHooks.moduleOverlays`; sprawdź koszt rysowania), `launcher`, `voice`, `discover`, `lektor` (procesor dźwięku w łańcuchu odtwarzacza), `picks`. Oczekiwane podejrzane: `TvModule.rootOverlays` → `warmUp` przy starcie (to dokładnie typ WY-101/WY-307), `ForkTvHooks.onProgrammeRead` w parserze EPG, nakładka modułów na całym oknie. Wynik: tabela „moduł → ms/CPU/klatki”.

## 4. Faza C: audyt Kicii → rozwiązania

1. Przeczytaj w całości: `docs/makieta-tv/v7/wydajnosc.md`, `pomiary/wydajnosc-ranking.md`, `pomiary/WYNIKI.md`, `WYNIKI2.md`, `kod-epg.md`, `kod-start-compose.md`, `kod-obrazy-siec.md`, `kod-nowe.md`, `docs/forge/AUDYT_NUVIO_FORK.md`, `docs/forge/PROMPT_OPTYMALIZACJA_MAKIETY.md`, `docs/essential-mode.md`, `baselineprofile/README.md`.
2. Dla każdego WY-nnn: czy dotyczy forka (jest ten sam kod? pliki Reshaped/bazy są wspólne), czy Kicia ma to **w kodzie** (grep w `app/src`), jaki poziom (1 = istniejące ustawienie/wartość domyślna, 2 = parametr, 3 = nowy plik, 4 = haczyk w pliku rdzenia), ile zyskuje według jej pomiaru. Zapisz tabelę w `plan/raporty/wydajnosc-audyt.md`.
3. Kolejność wdrażania: od największego zysku na Xiaomi i JMGO (wg fazy A/B), poziomy 1 i 2 najpierw. Najpierw te, które w Kicii istnieją w kodzie (profil bazowy i moduł `baselineprofile`, `MemoryBudget`, wyłączenie ładowania w tle, ograniczenie logowania OkHttp w buildach, czytanie zapisu EPG poza wątkiem startu, rozmycia na słabych urządzeniach, animacja zasłony Home, rekompozycja Home przy fokusie), potem te z samego zalecenia.
4. Zasady forka obowiązują (patrz `../CLAUDE.md`): kod 1:1 z Kicii tam, gdzie go przenosisz, w osobnym module (`app/src/main/java/com/nuvio/tv/fork/perf/…`), cienkie adaptery, każda funkcja z przełącznikiem w kategorii forka (domyślnie włączone tam, gdzie to czysta poprawa wydajności; po wyłączeniu zachowanie jak dziś), limit ok. 35 plików rdzenia z haczykami (`bash fork-tools/check.sh` przed każdym commitem; łatki plików Reshaped przez `fork-tools/patchstep.sh`), brak przepisywania i „ulepszania” cudzego kodu, oficjalny odtwarzacz zostaje, funkcje i dane nie znikają. Nie zajmuj się tłumaczeniami ani niczym spoza wydajności.
5. Nie wyłączaj funkcji, żeby „przyspieszyć”: poprawka ma zachować wynik (np. przewodnik ma mieć te same programy, tylko wczytane leniwie, w tle, na niskim priorytecie, z porcjowaniem).

## 5. Faza D: weryfikacja i kryteria

Po każdej paczce poprawek: build APK (v7a), instalacja na Xiaomi i JMGO, **te same pomiary** co w fazie A, wynik dopisany do `plan/raporty/wydajnosc-wyniki.md` (tabela przed/po, ten sam sprzęt, ta sama metoda, wiele powtórzeń; rozrzut). Kryteria ukończenia (podaj liczby rzeczywiste, nie deklaracje):
- zimny start do Home i do hero: skrócony względem fazy A na obu urządzeniach; brak okresów > 1 s bez klatek (przerwa 147 klatek jak w Kicii jest niedopuszczalna);
- klatki po terminie na Live TV i w przewodniku: spadek o co najmniej połowę względem fazy A, p99 wyraźnie niższe;
- zmiana kanału: czas do pierwszej klatki nie gorszy niż zmierzony na czystej bazie (`vendor/nuvio`) i bez skoków > 1 s w serii 20 zmian;
- bezczynność Home/Live TV: bliska 0 klatek/s i CPU;
- pamięć: PSS nie większe niż w fazie A, brak rosnącego swapu przez 5 minut oglądania;
- wszystkie testy JVM zielone (obecnie jedyne czerwone to 2 testy Trakt z powodu braku kluczy w `local.properties`), `check.sh` OK, brak nowych wyjątków w logcat;
- funkcje bez regresji: powtórz tekstowo przejście po wszystkich ekranach i modułach (lista w `STATUS.md`, sekcja „LISTA MODUŁÓW”).

## 6. Zasady pracy (obowiązują, właściciel je powtarzał)

- **Nie czekaj bezczynnie na build/testy.** Build w tle, w tym czasie pisz kolejne poprawki do katalogu pośredniego (`.stage*/`), wdrażaj po zakończeniu. Jeden build na komplet poprawek, nigdy dla jednej drobnej.
- Build APK i testy JVM uruchamiaj **osobno** (razem trwają ponad 10 min przy 8 GB RAM). Kompilacja powyżej 10 minut = błąd: przerwij i zbadaj. Flagi: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`, `./gradlew :app:assembleFullDebug -PforgeArm64Only=true -x lintVitalAnalyzeFullDebug -x lintVitalReportFullDebug` (lint pada błędem narzędzia). Czasy buildów zapisuj.
- **Co 15 minut** jedna linia statusu: co robisz teraz, na co czekasz i ile. Co ok. 30 s sprawdzaj obciążenie CPU podczas kompilacji.
- Testuj tekstowo (`uiautomator`, logcat, `dumpsys`) przed jakąkolwiek automatyzacją UI; zrzutów ekranu nie rób. Emulator wyłącznie `-no-window`, nigdy równocześnie z Gradle, zamykaj `adb emu kill`.
- Po każdym kroku: commit (końcówka `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`), wpis w `plan/STAN.md`, brak pushy bez polecenia właściciela.
- Gdy czegoś brakuje (klucze TMDB/Trakt, dostęp do urządzenia), zapisz w `plan/BLOKADY.md`, zagraj `afplay -v 10 /System/Library/Sounds/Sosumi.aiff` i weź następne zadanie; nie kończ pracy.
- Gdy kolejka jest pusta: wymyśl uzupełniające testy wydajności (np. 30 minut oglądania kanału, 100 zmian kanału, 10 cykli wejście/wyjście z Live TV, wyciek pamięci), testuj i naprawiaj do wyczerpania tokenów.

## 7. Wynik końcowy

`plan/raporty/wydajnosc-baza.md` (pomiar bazowy), `wydajnosc-audyt.md` (tabela WY-nnn z poziomem wdrożenia w Kicii i decyzją), `wydajnosc-wyniki.md` (przed/po), zaktualizowane `STATUS.md`, `HANDOFF.md`, `UPSTREAM_SOURCES.md` (co przeniesione z Kicii: ścieżki i SHA `58b3a0a`), lista poprawek z przełącznikami, oraz uczciwa lista tego, co nadal zacina i dlaczego.
