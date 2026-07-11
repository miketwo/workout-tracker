#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/dev-common.sh"
command="${1:-start}"
STATE="$DEV_STATE"
mkdir -p "$STATE"

case "$command" in
  start)
    require_windows_android_tools
    if windows_adb -s "$EMULATOR_SERIAL" get-state >/dev/null 2>&1; then
      echo "Emulator $AVD_NAME is already running."
    else
      echo "Starting hardware-accelerated Windows emulator $AVD_NAME..."
      (cd /mnt/c && ANDROID_SDK_ROOT="$WINDOWS_SDK_WIN" ANDROID_ADB_SERVER_PORT="$ADB_SERVER_PORT" \
        nohup "$WINDOWS_EMULATOR" -avd "$AVD_NAME" -port 5554 -gpu host -no-boot-anim -no-metrics \
        >"$STATE/emulator.log" 2>&1 </dev/null & echo $! >"$STATE/emulator.pid")
    fi
    wait_for_android "$EMULATOR_SERIAL"
    windows_adb -s "$EMULATOR_SERIAL" shell settings put global window_animation_scale 0.5 >/dev/null
    windows_adb -s "$EMULATOR_SERIAL" shell settings put global transition_animation_scale 0.5 >/dev/null
    windows_adb -s "$EMULATOR_SERIAL" shell settings put global animator_duration_scale 0.5 >/dev/null
    windows_adb -s "$EMULATOR_SERIAL" shell svc power stayon true >/dev/null
    echo "Emulator ready: $EMULATOR_SERIAL"
    ;;
  stop)
    if windows_adb -s "$EMULATOR_SERIAL" get-state >/dev/null 2>&1; then
      windows_adb -s "$EMULATOR_SERIAL" emu kill >/dev/null
      echo "Emulator stopped."
    else
      echo "Emulator is not running."
    fi
    ;;
  status)
    windows_adb devices -l
    ;;
  *) echo "Usage: $0 start|stop|status" >&2; exit 2 ;;
esac
