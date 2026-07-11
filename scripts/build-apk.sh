#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ ! -x "$ROOT/.tools/jdk17/bin/java" || ! -f "$ROOT/local.properties" ]]; then
  "$ROOT/scripts/bootstrap-android.sh"
fi

if [[ ! -f "$ROOT/.keys/workout-tracker.jks" ]]; then
  echo "The private signing key is missing. Restore .keys/workout-tracker.jks before building an update." >&2
  exit 1
fi

export JAVA_HOME="$ROOT/.tools/jdk17"
export ANDROID_HOME="$ROOT/.tools/android-sdk"
mkdir -p "$ROOT/dist"
cd "$ROOT"
./gradlew testDebugUnitTest lintRelease assembleRelease
cp app/build/outputs/apk/release/app-release.apk dist/workout-tracker.apk
echo "APK ready: $ROOT/dist/workout-tracker.apk"
