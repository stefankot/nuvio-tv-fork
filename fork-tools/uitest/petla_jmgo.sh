#!/bin/bash
# petla_jmgo.sh <N> [ścieżki...] — jedna pętla testów UX na JMGO: czekanie na wolny TV, starty ×10, pamięć, 50 ścieżek,
# pamięć po ścieżkach i po wyjściu z Live TV. Wynik: plan/ux/jmgo/cykl<N>/ (przebieg.log, starty.txt, pamiec.txt, sciezki.*).
set -u
R="$(cd "$(dirname "$0")/../.." && pwd)"; N=${1:?numer pętli}; shift
O="$R/plan/ux/jmgo/cykl$N"; mkdir -p "$O"
eval "$(bash "$R/fork-tools/test/jmgo_adb.sh" --env)"
bash "$R/fork-tools/test/jmgo_czekaj_wolny.sh" 1 180 || { echo "JMGO zajęty"; exit 3; }
echo "[$(date +%H:%M)] pętla $N: start"
bash "$R/fork-tools/uitest/perf_jmgo.sh" starty "$O" 10
bash "$R/fork-tools/uitest/perf_jmgo.sh" pamiec "$O" "po startach"
python3 -I "$R/fork-tools/uitest/sciezki_jmgo.py" --cykl "$N" --bez-czekania "$@"
bash "$R/fork-tools/uitest/perf_jmgo.sh" pamiec "$O" "po ścieżkach"
adb shell am start -a com.nuvio.fork.OPEN -n com.nuviodebug.com/com.nuvio.tv.MainActivity --es route reshaped_live_tv >/dev/null; sleep 15
bash "$R/fork-tools/uitest/perf_jmgo.sh" pamiec "$O" "w Live TV"
adb shell am start -a com.nuvio.fork.OPEN -n com.nuviodebug.com/com.nuvio.tv.MainActivity --es route home >/dev/null; sleep 15
bash "$R/fork-tools/uitest/perf_jmgo.sh" pamiec "$O" "po wyjściu z Live TV"
echo "[$(date +%H:%M)] pętla $N: koniec"
