# B11-1: pomiar jakości mowy po polsku (17.11.2026)

**Metoda (ograniczona):** 62 polecenia (lista w `plan/raporty/mowa-polecenia.txt`) zsyntezowane głosem macOS „Zosia” (czysty, bezszumowy głos syntetyczny, 16 kHz), rozpoznane przez faster-whisper (CTranslate2, INT8, CPU Mac, `language=pl`, beam 1). To NIE jest sherpa-onnx z aplikacji ani realny mikrofon pilota; wyniki są OPTYMISTYCZNE co do szumu i PESYMISTYCZNE/niepewne co do silnika. Metryka: dokładne zgodność po normalizacji (bez diakrytyków, małe litery), jeszcze BEZ dopasowania encji/parsera.

| model | dokładne trafienia | czas na polecenie (Mac, 4 wątki) |
|---|---|---|
| tiny | 15 / 62 (24%) | 0,22 s |
| base | 22 / 62 (35%) | 0,43 s |
| small | 38 / 62 (61%) | 1,34 s |

Typowe błędy: „pauza” → „Pajł za”/„Pausa”; „włącz napisy” → „Wącz na pisa”/„Włączne pisy”; „TVP jeden” → „TVP1” (to akurat poprawne dla parsera, bo liczebniki są normalizowane); „przewiń” → „Przewin/Przewim”.

## Wnioski
- tiny jest za słaby po polsku nawet na czystym głosie; base słaby; small wyraźnie najlepszy, ale 3–6× wolniejszy.
- Dokładne trafienie zaniża użyteczność: parser reguł (RuleParser) i dopasowanie encji tolerują fleksję i literówki, a warstwa znaczeniowa/model API ratuje resztę. Rzeczywista trafność poleceń po parserze NIE zmierzona (wymaga przepuszczenia transkrypcji przez Kotlin `RuleParser`; test JVM do dopisania).
- Rekomendacja progów B07-6: domyślnie small, gdy RAM > 3 GB i nie trwa odtwarzanie; tiny/base tylko jako ostateczność z jawnym ostrzeżeniem. Do potwierdzenia na telewizorze (CPU ARM znacznie wolniejszy niż Mac).
