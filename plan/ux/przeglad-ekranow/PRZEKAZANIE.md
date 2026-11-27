# Przekazanie — przegląd ekranów (27.11, ~18:10)

Prompt dla nowego wątku: **„Otwórz `NuvioFork/plan/ux/przeglad-ekranow/PRZEKAZANIE.md` i wykonaj.”**
Poprzednie przekazania: `.stage/prz10/PRZEKAZANIE-1650.md`, `.stage/prz10/PRZEKAZANIE-1605.md`, `.stage/prz09/PRZEKAZANIE-1455.md`.

## Zadanie (bez zmian)
Katalog `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`, gałąź `skin-niunia`. Wykonuj `plan/ux/przeglad-ekranow/PROMPT.md` w pętlach: kod → build → ręczne testy na JMGO ≥ 10 min → znaczniki w `NOTATKA.md` (`✅ TV <build>` / `❌ TV <build>: …` na końcu nagłówka `## …`) i sekcja pętli w `WYNIKI.md` (1. zdanie: ile poprawek, ile sprawdzono na TV) → SendUserFile → commit + push. Nie pytaj, nie przerywaj.
Nazewnictwo (właściciel się pomylił): **paczka** = porcja poprawek w kodzie (13, 14…), **prz10, prz11…** = kolejne buildy APK.

## Build i instalacja
- `bash fork-tools/build.sh prz13 :app:assembleFullDebug -x :app:lintVitalAnalyzeFullDebug -x :app:lintVitalReportFullDebug -x :app:lintVitalFullDebug -PforkAbi=armeabi-v7a "-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8"` w zakładce terminala (`run_in_terminal`); ~5–10 min. Lint wyłączony: padał na „Java heap space” (DECYZJE 27.11). Monitor: `tail -n0 -F /tmp/<log>.log | sed -l 's/\x1b\[[0-9;]*[A-Za-z]//g' | grep --line-buffered -aoE "e: file[^ ]*|[A-Za-z:]+ FAILED|What went wrong|Not enough memory|BUILD (SUCCESSFUL|FAILED)"`. Stan: `bash .stage/bst.sh <log>`.
- APK → `.stage/apk/<build>-v7a.apk`; instalacja w tle: `bash fork-tools/test/jmgo_instaluj.sh .stage/apk/<build>-v7a.apk > .stage/prz10/inst<N>.log 2>&1`.
- Logi: `bash .stage/prz10/lg10.sh [ile]` (od `.stage/prz10/mark`). Klawisze: `fork-tools/test/kf.sh` (z `KF_WAIT=1.5`); zrzut: `plan/ux/przeglad-ekranow/zrzut.sh NAZWA`; powiększenie: pełny `screencap` + PIL `crop` (python3 -I). `fx2.sh` (uiautomator) potrafi wisieć na Głównej — używaj logu `ForkFocus` z `kf.sh`. `adb logcat -d` bywa bardzo wolny — dawaj `timeout` Basha, nie łańcuch.
- adb wisi przy działającym porcie → `adb kill-server; adb start-server; adb connect 192.168.31.252:5555`.

## Stan
- Na JMGO: **prz12** (paczki 13–16 + okładki AIOMetadata z wątku `aiometadata-diagnoza`), md5 `73171e334bdf567e2bb5034c887703b6`. ~18:05 projektor zniknął z sieci („Host is down”), chwilę wcześniej coś grało — nie sterować na ślepo.
- Sprawdzone na TV: prz10 (pętla 5 w WYNIKI: Główna bez efektów, łaty pod tekstem, pas pod podpisami kart, menu = rozmyty sam panel, bez szarego pola opisu odcinka, hero p95 34 ms), prz11 (13: mały pasek postępu nad podpisem ✅).
- **Do sprawdzenia na prz12 (NAJPIERW):** paczka 16 — łata rozmycia: maska 80 % dawnej, miękkie brzegi 35 % krótszego boku (`NiuniaBlurPatch.kt`, `PATCH_SCALE`, `PATCH_SOFT`); zrzuty jak `p10-karta-tytulu.png`, `p10-szczegoly.png`, `p10-strona-odcinka.png`, `p10-wczytywanie.png` (ścieżka: Główna ▼▼ → „Popular · filmy” ► → OK = karta tytułu; OK na „O filmie” = Szczegóły; „Popular · seriale” → The Body → O serialu → ▼ → OK na odcinku = strona odcinka; tam fokus na „Wznów” — OK + zrzut po 0,8 s + Wstecz w jednym `adb shell`). Log: „łata rozmycia: … maska 80 %”. Wpisz wynik w WYNIKI pętla 6 („W prz12, do sprawdzenia”) i NOTATKA (CAŁA APLIKACJA).
- Polecenia właściciela 27.11: 16:47 „rozmycie po lewej na hero na home usuń … minimalne powierzchnie pod małymi napisami” (zrobione); ~17:20 pasek postępu IPTV mały nad tekstem (zrobione); ~17:50 „sporo większy radius i maska o 20 % mniejsza” (prz12, niesprawdzone). Interpretacja „radius” = miękkość brzegów maski (DECYZJE).

## Dalej (kolejka)
- Niesprawdzone, da się wywołać: kafle archiwum IPTV pod odtwarzaczem (nowy pasek nad podpisem + 22 pkt 2 okładki, np. Piotruś Królik / BBC CBeebies), ekran źródeł (`stream/NiuniaStreams.kt`), Mix YouTube (OK = 1. film), pusta playlista YouTube, plakaty z logo (rozmycie od 76 %), ekran błędu odtwarzacza.
- Rzadkie (nie wymuszaj): 03 klip < 3 min, 06 fokus po dołączeniu polskiej ścieżki, „Kropka nad i”, stos serialu, napisy wbudowane.
- BLOKADY (nie ruszaj bez decyzji właściciela): 11 pkt 2 większy podgląd kanału, 23 pkt 2 „Następne” z Mixu.
- Do decyzji właściciela (napisane w WYNIKI pętla 5): łata pod opisem na Głównej na jasnych zdjęciach — nie dodawaj bez jego słowa.

## Pułapki z testów
- OK na „Wznów”/„Odtwórz” uruchamia film na profilu właściciela od razu — fokus przed każdym OK; zmiany wpisz w WYNIKI.
- Na hero Głównej przycisk „Następny wyróżniony tytuł” = ►►► od „Odtwórz”; Wstecz na hero otwiera menu, Wstecz w menu je zamyka.
- Live TV: start na pigułce „Wszystkie”; ▼ wchodzi w przewodnik; „Oglądaj dalej” = ▲ ►►.
- Po teście: cisza `adb shell dumpsys media.audio_flinger | grep -c '^  Standby: no'` = 0; `.stage/jmgo_zajety.txt` → „wolny”.
