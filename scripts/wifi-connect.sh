#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/dev-common.sh"
require_windows_android_tools
mkdir -p "$DEV_STATE"

endpoint="${1:-}"
if [[ -z "$endpoint" ]]; then
  read -r -p "Current IP address and port from the main Wireless debugging screen: " endpoint
fi
windows_adb connect "$endpoint"
windows_adb -s "$endpoint" get-state >/dev/null
printf '%s\n' "$endpoint" > "$WIFI_DEVICE_FILE"
chmod 600 "$WIFI_DEVICE_FILE"
echo "Wireless deployment target updated to $endpoint."
