# Automatyczna jakość po włączeniu lektora

21 listopada 2026. Projektor JMGO N1 Ultra, Android 11, ARM32. Przypadek zgłoszony dla „Furiosa: Saga Mad Max”.

| Godzina w Polsce | Zdarzenie z logu JMGO |
| --- | --- |
| 18:31:28 | Odtwarzacz potwierdził obraz z pliku 4K. Pierwsze oczekiwanie na dane zaczęło się na pozycji 1,37 s z około 130 ms w buforze. |
| 18:31:33–35 | Lektor przygotował napisy i przełączył tor dźwięku na PCM. Ponowne wybranie dźwięku oraz przewinięcie do bieżącego miejsca zresetowały pozycję z 1,37 do 1,00 s. |
| 18:31:41–46 | Na pozycji 3,74 s wystąpiło około 4,7 s buforowania. Odtwarzanie 4K wróciło. |
| 18:31:49 | Echo Auto Quality podjęło decyzję o zmianie z 4K na 720p. Film grał wtedy z około 1,3 s w buforze. |
| 18:31:54 | Echo rozpoczęło otwieranie 720p po pięciu sekundach odliczania. |

Algorytm awaryjny potwierdził start źródła 4K. Decyzję o rozdzielczości podjął Echo Auto Quality. Pierwsze buforowanie wystąpiło przed uruchomieniem lektora. Lektor przełączył tor audio podczas tego buforowania, więc chwilowy stan odtwarzacza mógł dodatkowo wpłynąć na ocenę Echo. Log pozwala ustalić kolejność, a udział syntezy w obciążeniu systemu pozostaje niezmierzony.

## Zmiana

- `PlaybackStarvation.kt` wymaga 8 sekund kolejnych pomiarów niedopełnionego bufora podczas faktycznego buforowania. Jego obserwacje należą do jednej sesji odtwarzacza. Odtwarzanie, pauza, zmiana źródła i przerwa w obserwacji kończą serię pomiarów.
- `LektorPcm.kt` przed przełączeniem dźwięku odwołuje zaplanowaną zmianę jakości i zgłasza przerwę techniczną na 12 sekund. Przewijanie w ExoPlayerze zakłada 4 sekundy przerwy technicznej. Dłuższy okres z obu zdarzeń zachowuje pierwszeństwo. Awaryjne odzyskiwanie źródła wstrzymuje w tym czasie ocenę zastoju. Błąd odczytu nadal uruchamia odzyskiwanie bez czekania.
- `PlayerRuntimeControllerAutoQuality.kt` ponownie sprawdza aktualny stan po sondzie dźwięku i po odliczaniu do przełączenia. Gdy stan się poprawił, film pozostaje na aktualnym łączu.
- Przy rzeczywistym braku danych algorytm bierze pod uwagę pośrednie rozdzielczości. Używa rozmiaru konkretnego wydania i czasu filmu, żeby oszacować, czy np. plik 1080p zmieści się w przepustowości. Dla źródła bez wiarygodnego rozmiaru zostają dotychczasowe kryteria.

Commit `ff8a3906a`. Zmiany wpięć pozostają w `fork-tools/hooks.tsv`. Oryginalny plik polityki Echo zachowano bez zmian.

## Sprawdzenie na JMGO

- Na klipie 3840 × 2160 z celowo opóźnianymi segmentami wykonano przewijanie pilotem po pojawieniu się pierwszego obrazu. Log zarejestrował rozpoczęcie przerwy technicznej, a odtwarzanie pozostało w 4K. Po zakończeniu pomiaru projektor nadal odtwarzał początkowe źródło.
- Zatrzymano dopływ danych w osobnej próbie 4K. Odzyskiwanie źródła zachowało punkt 17,831 s, uruchomiło zapas 1920 × 1080 i potwierdziło obraz oraz postęp przy 18,534 s.
- Lektor AI w czasie prób sprzętowych był wyłączony. Przełączenie PCM w zgłoszonym przypadku ustalono z logu „Furiosy”, a reset pomiarów po zmianie toru sprawdzono w teście jednostkowym.
- Lokalna kompilacja ARM32 i 75 testów jednostkowych zakończyły się powodzeniem. APK pobrany z JMGO był identyczny z plikiem z kompilacji, SHA-256 `07f6d9f8fb3c40aea8a2f6bd40df128042de73523d9fb4adcdc69874e72b0c2e`. W tej kompilacji pominięto zadanie `lintVitalAnalyzeFullDebug`.
