#!/usr/bin/env python3
import argparse
import csv
from collections import defaultdict
from datetime import date
from pathlib import Path


STRENGTH_FILE = Path(__file__).with_name("workouts.csv")
STRENGTH_FIELDS = ["date", "exercise", "sets", "reps", "weight_lbs", "notes"]
CARDIO_FILE = Path(__file__).with_name("cardio.csv")
CARDIO_FIELDS = [
    "date",
    "activity",
    "distance_mi",
    "duration_min",
    "pace_min_per_mi",
    "intervals",
    "notes",
]


def ensure_csv(path, fieldnames):
    if path.exists():
        return
    with path.open("w", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()


def read_csv(path, fieldnames):
    ensure_csv(path, fieldnames)
    with path.open(newline="") as file:
        return list(csv.DictReader(file))


def add_entry(args):
    ensure_csv(STRENGTH_FILE, STRENGTH_FIELDS)
    row = {
        "date": args.date,
        "exercise": args.exercise,
        "sets": str(args.sets),
        "reps": str(args.reps),
        "weight_lbs": str(args.weight),
        "notes": args.notes,
    }
    with STRENGTH_FILE.open("a", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=STRENGTH_FIELDS)
        writer.writerow(row)
    print(f"Added {args.exercise}: {args.sets}x{args.reps} @ {args.weight} lb on {args.date}")


def list_entries(args):
    entries = read_csv(STRENGTH_FILE, STRENGTH_FIELDS)
    if args.exercise:
        needle = args.exercise.casefold()
        entries = [entry for entry in entries if needle in entry["exercise"].casefold()]
    entries = entries[-args.limit :]

    if not entries:
        print("No workout entries found.")
        return

    for entry in entries:
        notes = f" - {entry['notes']}" if entry.get("notes") else ""
        print(
            f"{entry['date']}: {entry['exercise']} "
            f"{entry['sets']}x{entry['reps']} @ {entry['weight_lbs']} lb{notes}"
        )


def summary(_args):
    totals = defaultdict(lambda: {"sessions": 0, "sets": 0, "reps": 0, "volume": 0.0})
    for entry in read_csv(STRENGTH_FILE, STRENGTH_FIELDS):
        exercise = entry["exercise"]
        sets = int(entry["sets"])
        reps = int(entry["reps"])
        weight = float(entry["weight_lbs"])
        totals[exercise]["sessions"] += 1
        totals[exercise]["sets"] += sets
        totals[exercise]["reps"] += sets * reps
        totals[exercise]["volume"] += sets * reps * weight

    if not totals:
        print("No workout entries found.")
        return

    for exercise, data in sorted(totals.items()):
        volume = int(data["volume"]) if data["volume"].is_integer() else data["volume"]
        print(
            f"{exercise}: {data['sessions']} session(s), "
            f"{data['sets']} sets, {data['reps']} reps, {volume} lb total volume"
        )


def format_pace(minutes):
    whole_minutes = int(minutes)
    seconds = round((minutes - whole_minutes) * 60)
    if seconds == 60:
        whole_minutes += 1
        seconds = 0
    return f"{whole_minutes}:{seconds:02d}"


def add_cardio(args):
    ensure_csv(CARDIO_FILE, CARDIO_FIELDS)
    pace = args.duration / args.distance
    row = {
        "date": args.date,
        "activity": args.activity,
        "distance_mi": str(args.distance),
        "duration_min": str(args.duration),
        "pace_min_per_mi": format_pace(pace),
        "intervals": args.intervals,
        "notes": args.notes,
    }
    with CARDIO_FILE.open("a", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CARDIO_FIELDS)
        writer.writerow(row)
    print(
        f"Added {args.activity}: {args.distance} mi in {args.duration} min "
        f"({format_pace(pace)}/mi) on {args.date}"
    )


def list_cardio(args):
    entries = read_csv(CARDIO_FILE, CARDIO_FIELDS)
    if args.activity:
        needle = args.activity.casefold()
        entries = [entry for entry in entries if needle in entry["activity"].casefold()]
    entries = entries[-args.limit :]

    if not entries:
        print("No cardio entries found.")
        return

    for entry in entries:
        intervals = f" - {entry['intervals']}" if entry.get("intervals") else ""
        notes = f" - {entry['notes']}" if entry.get("notes") else ""
        print(
            f"{entry['date']}: {entry['activity']} {entry['distance_mi']} mi "
            f"in {entry['duration_min']} min ({entry['pace_min_per_mi']}/mi)"
            f"{intervals}{notes}"
        )


def cardio_summary(_args):
    totals = defaultdict(lambda: {"sessions": 0, "distance": 0.0, "duration": 0.0})
    for entry in read_csv(CARDIO_FILE, CARDIO_FIELDS):
        activity = entry["activity"]
        totals[activity]["sessions"] += 1
        totals[activity]["distance"] += float(entry["distance_mi"])
        totals[activity]["duration"] += float(entry["duration_min"])

    if not totals:
        print("No cardio entries found.")
        return

    for activity, data in sorted(totals.items()):
        pace = format_pace(data["duration"] / data["distance"])
        print(
            f"{activity}: {data['sessions']} session(s), "
            f"{data['distance']:g} mi, {data['duration']:g} min, {pace}/mi average pace"
        )


def build_parser():
    parser = argparse.ArgumentParser(description="Track strength and cardio workouts.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    add = subparsers.add_parser("add", help="Add a workout entry.")
    add.add_argument("--date", default=date.today().isoformat())
    add.add_argument("--exercise", required=True)
    add.add_argument("--sets", type=int, required=True)
    add.add_argument("--reps", type=int, required=True)
    add.add_argument("--weight", type=float, required=True)
    add.add_argument("--notes", default="")
    add.set_defaults(func=add_entry)

    list_parser = subparsers.add_parser("list", help="List recent workout entries.")
    list_parser.add_argument("--exercise", help="Filter by exercise name.")
    list_parser.add_argument("--limit", type=int, default=20)
    list_parser.set_defaults(func=list_entries)

    summary_parser = subparsers.add_parser("summary", help="Summarize totals by exercise.")
    summary_parser.set_defaults(func=summary)

    cardio_add = subparsers.add_parser("cardio-add", help="Add a cardio entry.")
    cardio_add.add_argument("--date", default=date.today().isoformat())
    cardio_add.add_argument("--activity", required=True)
    cardio_add.add_argument("--distance", type=float, required=True, help="Distance in miles.")
    cardio_add.add_argument("--duration", type=float, required=True, help="Duration in minutes.")
    cardio_add.add_argument("--intervals", default="")
    cardio_add.add_argument("--notes", default="")
    cardio_add.set_defaults(func=add_cardio)

    cardio_list = subparsers.add_parser("cardio-list", help="List recent cardio entries.")
    cardio_list.add_argument("--activity", help="Filter by activity name.")
    cardio_list.add_argument("--limit", type=int, default=20)
    cardio_list.set_defaults(func=list_cardio)

    cardio_summary_parser = subparsers.add_parser(
        "cardio-summary", help="Summarize cardio totals by activity."
    )
    cardio_summary_parser.set_defaults(func=cardio_summary)

    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
