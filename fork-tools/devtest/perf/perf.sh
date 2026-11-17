#!/bin/bash
# Pomiary wydajnosci forka. Uzycie: DEV=<ip:port> perf.sh <krok> [argumenty]
# Metoda jak w Kicii (dumpsys gfxinfo framestats, am start -W, top -H), dostosowana do com.nuviodebug.com.
export PATH=$PATH:$HOME/Library/Android/sdk/platform-tools
PKG=${PKG:-com.nuviodebug.com}
RAW=${RAW:-$(cd "$(dirname "$0")/../../../plan/raporty/wydajnosc-surowe" && pwd)}
D="adb -s $DEV"
tag=$(echo "$DEV" | tr ':.' '__')
start_args="-n $PKG/com.nuvio.tv.MainActivity"
cmd=$1; shift
case $cmd in
  starts)  # zimny start xN: am start -W
    n=${1:-5}; for i in $(seq 1 $n); do
      $D shell am force-stop $PKG; sleep 3; $D shell input keyevent KEYCODE_WAKEUP
      $D shell am start -W -n $PKG/com.nuvio.tv.launcher.AppIconDefault | grep -E "TotalTime|WaitTime" | tr '\n' ' '; echo
    done ;;
  warm)
    n=${1:-5}; for i in $(seq 1 $n); do
      $D shell input keyevent KEYCODE_HOME; sleep 2
      $D shell am start -W -n $PKG/com.nuvio.tv.launcher.AppIconDefault | grep -E "TotalTime" | tr '\n' ' '; echo
    done ;;
  gfxreset) $D shell dumpsys gfxinfo $PKG reset >/dev/null ;;
  gfx)  # zrzuc statystyki klatek -> p50/p90/p99 itp
    $D shell dumpsys gfxinfo $PKG | grep -E "Total frames|Janky frames|50th|90th|95th|99th|Number Missed|Number High input|Number Slow UI|Number Slow bitmap|Number Slow issue|Number Frame deadline" | tr -s ' ' | tr '\n' ';'; echo ;;
  cpu)  # cpu aplikacji w % przez N sekund (suma utime+stime)
    s=${1:-30}; pid=$($D shell pidof $PKG | tr -d '\r'); a=$($D shell cat /proc/$pid/stat | awk '{print $14+$15}'); sleep $s
    b=$($D shell cat /proc/$pid/stat | awk '{print $14+$15}'); echo "cpu_ticks=$((b-a)) w ${s}s => $(python3 -c "print(round(($b-$a)/100/$s*100,1))")% jednego rdzenia" ;;
  mem) $D shell dumpsys meminfo $PKG | grep -E "TOTAL PSS|TOTAL RSS|Java Heap:|Native Heap:|Graphics:|TOTAL SWAP" | tr -s ' ' | tr '\n' ';'; echo; $D shell cat /proc/meminfo | grep -E "MemAvailable|SwapFree|SwapTotal" | tr -s ' ' | tr '\n' ';'; echo ;;
  net) $D shell cat /proc/net/dev | awk '/wlan0:/{sub(/wlan0:/,"");print "wlan0 rx="$1" tx="$9}' ;;
  topH) $D shell "top -H -b -n 1 -m 12 -p $($D shell pidof $PKG | tr -d '\r')" | tail -15 ;;
  key) for k in "$@"; do $D shell input keyevent KEYCODE_$k; done ;;
  keyrep) k=$1; n=$2; d=${3:-0.15}; for i in $(seq 1 $n); do $D shell input keyevent KEYCODE_$k; sleep $d; done ;;
  route) $D shell am start -n $PKG/com.nuvio.tv.MainActivity -a com.nuvio.fork.OPEN --es route $1 >/dev/null ;;
  *) echo "nieznany krok $cmd"; exit 1 ;;
esac
