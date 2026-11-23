# Inwentarz ustawień (23.11)

Pełna, automatyczna lista (469 wierszy: plik, grupa, typ, nazwa, klucz, opis): `inwentarz-surowy.md`, generowana przez
`python3 -I fork-tools/ustawienia/inwentarz.py > plan/ustawienia/inwentarz-surowy.md`. Ten plik streszcza ją według źródła
i podaje, gdzie każda część trafiła w nowym drzewie (`DRZEWO.md`). „Działa” = ekran otwiera się i zapisuje wartość (sprawdzone na JMGO w buildzie ust1 tam, gdzie zaznaczono w TERAZ).

## Gdzie ustawienia były pokazane przed 23.11
| Miejsce | Plik | Co zawiera | Problem |
|---|---|---|---|
| Tryb NOOB (Niunia) | `fork/settings/SimpleSettingsScreen.kt` → `skin/niunia/settings/NiuniaSettings.kt` | 11 wierszy + czynności i przełączniki SIMPLE modułów na końcu listy | osobny układ, pozycje modułów luzem pod listą |
| Tryb Prosty | `skin/kicia/settings/ForgeBasicSettings.kt` | 8 kategorii Nuvio (wygląd Kicii) | inny układ niż NOOB |
| Tryb Zaawansowany | `fork/settings/nav/ForkAdvancedSettings.kt` + `SettingsTree.kt` | 7 sekcji / 48 podkategorii (wygląd Kicii) | trzeci układ; kategorie Nuvio otwierane w pełnym ekranie |
| Pełne ustawienia Nuvio | `ui/screens/settings/SettingsScreen.kt` | 17 kategorii | nazwy techniczne (Nuvio Reshaped, Nuvio Forge, Integracje) |
| Kategoria „Nuvio Reshaped” | `NuvioReshapedSettingsContent.kt` | 21 grup (`*SettingsItems.kt`) jedna pod drugą | luzem, bez podziału |
| Kategoria „Nuvio Forge” | `skin/kicia/settings/ForgeSettingsContent.kt` | 12 wierszy: System, Live TV, Home | luzem; 3 duble z modułami |
| Moduły forka | `fork/registry` (12 modułów) | 36 przełączników + 30 czynności (SIMPLE/ESSENTIAL/ADVANCED) | widoczne tylko w „Wszystkich modułach” albo luzem w NOOB |
| Menu Live TV (Reshaped) | `ui/reshaped/livetv/LiveTvScreen.kt` | źródła, kategorie, odśwież, dźwięk podglądu… | zostaje w Live TV (to obsługa telewizji, nie ustawienia) |

## Liczby wg źródła (z inwentarza surowego)
| Źródło | Pozycji | Nowe miejsce |
|---|---|---|
| Odtwarzanie Nuvio (`Playback*Settings.kt`, 10 sekcji) | ~140 | Oglądanie (Wybór źródła, Następny odcinek, Pomijanie czołówek, Obraz, Wygląd odtwarzacza, Odtwarzacz W, Pamięć na film W, Torrenty W), Napisy i dźwięk (Język i wygląd napisów, Język i dźwięk) — każda pozycja drzewa otwiera ekran Odtwarzanie z ROZWINIĘTĄ swoją sekcją i fokusem na niej |
| Układ (`Layout*Settings.kt`) | 58 | Wygląd › Układ ekranu głównego |
| Integracje (Debrid, TMDB, MDBList, AnimeSkip) | 35 | Konta i dodatki › Usługi i dane filmów |
| YouTube (`YouTubeSettingsContent`) | 29 | Konta i dodatki › YouTube |
| Śledzenie (`TrackingSettingsScreen`) | 11 | Konta i dodatki › Trakt i Simkl |
| Zaawansowane (`NetworkSettingsScreen`, Ambient) | 18 | System › Pamięć i wydajność (W) |
| Wygląd (`ThemeSettingsScreen`) | 5 | Wygląd › Kolory, czcionka i język (N) |
| O aplikacji, aktualizacje | 9 | System › O aplikacji i aktualizacje |
| Reshaped: AutoSync, bańka, AudioSync | 13 | Napisy i dźwięk › Napisy równo z mową |
| Reshaped: formaty (DV, HDR10, dźwięk) | 8 | Oglądanie › Dolby Vision i dźwięk przestrzenny |
| Reshaped: zmiana źródła, łącze, przewijanie, Seekr | 7 | Oglądanie (Gdy film się zatnie, Dopasowanie do internetu W, Przewijanie) |
| Reshaped: czcionka napisów, model mowy | 2 | Napisy i dźwięk (Czcionka napisów, Rozpoznawanie mowy W) |
| Reshaped: menu pigułka, styl Netflix | 2 | Wygląd › Menu i styl (W) |
| Reshaped: Live TV (podgląd), synchronizacja Google | 8 | Telewizja › Podgląd kanałów; Konta › Kopia ustawień |
| Forge (`ForgeSettingsContent`) | 12 | Źródła i EPG → Telewizja › Listy kanałów i program (N); Logotypy → Logo kanałów; własny adres i kanały bez EPG → Telewizja › W; Układ Live TV, karta przypomnień → Telewizja › Pamięć (W, tylko Kicia); YouTube na głównym → Wygląd; autostart/launcher/asystent → wersje modułów (System, Pilot i głos) |
| Forge tryb nocny (`ForgeNightSettings`) | 3 | Wygląd › Tryb nocny › Dźwięk nocą i filtr (W) |
| Echo, KE (katalog IPTV, test łącza i sprzętu), Arc (pilot), One (YouTube) | 12 | Oglądanie › Szybszy start; Telewizja › Katalogi IPTV; System › Pomoc; Pilot i głos › Przyciski pilota |
| Przełączniki forka (`forkSwitchesSettingsItems`) | 5 | Kalendarz → Wygląd W; Pilot Arc → Pilot; dawne menu/szukanie → System W; zdalne ustawienia (testy) → tylko dawny ekran |

