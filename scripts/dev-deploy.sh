#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/dev-common.sh"
target=wifi
serial=""
mode=auto
build=true
apk=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --emulator) target=emulator ;;
    --wifi|--phone) target=wifi ;;
    --usb) target=usb ;;
    --serial) serial="$2"; shift ;;
    --release) mode=release ;;
    --debug) mode=debug ;;
    --no-build) build=false ;;
    --apk) apk="$2"; shift ;;
    -h|--help)
      echo "Usage: $0 [--wifi|--usb|--emulator|--serial SERIAL] [--release|--debug] [--no-build --apk PATH]"
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

require_windows_android_tools
if [[ -z "$serial" && "$target" == emulator ]]; then
  "$ROOT/scripts/dev-emulator.sh" start
  serial="$EMULATOR_SERIAL"
elif [[ -z "$serial" && "$target" == wifi ]]; then
  serial="$(connect_saved_wifi_device)"
elif [[ -z "$serial" ]]; then
  serial="$(find_usb_device)"
  [[ -n "$serial" ]] || { echo "No authorized USB Android device is visible to Windows ADB." >&2; exit 1; }
fi

if [[ "$mode" == auto ]]; then
  [[ "$target" == emulator ]] && mode=debug || mode=release
fi

if $build; then
  cd "$ROOT"
  if [[ "$mode" == release ]]; then
    ./scripts/build-apk.sh
    apk="$ROOT/dist/workout-tracker.apk"
  else
    export JAVA_HOME="$ROOT/.tools/jdk17" ANDROID_HOME="$ROOT/.tools/android-sdk"
    ./gradlew assembleDebug
    apk="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
  fi
fi

[[ -f "$apk" ]] || { echo "APK not found: $apk" >&2; exit 1; }
apk_windows="$(wslpath -w "$apk")"
echo "Installing $(basename "$apk") directly from WSL to $serial..."
windows_adb -s "$serial" install -r "$apk_windows"
windows_adb -s "$serial" shell am force-stop com.miketwo.workouttracker
windows_adb -s "$serial" shell am start -W -n com.miketwo.workouttracker/.HomeActivity >/dev/null
echo "Workout Tracker is running on $serial."
