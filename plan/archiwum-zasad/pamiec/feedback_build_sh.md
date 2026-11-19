---
name: gradle-tylko-przez-build-sh
description: "Gradle tylko przez fork-tools/build.sh (1 build naraz, 1 w kolejce); nigdy --stop/pkill demona"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 2fb32af7-ac15-4a07-8d9b-6f87c664bebf
  modified: 2026-11-19T15:02:25.471Z
---

Gradle w NuvioFork uruchamiaj WYŁĄCZNIE `bash fork-tools/build.sh <log> <zadania>` w zakładce terminala (zasada właściciela 19.11, `fork-tools/ZASADA_LIMIT_BUILDOW.md`). Nigdy `./gradlew --stop`, `pkill KotlinCompileDaemon` ani zatrzymywanie cudzego buildu.

**Why:** 19.11 równolegle pracują inne wątki na tej samej gałęzi; zatrzymanie demona przerwało build innego wątku, a dwa buildy naraz na 8 GB RAM to swap.

**How to apply:** przed buildem sprawdź `.build-lock/`; kod wyjścia 75 = anulowany w kolejce. Przed commitem `git pull`/sprawdź `git log`, bo inne wątki commitują. Powiązane: [[buildy-zbiorczo-bez-bezczynnosci]].
