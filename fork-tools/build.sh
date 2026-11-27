#!/bin/bash
# Jedyny dozwolony sposób uruchamiania Gradle w tym projekcie (ZASADA: ZASADY.md, sekcja 4).
#   najwyżej 1 build równocześnie, najwyżej 1 build w kolejce,
#   każde nowe zlecenie najpierw zeruje kolejkę (czekający build zostaje anulowany i zastąpiony nowym).
# Użycie (zawsze w zakładce terminala, postęp widoczny):
#   bash fork-tools/build.sh <nazwa-logu> <zadania i opcje gradle...>
#   np. bash fork-tools/build.sh build122 :app:assembleFullDebug -PforkAbi=armeabi-v7a
# Log: /tmp/<nazwa-logu>.log. Kod wyjścia = kod Gradle; 75 = anulowany w kolejce (zastąpiony nowszym zleceniem).

set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOCKS="$ROOT/.build-lock"
RUN="$LOCKS/run"      # katalog = trwa build; w środku pid
QUEUE="$LOCKS/queue"  # plik z pid czekającego zlecenia
NAME="${1:?podaj nazwę logu, np. build122}"; shift
[ $# -gt 0 ] || { echo "podaj zadania gradle"; exit 2; }
LOG="/tmp/$NAME.log"
mkdir -p "$LOCKS"
say() { echo "[build.sh $(date +%H:%M:%S)] $*"; }
alive() { [ -n "${1:-}" ] && kill -0 "$1" 2>/dev/null; }

# 1. Wyzeruj kolejkę: czekające zlecenie (jeśli jest) zostaje anulowane.
if [ -f "$QUEUE" ]; then
  old=$(cat "$QUEUE" 2>/dev/null)
  if alive "$old" && [ "$old" != "$$" ]; then say "zeruję kolejkę: anuluję czekające zlecenie pid $old"; kill "$old" 2>/dev/null; fi
fi
echo $$ > "$QUEUE"
say "w kolejce (pid $$): $*"

cleanup_queue() { [ "$(cat "$QUEUE" 2>/dev/null)" = "$$" ] && rm -f "$QUEUE"; }
trap 'cleanup_queue; say "anulowane w kolejce"; exit 75' TERM INT

# 2. Czekaj, aż skończy się bieżący build (także Gradle uruchomiony poza tym skryptem).
while :; do
  [ "$(cat "$QUEUE" 2>/dev/null)" = "$$" ] || { say "zastąpione nowszym zleceniem — anulowane"; exit 75; }
  holder=$(cat "$RUN/pid" 2>/dev/null)
  if [ -d "$RUN" ] && ! alive "$holder"; then say "usuwam martwą blokadę (pid ${holder:-?})"; rm -rf "$RUN"; fi
  stray=$(pgrep -f "gradle-wrapper.jar" | head -1)
  if [ -z "$stray" ] && mkdir "$RUN" 2>/dev/null; then echo $$ > "$RUN/pid"; break; fi
  sleep 5
done

# 3. Start: zlecenie schodzi z kolejki, blokada zwalniana na końcu.
cleanup_queue
trap 'rm -rf "$RUN"; say "build przerwany"; exit 130' TERM INT
trap 'rm -rf "$RUN"' EXIT
say "start: $* (log $LOG)"
cd "$ROOT" || exit 1
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
# Czas kompilacji dla napisu „built-…” w aplikacji (fork/buildage, tylko debug); plik poza gitem.
mkdir -p "$ROOT/app/src/main/assets/fork" && date +%s > "$ROOT/app/src/main/assets/fork/build_time.txt"
# 27.11: bez pamięci konfiguracji (włączonej w ~/.gradle/gradle.properties) — zadania NuvioTV 1.2.0-beta.4
# (downloadAudioSyncRuntime, buildUsenet*) padają z nią na „this.this$0 is null”; plików bazy nie zmieniamy.
./gradlew "$@" --console=rich --no-configuration-cache 2>&1 | tee "$LOG"
code=${PIPESTATUS[0]}
say "koniec: kod $code"
exit "$code"
