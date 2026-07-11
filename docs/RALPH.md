# Guarded RALPH feedback loop

RALPH watches explicitly approved issues, asks Codex to implement one in an isolated Git worktree, validates the result, and opens a pull request for human review. It never merges or pushes generated changes directly to `main`.

## GitHub issue workflow (default)

1. Create a GitHub issue with clear acceptance criteria.
2. Add the `ralph:ready` label only when the issue is safe for autonomous implementation.
3. Start the loop with `./scripts/ralph-start.sh`.
4. Watch it with `tail -f .ralph/ralph.log`.
5. RALPH installs the validated PR build into the local emulator automatically.
6. Review, test, and merge the resulting PR yourself.
7. Stop it with `./scripts/ralph-stop.sh`.

The loop changes labels from `ralph:ready` → `ralph:in-progress` → `ralph:awaiting-review`. Failures receive `ralph:failed` and are not retried until a human removes that label and restores `ralph:ready`.

Run a non-mutating poll check with:

```bash
./scripts/ralph-loop.sh --once --dry-run
```

Useful configuration:

```bash
RALPH_POLL_SECONDS=120 ./scripts/ralph-start.sh
RALPH_CODEX_TIMEOUT=5400 ./scripts/ralph-start.sh
```

## GitLab issue source

The code repository and resulting PRs still live on GitHub, but issues may be polled from GitLab. Create these four labels in the GitLab project: `ralph:ready`, `ralph:in-progress`, `ralph:awaiting-review`, and `ralph:failed`. Then provide a project-scoped API token and project path:

```bash
export RALPH_PROVIDER=gitlab
export RALPH_REPO=group/project
export GITLAB_TOKEN=your_project_access_token
export GITLAB_URL=https://gitlab.example.com  # omit for gitlab.com
./scripts/ralph-start.sh
```

Do not place tokens in the repository. `.env*` and `.ralph/` are ignored.

## Safety boundaries

- Only explicitly labelled issues are processed.
- Issue text is treated as untrusted data and cannot broaden the agent's authority.
- Codex receives workspace-write sandboxing in an isolated worktree and no signing key.
- The outer script—not Codex—runs Git operations, validation, and PR creation.
- Unit tests, Android lint, and a debug APK build must all pass.
- Every result requires human PR review and merge.
- Successful PR builds are deployed only to the local emulator, never automatically to the physical phone.
- Failed worktrees and logs remain under ignored `.ralph/` for diagnosis.
