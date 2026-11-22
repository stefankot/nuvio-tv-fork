# Autoodtwarzanie, zmiana źródła i napisy w Nuvio

21 listopada 2026, JMGO N1 Ultra (H931, Android 11, ARM32). Punktem wyjścia był build 162o, pakiet `com.nuviodebug.com`, wersja 1.2.0-beta.2. Suma MD5 APK z projektora była zgodna z lokalnym plikiem zbudowanym z drzewa `103205239`. Zmiany opracowano w osobnym katalogu roboczym i skompilowano lokalnie na Macu.

## Co wynikało z logów i kodu

- W starszej próbie filmu „The Drama” dodatki oddały 702 źródła. Echo miało wyniki prób łączy, ale po każdej awarii powtarzał się ranking całej listy. Między pierwszym limitem czasu a otwarciem kolejnego pliku minęło ok. 9,4 s. Kolejna próba dostała drugi duży plik 4K.
- Obsługa startu uznawała rosnącą pozycję za sukces również wtedy, gdy aktywne było samo audio i odtwarzacz nie zgłosił ścieżki wideo. Kilka mechanizmów mogło jednocześnie próbować zmienić źródło. Każda próba mogła wyświetlić własny komunikat.
- Na buildzie 162o syntetyczny plik bez wideo uruchomił następną próbę po 15 s. Od wejścia do odtwarzacza do obrazu z alternatywy minęło 21,6 s. Ponowna próba „The Drama” na buildzie 162o wykorzystała zapamiętany link i ruszyła po 6,2 s. Pokazała, że awaria zależy także od konkretnego łącza i wyniku wyboru.
- Próba na JMGO ujawniła też, że dawny warunek polskiej ścieżki audio kierował działający plik do tej samej obsługi co błąd odczytu. Log pokazał wykrytą ścieżkę AVC, po czym ruszała kolejna próba z powodu braku polskiego audio. W sesji automatycznej język audio wpływa teraz na wybór w rankingu.
- Wstępne pobieranie z Echo już pobierało listę źródeł na ekranie szczegółów. Odczyt języków ścieżek następował dopiero na ekranie źródeł. Istniejący czytnik indeksu MKV był używany na potrzeby tłumaczenia, bez cache innych wydań w zwykłym odtwarzaczu.

