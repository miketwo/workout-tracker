# Workout plan import format

Workout Tracker accepts version 1 strength plans as JSON. Importing a plan creates a new plan; it never overwrites an existing plan or workout history.

The quickest workflow is to open **Plans → Import a plan**, tap **Copy AI instructions**, give those instructions and your workout request to an LLM, then paste its JSON response back into the app. A saved `.json` file can also be selected with Android's file picker.

## Example

```json
{
  "format": "workout-tracker-plan",
  "version": 1,
  "plan": {
    "name": "Full Body",
    "type": "Strength",
    "notes": "Move deliberately and leave one or two reps in reserve.",
    "exercises": [
      {
        "name": "Goblet squat",
        "muscle_group": "Lower body",
        "sets": 3,
        "reps": 8,
        "weight": 40,
        "unit": "lb",
        "rest_seconds": 90,
        "duration_seconds": 0,
        "notes": "Keep the torso tall."
      },
      {
        "name": "Plank",
        "muscle_group": "Core",
        "sets": 3,
        "reps": 0,
        "weight": 0,
        "unit": "bodyweight",
        "rest_seconds": 60,
        "duration_seconds": 30,
        "notes": ""
      }
    ]
  }
}
```

## Contract

- `format` must be `workout-tracker-plan` and `version` must be `1`.
- `plan.name` is required and can contain at most 100 characters.
- Version 1 supports only `plan.type: "Strength"`.
- `plan.notes` is optional and can contain at most 2,000 characters.
- `plan.exercises` must contain 1–50 exercise objects. Array order is workout order.
- Exercise `name`, `sets`, `reps`, `weight`, and `unit` are required.
- `muscle_group` is optional and defaults to `Other`. Allowed values are `Upper body`, `Lower body`, `Core`, `Full body`, `Cardio`, and `Other`.
- `unit` must be `lb`, `kg`, `bodyweight`, `assisted lb`, or `assisted kg`. Bodyweight exercises must use weight `0`.
- `sets` must be 1–20. Ordinary sets use reps 1–1,000 and `duration_seconds: 0`. Timed sets use `reps: 0` and `duration_seconds` from 1–3,600.
- `rest_seconds` is optional, defaults to 90, and must be 0–3,600.
- Exercise `notes` is optional and can contain at most 1,000 characters.
- Weekdays and database IDs are intentionally absent. Plans are selected on demand rather than assigned to a day.

The importer accepts plain JSON or JSON wrapped in one complete Markdown code fence. It validates and previews the whole plan before saving it in a single database transaction.
