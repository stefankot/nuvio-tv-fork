# Szkło: spis powierzchni (24.11.2026)

Moduł `fork/glass/ForkGlass.kt`, ustawienie „Szkło” (Ustawienia → Wygląd → Skórka i język). Skórka: Niunia.
Warstwy: `fork_ekran` = cała treść ekranów (wpięcie NuvioNavHost, `ForkGlass.Host`); `fork_tlo` = tło ekranu (obraz hero + scrim, `forkGlassBackdrop`).
Powierzchnia w treści ekranu nie może próbkować `fork_ekran` (biblioteka nie rysuje szkła wewnątrz własnej warstwy), więc próbkuje `fork_tlo`.
Gdy warstwy nie ma (wideo w SurfaceView, ekran bez hero): zwykłe tło z tokenów pod obwódką szkła.

| plik | composable | rodzaj | warstwa | dzisiejsze tło (zapas) | stan |
|---|---|---|---|---|---|
| skin/niunia/components/NiuniaOrganisms.kt | NiuniaSidebarPanel | panel boczny (rozwinięty) | ekran | `Panel` + obwódka Fill12 | szkło Nav, 28 dp |
| skin/niunia/components/NiuniaOrganisms.kt | NiuniaTabPill | menu zwinięte (pigułka) | ekran | `Chrome` + Fill12 | szkło Nav, kapsuła |
| skin/niunia/nav/NiuniaNav.kt | NiuniaNavBar | nawigacja (poza NavHost) | — | — | `ForkGlass.Provide` (manager) |
| skin/niunia/components/NiuniaClock.kt | NiuniaClockCorner | zegar (sam tekst) | — | brak tła | bez zmian |
| skin/niunia/components/NiuniaButtons.kt | NiuniaButton Standard/Icon/Action | przyciski (hero: Odtwórz, +, i, ›; szczegóły; odtwarzacz) | tło | `Button` / `Haze` | szkło Button, kapsuła; fokus biały, skala 1,08 |
| skin/niunia/components/NiuniaButtons.kt | NiuniaButton Chip | chipy | tło | przezroczyste | szkło Chip |
| skin/niunia/components/NiuniaButtons.kt | NiuniaButton Tab/Segment/Key/Bare | zakładki, segmenty, klawisze | — | — | bez szkła (leżą w szklanej grupie) |
| skin/niunia/components/NiuniaOrganisms.kt | NiuniaCollection Tabs | grupa zakładek | tło | `Scrim30` | szkło Chip |
| skin/niunia/components/NiuniaAtoms.kt | NiuniaDots | kropki karuzeli hero | tło | `Scrim30` | szkło Chip |
| skin/niunia/components/NiuniaAtoms.kt | NiuniaSurface Panel/Glass/Tray/Frost | powierzchnie DS | tło | Fill12 / Panel / Chrome / Frost | szkło Panel / Nav |
| skin/niunia/components/NiuniaMolecules.kt | NiuniaContentBlock Panel | panel informacji (szczegóły) | tło | Fill12 | szkło Panel 16 dp |
| skin/niunia/components/NiuniaOrganisms.kt | NiuniaPanelOverlay | panel odtwarzacza (dźwięk, napisy, źródła) | tło (brak nad wideo) | `Panel` + Fill12 | szkło Panel; nad wideo zapas + obwódka |
| skin/niunia/player/NiuniaPlayerChrome.kt | panel „Więcej” | panel odtwarzacza | tło (brak) | #E61E1E22 + Line | szkło Panel (zapas nad wideo) |
| skin/niunia/player/NiuniaPlaybackNotice.kt | NiuniaPlaybackNotice | komunikat w odtwarzaczu | tło (brak) | FrostFlat | szkło Toast 16 dp (zapas nad wideo) |
| skin/niunia/live/NiuniaArchiveOffer.kt | NiuniaArchiveOfferCard | plakietka Live TV | tło | `Panel` | szkło Panel |
| skin/niunia/livetv/NiuniaLiveTv.kt | DayPicker | menu dni (EPG) | tło | Surface2 | szkło Menu 16 dp |
| skin/niunia/livetv/NiuniaLiveSearch.kt | przełącznik abc/123 (2×) | klawiatura Live TV | tło | biel 6% | szkło Chip |
| skin/niunia/search/NiuniaSearch.kt | przełącznik liter/cyfr | klawiatura wyszukiwania | tło | Fill12 | szkło Chip |
| skin/niunia/components/NiuniaDialog.kt | NiuniaDialog / NiuniaActionsDialog | okna, menu długiego OK (TitleActions, plakat, Oglądaj dalej) | ekran (osobne okno) | `Panel` + Fill12 | szkło Dialog 28 dp |
| skin/niunia/profile/NiuniaProfiles.kt | rząd profili | panel | tło | `Panel` + Fill12 | szkło Panel |
| skin/niunia/profile/NiuniaProfiles.kt | okno PIN | okno w ekranie | tło | #F53A3A3C | szkło Dialog (warstwa tło) |
| ui/components/NuvioDialog.kt (baza, wpięcie) | NuvioDialog | wspólne okno bazy (81 użyć) | ekran (osobne okno) | BackgroundElevated | szkło Dialog nad tłem okna |
| fork/ForkLog.kt + ForkHooks.moduleOverlays | ForkLog.say | komunikat | ekran | Toast systemu | pigułka szkła u góry; wyłączone = Toast |
| skin/niunia/components/NiuniaHeroPage.kt | tło strony hero (Główna, szczegóły) | źródło | — | obraz + scrim | warstwa `fork_tlo` (scrim przeniesiony z hero do tła) |
| skin/niunia/components/NiuniaOrganisms.kt | NiuniaHeroBackdrop (źródła, odcinek, YouTube, profile) | źródło | — | obraz + scrim | warstwa `fork_tlo` |

## Poza zakresem (z powodem)
- Karty z obrazem (plakaty, miniatury, kafle Live TV, EPG): treść, nie szkło.
- Znaczniki na kartach i w linii meta (NiuniaTag: jakość, wiek, LIVE, ocena): część treści.
- Wiersze listy ustawień (NiuniaListRow Setting na `Surface`): leżą na nieprzezroczystym tle ekranu ustawień, nie ma czego próbkować.
- Nawigacja bazy poza Niunią (ModernSidebarBlurPanel, CollapsedSidebarPill w MainActivity, PillNavigationBar bazy) i bazowy StreamScreen: używane tylko przy innych układach/skórkach; wpięcia nie mieszczą się w limicie 65 plików (patrz DECYZJE, Haze).
- Okna `androidx.compose.ui.window.Dialog` bez NuvioDialog/NiuniaDialog i menu bazy (DropdownMenu): poza wspólnymi miejscami.