Potwierdzenie wideo korzysta z raportów odtwarzacza o aktywnej ścieżce i pierwszej klatce. [Dokumentacja Media3](https://developer.android.com/reference/androidx/media3/common/Player.Listener#onRenderedFirstFrame()).

## Wzorce z istniejących forków

| Kod porównawczy | Zachowanie wykorzystane w poprawce |
| --- | --- |
| [Arc – SourceFailoverSelection](https://github.com/ysosrs123/NuvioTV-Fork/blob/343b27591b65ca57372c2a7d420e767a8f86260c/app/src/main/java/com/nuvio/tv/ui/screens/player/SourceFailoverSelection.kt) | Identyfikacja odrzuconych źródeł, ta sama treść i próba otwarcia alternatywy w pozycji sprzed awarii. |
| [Echo – AutoPlayRace](https://github.com/Laking22/Nuvio-Echo/blob/e952419559bdcc8438504d177ddcf494b85d3a46/app/src/main/java/com/nuvio/tv/core/player/AutoPlayRace.kt) | Wstępne równoległe próby łączy z ograniczeniem liczby i ponownym użyciem gotowych wyników. Odtwarzacz potwierdza wideo osobno. |
| [KE – AutoPlaySelection](https://github.com/Arshad-Kamal/Nuvio-KE/blob/0a87a08648defef02857b4208d5c60234f9aa1bb/app/src/main/java/com/nuvio/tv/core/player/AutoPlaySelection.kt) | Istniejąca ocena wydań, dekodowalności i wydajności łącza. |

## Zmiany

- `StreamAutoPlaySelector.kt`, `StreamScreenViewModel.kt`, `EchoRaceHook.kt` i `AutoSourceRecoveryPlan.kt` współdzielą kolejność źródeł policzoną w tle. Pierwsze zapytania Echo mogą się zacząć jeszcze podczas napływania listy. Kolejne próby korzystają z listy sesji i pomijają rozpoznane klipy zastępcze, powtórki oraz pliki oznaczone jako niegotowe.
- `PlayerAutoSourceRecovery.kt` rozpoznaje poprawny start po wideo i rosnącej pozycji. Ścieżka audio bez wideo, ostateczny błąd lub zbyt długi start uruchamiają następną próbę. Po rozpoczęciu filmu sesja wykrywa również błąd odczytu albo 12 sekund buforowania bez postępu. Po przełączeniu przywraca pozycję i potwierdza, że film dalej się odtwarza. Limit jednej serii to trzy próby i 45 sekund. Po wyczerpaniu prób aplikacja wyświetla jeden krótki komunikat i otwiera listę źródeł.
- `EchoSourcePreload.kt` uruchamia `TrackScout.kt` już na szczegółach filmu lub odcinka. Sonda czyta nagłówki do 12 wydań tytułu w tle, w tym wydań w niższej rozdzielczości, i zapamiętuje języki audio i napisów. Ranking KE zachowuje preferencję polskiego audio. Samo posiadanie polskich napisów przestało obniżać wybieraną jakość wideo.
- `EmbeddedSourceSubtitles.kt` i istniejący `MkvSubtitleText.kt` zapisują kompletne polskie ścieżki tekstowe z indeksowanych plików MKV/WebM jako lokalne SRT. Plik pasuje automatycznie po identyfikatorze filmu lub odcinka, oznaczeniu wersji montażu i czasie trwania różniącym się najwyżej o sekundę. Odtwarzacz najpierw używa osadzonych napisów z aktualnego filmu, następnie zgodnego cache w preferowanym języku, potem napisów z dodatków. Wyłączenie napisów i ręczny wybór mają pierwszeństwo.
- Wpięcia są zapisane w `fork-tools/hooks.tsv`, a zmiany selektora także w `fork-tools/patches/stabilnosc/StreamAutoPlaySelector/02-shared-source-order.patch`.

Cache między wydaniami obsługuje tekstowe napisy, do których plik udostępnia indeks z pozycjami bloków oraz żądania zakresowe. Napisy obrazkowe PGS/VobSub mogą nadal działać w pliku, z którego pochodzą. Identyczny czas i oznaczenie montażu nie zawsze gwarantują synchronizację w innym wydaniu.

## Próby na fizycznym JMGO

| Próba | Pomiar |
| --- | --- |
| Plik audio bez wideo, źródło zapasowe z obrazem | Od wejścia do odtwarzacza do obrazu z zapasu minęło 21,6 s na 162o i 9,2 s na nowym APK. Po zmianie pozycja rosła i odtwarzacz potwierdził wideo. To pomiar dla konkretnej pary kontrolowanych wydań. |
| Awaria HLS podczas odtwarzania | Po HTTP 410 na fragmencie nowe APK zachowało pozycję 11 774 ms. Potwierdziło obraz i postęp na źródle zapasowym przy 12 560 ms. Sesja multimediów zgłaszała odtwarzanie. |
| Wstrzymany dopływ danych bez błędu HTTP przed zmianą | W próbie poprzedniego APK z tym samym mechanizmem wznowienia po ok. 14 s buforowania bez postępu sesja zmieniła źródło ze wskazaniem 5 784 ms. Odtwarzanie zapasu potwierdzono przy 6 453 ms. |
| Lepsze wideo i polskie napisy z innego wydania | Nowe APK jeszcze na szczegółach zapisało 14 kwestii z MKV 640×360. Odtwarzacz wybrał wideo 1280×720 z oryginalnym dźwiękiem i załadował 14 kwestii z lokalnego SRT jako „Napisy wbudowane z innego wydania”. Dostępne były równocześnie polskie napisy z testowego dodatku. |
| Wszystkie trzy źródła zwracają HTTP 410 | Po trzech próbach nowe APK pokazało listę źródeł. Obsługa wyczerpania prób uruchomiła się raz. Lista wciąż oznaczała ostatnią próbę etykietą „Odtwarzane”. |

W próbie filmu „The Drama” na wcześniejszym APK z poprawką pierwsze źródło dało obraz 1920×1040 po ok. 7 s od uruchomienia odtwarzacza. Pozostałe próby używały kontrolowanych plików i wbudowanego odtwarzacza ExoPlayer. W kontrolowanych próbach wyłączono automatycznego lektora i wyścig na lokalnych łączach. Próby na docelowym APK przebiegły z preferencją polskiego audio.

## Kompilacja i zapis w projekcie

- Kompilacja ARM32 zakończyła się lokalnie w 14 min 57 s. Zaliczone 71 testów jednostkowych dla rankingu i odzyskiwania źródła oraz kontrola wpięć `fork-tools/check.sh` (65 plików). W końcowej kompilacji pominięto zadanie `lintVitalAnalyzeFullDebug`.
- Podpis APK był zgodny z wcześniejszą instalacją. Plik odczytany przez ADB z JMGO był identyczny z plikiem z kompilacji. SHA-256 `fde430294d2df67bbd7f7394b640b1fb6f60cbee5df11dc35b26046d906295a5`, MD5 `7e3a155cc2304eba31be558fe8ec9049`.
- Commit z kodem `25b5491624bbba7a0ca0220276287fe1fd272a9e` został scalony lokalnie do bieżącej gałęzi `claude/paczka-oczywiste` w katalogu `NuvioFork/.claude/worktrees/paczka-oczywiste` (scalenie `2f6fab0a1`). Porównanie katalogów `app` i `fork-tools` potwierdziło zgodność z commitem użytym do kompilacji.
- Po próbach przywrócono zapisane ustawienia, usunięto tymczasowy dodatek testowy i jego przekierowanie portu. Aplikację zostawiono na ekranie głównym.
