# Wydajność: wyniki przed/po (Xiaomi TV Box, debug, 17.11.2026)

Zakres wg polecenia właściciela: tylko moduły forka. JMGO niezmierzony (BLOKADY.md). Surowe: `wydajnosc-surowe/`.

| Pomiar | Przed | Po |
|---|---|---|
| Live TV, bezczynność z grającym podglądem (CPU 20 s) | 46–86% rdzenia | 53% w pierwszej minucie, **1,2% po auto-stop (60 s)** |
| Live TV, ruch listy 20 × ▼ | 33–49% zacinających się | 38–46% (bez zmiany; nie jest dziełem forka) |
| Start, CPU 60 s | 17% rdzenia | rozgrzewka przewodnika opóźniona o 8 s (na tym TV i tak pomijana) |

## Wniosek z A/B
- Wyłączenie modułu `tv` nie zmienia zacinania listy Live TV (46%/38% bez, 43–49% z): koszt siedzi w liście Reshaped, nie w module forka. Nie ruszane (cudzy kod).
- Podgląd kanału (TextureView ~50 kl./s) był jedynym zmierzonym dużym obciążeniem modułu: łatka `PerfFlags` zatrzymuje go po 60 s bez zmiany kanału (przełącznik Telewizja → „Zatrzymaj podgląd kanału po minucie”, domyślnie włączony; zmiana kanału wznawia).

## Nie zrobione / nadal zacina
- Przewijanie listy Live TV na Xiaomi: ~40% klatek po terminie (Reshaped, p90 150–200 ms). Wymaga profilowania kompozycji listy.
- Pomiar JMGO, wariant release, 30 min oglądania, wyciek pamięci, zmiana kanału w serii 20, przewodnik: niewykonane w tej sesji.
- Narzędzia/Moduły płynne (1%).
