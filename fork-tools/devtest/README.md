# Narzędzia do testów na urządzeniach (tekstowo, bez zrzutów)

- `emu.py`: pomocnik Pythona (`DEV=<ip:port> python3`): `dump()`, `texts()`, `key()`, `label_at_focus()`, `start_app()`, `tap_text()`. Dotyk (`adb input tap`) NIE aktywuje przycisków TV: używać `key("DPAD_...")`.
- `install_device.sh <ip:port>`: dobiera APK wg ABI (Xiaomi TV Box 3rd gen = tylko armeabi-v7a), instaluje, sprawdza sumę kontrolną.
- Skrót nawigacji (tylko build debug): `adb -s $DEV shell am start -n com.nuviodebug.com/com.nuvio.tv.MainActivity -a com.nuvio.fork.OPEN --es route <trasa>` (np. `reshaped_live_tv`, `fork_tools`, `fork_modules`, `fork_tv_search`, `fork_discover`, `fork_picks`, `settings`, `home`).
- Szybkie klawisze: `adb shell input keyevent KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN ...` (wiele naraz).
- ADB po Wi-Fi z macOS: gdy `adb connect` daje „No route to host” mimo otwartego portu (`nc -z IP PORT`), wykonaj `adb kill-server; adb start-server` i połącz ponownie.
- Pomiary wydajności z Kicii: `../nuvio-tv-kicia/docs/makieta-tv/v7/pomiary/pomiar.sh` (kroki: starts, starts12, screens, tabs, mem, lib_redo, build).
