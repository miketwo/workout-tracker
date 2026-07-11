#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/dev-common.sh"
TOOLS="$ROOT/.tools/windows-setup"
WINDOWS_JDK="$WINDOWS_PROFILE/AppData/Local/WorkoutTracker/jdk17"
WINDOWS_JDK_WIN="$(wslpath -w "$WINDOWS_JDK")"
CMDLINE_VERSION=13114758

mkdir -p "$TOOLS" "$WINDOWS_JDK" "$WINDOWS_SDK/cmdline-tools"

if [[ ! -x "$WINDOWS_JDK/bin/java.exe" ]]; then
  echo "Downloading Windows JDK 17..."
  curl -L --fail --retry 3 -o "$TOOLS/jdk17.zip" \
    'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse'
  rm -rf "$TOOLS/jdk-extract" && mkdir -p "$TOOLS/jdk-extract"
  unzip -q "$TOOLS/jdk17.zip" -d "$TOOLS/jdk-extract"
  cp -a "$TOOLS"/jdk-extract/*/* "$WINDOWS_JDK/"
fi

if [[ ! -x "$WINDOWS_SDK/cmdline-tools/latest/bin/sdkmanager.bat" ]]; then
  echo "Downloading Windows Android command-line tools..."
  curl -L --fail --retry 3 -o "$TOOLS/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-win-${CMDLINE_VERSION}_latest.zip"
  rm -rf "$TOOLS/cmdline-extract" "$WINDOWS_SDK/cmdline-tools/latest"
  mkdir -p "$TOOLS/cmdline-extract" "$WINDOWS_SDK/cmdline-tools/latest"
  unzip -q "$TOOLS/cmdline-tools.zip" -d "$TOOLS/cmdline-extract"
  cp -a "$TOOLS/cmdline-extract/cmdline-tools/"* "$WINDOWS_SDK/cmdline-tools/latest/"
fi

SDKMANAGER="$WINDOWS_SDK_WIN\\cmdline-tools\\latest\\bin\\sdkmanager.bat"
AVDMANAGER="$WINDOWS_SDK_WIN\\cmdline-tools\\latest\\bin\\avdmanager.bat"
echo "Accepting Android SDK licenses and installing the emulator (several GB on first run)..."
(
  cd /mnt/c
  set +o pipefail
  yes | /mnt/c/Windows/System32/cmd.exe /d /c \
    "set JAVA_HOME=$WINDOWS_JDK_WIN&& set ANDROID_SDK_ROOT=$WINDOWS_SDK_WIN&& set HTTP_PROXY=&& set HTTPS_PROXY=&& $SDKMANAGER --licenses"
) >/dev/null
(cd /mnt/c && /mnt/c/Windows/System32/cmd.exe /d /c \
  "set JAVA_HOME=$WINDOWS_JDK_WIN&& set ANDROID_SDK_ROOT=$WINDOWS_SDK_WIN&& set HTTP_PROXY=&& set HTTPS_PROXY=&& $SDKMANAGER platform-tools emulator platforms;android-36 system-images;android-36;google_apis;x86_64")

AVD_DIR="$WINDOWS_PROFILE/.android/avd/$AVD_NAME.avd"
if [[ ! -d "$AVD_DIR" ]]; then
  printf 'no\n' | (cd /mnt/c && /mnt/c/Windows/System32/cmd.exe /d /c \
    "set JAVA_HOME=$WINDOWS_JDK_WIN&& set ANDROID_SDK_ROOT=$WINDOWS_SDK_WIN&& $AVDMANAGER create avd --force --name $AVD_NAME --package system-images;android-36;google_apis;x86_64 --device pixel_8_pro")
fi

CONFIG="$AVD_DIR/config.ini"
sed -i 's/^disk.dataPartition.size=.*/disk.dataPartition.size=6G/;s/^hw.gpu.enabled=.*/hw.gpu.enabled=yes/;s/^hw.keyboard=.*/hw.keyboard=yes/;s/^hw.ramSize=.*/hw.ramSize=4096/' "$CONFIG"
echo "Windows Pixel 8 Pro Android 16 emulator is ready: $AVD_NAME"
