#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$ROOT/.ralph/ralph.pid"
SESSION="workout-tracker-ralph"
if command -v tmux >/dev/null && tmux has-session -t "$SESSION" 2>/dev/null; then
  tmux kill-session -t "$SESSION"
  rm -f "$PID_FILE"
  echo "RALPH stopped."
  exit 0
fi
if [[ ! -f "$PID_FILE" ]]; then echo "RALPH is not running."; exit 0; fi
pid="$(cat "$PID_FILE")"
if kill -0 "$pid" 2>/dev/null; then kill "$pid"; fi
rm -f "$PID_FILE"
echo "RALPH stopped."
