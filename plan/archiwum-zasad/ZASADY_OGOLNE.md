# Zasady ogólne (właściciel)

## Jak najmniejsze zmiany w kodzie bazowym i forków (19.11.2026)
- Cel: łatwe scalanie nowych wydań bazy (NuvioTV) i forków (KE, Echo, Reshaped, Arc…).
- Kod bazy i forków kopiuj 1:1; NIE edytuj ich plików, jeśli da się inaczej. Nowe rzeczy w osobnych plikach/modułach (np. `fork/…`, funkcje rozszerzające w tym samym pakiecie, adaptery).
- Gdy wpięcie w plik bazy jest konieczne: jedna linia wywołania (hook) do kodu forka, opisana komentarzem, zapisana jako łatka (`fork-tools/patches/`).
- Nie podmieniaj całych plików bazy wersją z forka; brakujące funkcje dodaj obok.
- Konstrukcja modułowa: każda funkcja do włączenia/wyłączenia i usunięcia bez ruszania reszty.

Zobacz też: `ZASADA_BUILDY_W_PACZKACH.md`.

## Wszystkie moduły zapisują do logu (19.11.2026, ZASADA STAŁA)
- Każdy moduł forka i każda kopia z innego forka (przez adapter) zapisuje do logu (`android.util.Log`, tag modułu, np. `ForkLektor`, `ForkAiSubtitles`, `ForkRanking`): start i koniec ważnych czynności, wybory (co i dlaczego), wyniki oraz KAŻDY błąd z typem wyjątku i komunikatem (`${e.javaClass.simpleName}: ${e.message}`).
- Żaden błąd nie może zostać „połknięty” (`runCatching { … }.getOrNull()`, `catch {}` bez logu, błąd tylko w polu statusu). Jeśli wynik jest pomijany, najpierw `Log.w`.
- Błąd, który użytkownik odczuwa (funkcja nic nie robi), dodatkowo krótki komunikat na ekranie — co się stało i co zrobić.
- Bez treści prywatnych w logu: bez kluczy API, haseł, tokenów i tekstu dialogów (dopuszczalne: liczby, nazwy dodatków, kody języków, krótkie fragmenty diagnostyczne).
- Logi muszą trafiać do pełnego logu aplikacji (`fork/family/FullLog`) i panelu, żeby dało się je sprawdzić po zgłoszeniu właściciela.

## Efekty interfejsu tylko natywnie (właściciel, 19.11.2026)
Rozmycia i wszystkie inne efekty interfejsu realizujemy wyłącznie tam, gdzie Android obsługuje je natywnie (np. rozmycie = RenderEffect od Androida 12 / API 31, refrakcja = RuntimeShader od API 33). Na starszych wersjach (np. JMGO, Android 11) efektu NIE symulujemy programowo (żadnego liczenia pikseli w aplikacji) — zostaje zwykłe tło/kolor z tokenów skórki.
