#!/bin/bash
# prefs.sh — ustawienia forka na TV/emulatorze bez klikania (serwer DevPrefsServer, tylko build debug, 127.0.0.1:8765).
# Urządzenie: ANDROID_SERIAL (domyślnie JMGO 192.168.31.252:5555). Zrzuty w NuvioFork/.stage/prefs/ (poza gitem: mogą mieć klucze).
#
#   prefs.sh stores                         lista magazynów (aktywny profil)
#   prefs.sh get <magazyn> [wzorzec]        klucze magazynu (wzorzec = grep po nazwie klucza)
#   prefs.sh find <wzorzec>                 szukaj klucza we wszystkich magazynach
#   prefs.sh set <magazyn> <klucz> <wartość> [typ]   typ: bool|int|long|float|double|string|set (set: '["a","b"]')
#   prefs.sh del <magazyn> <klucz>
#   prefs.sh pull [plik]                    zrzut wszystkich ustawień do pliku (domyślnie .stage/prefs/<data>.json)
#   prefs.sh push <plik>                    wgraj zrzut (zmienia tylko różnice)
#   prefs.sh restart                        uruchom aplikację od nowa (dla ustawień czytanych przy starcie)
# Magazyn SharedPreferences: sp:<nazwa>, np. sp:fork_features. Inny profil: PROFILE=2 prefs.sh get ...
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ANDROID_SERIAL="${ANDROID_SERIAL:-192.168.31.252:5555}"
PKG=com.nuviodebug.com
URL=http://127.0.0.1:8765
P="${PROFILE:+&profile=$PROFILE}"

[[ "$ANDROID_SERIAL" == *:* ]] && adb connect "$ANDROID_SERIAL" >/dev/null 2>&1
adb forward tcp:8765 tcp:8765 >/dev/null || { echo "adb forward nieudany ($ANDROID_SERIAL)"; exit 1; }

enc() { python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1],safe=""))' "$1"; }
call() {
  out=$(curl -s --max-time 15 "$@") || { echo "brak odpowiedzi: czy aplikacja jest uruchomiona (build debug, przełącznik „Zdalne ustawienia”)?"; exit 1; }
  printf '%s\n' "$out"
}

cmd="${1:-}"; shift || true
case "$cmd" in
  stores) call "$URL/stores?x=1$P" ;;
  get)
    out=$(call "$URL/get?store=$(enc "$1")$P")
    if [ -n "${2:-}" ]; then printf '%s' "$out" | python3 -c 'import json,sys;d=json.load(sys.stdin);q=sys.argv[1].lower();[print(k,"=",json.dumps(v["v"],ensure_ascii=False),"("+v["t"]+")") for k,v in d.items() if q in k.lower()]' "$2"
    else printf '%s' "$out" | python3 -c 'import json,sys;d=json.load(sys.stdin);[print(k,"=",json.dumps(v["v"],ensure_ascii=False)[:300],"("+v["t"]+")") for k,v in d.items()] if "error" not in d else print(d)'; fi ;;
  find) call "$URL/dump?x=1$P" | python3 -c 'import json,sys;d=json.load(sys.stdin);q=sys.argv[1].lower();[print(s+" / "+k,"=",json.dumps(v["v"],ensure_ascii=False)[:300],"("+v["t"]+")") for s,kv in d.items() for k,v in kv.items() if q in k.lower()]' "$1" ;;
  set) call "$URL/set?store=$(enc "$1")&key=$(enc "$2")&value=$(enc "$3")${4:+&type=$4}$P" ;;
  del) call "$URL/del?store=$(enc "$1")&key=$(enc "$2")$P" ;;
  pull)
    f="${1:-$ROOT/.stage/prefs/$(date +%Y%m%d-%H%M%S).json}"; mkdir -p "$(dirname "$f")"
    call "$URL/dump?x=1$P" > "$f" && echo "zapisano: $f" ;;
  push) call -X POST --data-binary @"$1" "$URL/apply?x=1$P" ;;
  restart) adb shell am force-stop $PKG; adb shell am start -n $PKG/com.nuvio.tv.launcher.AppIconDefault >/dev/null; echo "uruchomiono ponownie" ;;
  *) sed -n 2,15p "$0"; exit 2 ;;
esac
