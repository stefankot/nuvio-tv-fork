# Zasada: buildy tylko w dużych paczkach (właściciel, 18.11.2026)

- **Nie wolno robić buildu z jedną poprawką.** Build (Gradle, APK) uruchamia się dopiero po zebraniu dużej paczki poprawek.
- Drobne poprawki znalezione w trakcie testów zapisuj w kodzie i na liście paczki (`NuvioFork/plan/TERAZ.md`), commit bez buildu; wejdą do następnego dużego buildu.
- Testy na urządzeniu w tym czasie prowadź na ostatnim zbudowanym APK.
- Gdy paczka jest gotowa: jeden build, jedna architektura (JMGO/Xiaomi `armeabi-v7a`), w zakładce terminala z `--console=rich | tee /tmp/buildN.log`.
