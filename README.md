# Workout Tracker

A private, phone-first Android workout planner and log. Its guiding principle is simple: make decisions while planning, then make the workout itself feel like following clear instructions.

## Current Android MVP

- Suggests the plan assigned to today while keeping alternate plans one tap away.
- Creates and edits weekday-based strength, run, swim, and mixed plans.
- Guides strength work one set at a time with a persistent overall progress meter.
- Records actual reps, decimal weight, pounds/kilograms, assisted/bodyweight work, and optional reps in reserve.
- Shows the matching set from the previous workout.
- Supports skipping and undoing sets, skipping an exercise, adding an extra set, and choosing another exercise when equipment is occupied.
- Keeps completed workout history frozen when a future plan changes.
- Provides full-screen rest timers and pauseable timed exercises such as planks.
- Runs jog/walk intervals in the background with locked-screen notifications, audio warnings, transition sounds, haptics, and pause/end controls.
- Logs run and pool-swim totals manually, including intervals, pool lengths, and imperial or metric units.
- Shows basic strength/cardio history and total lifting volume, and celebrates heaviest-weight and set-volume records.
- Exports a complete JSON backup or readable CSV history through Android's file picker.
- Imports the existing July 9 strength and treadmill entries into a fresh install.

Cloud synchronization, Google Calendar reading, screenshot/LLM import, advanced charts, circuits/supersets, and automatic progression proposals remain future work.

## Build

The repository includes a Gradle wrapper and a bootstrap script that downloads a project-local JDK and Android SDK on Linux/WSL:

```bash
./scripts/bootstrap-android.sh
./scripts/build-apk.sh
```

The signed result is `dist/workout-tracker.apk`. Preserve `.keys/workout-tracker.jks`: every future update must use this same private signing key. Back it up somewhere secure together with the app's JSON export.

## Install on a Pixel by USB

1. On the phone, open **Settings → About phone** and tap **Build number** seven times.
2. Open **Settings → System → Developer options** and enable **USB debugging**.
3. Connect the phone with a data-capable USB cable and choose **File transfer** if Android asks.
4. Approve the phone's **Allow USB debugging** prompt.
5. Run:

```bash
./scripts/install-usb.sh
```

The `-r` install mode preserves data when updating an already installed copy signed with this repository's key. USB installation does not require enabling “Install unknown apps.” If WSL cannot see the USB device, copy `dist/workout-tracker.apk` to Windows and use the Windows Android `adb install -r` command, or copy the APK to the phone and open it from Files.

## Data safety

Android normally deletes an app's private local database when the app is uninstalled, and an app cannot reliably intercept its own uninstall. Before uninstalling, open **History & progress → Back up or export data → Export full JSON backup**. Android automatic backup and device transfer are enabled, but the explicit JSON file is the safest portable copy.

## Original command-line log

The original `tracker.py`, `workouts.csv`, and `cardio.csv` are archived locally under the ignored `.legacy-original/` directory. Their July 9 entries are seeded into a fresh Android database, but the CSV structure does not constrain the Android data model.

## Verification

`scripts/build-apk.sh` runs JVM unit tests, Android lint, and a signed release build. Useful individual commands are:

```bash
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew assembleRelease
```

## Automated feedback loop

The guarded RALPH loop can turn explicitly approved GitHub or GitLab issues into tested GitHub pull requests without merging them automatically. See [docs/RALPH.md](docs/RALPH.md), then run:

```bash
./scripts/ralph-start.sh
tail -f .ralph/ralph.log
```

For the complete WSL-build → Windows-emulator → reviewed PR → physical-Pixel workflow, see [docs/DEV_CYCLE.md](docs/DEV_CYCLE.md). The common commands are:

```bash
./scripts/dev-emulator.sh start
./scripts/feedback.sh "Describe the improvement"
./scripts/dev-deploy.sh --emulator
./scripts/dev-deploy.sh --phone --release
```
