| # | plik | grupa | typ | nazwa | klucz | opis |
|---|---|---|---|---|---|---|
| 1 | skin/kicia/settings/ForgeNightSettings.kt |  | przełącznik | Filtr niebieskiego światła |  | Ciepłe barwy ekranu wieczorem |
| 2 | skin/kicia/settings/ForgeNightSettings.kt |  | przełącznik | Zmniejszona dynamika dźwięku |  | Ciche sceny głośniejsze, głośne cichsze |
| 3 | skin/kicia/settings/ForgeNightSettings.kt |  | wybór/czynność | Harmonogram |  | {null} |
| 4 | skin/kicia/settings/ForgeSettingsContent.kt | System | przełącznik | Uruchamiaj z telewizorem |  | {if (autostart && !canStart} |
| 5 | skin/kicia/settings/ForgeSettingsContent.kt | System | wybór/czynność | Zgoda na start po włączeniu |  | Otwiera ustawienia systemu: włącz „Wyświetlanie nad innymi aplikacjami” dla Nuvio |
| 6 | skin/kicia/settings/ForgeSettingsContent.kt | System | przełącznik | Nuvio jako ekran główny (launcher) |  | {when {} |
| 7 | skin/kicia/settings/ForgeSettingsContent.kt | System | wybór/czynność | Nuvio jako asystent |  | Przycisk Asystenta na pilocie otwiera Szukaj w Nuvio z mikrofonem. Wybór w ustawieniach systemu (wyłącza Asystenta Google) |
| 8 | skin/kicia/settings/ForgeSettingsContent.kt | System | wybór/czynność | Tryb NOOB |  | Tylko najważniejsze opcje na jednym ekranie (z niego wrócisz do pełnych) |
| 9 | skin/kicia/settings/ForgeSettingsContent.kt | Live TV | wybór/czynność | Źródła i EPG |  | Listy kanałów oraz adres EPG, np. https://epg.ovh/pl.gz (okno źródeł Nuvio) |
| 10 | skin/kicia/settings/ForgeSettingsContent.kt | Live TV | wybór/czynność | Logotypy kanałów |  | Skąd brać logo: z EPG (epg.ovh/logo, zalecane), z listy M3U lub spod własnego adresu |
| 11 | skin/kicia/settings/ForgeSettingsContent.kt | Live TV | wybór/czynność | Własny adres logotypów |  | Wzór z {name} = nazwa kanału z EPG, np. $logoPattern (włącz „Własny adres” wyżej) |
| 12 | skin/kicia/settings/ForgeSettingsContent.kt | Live TV | wybór/czynność | Kanały bez programu EPG |  | Kanały z listy, dla których EPG nie ma żadnych programów (nazwy z listy nie pasują do EPG albo kanału tam nie ma) |
| 13 | skin/kicia/settings/ForgeSettingsContent.kt | Live TV | wybór/czynność | Układ Live TV |  | {if (browseB} |
| 14 | skin/kicia/settings/ForgeSettingsContent.kt | Live TV | przełącznik | Karta przypomnień |  | Gdy program z „Przypomnij mi” się zaczyna, karta w rogu obrazu |
| 15 | skin/kicia/settings/ForgeSettingsContent.kt | Home | przełącznik | Półka „YouTube dla Ciebie” |  | Filmy z YouTube na ekranie głównym (więcej danych i pamięci) |
| 16 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Sources |  | [EN] Add, edit or remove your M3U, Xtream and Stalker logins |
| 17 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Categories |  | [EN] Show, hide, rename and order categories and channels |
| 18 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] My playlists |  | [EN] Make, rename, order or delete your own channel lists |
| 19 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Reload channels and guide |  | {stringResource(if (loading)} |
| 20 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Preview sound |  | {stringResource(if (previewSound)} |
| 21 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Show Favorites |  | {stringResource(if (showFavorites)} |
| 22 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Show All channels |  | {stringResource(if (showAll)} |
| 23 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Prefer HLS for replays |  | {stringResource(if (preferHls)} |
| 24 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | [EN] Guide refresh |  | [EN] Every %1$d hours. Channel lists refresh each time Live TV opens. |
| 25 | ui/reshaped/livetv/LiveTvScreen.kt |  | menu TV | ? |  |  |
| 26 | ui/screens/settings/AboutScreen.kt | {null} | wybór/czynność | Polityka prywatności |  | Wyświetl politykę prywatności |
| 27 | ui/screens/settings/AboutScreen.kt | {null} | wybór/czynność | Wesprzyj Nuvio |  | Podziękowania i autorzy projektu |
| 28 | ui/screens/settings/AboutScreen.kt | {null} | wybór/czynność | Licencje i atrybucje |  | Źródła danych, podziękowania i licencje Android |
| 29 | ui/screens/settings/AmbientModeSettings.kt | Tryb Ambient | przełącznik | Tryb Ambient |  | Wypełnij ekran tłami po okresie bez użycia pilota |
| 30 | ui/screens/settings/AmbientModeSettings.kt | Tryb Ambient | wybór/czynność | Uruchom po |  | Czas oczekiwania przed uruchomieniem trybu Ambient |
| 31 | ui/screens/settings/AmbientModeSettings.kt | Tryb Ambient | wybór/czynność | Katalogi |  | Wybierz do 3 katalogów jako źródło grafik |
| 32 | ui/screens/settings/AnimeSkipSettingsScreen.kt | Włącz Anime Skip | przełącznik | Włącz Anime Skip |  | Pobieraj znaczniki czasu z anime-skip.com |
| 33 | ui/screens/settings/AnimeSkipSettingsScreen.kt | Włącz Anime Skip | wybór/czynność | Client ID |  | Wymagany do pobierania znaczników czasu z anime-skip.com |
| 34 | ui/screens/settings/AsrModelSettingsItems.kt |  | ekran | Model rozpoznawania mowy: ${choice.label} |  | ~${choice.sizeMb} MB. Kliknij, aby zmienić; pobierz model poniżej (synchronizacja napisów z audio). |
| 35 | ui/screens/settings/AudioSyncSettingsItems.kt |  | przełącznik | Sync to audio when AutoSync can't |  | When there are no embedded subtitles or the match is too weak, aligns the subtitle with the dialogue by listening to the audio. |
| 36 | ui/screens/settings/AudioSyncSettingsItems.kt |  | ekran | Speech recognition model (${model.sizeMb} MB) |  | {when {} |
| 37 | ui/screens/settings/AudioSyncSettingsItems.kt |  | wybór/czynność | [EN] Próbkowanie dźwięku z wyprzedzeniem |  | [EN] Wybierz, ile danych AudioSync może wykorzystać na próbki dialogu spoza bieżącego fragmentu. |
| 38 | ui/screens/settings/AudioSyncSettingsItems.kt |  | przełącznik | Sample across the film on metered networks |  | Syncs much faster by reading a few short parts of the film ahead of playback, also on metered connections. Uses up to 150 MB per film. |
| 39 | ui/screens/settings/AudioSyncSettingsItems.kt |  | przełącznik | [EN] Show AudioSync statistics |  | [EN] Shows a live panel in the top-left corner of the player with what audio sync has heard and why it is or isn't synced yet. |
| 40 | ui/screens/settings/AudioSyncSettingsItems.kt |  | ekran | Send audio sync log to phone |  | Scan a QR code with a phone on the same network, then download the Audio sync log. No audio or subtitle text is included. |
| 41 | ui/screens/settings/AutoSourceRecoverySettingsItems.kt |  | przełącznik | Automatycznie zmieniaj źródło |  | {stringResource(if (available)} |
| 42 | ui/screens/settings/AutoSyncBubbleSettingsItems.kt |  | przełącznik | [EN] Bubble notifications |  | [EN] Show AutoSync progress and results in a small animated frosted bubble at the bottom of the player instead of plain toasts |
| 43 | ui/screens/settings/AutoSyncBubbleSettingsItems.kt |  | przełącznik | [EN] Only show sync failures |  | [EN] Hide AutoSync and AudioSync progress and success notifications. Show a notification only when sync fails. |
| 44 | ui/screens/settings/AutoSyncSettingsItems.kt |  | przełącznik | Automatyczna synchronizacja napisów |  | Dopasuj napisy z dodatku do napisów osadzonych w wideo. |
| 45 | ui/screens/settings/AutoSyncSettingsItems.kt |  | suwak | Tolerancja synchronizacji |  | Zachowaj oryginalny czas, gdy wymagana korekta jest równa lub mniejsza od tej wartości. |
| 46 | ui/screens/settings/AutoSyncSettingsItems.kt |  | przełącznik | Dzienniki debugowania Auto Sync |  | Zapisz szczegółowy raport czasów Auto Sync i skopiuj go do schowka, aby zgłosić trudne przypadki napisów. |
| 47 | ui/screens/settings/AutoSyncSettingsItems.kt |  | ekran | [EN] Send debug logs to phone |  | [EN] Show a QR code to download AutoSync and audio sync logs on your phone |
| 48 | ui/screens/settings/AutoSyncSettingsItems.kt |  | przełącznik | Dokładne wyszukiwanie synchronizacji |  | Porównuj kolejne ścieżki napisów w poszukiwaniu lepszego dopasowania przed synchronizacją. Po wyłączeniu pierwsze silne dopasowanie jest uży |
| 49 | ui/screens/settings/ConnectionSpeedSettingsItems.kt |  | przełącznik | [EN] Match streams to connection |  | [EN] Keep your addon's stream order, but move streams likely too heavy for your connection to the end. Speed is learned from your own playba |
| 50 | ui/screens/settings/ContentDiscoverySettingsContent.kt | Dodatki | wybór/czynność | Dodatki |  | Zarządzaj dodatkami, kolejnością katalogów i kolekcjami |
| 51 | ui/screens/settings/ContentDiscoverySettingsContent.kt | Dodatki | wybór/czynność | Serwery mediów |  | Połącz Jellyfin lub Emby, aby przeglądać i odtwarzać własną bibliotekę. |
| 52 | ui/screens/settings/ContentDiscoverySettingsContent.kt | Dodatki | wybór/czynność | Wtyczki |  | Zarządzaj repozytoriami i dostawcami strumieni |
| 53 | ui/screens/settings/DebridSettingsScreen.kt |  | przełącznik | Biblioteka w chmurze |  | Przeglądaj i odtwarzaj pliki z połączonych kont. |
| 54 | ui/screens/settings/DebridSettingsScreen.kt |  | przełącznik | Włącz źródła |  | Pokazuj odtwarzalne wyniki z połączonych kont. |
| 55 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Rozwiązuj za pomocą |  | Wybierz, które połączone konto obsługuje odtwarzalne linki. |
| 56 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | {provider.displayName} |  | {if (provider.authMethod == DebridProviderAuthMethod.DeviceCo} |
| 57 | ui/screens/settings/DebridSettingsScreen.kt |  | przełącznik | Przygotuj linki |  | Rozwiąż pierwsze źródła przed rozpoczęciem odtwarzania. |
| 58 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Źródła do przygotowania |  | {null} |
| 59 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Edytor webowy |  | Konfiguruj wyświetlanie, filtry i sortowanie źródeł z innego urządzenia. |
| 60 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Resetuj formatowanie |  | Przywróć domyślne formatowanie źródeł. |
| 61 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Maks. wyników |  | Ogranicz liczbę wyświetlanych źródeł Direct Debrid. |
| 62 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Sortuj strumienie |  | Wybierz kolejność źródeł Direct Debrid. |
| 63 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Limit na rozdzielczość |  | Ogranicz powtarzające się wyniki 2160p, 1080p, 720p po sortowaniu. |
| 64 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Limit na jakość |  | Ogranicz powtarzające się wyniki BluRay, WEB-DL, REMUX po sortowaniu. |
| 65 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | Zakres rozmiaru |  | Filtruj strumienie według rozmiaru pliku. |
| 66 | ui/screens/settings/DebridSettingsScreen.kt |  | wybór/czynność | {title} |  | {subtitle} |
| 67 | ui/screens/settings/EchoPlaybackSettingsItems.kt |  | przełącznik | {title} |  | {subtitle} |
| 68 | ui/screens/settings/EchoPlaybackSettingsItems.kt |  | przełącznik | Polski dźwięk z innego wydania |  | Dobiera najlepszy obraz i synchronizuje z nim pasującą polską ścieżkę. |
| 69 | ui/screens/settings/EchoPlaybackSettingsItems.kt |  | przełącznik | Preloading źródeł na ekranie szczegółów |  | Wyszukuje źródła w tle, gdy otwierasz film lub serial, żeby Play startował szybciej |
| 70 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Podstawy odtwarzania | wybór/czynność | Wybór streamu |  | Wybieraj ręcznie lub automatycznie odtwarzaj pierwszy dostępny stream. |
| 71 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Podstawy odtwarzania | przełącznik | Automatyczne odtwarzanie następnego odcinka |  | Kontynuuj do następnego odcinka po zakończeniu streamu. |
| 72 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Podstawy odtwarzania | przełącznik | Rekomendacje po odtwarzaniu |  | Pokaż rekomendowane filmy i seriale po zakończeniu odtwarzania. |
| 73 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Podstawy odtwarzania | przełącznik | Streamy P2P |  | Zezwalaj na źródła streamów peer-to-peer. |
| 74 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Napisy i audio | wybór/czynność | Język napisów |  | Preferowany język napisów. |
| 75 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Napisy i audio | przełącznik | Używaj wymuszonych napisów |  | Preferuj wymuszone napisy, gdy dźwięk pasuje do języka napisów; jeśli są niedostępne, nie wybieraj niczego |
| 76 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Napisy i audio | wybór/czynność | Język audio |  | Preferowany język audio. |
| 77 | ui/screens/settings/EssentialPlaybackSettingsContent.kt | Napisy i audio | wybór/czynność | Priorytet dekodera |  | Kontroluje czy używane są dekodery sprzętowe czy programowe (FFmpeg) dla audio i wideo |
| 78 | ui/screens/settings/ForkModuleSettingsItems.kt |  | przełącznik | {m.title} |  |  |
| 79 | ui/screens/settings/ForkModuleSettingsItems.kt |  | wybór/czynność | {a.title} |  | {a.description} |
| 80 | ui/screens/settings/ForkModuleSettingsItems.kt |  | przełącznik | {s.title} |  | {s.description} |
| 81 | ui/screens/settings/ForkSwitchesSettingsItems.kt |  | przełącznik | {title} |  | {subtitle} |
| 82 | ui/screens/settings/ForkSwitchesSettingsItems.kt |  | przełącznik | Kalendarz (Echo) | fork_calendar | Pozycja Kalendarz w menu |
| 83 | ui/screens/settings/ForkSwitchesSettingsItems.kt |  | przełącznik | Wyszukiwarka ustawień | fork_settings_search | Przycisk wyszukiwania w Ustawieniach |
| 84 | ui/screens/settings/ForkSwitchesSettingsItems.kt |  | przełącznik | Menu ustawień w kategoriach | fork_settings_nav | Tryb zaawansowany: 7 kategorii z podkategoriami zamiast długiej listy |
| 85 | ui/screens/settings/ForkSwitchesSettingsItems.kt |  | przełącznik | Zdalne ustawienia (testy) | com.nuvio.tv.fork.devprefs.DevPrefsServer.KEY | Pozwala zmieniać ustawienia z komputera przez kabel. Działa po ponownym uruchomieniu |
| 86 | ui/screens/settings/ForkSwitchesSettingsItems.kt |  | przełącznik | Pilot Arc poza odtwarzaczem | fork_arc_remote | Przyciski przypisane do „Nic” są wyciszone w całej aplikacji |
| 87 | ui/screens/settings/KeCatalogSettingsItems.kt |  | przełącznik | YouTube (Nuvio One) |  | Przeglądanie i odtwarzanie YouTube |
| 88 | ui/screens/settings/KeCatalogSettingsItems.kt |  | ekran | ? |  |  |
| 89 | ui/screens/settings/KeCatalogSettingsItems.kt |  | przełącznik | Katalog IPTV (KE) |  | Kanały wg kraju, kategorii i języka z repozytorium KE + test łączy |
| 90 | ui/screens/settings/KeCatalogSettingsItems.kt |  | ekran | ? |  |  |
| 91 | ui/screens/settings/KeCatalogSettingsItems.kt |  | ekran | ? |  |  |
| 92 | ui/screens/settings/KeCatalogSettingsItems.kt |  | ekran | ? |  |  |
| 93 | ui/screens/settings/KeCatalogSettingsItems.kt |  | ekran | ? |  |  |
| 94 | ui/screens/settings/KeCatalogSettingsItems.kt |  | ekran | ? |  |  |
| 95 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Pokaż Kontynuuj oglądanie |  | Pokaż rząd Kontynuuj oglądanie na ekranie głównym. |
| 96 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Używaj miniatur odcinków w Kontynuuj oglądanie |  | Używaj miniatur odcinków jako domyślnego obrazu. Po wyłączeniu używa tła serii. |
| 97 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Rozmyj nieobejrzane w Kontynuuj oglądanie |  | Rozmyj miniatury następnych odcinków w sekcji Kontynuuj oglądanie |
| 98 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Następny z najdalszego odcinka |  | Pokazuj następny odcinek na podstawie najdalszego obejrzanego. Wyłącz przy ponownym oglądaniu, aby używać ostatnio obejrzanego odcinka. |
| 99 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Pokaż niewyemitowane odcinki |  | Uwzględnij nadchodzące odcinki w Kontynuuj oglądanie przed ich emisją. |
| 100 | ui/screens/settings/LayoutCardSettings.kt |  | wybór/czynność | Po wybraniu |  | Co otwiera się po wybraniu elementu Kontynuuj oglądanie |
| 101 | ui/screens/settings/LayoutCardSettings.kt |  | wybór/czynność | Kolejność sortowania |  | Jak elementy Kontynuuj oglądanie są ułożone |
| 102 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Rozwiń zaznaczony plakat do tła |  | Rozwiń zaznaczony plakat po opóźnieniu bezczynności. |
| 103 | ui/screens/settings/LayoutCardSettings.kt |  | suwak | Opóźnienie rozwinięcia tła |  | Jak długo czekać przed rozwinięciem zaznaczonych kart. |
| 104 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | {stringResource(                 if (isModern)} |  | {stringResource(                 if (isModern)} |
| 105 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Odtwarzaj zwiastun bez dźwięku |  | {stringResource(                     if (isModern)} |
| 106 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Kontynuuj zwiastun w szczegółach |  | Zwiastun gra dalej po otwarciu strony szczegółów. |
| 107 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Tryb poziomy dla plakatów |  | Używaj poziomych kart wszędzie. |
| 108 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Zawsze tło z logo dla poziomych plakatów |  | Ignoruj poziomy plakat z dodatku i użyj tła z nakładką logo. |
| 109 | ui/screens/settings/LayoutCardSettings.kt |  | wybór/czynność | Skonfiguruj przez telefon |  | {if (isActive} |
| 110 | ui/screens/settings/LayoutCardSettings.kt |  | wybór/czynność | Wyczyść |  | {null} |
| 111 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | {stringResource(label)} |  | {null} |
| 112 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | Włącz efekt głębi |  | Dodaje podświetloną górną krawędź i delikatny połysk do kart graficznych, tworząc subtelne wrażenie głębi. |
| 113 | ui/screens/settings/LayoutCardSettings.kt |  | wybór/czynność | Dostrojenie |  | Użyj lewo i prawo na pilocie, aby regulować każdą wartość. |
| 114 | ui/screens/settings/LayoutCardSettings.kt |  | przełącznik | {title} |  | {null} |
| 115 | ui/screens/settings/LayoutCardSettings.kt |  | suwak | Poświata krawędzi |  |  |
| 116 | ui/screens/settings/LayoutCardSettings.kt |  | suwak | Połysk |  |  |
| 117 | ui/screens/settings/LayoutCardSettings.kt |  | suwak | Pokrycie krawędzi |  |  |
| 118 | ui/screens/settings/LayoutDetailSettings.kt |  | wybór/czynność | Styl opcji odcinka |  | Wybierz wygląd ekranu po przytrzymaniu odcinka. |
| 119 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Losowanie |  | Losuj odcinki z dowolnego serialu. Wyłączenie wstrzymuje losowanie. |
| 120 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Plakaty sezonów |  | Pokazuj sezony jako plakaty zamiast zakładek tekstowych, jeśli są dostępne. |
| 121 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Rozmyj nieobejrzane odcinki |  | Rozmyj miniatury odcinków do czasu obejrzenia, aby uniknąć spoilerów. |
| 122 | ui/screens/settings/LayoutDetailSettings.kt |  | wybór/czynność | Oceny odcinków |  | Wybierz, które oceny odcinków mają być widoczne. |
| 123 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Auto-odtwarzanie zwiastunów |  | Automatycznie odtwarzaj zwiastuny na ekranie szczegółów po okresie bezczynności |
| 124 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Odtwarzaj w tle |  | Odtwarzaj zwiastun w miejscu tła, gdy strona jest widoczna. |
| 125 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Pauza przy przewijaniu |  | Wstrzymaj zwiastun podczas przewijania i wznów po powrocie na górę. |
| 126 | ui/screens/settings/LayoutDetailSettings.kt |  | suwak | Opóźnienie zwiastuna |  | Odtwarzaj zwiastuny w rozdzielczości powyżej 1080p, gdy są dostępne. Zużywa znacznie więcej pamięci; domyślnie wyłączone na urządzeniach z 2 |
| 127 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Zwiastuny 4K |  | Odtwarzaj zwiastuny w rozdzielczości powyżej 1080p, gdy są dostępne. Zużywa znacznie więcej pamięci; domyślnie wyłączone na urządzeniach z 2 |
| 128 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Napisy w zwiastunach |  | Pokazuj napisy w zwiastunach w preferowanym języku napisów, jeśli są dostępne. |
| 129 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Pokaż przycisk zwiastuna |  | Pokaż przycisk zwiastuna na stronie szczegółów (tylko gdy zwiastun jest dostępny). |
| 130 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Preferuj metadane z zewnętrznego dodatku |  | Używaj metadanych z zewnętrznego dodatku zamiast dodatku katalogowego. |
| 131 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Pokaż pełną datę premiery |  | Dla filmów pokazuj pełną datę premiery zamiast samego roku. |
| 132 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Znaczniki rozmiaru |  | Pokazuj znaczniki rozmiaru pliku w wynikach źródeł i panelach odtwarzacza. |
| 133 | ui/screens/settings/LayoutDetailSettings.kt |  | wybór/czynność | Pozycja odznak |  | {null} |
| 134 | ui/screens/settings/LayoutDetailSettings.kt |  | wybór/czynność | Adresy URL znaczników Fusion |  | {streamBadgeRulesPreview(streamBadgeUiState} |
| 135 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Pokaż w odtwarzaczu |  | Wyświetlaj znaczniki wideo i audio pod tytułem w odtwarzaczu. |
| 136 | ui/screens/settings/LayoutDetailSettings.kt |  | wybór/czynność | Znaczniki w odtwarzaczu |  | {playerBadgesLabel(streamBadgeUiState} |
| 137 | ui/screens/settings/LayoutDetailSettings.kt |  | przełącznik | Logo dodatku |  | Pokazuj logo i nazwę dodatku obok źródeł. |
| 138 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Pełnoekranowe tło hero |  | Rozciągnij tło hero na cały ekran. |
| 139 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Gradient zaznaczonego elementu |  | Wmieszaj kolory grafiki w prawą stronę klasycznego ekranu głównego. |
| 140 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Pokaż sekcję hero |  | Wyświetl karuzelę hero na górze strony głównej. |
| 141 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Pokaż etykiety plakatów |  | Pokaż tytuły pod plakatami w wierszach, siatce i widoku wszystkich. |
| 142 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Pokaż nazwę dodatku |  | Pokaż nazwę źródła pod tytułami katalogów. |
| 143 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Pokaż typ katalogu |  | Pokaż sufiks typu obok nazwy katalogu (Film/Serial). |
| 144 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Ukryj niewydane treści |  | Ukryj filmy i seriale, które jeszcze nie zostały wydane. |
| 145 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Ogólne oceny |  | {stringResource(             if (uiState.homeImdbRatingsVisib} |
| 146 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Automatyczne przewijanie opisów |  | Powoli przewijaj długie opisy na ekranie głównym zamiast je ucinać. |
| 147 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Nowoczesny pasek boczny |  | Włącz pływającą nawigację boczną. |
| 148 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Rozmycie nowoczesnego paska bocznego |  | Przełącz efekt rozmycia dla nowoczesnych powierzchni bocznych. |
| 149 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Ukryj pływający wskaźnik |  | Ukryj pływający wskaźnik nawigacji na ekranie głównym. |
| 150 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Zwiń pasek boczny |  | Ukryj pasek boczny domyślnie; pokaż po skupieniu. |
| 151 | ui/screens/settings/LayoutHomeSettings.kt |  | przełącznik | Pokaż Odkrywaj w wyszukiwarce |  | Pokaż sekcję przeglądania gdy wyszukiwarka jest pusta. |
| 152 | ui/screens/settings/LayoutHomeSettings.kt |  | wybór/czynność | Lokalizacja Odkrywaj |  | {null} |
| 153 | ui/screens/settings/LiveTvSettingsItems.kt |  | przełącznik | [EN] Live TV |  | [EN] Show Live TV in the menu, for M3U, Xtream and Stalker channel lists |
| 154 | ui/screens/settings/LiveTvSettingsItems.kt |  | przełącznik | [EN] Channel previews |  | [EN] Play a small picture of the channel you are on in the Live TV list |
| 155 | ui/screens/settings/LiveTvSettingsItems.kt |  | przełącznik | [EN] Preview sound |  | [EN] Hear the channel in the preview. It fades in, and stops when you leave the list |
| 156 | ui/screens/settings/MDBListSettingsScreen.kt |  | przełącznik | Włącz oceny MDBList |  | Pobieraj oceny od zewnętrznych dostawców na ekranie szczegółów |
| 157 | ui/screens/settings/MDBListSettingsScreen.kt |  | wybór/czynność | Klucz API |  | Użyj konta MDBList połączonego w Śledzeniu lub wprowadź oddzielny klucz API dla ocen. |
| 158 | ui/screens/settings/MDBListSettingsScreen.kt |  | przełącznik | Oceny na stronie głównej |  | Pokaż oceny MDBList na karuzeli głównej (modern i classic/grid) |
| 159 | ui/screens/settings/MdbListLibraryListsDialog.kt |  | przełącznik | {option.name} |  | {null} |
| 160 | ui/screens/settings/MediaServerSettingsScreen.kt | Włączony | przełącznik | Włączony |  | Pokazuj ten serwer na stronie głównej, w wyszukiwaniu i w wynikach strumieniowania. |
| 161 | ui/screens/settings/MediaServerSettingsScreen.kt | Włączony | przełącznik | Użyj metadanych z dodatków |  | Tytuły otwierają się ze szczegółami z Twoich dodatków i synchronizują się jak inne tytuły z katalogu. Odtwarzanie i oznaczenia obejrzanych n |
| 162 | ui/screens/settings/MediaServerSettingsScreen.kt | Włączony | przełącznik | Importuj postęp oglądania |  | Dodaj tytuły oglądane w %1$s do Kontynuuj oglądanie wraz z oznaczeniami obejrzanych. Odtwarzanie wznawia się z %1$s niezależnie od tego usta |
| 163 | ui/screens/settings/MediaServerSettingsScreen.kt | Włączony | przełącznik | {library.name} |  | {null} |
| 164 | ui/screens/settings/MediaServerSettingsScreen.kt | Włączony | wybór/czynność | Odśwież biblioteki |  | {null} |
| 165 | ui/screens/settings/MediaServerSettingsScreen.kt | Włączony | wybór/czynność | Zaloguj się ponownie |  | {null} |
| 166 | ui/screens/settings/MediaServerSettingsScreen.kt | Włączony | wybór/czynność | Usuń serwer |  | {null} |
| 167 | ui/screens/settings/MediaServersSettingsScreen.kt |  | wybór/czynność | {connection.name} |  | {connection.statusText(} |
| 168 | ui/screens/settings/MediaServersSettingsScreen.kt |  | wybór/czynność | {provider.displayName} |  | {when (provider.id} |
| 169 | ui/screens/settings/NetflixUiSettingsItems.kt |  | przełącznik | [EN] Netflix-style Home |  | [EN] A billboard on top and rows whose focused title widens to show its details. Only the look changes; rows, settings and actions stay the  |
| 170 | ui/screens/settings/NetworkSettingsScreen.kt | Tryb doświadczenia | wybór/czynność | Przełącz na Podstawowy |  | Ukryj zaawansowane opcje bez zmiany zapisanych wartości. |
| 171 | ui/screens/settings/NetworkSettingsScreen.kt | Wydajność i nawigacja | przełącznik | Szybka nawigacja pozioma |  | Zwiększ prędkość powtarzania D-pad w wierszach przy zachowaniu ograniczania powtórzeń. |
| 172 | ui/screens/settings/NetworkSettingsScreen.kt | Wydajność i nawigacja | przełącznik | Przewijanie fokusa Nuvio |  | Używaj niestandardowej globalnej animacji przewijania i pozycjonowania fokusa Nuvio. |
| 173 | ui/screens/settings/NetworkSettingsScreen.kt | Wydajność i nawigacja | przełącznik | Dekodowanie obrazu RGB565 |  | Użyj oszczędnego 16-bitowego koloru dla kwalifikujących się nieprzezroczystych obrazów. Zmiana wymaga restartu aplikacji. |
| 174 | ui/screens/settings/NetworkSettingsScreen.kt | Wydajność i nawigacja | przełącznik | Sprzętowe dekodowanie obrazów |  | Trzymaj zdekodowane obrazy tylko w pamięci grafiki. Zużywa mniej RAM, ale na słabszych GPU ładowanie nowych obrazów może przycinać. |
| 175 | ui/screens/settings/NetworkSettingsScreen.kt | Uruchamianie i zamykanie | przełącznik | Ekran startowy |  | Pokaż logo i wskaźnik ładowania po wyborze profilu |
| 176 | ui/screens/settings/NetworkSettingsScreen.kt | Uruchamianie i zamykanie | przełącznik | Zapamiętaj ostatni profil |  | Zapamiętaj ostatnio wybrany profil przy uruchomieniu |
| 177 | ui/screens/settings/NetworkSettingsScreen.kt | Uruchamianie i zamykanie | przełącznik | Pełne zamknięcie aplikacji |  | Całkowicie zamyka aplikację i zwalnia pamięć przy wyjściu. Wymaga dwukrotnego naciśnięcia wstecz w celu potwierdzenia. |
| 178 | ui/screens/settings/NetworkSettingsScreen.kt | Diagnostyka | przełącznik | Raporty awarii Sentry |  | Wysyłaj raporty awarii i ANR z bezpiecznym kontekstem. Domyślnie włączone. |
| 179 | ui/screens/settings/NetworkSettingsScreen.kt | Diagnostyka | przełącznik | Raporty problemów z odtwarzaniem |  | Pokaż przyciski zgłaszania problemów podczas odtwarzania, długiego ładowania i błędów odtwarzania |
| 180 | ui/screens/settings/NetworkSettingsScreen.kt | Diagnostyka | przełącznik | Nakładka statystyk odtwarzania |  | Pokaż przycisk w informacjach o strumieniu, umożliwiający podgląd na żywo użycia CPU, pamięci, bufora, bitrate i temperatury podczas odtwarz |
| 181 | ui/screens/settings/NetworkSettingsScreen.kt | Testy sieci | wybór/czynność | {stringResource(                         if (isRunning)} |  | Zmierz prędkość pobierania i opóźnienie |
| 182 | ui/screens/settings/NetworkSettingsScreen.kt | Testy sieci | wybór/czynność | {stringResource(                         if (isStreamRunning)} |  | {if (hasStream} |
| 183 | ui/screens/settings/NetworkSettingsScreen.kt | Pamięć podręczna | wybór/czynność | Wyczyść pamięć podręczną Kontynuuj oglądanie |  | {if (cleared} |
| 184 | ui/screens/settings/NetworkSettingsScreen.kt | Diagnostyka DV | wybór/czynność | Tryb konwersji |  | Tryb konwersji (wybierany automatycznie, nie zmieniaj bez instrukcji). Wybierz, aby wymusić; Brak używa auto. Aktywne tylko gdy Obsługa DV7  |
| 185 | ui/screens/settings/PillNavSettingsItems.kt |  | przełącznik | [EN] Pill menu |  | {if (forcedByNetflixUi} |
| 186 | ui/screens/settings/PlaybackAudioSettings.kt |  | wybór/czynność | Preferowany język audio |  | {null} |
| 187 | ui/screens/settings/PlaybackAudioSettings.kt |  | wybór/czynność | Drugi preferowany język |  | {null} |
| 188 | ui/screens/settings/PlaybackAudioSettings.kt |  | przełącznik | Pomijaj ciszę |  | Pomijaj ciche fragmenty podczas odtwarzania |
| 189 | ui/screens/settings/PlaybackAudioSettings.kt |  | wybór/czynność | Tryb nocny |  | Wzmacnia dialogi i ciche dźwięki, dzięki czemu możesz oglądać przy niskiej głośności |
| 190 | ui/screens/settings/PlaybackAudioSettings.kt |  | przełącznik | Zapamiętaj opóźnienie dźwięku per urządzenie |  | Zapisz i przywróć opóźnienie dźwięku w odtwarzaczu dla bieżącego wyjścia audio |
| 191 | ui/screens/settings/PlaybackAudioSettings.kt |  | wybór/czynność | Priorytet dekodera |  | {null} |
| 192 | ui/screens/settings/PlaybackAudioSettings.kt |  | przełącznik | Włącz downmix |  | Używa ścieżki downmix FFmpeg. Wymaga opcji Preferuj dekodery aplikacji (FFmpeg). Po wyłączeniu audio korzysta z poprzedniej ścieżki Android/ |
| 193 | ui/screens/settings/PlaybackAudioSettings.kt |  | wybór/czynność | Liczba kanałów |  | {null} |
| 194 | ui/screens/settings/PlaybackAudioSettings.kt |  | przełącznik | Zachowaj oryginalne audio przy downmix |  | Po wyłączeniu normalizacja downmix może zmniejszyć ogólną głośność. |
| 195 | ui/screens/settings/PlaybackAudioSettings.kt |  | wybór/czynność | Poziom LFE przy downmiksie |  | Miksuje kanał subwoofera (LFE) do pozostałych głośników zamiast go pomijać. Tylko dla układów bez LFE, takich jak 2.0. |
| 196 | ui/screens/settings/PlaybackAudioSettings.kt |  | przełącznik | Tunelowane odtwarzanie |  | Synchronizacja audio/wideo na poziomie sprzętu. Może poprawić odtwarzanie na niektórych urządzeniach Android TV |
| 197 | ui/screens/settings/PlaybackAudioSettings.kt |  | przełącznik | Wymuś transkodowanie AC-3 (Optyczne/SPDIF) |  | Transkoduje formaty wielokanałowe (TrueHD, DTS, AAC itp.) do Dolby Digital 5.1 w czasie rzeczywistym dla połączeń optycznych/SPDIF. |
| 198 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | Automatyczny wybór strumienia |  | {null} |
| 199 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | Wzorzec Regex |  | {settings.streamAutoPlayRegex.ifBlank { stringResource(R.stri} |
| 200 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | [EN] Ranked regex rules |  | {if (settings.streamAutoPlayRankedRegex.isEmpty(} |
| 201 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Przejdź do kolejnego pasującego źródła przy błędzie |  | {stringResource(                 if (blockedByEngineSwitch)} |
| 202 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | suwak | Źródła do sprawdzenia przy błędzie |  | Ile kolejnych pasujących źródeł sprawdzić, zanim pojawi się błąd. |
| 203 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | suwak | Czas na uruchomienie źródła |  | Jak długo czekać na uruchomienie źródła, zanim zostanie sprawdzone kolejne. |
| 204 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | Zakres źródeł auto-play |  | {null} |
| 205 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | Dozwolone dodatki |  | {null} |
| 206 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | Dozwolone wtyczki |  | {null} |
| 207 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | suwak | Limit czasu wyboru strumienia |  | Czas oczekiwania na dodatki przed wyborem. |
| 208 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Wstępne ładowanie źródeł następnego odcinka |  | Rozpocznij wyszukiwanie źródeł w tle zanim pojawi się przycisk następnego odcinka. |
| 209 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Ponownie użyj ostatniego linku |  | Auto-odtwarzaj ostatni działający strumień dla tego samego filmu/odcinka gdy cache jest aktualny |
| 210 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | Czas ważności cache linku |  | {null} |
| 211 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Auto-odtwarzaj następny odcinek |  | Automatycznie rozpocznij następny odcinek gdy pojawi się monit. |
| 212 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Fallback gdy grupa binge nie zostanie znaleziona |  | Automatycznie wybierz pierwszy dostępny strumień jeśli grupa binge nie zostanie znaleziona. Gdy wyłączone, wyświetla wybór źródła. |
| 213 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | wybór/czynność | Określanie progu następnego odcinka |  | {null} |
| 214 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | suwak | Próg procentowy |  | Zapasowy gdy brak znacznika outro. |
| 215 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | suwak | Próg minutowy |  | Zapasowy gdy brak znacznika outro. |
| 216 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Czy nadal oglądasz? |  | Pytaj po kolejnych automatycznie odtworzonych odcinkach. |
| 217 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | suwak | Próg odcinków |  | Liczba automatycznie odtworzonych odcinków przed zapytaniem. |
| 218 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Preferuj grupę Binge (następny odcinek) |  | Najpierw spróbuj tego samego profilu źródła przed normalnymi regułami auto-odtwarzania. |
| 219 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Ponownie użyj grupy Binge |  | Zapamiętaj i ponownie użyj ostatniej grupy binge między sesjami (Kontynuuj oglądanie, Szczegóły itp.). |
| 220 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | przełącznik | Rekomendacje po odtwarzaniu |  | Pokaż rekomendowane filmy i seriale po zakończeniu odtwarzania. |
| 221 | ui/screens/settings/PlaybackAutoPlaySettings.kt |  | suwak | Czas rekomendacji filmowych |  | Wybierz kiedy pojawiają się rekomendacje filmowe. Odcinki korzystają z ustawienia progu następnego odcinka. |
| 222 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Pamięć natywna ExoPlayer |  | Włącz natywny alokator pamięci poza stertą i optymalizacje przewijania. |
| 223 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Niestandardowe bufory odtwarzania |  | Nadpisz domyślne buforowanie Media3 wartościami poniżej. Gdy wyłączone, odtwarzacz używa standardowych buforów Media3. |
| 224 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Niestandardowa sieć |  | Używaj wielu równoległych połączeń do pobierania progresywnych strumieni. Gdy wyłączone, używane jest jedno połączenie. |
| 225 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Min. czas buforowania |  | Minimalna ilość multimediów do buforowania. Odtwarzacz będzie utrzymywał co najmniej tyle treści zbuforowanej przed bieżącą pozycją. |
| 226 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Maks. czas buforowania |  | Maksymalna ilość multimediów do buforowania. Musi być co najmniej tyle co minimalna wartość. Wyższe wartości zużywają więcej pamięci, ale za |
| 227 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Bufor początkowy |  | Ile treści musi być zbuforowane przed rozpoczęciem odtwarzania. Niższe wartości startują szybciej, ale mogą powodować zacięcia na wolnych po |
| 228 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Bufor po ponownym buforowaniu |  | Ile treści zbuforować po zatrzymaniu odtwarzania z powodu buforowania. Wyższe wartości zmniejszają powtarzające się przerwy. |
| 229 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Czas bufora wstecznego |  | Ile już odtworzonej treści trzymać w pamięci. Umożliwia szybkie przewijanie wstecz bez ponownego pobierania. Ustaw 0, aby wyłączyć i zaoszcz |
| 230 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Zarządzany budżet pamięci |  | Ogranicza bufor do bezpiecznej części pamięci urządzenia. Wyłącz, aby ręcznie ustawić docelowy rozmiar bufora (zaawansowane — duże wartości  |
| 231 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Docelowy rozmiar bufora |  | Maksymalna ilość RAM używana do buforowania w przód. Maksimum jest obliczane z dostępnej pamięci urządzenia, więc urządzenia z większą ilośc |
| 232 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Pozwól na większy docelowy bufor |  | Usuwa limit pamięci urządzenia dla suwaka docelowego rozmiaru bufora, pozwalając na wartości do 2GB. Może crashować na urządzeniach z mniej  |
| 233 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Cache dyskowy VOD |  | Zapisuj pobrane bajty na dysku dla bieżącego strumienia. Rozszerza natychmiastowe przewijanie wstecz poza bufor w pamięci i przetrwa krótkie |
| 234 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Automatyczny rozmiar |  | Gdy włączony, rozmiar cache jest obliczany z wolnego miejsca na dysku. Wyłącz, aby wybrać rozmiar ręcznie. |
| 235 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Rozmiar cache VOD |  | Maksymalne użycie dysku dla progresywnego cache VOD (usuwanie LRU). |
| 236 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | [EN] Enable HTTP/2 |  | [EN] Enable HTTP/2 protocol for network connections, allowing request multiplexing and faster handshake times. |
| 237 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | przełącznik | Połączenia równoległe |  | Używaj wielu połączeń równolegle do pobierania strumieni. Aktywuj, gdy doświadczasz buforowania mimo wystarczającej prędkości pobierania. |
| 238 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Liczba połączeń |  | Liczba równoległych połączeń TCP. Wyższe wartości zwiększają zużycie pamięci i przepustowość, ale z malejącymi zyskami. |
| 239 | ui/screens/settings/PlaybackBufferNetworkSettings.kt |  | suwak | Rozmiar fragmentu |  | Rozmiar każdego fragmentu pobierania na połączenie. Większe fragmenty zmniejszają narzut, ale zużywają więcej pamięci. |
| 240 | ui/screens/settings/PlaybackP2pSettings.kt |  | przełącznik | Włącz P2P |  | {null} |
| 241 | ui/screens/settings/PlaybackP2pSettings.kt |  | przełącznik | Ukryj statystyki torrentów |  | Ukryj bufor, seedy, peery i prędkość pobierania podczas ładowania i odtwarzania |
| 242 | ui/screens/settings/PlaybackP2pSettings.kt |  | wybór/czynność | Profil torrenta |  | {stringResource(profileDescription(p2p.profile)} |
| 243 | ui/screens/settings/PlaybackP2pSettings.kt |  | wybór/czynność | Rozmiar cache torrentów |  | Pobrane dane torrentów przechowywane na urządzeniu w celu szybszego ponownego odtwarzania |
| 244 | ui/screens/settings/PlaybackP2pSettings.kt |  | wybór/czynność | Wyczyść cache torrentów |  | {p2p.cacheSummary} |
| 245 | ui/screens/settings/PlaybackPlayerSettings.kt |  | wybór/czynność | Domyślny odtwarzacz |  | {null} |
| 246 | ui/screens/settings/PlaybackPlayerSettings.kt |  | wybór/czynność | Silnik wewnętrzny |  | {null} |
| 247 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Automatyczne przełączanie silnika przy błędzie |  | {stringResource(             if (blockedBySourceFallback)} |
| 248 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Zwalniaj dekoder w tle |  | Zwalnia dekoder wideo, gdy Nuvio przechodzi w tło. Włącz, jeśli po powrocie do aplikacji odtwarzanie kończy się błędem dekodera. Po powrocie |
| 249 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Przekazuj napisy do zewnętrznego odtwarzacza |  | Pobierz napisy w preferowanym języku z dodatków i przekaż je do zewnętrznego odtwarzacza. |
| 250 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Wysyłaj znaczniki intro i outro |  | Przekazuj rozwiązane znaczniki pominięcia intro/outro do zewnętrznego odtwarzacza w celu automatycznego pomijania. Działa tylko w odtwarzacz |
| 251 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Pomiń intro |  | Używaj introdb.app do wykrywania intro i podsumowań. |
| 252 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | {title} |  | {subtitle} |
| 253 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Ekan ładowania |  | Pokaż ekran ładowania do pojawienia się pierwszej klatki. |
| 254 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Pokaż status ładowania |  | Wyświetlaj postęp krok po kroku podczas inicjalizacji odtwarzacza. |
| 255 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Ekran pauzy |  | Pokaż ekran ze szczegółami po 5 sekundach pauzy. |
| 256 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Zegar OSD |  | Pokaż aktualny czas i czas zakończenia podczas wyświetlania kontrolek. |
| 257 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Ostrzeżenie o charakterze treści |  | Pokaż ostrzeżenie o nagości, przemocy i wulgarności na początku odtwarzania. |
| 258 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Pokaż rozdziały |  | Odczytuj rozdziały z plików MKV, aby oznaczać je na pasku postępu i przeskakiwać między nimi. |
| 259 | ui/screens/settings/PlaybackPlayerSettings.kt |  | przełącznik | Pokaż tytuł rozdziału |  | Pokazuj nazwę bieżącego rozdziału nad paskiem postępu. |
| 260 | ui/screens/settings/PlaybackRemoteButtons.kt |  | wybór/czynność | [EN] remote_buttons_setup_title |  | [EN] remote_buttons_setup_subtitle |
| 261 | ui/screens/settings/PlaybackRemoteButtons.kt |  | wybór/czynność | [EN] remote_buttons_number_keys |  | [EN] remote_buttons_number_keys_desc |
| 262 | ui/screens/settings/PlaybackRemoteButtons.kt |  | wybór/czynność | {remoteKeyLabel(context} |  | {remoteRowSubtitle(context} |
| 263 | ui/screens/settings/PlaybackRemoteButtons.kt |  | wybór/czynność | [EN] remote_buttons_reset |  | {null} |
| 264 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | wybór/czynność | Preferowany język |  | {null} |
| 265 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | wybór/czynność | Drugi preferowany język |  | {null} |
| 266 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | przełącznik | Używaj wymuszonych napisów |  | Preferuj wymuszone napisy, gdy dźwięk pasuje do języka napisów; jeśli są niedostępne, nie wybieraj niczego |
| 267 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | przełącznik | Pokaż tylko preferowane języki |  | Ukryj wszystkie inne języki napisów na liście wyboru |
| 268 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | przełącznik | Usuń napisy SDH |  | Ukryj opisy dźwięków i etykiety mówców z napisów tekstowych |
| 269 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | wybór/czynność | Wygląd napisów |  | Czcionka, kolory, tło i cień |
| 270 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | przełącznik | Osobny styl HDR / Dolby Vision |  | Użyj innego koloru tekstu i niższej jasności napisów dla wideo HDR i Dolby Vision |
| 271 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | kolor | Kolor tekstu HDR |  |  |
| 272 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | suwak | Jasność napisów HDR |  | Zastosuj jasność HDR do elementów sterowania i menu odtwarzacza wyświetlanych na wideo HDR i Dolby Vision |
| 273 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | przełącznik | Przyciemnij elementy sterowania w HDR |  | Zastosuj jasność HDR do elementów sterowania i menu odtwarzacza wyświetlanych na wideo HDR i Dolby Vision |
| 274 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | przełącznik | Używaj libass dla napisów ASS/SSA |  | Zaawansowane renderowanie ASS/SSA (style, pozycjonowanie, animacje) |
| 275 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | wybór/czynność | Tryb renderowania ASS |  | {if (settings.effectiveTunnelingEnabled} |
| 276 | ui/screens/settings/PlaybackSubtitleSettings.kt |  | wybór/czynność | Rozdzielczość renderowania napisów |  | Niższa rozdzielczość poprawia wydajność przy ciężkim typesettingu kosztem ostrości |
| 277 | ui/screens/settings/PlaybackVideoSettings.kt |  | wybór/czynność | Dopasowanie częstotliwości klatek |  | {null} |
| 278 | ui/screens/settings/PlaybackVideoSettings.kt |  | przełącznik | Dopasowanie rozdzielczości |  | {stringResource(             if (resolutionSwitchingSupported} |
| 279 | ui/screens/settings/PlaybackVideoSettings.kt |  | wybór/czynność | Obsługa Dolby Vision Profile 7 |  | {null} |
| 280 | ui/screens/settings/PlaybackVideoSettings.kt |  | przełącznik | Zachowaj mapowanie DV (DV7 na DV8.1) |  | Zachowuje oryginalne mapowanie tonalne zamierzone przez twórcę kosztem nieco większego przetwarzania na klatkę. |
| 281 | ui/screens/settings/PlaybackVideoSettings.kt |  | przełącznik | Konwertuj DV5 na DV8.1 |  | Sygnalizuj strumienie Profile 5 jako Profile 8.1 dla wyjścia kompatybilnego z HDR10. Pomaga na urządzeniach bez natywnego dekodera DV5. |
| 282 | ui/screens/settings/PlaybackVideoSettings.kt |  | przełącznik | Usuń metadane HDR10+ |  | Usuwa dynamiczne metadane HDR10+ ze strumieni. Gdy włączono również Konwertuj do DV8.1, usuwanie następuje po konwersji. |
| 283 | ui/screens/settings/PlaybackVideoSettings.kt |  | przełącznik | Czysto czarne paski |  | Zostaw paski wokół obrazu niemalowane, aby HDR zachował czystą czerń. Wyłącz jeśli na paskach pojawia się jasna linia. |
| 284 | ui/screens/settings/PlaybackVideoSettings.kt |  | wybór/czynność | Dekodowanie sprzętowe (tylko MPV) |  | {null} |
| 285 | ui/screens/settings/PlaybackVideoSettings.kt |  | przełącznik | Awaryjne dekodowanie Hi10P G-NEXT SW (tylko MPV) |  | Użyj programowego dekodowania G-NEXT, gdy szczegóły strumienia wskazują H.264 Hi10P. Domyślnie wyłączone; włącz tylko jeśli odtwarzanie Hi10 |
| 286 | ui/screens/settings/PlaybackVideoSettings.kt |  | wybór/czynność | {stringResource(buttonRes)} |  | {null} |
| 287 | ui/screens/settings/PlexSignInDialog.kt |  | wybór/czynność | {server.name} |  | {server.description(state.serversLoading} |
| 288 | ui/screens/settings/PlexSignInDialog.kt |  | wybór/czynność | {user.name} |  | {user.tags(} |
| 289 | ui/screens/settings/ProfileSettingsContent.kt | Zarządzaj profilami | wybór/czynność | Zarządzaj profilami |  | {null} |
| 290 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | przełącznik | [EN] Arc format handling |  | [EN] Lossless passthrough for TrueHD and DTS-HD, surround matched to your audio system, and Arc's recovery when a receiver refuses a format. |
| 291 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | wybór/czynność | [EN] Surround format |  | {stringResource(                 when {                     d} |
| 292 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | wybór/czynność | [EN] Decode channel target |  | [EN] Channel layout for audio the app decodes. Auto follows what your audio system reports. |
| 293 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | przełącznik | [EN] Add HDR10 metadata when Dolby Vision is stripped |  | [EN] For Dolby Vision Profile 8 played as its HDR10 base layer, adds the source's mastering metadata (MaxCLL, MaxFALL and peak brightness) s |
| 294 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | wybór/czynność | [EN] Test lossless passthrough again |  | [EN] Tests IEC 61937 output again. Use this if TrueHD or DTS-HD fall back to PCM after changing receiver or input. |
| 295 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | wybór/czynność | [EN] Forget learned audio refusals |  | [EN] Formats your receiver refused to open are not sent to it as passthrough. This lets them be tried again. |
| 296 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | wybór/czynność | [EN] Forget tunnel stalls |  | {if (labels.isEmpty(} |
| 297 | ui/screens/settings/ReshapedFormatsSettingsItems.kt |  | przełącznik | {stringResource(titleRes)} |  | {stringResource(subtitleRes)} |
| 298 | ui/screens/settings/ReshapedSettingsItems.kt |  | przełącznik | {title} |  | {subtitle} |
| 299 | ui/screens/settings/ReshapedSettingsItems.kt |  | przełącznik | {title} |  | {subtitle} |
| 300 | ui/screens/settings/ReshapedSettingsItems.kt |  | ekran | {title} |  | {subtitle} |
| 301 | ui/screens/settings/ReshapedSettingsItems.kt |  | wybór/czynność | {title} |  | {subtitle} |
| 302 | ui/screens/settings/ReshapedSettingsItems.kt |  | suwak | {title} |  | {subtitle} |
| 303 | ui/screens/settings/ReshapedSettingsItems.kt |  | suwak | {title} |  | {subtitle} |
| 304 | ui/screens/settings/ReshapedSettingsItems.kt |  | suwak | {title} |  | {subtitle} |
| 305 | ui/screens/settings/ReshapedSettingsItems.kt |  | suwak | {title} |  | {subtitle} |
| 306 | ui/screens/settings/ReshapedSyncSettingsItems.kt |  | ekran | [EN] Sync with Google |  | {stringResource(                         if (configured)} |
| 307 | ui/screens/settings/ReshapedSyncSettingsItems.kt |  | ekran | [EN] Google account |  | {email?.takeIf(String::isNotBlank} |
| 308 | ui/screens/settings/ReshapedSyncSettingsItems.kt |  | przełącznik | [EN] Sync Reshaped settings |  | [EN] AutoSync, seek previews and other Reshaped options. Buffer sizes and channel previews stay per device |
| 309 | ui/screens/settings/ReshapedSyncSettingsItems.kt |  | przełącznik | [EN] Sync Live TV |  | [EN] Sources with their logins, favourites, categories and the last channel |
| 310 | ui/screens/settings/ReshapedSyncSettingsItems.kt |  | ekran | [EN] Sync now |  | {when {} |
| 311 | ui/screens/settings/SeekPreviewSettingsItems.kt |  | przełącznik | [EN] Generate previews on device |  | [EN] Builds seek previews in the background while you watch, using cached playback data or small downloads when resources allow. Playback st |
| 312 | ui/screens/settings/SeekPreviewSettingsItems.kt |  | przełącznik | [EN] Use a smaller copy for previews |  | [EN] Use an available smaller file after checking that its pictures and timing match. |
| 313 | ui/screens/settings/SeekPreviewSettingsItems.kt |  | przełącznik | [EN] Prepare next episode previews |  | [EN] Prepare previews from preloaded episode sources when playback has resources to spare. |
| 314 | ui/screens/settings/SeekrKeySettingsItems.kt |  | ekran | Klucz API Seekr |  | {stringResource(                 if (userKey.isBlank()} |
| 315 | ui/screens/settings/SettingsDesignSystem.kt |  | przełącznik | ? |  |  |
| 316 | ui/screens/settings/SettingsDesignSystem.kt |  | wybór/czynność | ? |  |  |
| 317 | ui/screens/settings/SettingsScreen.kt | Przełącz na Zaawansowany | wybór/czynność | Przełącz na Zaawansowany |  | Pokaż pełny układ, wtyczki, integracje, katalogi, kolekcje i ustawienia dostrajania. |
| 318 | ui/screens/settings/SettingsScreen.kt | Debrid | wybór/czynność | Debrid |  | Eksperymentalne źródła z kont chmurowych |
| 319 | ui/screens/settings/SettingsScreen.kt | Debrid | wybór/czynność | TMDB |  | Kontrolki wzbogacania metadanych |
| 320 | ui/screens/settings/SettingsScreen.kt | Debrid | wybór/czynność | Oceny MDBList |  | Zewnętrzni dostawcy ocen |
| 321 | ui/screens/settings/SettingsScreen.kt | Debrid | wybór/czynność | Anime-Skip |  | Znaczniki czasu czołówek i napisów końcowych anime |
| 322 | ui/screens/settings/SettingsSearchOverlay.kt |  | wybór/czynność | {result.title} |  | {result.subtitle} |
| 323 | ui/screens/settings/SettingsSectionComponents.kt |  | wybór/czynność | {title} |  | {description} |
| 324 | ui/screens/settings/SettingsValueRows.kt |  | suwak | {title} |  | {subtitle} |
| 325 | ui/screens/settings/SettingsValueRows.kt |  | suwak | {title} |  | {subtitle} |
| 326 | ui/screens/settings/SettingsValueRows.kt |  | kolor | ? |  |  |
| 327 | ui/screens/settings/SiloSignInDialog.kt |  | wybór/czynność | {profile.name} |  | {profile.tags(} |
| 328 | ui/screens/settings/SkinSettingsItems.kt |  | ekran | ? |  |  |
| 329 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | wybór/czynność | Czcionka |  | {if (fontMissing} |
| 330 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Rozmiar |  | Używaj pogrubionej czcionki dla napisów |
| 331 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | przełącznik | Pogrubienie |  | Używaj pogrubionej czcionki dla napisów |
| 332 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Krycie tekstu |  |  |
| 333 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Krycie tła |  |  |
| 334 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Zaokrąglenie rogów |  | {stringResource(R.} |
| 335 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | przełącznik | Obrys |  | Dodaj obrys wokół tekstu napisów dla lepszej widoczności |
| 336 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Grubość obrysu |  | {s} |
| 337 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | przełącznik | Cień |  | Oddziel tekst od jasnych scen |
| 338 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Krycie cienia |  |  |
| 339 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Rozmiar cienia |  |  |
| 340 | ui/screens/settings/SubtitleAppearanceEditor.kt |  | suwak | Przesunięcie pionowe |  |  |
| 341 | ui/screens/settings/SubtitleFontSettingsItems.kt |  | ekran | [EN] Subtitle font |  | {font?.familyName ?: stringResource(R.string.subtitle_font_de} |
| 342 | ui/screens/settings/ThemeSettingsScreen.kt | Motyw kolorystyczny | przełącznik | Tryb AMOLED |  | Użyj czystej czerni jako tła aplikacji |
| 343 | ui/screens/settings/ThemeSettingsScreen.kt | Motyw kolorystyczny | przełącznik | Czysto czarne powierzchnie |  | Karty, panele i kontenery również będą czysto czarne |
| 344 | ui/screens/settings/ThemeSettingsScreen.kt | Grafika launchera | wybór/czynność | Ikona i baner aplikacji |  | Zmień ikonę i baner aplikacji |
| 345 | ui/screens/settings/ThemeSettingsScreen.kt | Czcionka i język | wybór/czynność | Czcionka aplikacji |  | Wybierz preferowaną czcionkę |
| 346 | ui/screens/settings/ThemeSettingsScreen.kt | Czcionka i język | wybór/czynność | Język aplikacji |  | Nadpisz język systemowy |
| 347 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Włącz wzbogacanie TMDB |  | Używaj TMDB jako źródła metadanych do uzupełniania danych z dodatków |
| 348 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Włącz w Modern Home |  | Stosuj wzbogacanie TMDB również w Modern Home hero i zaznaczonych kartach |
| 349 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Wzbogacaj Kontynuuj oglądanie |  | Stosuj wzbogacanie TMDB dla elementów Kontynuuj oglądanie |
| 350 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | wybór/czynność | Język |  | Język metadanych TMDB dla tytułu, logo i włączonych pól |
| 351 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Grafiki |  | Logo i obrazy tła z TMDB |
| 352 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Podstawowe informacje |  | Opis, gatunki i ocena z TMDB |
| 353 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Szczegóły |  | Czas trwania, status, kraj i język z TMDB |
| 354 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Obsada |  | Obsada ze zdjęciami, reżyser i scenarzysta z TMDB |
| 355 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Produkcje |  | Firmy produkcyjne z TMDB |
| 356 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Sieci |  | Sieci z logo z TMDB |
| 357 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Odcinki |  | Tytuły, opisy, miniatury i czas trwania odcinków z TMDB |
| 358 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Plakaty sezonów |  | Plakaty sezonów z TMDB w zakładkach sezonów na stronie szczegółów |
| 359 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Zwiastuny |  | Kandydaci na zwiastuny z filmów TMDB dla sekcji zwiastunów |
| 360 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Podobne |  | Tła rekomendacji TMDB na stronie szczegółów |
| 361 | ui/screens/settings/TmdbSettingsScreen.kt | Włącz wzbogacanie TMDB | przełącznik | Kolekcje |  | Kolekcje filmów TMDB w kolejności wydania |
| 362 | ui/screens/settings/TrackingSettingsScreen.kt | Konta | wybór/czynność | Trakt |  | {traktPresentation.subtitle} |
| 363 | ui/screens/settings/TrackingSettingsScreen.kt | Konta | wybór/czynność | Simkl |  | {simklPresentation.subtitle} |
| 364 | ui/screens/settings/TrackingSettingsScreen.kt | Konta | wybór/czynność | MDBList |  | {mdbListPresentation.subtitle} |
| 365 | ui/screens/settings/TrackingSettingsScreen.kt | Źródła | wybór/czynność | Źródło biblioteki |  | Wybierz, której biblioteki używać do zapisywania i przeglądania kolekcji |
| 366 | ui/screens/settings/TrackingSettingsScreen.kt | Źródła | wybór/czynność | Postęp oglądania |  | Wybierz źródło postępu dla wznawiania i kontynuowania oglądania |
| 367 | ui/screens/settings/TrackingSettingsScreen.kt | Źródła | wybór/czynność | Źródło „Podobne" |  | Wybierz skąd pobierane są rekomendacje na stronach szczegółów |
| 368 | ui/screens/settings/TrackingSettingsScreen.kt | Źródła | wybór/czynność | Oceny |  | Gdzie zapisywane są Twoje oceny i skąd są wyświetlane |
| 369 | ui/screens/settings/TrackingSettingsScreen.kt | Funkcje Trakt | wybór/czynność | Okno kontynuowania oglądania |  | {if (!traktProgressActive} |
| 370 | ui/screens/settings/TrackingSettingsScreen.kt | Funkcje Trakt | przełącznik | Komentarze |  | Pokaż recenzje Trakt na stronach metadanych |
| 371 | ui/screens/settings/TrackingSettingsScreen.kt | Funkcje Simkl | wybór/czynność | Preferencja ID anime |  | Kontroluje sposób identyfikacji anime. Wybór MAL lub Kitsu daje każdemu sezonowi osobny wpis zamiast grupowania pod jednym ID IMDB. |
| 372 | ui/screens/settings/TrackingSettingsScreen.kt | Funkcje MDBList | wybór/czynność | Listy biblioteki |  | Wybierz, które listy MDBList mają się pojawiać w Bibliotece i w Zarządzaj listami. Lista "Do obejrzenia" jest zawsze widoczna. |
| 373 | ui/screens/settings/UpdateChannelSettings.kt |  | wybór/czynność | Kanał aktualizacji |  | Wybierz rodzaj aktualizacji aplikacji |
| 374 | ui/screens/settings/UpdateChannelSettings.kt |  | przełącznik | Automatyczny baner aktualizacji |  | Pokaż baner gdy dostępna jest nowa wersja |
| 375 | ui/screens/settings/UpdateChannelSettings.kt |  | przełącznik | Optymalizuj aktualizacje podczas instalacji |  | Szybsze pierwsze uruchomienie po aktualizacji, ale aplikacja zamyka się na około minutę podczas instalacji. Wyłącz, aby zamiast tego użyć ek |
| 376 | ui/screens/settings/UpdateChannelSettings.kt |  | przełącznik | Instaluj aktualizacje automatycznie |  | Pobieraj nowe wersje w tle i instaluj je, gdy urządzenie jest bezczynne |
| 377 | ui/screens/settings/UpdateChannelSettings.kt |  | przełącznik | Powiadom po automatycznej aktualizacji |  | Pokaż baner przy następnym uruchomieniu po automatycznej aktualizacji |
| 378 | ui/screens/settings/UpdateChannelSettings.kt |  | wybór/czynność | Sprawdź aktualizacje |  | Pobierz najnowsze wydanie |
| 379 | ui/screens/settings/YouTubeSettingsContent.kt | Account | wybór/czynność | Cancel |  | Waiting for you to approve on your other device |
| 380 | ui/screens/settings/YouTubeSettingsContent.kt | Account | wybór/czynność | Signed in |  | {state.accountName ?: "Your OneTube account is connected"} |
| 381 | ui/screens/settings/YouTubeSettingsContent.kt | Account | wybór/czynność | Sign out |  | Removes the account from this device |
| 382 | ui/screens/settings/YouTubeSettingsContent.kt | Account | wybór/czynność | Sign in |  | {state.error ?: "Get a code to enter at youtube.com/activate"} |
| 383 | ui/screens/settings/YouTubeSettingsContent.kt | Header | przełącznik | Clock |  | Show the time in the OneTube header |
| 384 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | przełącznik | Thumbnail previews with sound |  | Play a preview when you pause on a OneTube video |
| 385 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | przełącznik | Automatically select subtitles |  | Start OneTube videos with your preferred subtitles when available |
| 386 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | wybór/czynność | Content region |  | The country whose videos show in News and Live. Automatic follows this device. |
| 387 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | wybór/czynność | Posters per row |  | How many videos fit across OneTube rows and grids |
| 388 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | przełącznik | Autoplay next video |  | When a video ends, play a related video, or the next one in the list |
| 389 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | wybór/czynność | Device compatibility |  | {YouTubeSettings.compatibilityDescription(context} |
| 390 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | wybór/czynność | Video format |  | {if (YouTubeSettings.compatibilityActive(context} |
| 391 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | wybór/czynność | Default quality |  | {if (quality <= 0 && YouTubeSettings.compatibilityActive(cont} |
| 392 | ui/screens/settings/YouTubeSettingsContent.kt | Playback | wybór/czynność | Volume offset |  | Makes OneTube quieter or louder than the other sections. Negative is quieter. |
| 393 | ui/screens/settings/YouTubeSettingsContent.kt | DeArrow | przełącznik | Better titles |  | Plain titles where the community has written one |
| 394 | ui/screens/settings/YouTubeSettingsContent.kt | DeArrow | przełącznik | Better thumbnails |  | Thumbnails picked from the video itself |
| 395 | ui/screens/settings/YouTubeSettingsContent.kt | Feeds | przełącznik | Hide Shorts |  | Short vertical videos |
| 396 | ui/screens/settings/YouTubeSettingsContent.kt | Feeds | przełącznik | Hide watched videos |  | Videos you've mostly watched, on this device or on your account |
| 397 | ui/screens/settings/YouTubeSettingsContent.kt | Feeds | przełącznik | Hide upcoming videos |  | Premieres and streams that haven't started, which can't be played yet |
| 398 | ui/screens/settings/YouTubeSettingsContent.kt | Hidden words and channels | wybór/czynność | Add a word or phrase |  | Press OK to type one |
| 399 | ui/screens/settings/YouTubeSettingsContent.kt | Hidden words and channels | wybór/czynność | {word} |  | Press OK to stop hiding this word |
| 400 | ui/screens/settings/YouTubeSettingsContent.kt | Hidden words and channels | wybór/czynność | {name} |  | Hidden channel. Press OK to show it again |
| 401 | ui/screens/settings/YouTubeSettingsContent.kt | Tabs | przełącznik | Open on the last tab used |  | Off: the page always opens on the first tab |
| 402 | ui/screens/settings/YouTubeSettingsContent.kt | Tabs | wybór/czynność | Change the order |  | Pick a tab, then move it up or down |
| 403 | ui/screens/settings/YouTubeSettingsContent.kt | Tabs | przełącznik | {tab.title} |  | {when {} |
| 404 | ui/screens/settings/YouTubeSettingsContent.kt | SponsorBlock | przełącznik | Skip marked segments |  | Jumps past them and shows what was skipped |
| 405 | ui/screens/settings/YouTubeSettingsContent.kt | SponsorBlock | przełącznik | Ask before skipping |  | Shows a card when a marked part starts; press OK to skip it |
| 406 | ui/screens/settings/YouTubeSettingsContent.kt | SponsorBlock | przełącznik | Show a notice when skipping |  | A short message such as \"Skipped: Sponsor\" |
| 407 | ui/screens/settings/YouTubeSettingsContent.kt | SponsorBlock | przełącznik | {label} |  | {null} |
| 408 | fork/ai/subtitles/AiSubtitlesModule.kt | AI napisy | przełącznik ESSENTIAL | Druga kontrola każdego bloku (recenzja) | K_REVIEW | Lepsza jakość, ok. 2× koszt i czas [dom. true] |
| 409 | fork/ai/subtitles/AiSubtitlesModule.kt | AI napisy | przełącznik ESSENTIAL | Oryginał tam, gdzie tłumaczenie niegotowe | K_FALLBACK | Wyłączone = brak napisów w tych miejscach [dom. true] |
| 410 | fork/ai/subtitles/AiSubtitlesModule.kt | AI napisy | przełącznik ESSENTIAL | Powiadomienie w rogu podczas tłumaczenia | K_NOTICE |  [dom. true] |
| 411 | fork/ai/subtitles/AiSubtitlesModule.kt | AI napisy | przełącznik ESSENTIAL | Napisy AI, gdy brak dobrych polskich | K_AUTO | Włączają się same, gdy nie ma polskich napisów albo nie pasują do filmu [dom. false] |
| 412 | fork/ai/subtitles/AiSubtitlesModule.kt | ai_subtitles | czynność SIMPLE | AI napisy: klucz, model, limity, zapisane |  | Gemini: klucz API, zgoda, limit wydatków, eksport SRT/VTT |
| 413 | fork/discover/DiscoverModule.kt | Odkrywaj | przełącznik ESSENTIAL | Rząd:  | discover_show_$id |  [dom. true] |
| 414 | fork/discover/DiscoverModule.kt | discover | czynność SIMPLE | Odkrywaj telewizję (rzędy) |  | Co oglądać: wznów, nowe odcinki, gatunki z archiwum |
| 415 | fork/family/FamilyModule.kt | Rodzina | przełącznik SIMPLE | Statystyki używania dla panelu | KEY | Anonimowe liczniki ekranów, przycisków i funkcji (bez tytułów) — pomagają ulepszać wygląd i obsługę [dom. true] |
| 416 | fork/family/FamilyModule.kt | family | czynność SIMPLE | Rodzina: wspólne ustawienia |  | Udostępnij ustawienia albo dołącz kodem 6 cyfr |
| 417 | fork/launcher/LauncherModule.kt | Telewizja | przełącznik SIMPLE | Uruchamiaj z telewizorem | launcher_autostart | Nuvio startuje po włączeniu telewizora [dom. false] |
| 418 | fork/launcher/LauncherModule.kt | Telewizja | przełącznik ESSENTIAL | Blokada wyjścia | K_LOCK | Pięć razy Wstecz w 3 sekundy otwiera „Wyjście i ustawienia” [dom. false] |
| 419 | fork/launcher/LauncherModule.kt | Obrazki | przełącznik ESSENTIAL | Tła i duże obrazki w pełnej jakości | KEY | Ostrzejsze na telewizorach 4K, wczytują się trochę dłużej [dom. true] |
| 420 | fork/launcher/LauncherModule.kt | Obrazki | przełącznik ADVANCED | Gotowe rozmycia teł | KEY | Rozmycie liczone raz i zapamiętane — płynniejsze przewijanie, także na starszych telewizorach [dom. true] |
| 421 | fork/launcher/LauncherModule.kt | launcher | czynność SIMPLE | Po uruchomieniu pokaż: ekran główny |  |  |
| 422 | fork/launcher/LauncherModule.kt | launcher | czynność SIMPLE | Po uruchomieniu pokaż: telewizję |  |  |
| 423 | fork/launcher/LauncherModule.kt | launcher | czynność SIMPLE | Po uruchomieniu pokaż: ostatni kanał |  |  |
| 424 | fork/launcher/LauncherModule.kt | launcher | czynność ESSENTIAL | Nuvio jako ekran główny |  | Otwiera systemowy wybór ekranu głównego |
| 425 | fork/lektor/LektorModule.kt | Lektor | przełącznik ESSENTIAL | Najpierw polskie napisy z filmu | KEY | Na starcie filmu zamienia obce napisy na polskie: najpierw te z pliku, potem z dodatków [dom. true] |
| 426 | fork/lektor/LektorModule.kt | Lektor | przełącznik ESSENTIAL | Włączaj automatycznie, gdy brak polskiego dźwięku | lektor_auto | Przy polskich napisach i braku polskiej ścieżki [dom. false] |
| 427 | fork/lektor/LektorModule.kt | Lektor | przełącznik ADVANCED | Zapisuj nagrania lektora w małych plikach | KEY | Kwestie raz wygenerowane zajmują ok. 24 razy mniej miejsca [dom. true] |
| 428 | fork/lektor/LektorModule.kt | Lektor | przełącznik ESSENTIAL | Nagrania lektora na Dysku Google | KEY_DRIVE | Przed generowaniem sprawdza Dysk konta z synchronizacji; nowe kwestie trafiają na Dysk [dom. true] |
| 429 | fork/lektor/LektorModule.kt | Lektor | przełącznik ESSENTIAL | Pod lektorem ściszaj głównie aktorów | K_BAND | Muzyka i efekty zostają głośniej, lektor jest wyraźniejszy [dom. true] |
| 430 | fork/lektor/LektorModule.kt | lektor | czynność ESSENTIAL | Lektor: głośność względem filmu (100% / 70% / 50% / 130% / 160%) |  | Kolejne naciśnięcia przełączają wartość; też w odtwarzaczu: Więcej |
| 431 | fork/lektor/LektorModule.kt | lektor | czynność ESSENTIAL | Lektor: ściszanie filmu pod lektorem (−6 / −3 / bez / −10 / −14 dB) |  | Kolejne naciśnięcia przełączają wartość; też w odtwarzaczu: Więcej |
| 432 | fork/lektor/LektorModule.kt | lektor | czynność ADVANCED | Lektor: wyczyść pamięć podręczną |  | Usuwa zsyntezowane kwestie (limit 200 MB) |
| 433 | fork/net/KeNetworkModule.kt | Test łącza | przełącznik ESSENTIAL | Test łącza sam (po zamknięciu filmu) | KEY | Raz na usługę, gdy nie ma jej pomiaru; przerywany, gdy włączysz film [dom. true] |
| 434 | fork/net/KeNetworkModule.kt | ke_network | czynność SIMPLE | Test łącza (ostatni film) |  | Mierzy prędkość usługi na ostatnio oglądanym filmie i ustawia pobieranie równoległe.  |
| 435 | fork/night/NightModule.kt | Tryb nocny | przełącznik SIMPLE | Ciepła nakładka obrazu | K_OVERLAY | Bursztynowy odcień ekranu w godzinach nocnych [dom. true] |
| 436 | fork/night/NightModule.kt | Tryb nocny | przełącznik ESSENTIAL | Przyciemnienie | K_DIM | Dodatkowo przyciemnia obraz [dom. false] |
| 437 | fork/night/NightModule.kt | Tryb nocny | przełącznik ADVANCED | Nie stosuj przy HDR i Dolby Vision | K_HDR | Nakładka wyłączona przy filmach HDR [dom. true] |
| 438 | fork/night/NightModule.kt | night | czynność SIMPLE | Tryb nocny: według godzin 22:00–07:00 |  | Włącza harmonogram |
| 439 | fork/night/NightModule.kt | night | czynność SIMPLE | Tryb nocny: włączony |  |  |
| 440 | fork/night/NightModule.kt | night | czynność SIMPLE | Tryb nocny: wyłączony |  |  |
| 441 | fork/night/NightModule.kt | night | czynność SIMPLE | Tryb nocny: początek 21:00 / 22:00 / 23:00 (kolejno) |  | Kolejne naciśnięcia zmieniają godzinę rozpoczęcia |
| 442 | fork/night/NightModule.kt | night | czynność SIMPLE | Tryb nocny: koniec 06:00 / 07:00 / 08:00 (kolejno) |  | Kolejne naciśnięcia zmieniają godzinę zakończenia |
| 443 | fork/night/NightModule.kt | night | czynność SIMPLE | Siła: lekka / średnia / mocna (kolejno) |  |  |
| 444 | fork/picks/PicksModule.kt | picks | czynność SIMPLE | Na co masz ochotę? |  | Wybierz nastrój, dostaniesz listę filmów |
| 445 | fork/sources/SourcesModule.kt | sources | czynność SIMPLE | {(if (answer == id) "✓ " else "") + text} |  |  |
| 446 | fork/sources/SourcesModule.kt | sources | czynność SIMPLE | Napisy: małe |  | Rozmiar napisów na półprzezroczystym tle |
| 447 | fork/sources/SourcesModule.kt | sources | czynność SIMPLE | Napisy: średnie |  |  |
| 448 | fork/sources/SourcesModule.kt | sources | czynność SIMPLE | Napisy: duże |  |  |
| 449 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Telewizja w pamięci tylko na ekranie telewizji | KEY | Po wyjściu z telewizji kanały i przewodnik zwalniają pamięć (zostają zapisane); pierwsze wejście trwa chwilę dłużej [dom. true] |
| 450 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Bez filmów i seriali na liście kanałów | KEY | Filmy i seriale z list IPTV nie są pokazywane jako kanały; telewizja działa szybciej i się nie zawiesza [dom. true] |
| 451 | fork/tv/TvModule.kt | Telewizja | przełącznik ADVANCED | Zatrzymaj podgląd kanału po minucie | PREVIEW_AUTOSTOP_KEY | Oszczędza procesor słabych telewizorów; zostaje logo, zmiana kanału wznawia podgląd [dom. true] |
| 452 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Rząd „Dla Ciebie” nad przewodnikiem | tv_for_you_row | Karty: wznów, nowy odcinek, na żywo [dom. true] |
| 453 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Archiwum bez pytania „czy na pewno” | KEY | Wstecz od razu wraca do przewodnika, ▲▼ od razu zmienia kanał [dom. true] |
| 454 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Przypomnienia o programach | tv_reminders | Karta nad obrazem na chwilę przed startem [dom. true] |
| 455 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Automatyczne obserwowane programy | tv_auto_follow | Serial staje się obserwowany po 2 obejrzanych odcinkach (można cofnąć) [dom. true] |
| 456 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Surowe liczenie obejrzanych | tv_watched_strict | Odcinek jest obejrzany dopiero po 80% czasu (zamiast 50%) [dom. false] |
| 457 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Najpierw kanały z mojego kraju | KEY_PREFER | Kanały z wybranego kraju (domyślnie Polska) na początku każdej listy; kraj zmienisz niżej [dom. true] |
| 458 | fork/tv/TvModule.kt | Telewizja | przełącznik ESSENTIAL | Tylko kanały z mojego kraju | KEY_ONLY | Kanały z innych krajów w ogóle się nie pokazują [dom. false] |
| 459 | fork/tv/TvModule.kt | Telewizja | przełącznik ADVANCED | Przewodnik uzupełniający dla kraju listy | KEY | Dodaje drugi przewodnik (obrazy, odcinki), gdy rozpozna kraj kanałów [dom. true] |
| 460 | fork/tv/TvModule.kt | tv | czynność ESSENTIAL | Zmień kraj IPTV |  | Kraj kanałów dla „Najpierw / Tylko kanały z mojego kraju” (domyślnie Polska); każde naciśnięcie — następny kraj |
| 461 | fork/tv/TvModule.kt | tv | czynność ADVANCED | Wyczyść historię oglądania TV |  | Kasuje stan obejrzanych odcinków, obserwowane i statystykę kanałów |
| 462 | fork/tvdb/TvdbModule.kt | tvdb | czynność ESSENTIAL | TVDB: sprawdź połączenie |  | Gdy TMDB nie zna programu z przewodnika, opis, plakat i obsada przychodzą z TheTVDB. Stan:  |
| 463 | fork/voice/VoiceModule.kt | Sterowanie głosem | przełącznik ESSENTIAL | Przycisk „Powiedz” w wyszukiwarce | KEY | Włącza nasłuch poleceń bez przycisku na pilocie [dom. true] |
| 464 | fork/voice/VoiceModule.kt | Sterowanie głosem | przełącznik ESSENTIAL | Rozpoznawanie mowy offline (Whisper) | voice_whisper | Wymaga pobranego modelu mowy (ustawienia napisów: model mowy); działa bez internetu [dom. false] |
| 465 | fork/voice/VoiceModule.kt | Sterowanie głosem | przełącznik ESSENTIAL | Przycisk asystenta otwiera nasłuch | voice_key_service | Wymaga włączenia usługi ułatwień dostępu Nuvio w systemie [dom. false] |
| 466 | fork/voice/VoiceModule.kt | voice | czynność SIMPLE | Przycisk mikrofonu: włącz usługę Nuvio |  | Otwiera Ułatwienia dostępu: wybierz „Nuvio Debug” i włącz. Usługa przechwytuje tylko przycisk głosu |
| 467 | fork/voice/VoiceModule.kt | voice | czynność SIMPLE | Przycisk mikrofonu: naucz przycisk |  | Po wybraniu naciśnij w ciągu 15 s przycisk mikrofonu (albo inny) na pilocie |
| 468 | fork/voice/VoiceModule.kt | voice | czynność SIMPLE | Ustaw Nuvio jako asystenta |  | Przycisk asystenta otworzy nasłuch Nuvio zamiast asystenta systemu (Android 12+) |
| 469 | fork/voice/VoiceModule.kt | voice | czynność SIMPLE | Sterowanie głosem: przykłady poleceń |  | Lista tego, co można powiedzieć |
