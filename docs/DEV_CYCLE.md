# Tight emulator-to-phone development cycle

The app builds in WSL, while the Pixel 8 Pro Android 16 (API 36) emulator runs hardware-accelerated on Windows. Windows ADB reads APKs directly from their WSL UNC paths, so no manual copies or PowerShell commands are needed.

## Daily loop

Start the emulator once:

```bash
./scripts/dev-emulator.sh start
```

Submit feedback directly to RALPH:

```bash
./scripts/feedback.sh "Make the rest timer text larger" <<'EOF'
The countdown should dominate the rest screen and remain legible from several feet away.
Acceptance criteria:
- Increase the countdown prominence without hiding the next-exercise preview.
- Preserve portrait layout on Pixel 8 Pro dimensions.
EOF
```

RALPH claims the issue, makes an isolated branch, runs tests/lint/build, opens a PR, and automatically installs that PR's debug APK into the running emulator. Watch progress with:

```bash
tail -f .ralph/ralph.log
```

After testing the PR build in the emulator, merge and redeploy the final `main` build:

```bash
./scripts/accept-pr.sh 12            # merge and refresh emulator
./scripts/accept-pr.sh 12 --phone    # merge and install signed release on connected Pixel
```

For a change already present in the working tree, bypass issues and deploy immediately:

```bash
./scripts/dev-deploy.sh --emulator
./scripts/dev-deploy.sh --phone --release
```

## Emulator lifecycle

```bash
./scripts/dev-emulator.sh status
./scripts/dev-emulator.sh stop
./scripts/dev-emulator.sh start
```

The one-time setup is reproducible with `./scripts/setup-windows-emulator.sh`. It installs a local Windows JDK, Android command-line tools, emulator, and Pixel 8 Pro Android 16 image under the Windows user profile. The several-gigabyte SDK/AVD remains outside Git.

## Boundaries

- RALPH still requires human PR review and never merges automatically.
- The emulator gets debug builds; the physical phone gets the consistently signed release build.
- Emulator data and phone data are separate.
- Rebooting Windows stops the emulator and WSL RALPH process; rerun `dev-emulator.sh start` and `ralph-start.sh`.
