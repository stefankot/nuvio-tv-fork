---
name: feedback-limit-buildow
description: "Ostry limit Gradle — 1 build naraz, 1 w kolejce, nowe zlecenie zeruje kolejkę; tylko przez fork-tools/build.sh"
metadata:
  node_type: memory
  type: feedback
  originSessionId: b166a009-6571-4f92-86c1-07435a2e8088
  modified: 2026-11-19T14:59:26.602Z
---

Najwyżej 1 build Gradle równocześnie, najwyżej 1 w kolejce, a przed każdym buildem kolejka jest zerowana (nowe zlecenie anuluje czekające). Uruchamiać wyłącznie `bash fork-tools/build.sh <nazwa-logu> <zadania...>` w `NuvioFork`, w zakładce terminala; nigdy bezpośrednio `./gradlew`, nigdy `--stop`/`pkill` cudzego buildu.

**Why:** 19.11.2026 dwa wątki skompilowały naraz w tym samym katalogu — wspólny log, jeden zatrzymał demona drugiego; Mac ma 8 GB RAM.

**How to apply:** każdy build/kompilacja/test Gradle przez skrypt; kod wyjścia 75 = anulowany w kolejce (nie błąd). Powiązane: [[feedback_buildy_zbiorczo]].
