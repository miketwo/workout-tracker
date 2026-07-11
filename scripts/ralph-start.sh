#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE="$ROOT/.ralph"
SESSION="workout-tracker-ralph"
mkdir -p "$STATE"

if command -v tmux >/dev/null && tmux has-session -t "$SESSION" 2>/dev/null; then
  echo "RALPH is already running in tmux session $SESSION"
  exit 0
fi

"$ROOT/scripts/ralph-setup.sh"
if command -v tmux >/dev/null; then
  tmux new-session -d -s "$SESSION" "cd '$ROOT' && exec '$ROOT/scripts/ralph-loop.sh' >>'$STATE/ralph.log' 2>&1"
  tmux list-panes -t "$SESSION" -F '#{pane_pid}' | head -n 1 > "$STATE/ralph.pid"
  echo "RALPH started in tmux session $SESSION. Log: $STATE/ralph.log"
else
  nohup "$ROOT/scripts/ralph-loop.sh" >>"$STATE/ralph.log" 2>&1 </dev/null &
  echo $! > "$STATE/ralph.pid"
  echo "RALPH started as PID $!. Log: $STATE/ralph.log"
fi
