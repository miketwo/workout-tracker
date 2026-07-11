#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT/dist/workout-tracker.apk"
ADB="$ROOT/.tools/android-sdk/platform-tools/adb"

if [[ ! -f "$APK" ]]; then "$ROOT/scripts/build-apk.sh"; fi
if [[ ! -x "$ADB" ]]; then "$ROOT/scripts/bootstrap-android.sh"; fi

echo "Connect the Pixel with USB debugging enabled and approve its authorization prompt."
"$ADB" start-server
"$ADB" devices -l
"$ADB" install -r "$APK"
echo "Installed Workout Tracker. Existing app data was preserved."
