#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
pr="${1:?Usage: $0 PR_NUMBER [--phone]}"
deploy=emulator
[[ "${2:-}" == "--phone" ]] && deploy=phone

[[ -z "$(git status --porcelain)" ]] || { echo "Working tree must be clean before accepting a PR." >&2; exit 1; }
state="$(gh pr view "$pr" --json state --jq .state)"
[[ "$state" == OPEN ]] || { echo "PR #$pr is not open (state: $state)." >&2; exit 1; }

gh pr merge "$pr" --merge --delete-branch
git switch main
git pull --ff-only origin main
if [[ "$deploy" == phone ]]; then
  ./scripts/dev-deploy.sh --phone --release
else
  ./scripts/dev-deploy.sh --emulator
fi
