# Workout Tracker

A small local workout log for strength and cardio sessions.

## Files

- `workouts.csv` stores strength training history.
- `cardio.csv` stores cardio history.
- `tracker.py` adds, lists, and summarizes entries.

## Current Log

The first entry is already recorded:

- 2026-07-09: Lat pulldown, about 100 lb, 3 sets of 8 reps
- 2026-07-09: Treadmill run, about 2 miles in 26 minutes, roughly 6 min jog / 2 min walk intervals

## Usage

List recent strength entries:

```bash
python3 tracker.py list
```

Add an entry:

```bash
python3 tracker.py add --exercise "Lat pulldown" --sets 3 --reps 8 --weight 100 --notes "Felt solid"
```

Show summary totals by exercise:

```bash
python3 tracker.py summary
```

Add a cardio entry:

```bash
python3 tracker.py cardio-add --activity "Treadmill run" --distance 2 --duration 26 --intervals "6 min jog / 2 min walk"
```

List recent cardio entries:

```bash
python3 tracker.py cardio-list
```

Show cardio summary totals:

```bash
python3 tracker.py cardio-summary
```

The default date for new entries is today's local date. Use `--date YYYY-MM-DD` to log a workout for another day.
