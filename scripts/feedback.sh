#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
title="${1:?Usage: $0 'Issue title' ['Issue description']; description may also be piped on stdin}"
shift
if [[ $# -gt 0 ]]; then
  body="$*"
elif [[ ! -t 0 ]]; then
  body="$(cat)"
else
  body="Implement the requested improvement while preserving the app's low-cognitive-overhead workout flow."
fi

url="$(gh issue create --repo "$(gh repo view --json nameWithOwner --jq .nameWithOwner)" \
  --title "$title" --body "$body" --label 'ralph:ready')"
echo "Feedback submitted and approved for RALPH: $url"
