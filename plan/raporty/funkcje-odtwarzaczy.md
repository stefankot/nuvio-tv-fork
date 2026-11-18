# Funkcje odtwarzaczy forków: co jest w naszym forku (18.11.2026)

Sprawdzone w kodzie gałęzi `hybryda-zrodel`. Porównałem pliki każdego forka (`upstream/*`), których nie ma w bazie Nuvio, i sprawdziłem, czy ktoś je wywołuje. Wyniki testów pochodzą z `STATUS.md` (emulator).

## Jest i działa (sprawdzone na emulatorze)
| Funkcja | Skąd | Uwagi |
|---|---|---|
| Dopasowanie napisów do dźwięku (AutoSync) | Reshaped | podpięte, test OK; włączone domyślnie |
| Rozpoznawanie mowy do synchronizacji (Whisper) | Reshaped + fork | wybór modelu w ustawieniach; trwałość modelu niesprawdzona |
| Automatyczna jakość + przełączanie w trakcie (hot swap) | Echo | test OK: 720p → 1080p w tym samym miejscu filmu; wyłączana przy „Oszczędzaniu łącza” |
| Ręczny wybór rozdzielczości w trakcie | Echo | test OK: 1080p → 4K, wyścig 8 źródeł, pozycja zachowana |
| Wyścig źródeł (start z najszybszego) | Echo | test OK; BRAK przy przechodzeniu do następnego odcinka |
| Menu języka dźwięku (zmiana źródła na takie z wybranym językiem) | Echo | test OK |
| Intro przy starcie odtwarzania | Echo | test OK |
| Miniatury przy przewijaniu | Reshaped | zamiast wersji z Echo i Arc |
| Dolby Vision, HDR10, przekazywanie dźwięku (IEC, TrueHD/MAT) | Reshaped | wszystkie pliki są w forku |
| Automatyczna zmiana źródła po awarii | Reshaped | podpięte |
| IPTV: archiwum (catch-up) i pauza na żywo | Reshaped + Kicia | archiwum, gdy dostawca je udostępnia; pauza w obrębie bufora strumienia |
| Pomijanie czołówki z bazy danych (IntroDB/AniSkip) | baza Nuvio | przycisk „Pomiń intro” |

## Jest kod, ale nie działa albo jest niepodpięty
- Echo: powrót do innego źródła po błędzie dekodera (podpięty, ale w teście nie przełączył).
- KE: test i limit łącza (etap 1 w toku, kompilacja nieukończona); rozgrzewanie połączenia KE skopiowane, niepodpięte.

## Brak w forku (z ważniejszych)
**Echo**
- Wykrywanie napisów końcowych z obrazu (`CreditsDetector`) i dopasowanie czasów pomijania do konkretnego pliku (`SkipTimeFitter`: nazwy rozdziałów, klatki kluczowe). Dziś „Pomiń intro” bierze czasy z bazy danych, które przy innym wydaniu mogą być przesunięte.
- Szybszy start (`FastPlaybackStart`: start po 1,5 s bufora zamiast domyślnych 5 s w bazie) i rozgrzewanie połączenia przed startem (`PlaybackConnectionPrewarmer`). JEST natomiast: wcześniejsze wyszukiwanie źródeł na ekranie szczegółów (`fork/echo/EchoSourcePreload`, przełącznik `fork_detail_preload`, domyślnie ON) oraz ręczne ustawienie „bufor przed startem” w bazie.

**One**
- Pauza i cofanie kanału bez archiwum (`LiveTvTimeshift`): po pauzie aplikacja nagrywa kanał do pamięci podręcznej telewizora i odtwarza z nagrania.
- Napisy na żywo (`LiveCaptions`): ukryte napisy CEA-608/708 w kanałach (głównie USA).
- VLC jako trzeci silnik odtwarzania (`PlayerRuntimeControllerVlc`, biblioteka libvlc 3.7.7).
- Multiview (kilka kanałów naraz), nagrywanie programów, sekcja Sport, VOD z IPTV, Stalker.

**Arc**
- Przyspieszanie wolnego pobierania (`AutoSpeedup`), pamięć szybkości serwerów (`HostSpeeds`), wznawianie po zerwaniu sieci (`NetworkDropRecovery`), zapasowy serwer (`ServerSourceFailover`).
- Pełny Dolby Vision FEL na Amlogic (`AmlDvFel*`), programowy dekoder AV1, panel statystyk, „Zgłoś problem”, przesuwanie napisów pilotem (`SubtitleNudge`), nowy pasek sterowania (`V2PlayerControls`).
- (Około 74 z 273 plików Arc są w forku, ale weszły przez Reshaped, który dzieli z Arc kod dźwięku i miniatur.)

**KE**
- Ochrona przed zawieszeniem obrazu (`PlayerFreezeRecoveryPolicy`), przełączanie między ExoPlayer a mpv przy błędzie (`MpvEngineHopPolicy`), dokładniejsze dopasowanie liczby klatek (`Afr*Probe`), naprawa VC-1, ekran błędu z podpowiedziami (`PlaybackFailure*`).

## Objaśnienia
- **Napisy na żywo (One):** kanały TV, zwłaszcza amerykańskie, często mają napisy ukryte w samym sygnale obrazu, tak jak telegazeta str. 777. Odtwarzacz szuka ich we wszystkich standardowych miejscach (CC1–CC4 i cyfrowe 708), sprawdza, gdzie naprawdę jest tekst, i tylko te ścieżki pokazuje w menu napisów. W polskich kanałach IPTV ten standard prawie nie występuje (u nas napisy idą jako DVB/teletekst), więc pożytek byłby mały.
- **VLC jako zapasowy silnik (One):** VLC otwiera formaty i strumienie, przy których ExoPlayer i mpv się poddają (stare AVI, nietypowe strumienie TS z IPTV, niektóre kodeki dźwięku). Mamy już dwa silniki (ExoPlayer i mpv), więc VLC byłby trzecią deską ratunku. Koszt: biblioteka ok. 30–60 MB na architekturę i osobna ścieżka w kontrolerze odtwarzacza.
