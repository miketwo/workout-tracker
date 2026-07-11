#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/scripts/dev-common.sh"
require_windows_android_tools
mkdir -p "$DEV_STATE"

pair_endpoint="${1:-}"
pair_code="${2:-}"
connect_endpoint="${3:-}"

if [[ -z "$pair_endpoint" ]]; then
  read -r -p "Pairing IP address and port shown under 'Pair device with pairing code': " pair_endpoint
fi
if [[ -z "$pair_code" ]]; then
  read -r -s -p "Six-digit Wi-Fi pairing code: " pair_code
  echo
fi

printf '%s\n' "$pair_code" | windows_adb pair "$pair_endpoint"

if [[ -z "$connect_endpoint" ]]; then
  read -r -p "IP address and port on the main Wireless debugging screen: " connect_endpoint
fi
windows_adb connect "$connect_endpoint"
windows_adb -s "$connect_endpoint" get-state >/dev/null
printf '%s\n' "$connect_endpoint" > "$WIFI_DEVICE_FILE"
chmod 600 "$WIFI_DEVICE_FILE"
model="$(windows_adb -s "$connect_endpoint" shell getprop ro.product.model | tr -d '\r')"
echo "Paired and connected to $model at $connect_endpoint."
echo "Future changes can be deployed with: ./scripts/dev-deploy.sh"
