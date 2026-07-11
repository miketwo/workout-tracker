#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE="$ROOT/.ralph"
mkdir -p "$STATE"

if [[ -f "$STATE/ralph.pid" ]] && kill -0 "$(cat "$STATE/ralph.pid")" 2>/dev/null; then
  echo "RALPH is already running as PID $(cat "$STATE/ralph.pid")"
  exit 0
fi

"$ROOT/scripts/ralph-setup.sh"
nohup "$ROOT/scripts/ralph-loop.sh" >>"$STATE/ralph.log" 2>&1 </dev/null &
echo $! > "$STATE/ralph.pid"
echo "RALPH started as PID $!. Log: $STATE/ralph.log"
