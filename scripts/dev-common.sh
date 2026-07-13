#!/usr/bin/env bash

DEV_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB_SERVER_PORT="${ANDROID_ADB_SERVER_PORT:-5038}"
EMULATOR_SERIAL="${WORKOUT_EMULATOR_SERIAL:-emulator-5554}"
AVD_NAME="${WORKOUT_AVD_NAME:-WorkoutTracker_Pixel8Pro_API36}"
DEV_STATE="$DEV_ROOT/.dev-state"
WIFI_DEVICE_FILE="$DEV_STATE/wifi-device"

if [[ -z "${WINDOWS_USERNAME:-}" ]]; then
  WINDOWS_USERNAME="$(cd /mnt/c && /mnt/c/Windows/System32/cmd.exe /d /c 'echo %USERNAME%' 2>/dev/null | tr -d '\r' | tail -n 1)"
fi
WINDOWS_PROFILE="${WINDOWS_PROFILE:-/mnt/c/Users/$WINDOWS_USERNAME}"
WINDOWS_SDK="${WINDOWS_ANDROID_SDK:-$WINDOWS_PROFILE/AppData/Local/Android/Sdk}"
WINDOWS_SDK_WIN="$(wslpath -w "$WINDOWS_SDK")"
WINDOWS_ADB="$WINDOWS_SDK/platform-tools/adb.exe"
WINDOWS_EMULATOR="$WINDOWS_SDK/emulator/emulator.exe"

windows_adb() {
  (cd /mnt/c && ANDROID_ADB_SERVER_PORT="$ADB_SERVER_PORT" "$WINDOWS_ADB" "$@")
}

require_windows_android_tools() {
  if [[ ! -x "$WINDOWS_ADB" || ! -x "$WINDOWS_EMULATOR" ]]; then
    echo "Windows Android tools are missing. Run ./scripts/setup-windows-emulator.sh first." >&2
    return 1
  fi
}

wait_for_android() {
  local serial="$1" attempts="${2:-90}"
  for ((i=1; i<=attempts; i++)); do
    if [[ "$(windows_adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "Android device $serial did not finish booting." >&2
  return 1
}

find_physical_device() {
  windows_adb devices | tr -d '\r' | awk '$2=="device" && $1 !~ /^emulator-/ {print $1; exit}'
}

find_usb_device() {
  windows_adb devices | tr -d '\r' | awk '$2=="device" && $1 !~ /^emulator-/ && $1 !~ /:/ {print $1; exit}'
}

connect_saved_wifi_device() {
  local saved="" connected="" discovered="" endpoint
  [[ -f "$WIFI_DEVICE_FILE" ]] && saved="$(tr -d '[:space:]' < "$WIFI_DEVICE_FILE")"
  # ADB on Windows persists across Codex sessions. Prefer its live Wi-Fi transport
  # over the last saved port because Android may rotate that port at any time.
  connected="$(windows_adb devices 2>/dev/null | tr -d '\r' | awk '$2=="device" && $1 ~ /:/ && $1 !~ /^emulator-/ {print $1; exit}')"
  discovered="$(windows_adb mdns services 2>/dev/null | tr -d '\r' | awk '$2=="_adb-tls-connect._tcp" && $3 ~ /:/ {print $3; exit}')"
  for endpoint in "$connected" "$saved" "$discovered"; do
    [[ -n "$endpoint" ]] || continue
    windows_adb connect "$endpoint" >/dev/null 2>&1 || true
    if windows_adb -s "$endpoint" get-state >/dev/null 2>&1; then
      mkdir -p "$DEV_STATE"
      printf '%s\n' "$endpoint" > "$WIFI_DEVICE_FILE"
      chmod 600 "$WIFI_DEVICE_FILE"
      printf '%s\n' "$endpoint"
      return 0
    fi
  done
  echo "No paired Pixel is reachable over Wi-Fi." >&2
  echo "Enable Wireless debugging, then run ./scripts/wifi-pair.sh (first use) or ./scripts/wifi-connect.sh (already paired)." >&2
  return 1
}
