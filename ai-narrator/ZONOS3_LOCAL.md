# ZONOS3-Mini: synteza na Android TV, przygotowanie audio przed seansem

**Stan: prototyp.** Kod pobierania modelu i planowania audio jest w gałęzi, lecz nie ma skompilowanego i przetestowanego silnika ZONOS3 dla Android ARM64. Brak natywnego pliku wykonawczego **blokuje syntezę**; nie wolno mylić pobrania GGUF z działającą funkcją.

## Działanie

1. Wybierz ZONOS3-Mini zamiast Gemini. ZONOS3 jest uruchamiany **na samym Android TV** (lokalna pętla 127.0.0.1), bez zewnętrznego serwera LAN. Nikt nie pobiera modelu automatycznie.
2. Dopiero przycisk **Pobierz / wznów pobieranie modelu** pobiera dwa pliki GGUF, łącznie 1 115 476 064 bajtów, z przypiętej rewizji Hugging Face. Są kontrolowane rozmiary, miejsce na dysku, wznowienie i SHA-256.
3. Wstrzymaj film i użyj **Przygotuj 1. połowę** lub **Przygotuj 2. połowę**. Wymagana jest pełna lista napisów z czasami. Tekst jest syntezowany kolejno, a ekran pokazuje postęp i szacowany pozostały czas.
4. Jedna sesja syntezy ma limit 15 minut. Nie ma danych, czy pół filmu powstanie w tym czasie na konkretnym procesorze. Jeśli nie, sesję można wznowić; już wykonane kwestie nie są generowane ponownie.
5. Podczas filmu ZONOS3 **wyłącznie odtwarza wcześniej zapisane WAV**. Synteza drugiej połowy może być wykonana podczas przerwy w projekcji; nie uruchamia się równolegle z filmem.

## Zapis i bezpieczeństwo

Model jest w prywatnej trwałej pamięci aplikacji: filesDir/zonos3-mini/v1. Audio jest trwale zapisywane w filesDir/ai-narrator/zonos3-prepared/v1, poza katalogiem cache Androida. Klucz pliku obejmuje wariant modelu i tekst, pozwalając wykorzystać identyczne kwestie w kolejnych sesjach. Limit nagrań 2 GiB; zachowywane jest 500 MiB wolnego miejsca. Zabraknie miejsca — generowanie staje, gotowe nagrania pozostają. Ich kasowanie wymaga osobnego potwierdzenia.

## Pamięć RAM: 2 GB, 3 GB, 4 GB

Progi aplikacji to zasady bezpieczeństwa, **nie pomiary wydajności ZONOS3**. Sprawdzana jest wolna RAM, stan lowMemory i zużycie procesu. W trybie przygotowania z pauzą wymagane jest przed uruchomieniem co najmniej:

| Urządzenie | Minimalna wolna RAM na start |
|---|---:|
| Xiaomi TV Box S 3rd Gen (2 GB) | 1 250 MiB |
| Onn 4K Pro (3 GB) | 1 500 MiB |
| Projektor (4 GB) | 1 750 MiB |

Dodatkowo rezerwa systemowa wynosi odpowiednio przynajmniej 450/520/700 MiB. Kontrola wolnej RAM powtarzana jest podczas pracy i przerywa syntezę, gdy zaczyna brakować pamięci lub użytkownik wznawia film. Proces natywny ma pojedyncze zadanie CPU. 2 GB może nie wystarczyć nawet przy pauzie — wtedy aplikacja ma odmówić uruchomienia, nie zawiesić systemu.

## Natywny silnik: wymagane wdrożenie

Dodano skrypt eksperymentalny ai-narrator/tools/build_android_zonos3.sh. Próbuje on skompilować natywny serwer C++/ggml ze źródeł Zyphra przez Android NDK i umieścić go w ai-narrator/jniLibs/arm64-v8a/libzonos3_server.so. Forkowy Gradle wskazuje ten katalog. **Skrypt nie był wykonany**, nie potwierdzono kompilacji, uruchomienia ELF/PIE ani wymaganej ilości RAM. Bez tego pliku interfejs wyświetla brak silnika i nie próbuje syntezy. Test jest wymagany na każdym urządzeniu.

Źródła: https://github.com/Zyphra/ZONOS3-Mini/tree/main/servers/ggml oraz https://huggingface.co/Zyphra/ZONOS3-Mini. Wagi Apache 2.0, kod referencyjny MIT.

## Dodatkowe ograniczenia

- Aktualnie jest to **czytanie polskich napisów**, nie tłumaczenie zagranicznych dialogów. Tłumaczenie to odrębna funkcja.
- Adapter posiada pełną listę czasową głównie przez subtitleAutoSyncCues. Bez pełnych napisów (zwykłe SRT/VTT nie zawsze są tu dostępne) przygotowanie jest zablokowane. Pełna ekstrakcja z wybranej ścieżki napisów nadal wymaga implementacji.
- Prawdziwego czasu przygotowania nie zmierzono. Limit 15 minut nie stanowi obietnicy ukończenia połowy filmu.
- Brak automatycznego przełączenia na Gemini. Nie istnieje ścieżka syntezy ZONOS3 w trakcie odtwarzania.
- Brak potwierdzonych testów kompilacji, ARM64, ExoPlayer/mpv, synchronizacji i odporności na OOM.

## Warunki odbioru

Kompilacja APK i uruchomienie rzeczywistego silnika ARM64, test na modelach Xiaomi 2 GB, Onn 3 GB i projektor 4 GB, pomiary RAM/latencji, pełna lista napisów, przygotowanie obu połówek, wznowienie po restarcie, brak syntezy w trakcie filmu, niezmieniona synchronizacja upstream. Dopóki tych testów nie ma, stan pozostaje **prototyp / BRAK DANYCH**.