## Moduły forka (rejestr) — klucz, domyślnie, poziom → miejsce
Poziom: S = SIMPLE (tryb prosty), E = ESSENTIAL, A = ADVANCED (pod Więcej). Pełne odwzorowanie i nadpisane nazwy: sloty `m(...)` w `SettingsTreeSpec.layout`; test `SettingsTreeSpecTest` pilnuje, żeby żadna pozycja nie została bez miejsca.
| Moduł | Klucze / czynności (domyślnie) | Miejsce w drzewie |
|---|---|---|
| tv (Telewizja) | tv_country_prefer (tak), tv_country_only (nie), „Zmień kraj IPTV”, tv_skip_vod (tak), tv_plain_names (tak), tv_extra_guide (tak, A), tv_reminders (tak), tv_no_ask, tv_for_you_row (tak), tv_auto_follow, tv_watched_strict (nie), „Wyczyść historię” (A), tv_ram_only_in_tv (tak), tv_guide_db (A), tv_guide_reuse (A), perf_preview_autostop (A) | Telewizja: Kanały i program / Oglądanie telewizji / Pamięć telewizji |
| sources | 3 odpowiedzi „polski czy jakość”, 3 rozmiary napisów (S) | jeden wiersz z wartością: Oglądanie › Polski dźwięk czy jakość; Napisy › Rozmiar napisów |
| night | night_overlay (tak, S), night_dim (nie), night_skip_hdr (tak, A), tryb/początek/koniec/siła (S) | Wygląd › Tryb nocny (tryb = jeden wiersz z wartością) |
| launcher | launcher_autostart (S), launcher_lock (nie), images_original (tak), images_preblur (tak, A), „Po uruchomieniu pokaż” ×3 (S), „Nuvio jako ekran główny” | System › Start; Pilot › Blokada wyjścia; Wygląd › Ekran główny |
| voice | voice_ui_button (tak), voice_whisper, voice_key_service, 4 czynności (S) | Pilot i głos › Głos |
| lektor (wyłączony domyślnie przez autora, włączony w ON_BY_DEFAULT) | subs_polish_first (tak), lektor_auto, lektor_store_opus (A), lektor_drive (tak), lektor_band_duck (tak), głośność, ściszanie, wyczyść (A) | Napisy i dźwięk › Napisy (polskie z filmu) i › Dźwięk i lektor |
| ai_subtitles | ai_sub_review, ai_sub_fallback_original, ai_sub_notice (tak), ai_subs_auto_pl, „ustawienia napisów AI” (S) | Napisy i dźwięk › Napisy AI |
| discover | discover_show_<rząd> (tak), „Odkrywaj telewizję” (S) | Wygląd › Ekran główny (rzędy pod Więcej) |
| picks, family, ke_network, tvdb | „Na co masz ochotę?”, „Rodzina” + family_usage_stats, test łącza + ke_auto_network_test, „TVDB: sprawdź połączenie” | Wygląd › Ekran główny; Konta › Ty i domownicy / Obejrzane (W); System › Pomoc; Telewizja › IPTV (W) |

## Duplikaty znalezione (w drzewie zostaje jedna wersja)
| Ustawienie | Wersje | W drzewie |
|---|---|---|
| Uruchamiaj z telewizorem | `launcher_autostart` (moduł) i Forge `ForgeSystemOptions.autostart` | moduł launcher (Forge zostaje tylko w dawnym ekranie) |
| Nuvio jako ekran główny | czynność launcher i przełącznik Forge | czynność launcher |
| Nuvio jako asystent | czynność voice i wiersz Forge | czynność voice |
| Karta przypomnień | `tv_reminders` (moduł) i `ForgePrefs.reminderCard` (Kicia) | oba — Forge opisane jako „(skórka Kicia)”, pod Więcej |
| Filtr nocny | NightModule (ciepła nakładka) i ForgeNight (filtr niebieskiego światła) | NightModule na wierzchu, Forge pod Więcej |

## Szukanie — stan przed poprawką
`ui/screens/settings/SettingsSearch.kt` (kopia z Nuvio One): indeks = nazwy kategorii z szyny + ok. 25 angielskich wierszy („Subtitles”, „Audio”, „Buffer & Network”…), słowa kluczowe po angielsku. Polski użytkownik: „napisy”, „pilot”, „lektor”, „tryb nocny”, „kraj” → brak wyników; pozycji forka i Reshaped (21 grup) w ogóle nie ma; wynik z sekcją Odtwarzania otwierał tylko kategorię (`playbackSection` nieużywane w `SettingsScreen`). Nowe szukanie: `fork/settings/tree/SettingsTreeSearch.kt` (test `SettingsTreeSearchTest`). Dawne szukanie zostaje w dawnym ekranie (inne skórki).
