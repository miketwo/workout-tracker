#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="$ROOT/.tools"
JDK="$TOOLS/jdk17"
SDK="$TOOLS/android-sdk"

mkdir -p "$TOOLS"

if [[ ! -x "$JDK/bin/java" ]]; then
  echo "Downloading a local Temurin JDK 17..."
  curl -L --fail --retry 3 -o "$TOOLS/jdk17.tar.gz" \
    'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse'
  mkdir -p "$JDK"
  tar -xzf "$TOOLS/jdk17.tar.gz" -C "$JDK" --strip-components=1
  rm "$TOOLS/jdk17.tar.gz"
fi

if [[ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]]; then
  echo "Downloading Android command-line tools..."
  curl -L --fail --retry 3 -o "$TOOLS/cmdline-tools.zip" \
    'https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip'
  mkdir -p "$SDK/cmdline-tools"
  unzip -q "$TOOLS/cmdline-tools.zip" -d "$SDK/cmdline-tools"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm "$TOOLS/cmdline-tools.zip"
fi

export JAVA_HOME="$JDK"
export PATH="$JAVA_HOME/bin:$PATH"
unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" \
  'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'

printf 'sdk.dir=%s\n' "$SDK" > "$ROOT/local.properties"
echo "Android build tools are ready."
