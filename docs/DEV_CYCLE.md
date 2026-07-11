# Spoken-change to Pixel development cycle

The primary loop is direct: describe a change in the Codex app, let Codex implement and verify it, then install the consistently signed release directly from WSL to the Pixel over Android Wireless Debugging. No GitHub issue, background agent, APK copy, USB cable, or PowerShell step is involved.

## One-time wireless pairing

The computer and Pixel must be on the same Wi-Fi network.

1. On the Pixel, open **Settings → System → Developer options → Wireless debugging** and enable it.
2. Tap **Pair device with pairing code**. Keep that dialog open.
3. In WSL, run `./scripts/wifi-pair.sh`.
4. Enter the dialog's IP address and pairing port, followed by its six-digit code.
5. When prompted again, enter the separate **IP address & port** shown on the main Wireless debugging screen.

Pairing is normally remembered. The deploy script uses Android's local-network discovery to follow port changes automatically. If discovery is unavailable after a reboot or network change, open the main Wireless debugging screen and run:

```bash
./scripts/wifi-connect.sh CURRENT_IP:CURRENT_PORT
```

## Daily loop

Tell Codex the change you want. Repository instructions tell it to implement the change, run tests and lint, build the signed release, install it over Wi-Fi, and launch Workout Tracker automatically.

The underlying command is simply:

```bash
./scripts/dev-deploy.sh
```

It builds and verifies the release, reads the APK directly from WSL through Windows ADB, updates the existing app without clearing data, and opens it on the phone.

## Optional emulator and USB targets

The Android 16 Pixel 8 Pro emulator remains available:

```bash
./scripts/dev-emulator.sh start
./scripts/dev-deploy.sh --emulator
./scripts/dev-emulator.sh stop
```

USB remains a fallback:

```bash
./scripts/install-usb.sh
```

The emulator receives fast debug builds. The real Pixel receives releases signed with the repository's dedicated local key, preserving compatibility with the currently installed app and its data.
