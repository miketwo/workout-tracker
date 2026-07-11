#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
provider="${RALPH_PROVIDER:-github}"
repo="${RALPH_REPO:-$(gh repo view --json nameWithOwner --jq .nameWithOwner)}"

if [[ "$provider" == "github" ]]; then
  gh auth status >/dev/null
  gh label create 'ralph:ready' --repo "$repo" --color 0E8A16 --description 'Approved for automated implementation' --force
  gh label create 'ralph:in-progress' --repo "$repo" --color FBCA04 --description 'Currently being handled by RALPH' --force
  gh label create 'ralph:awaiting-review' --repo "$repo" --color 1D76DB --description 'Automated PR is ready for human review' --force
  gh label create 'ralph:failed' --repo "$repo" --color B60205 --description 'Automation needs human attention before retry' --force
else
  echo "For GitLab, create the four labels documented in docs/RALPH.md or use your project label settings." >&2
  echo "RALPH GitLab label setup requires manual confirmation for project $repo."
  exit 0
fi

echo "RALPH labels are ready for $provider repository $repo."
