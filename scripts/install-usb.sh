#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "Connect the Pixel with USB debugging enabled and approve its authorization prompt."
"$ROOT/scripts/dev-deploy.sh" --usb --release
