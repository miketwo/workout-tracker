package com.miketwo.workouttracker;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/** Commits an already validated imported plan as one all-or-nothing operation. */
final class PlanImporter {
    private PlanImporter() {}

    static long save(Db database, PlanImport.PlanDraft plan) {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues planValues = new ContentValues();
            planValues.put("name", plan.name);
            planValues.put("type", plan.type);
            planValues.put("notes", plan.notes);
            long planId = db.insertOrThrow("plans", null, planValues);

            for (int position = 0; position < plan.exercises.size(); position++) {
                PlanImport.ExerciseDraft exercise = plan.exercises.get(position);
                boolean bodyweight = "bodyweight".equals(exercise.unit);
                boolean assistance = exercise.unit.startsWith("assisted");
                ContentValues values = new ContentValues();
                values.put("plan_id", planId);
                values.put("name", exercise.name);
                values.put("position", position);
                values.put("sets", exercise.sets);
                values.put("reps", exercise.reps);
                values.put("weight", exercise.weight);
                values.put("unit", assistance ? (exercise.unit.endsWith("kg") ? "kg" : "lb") : exercise.unit);
                values.put("duration_seconds", exercise.durationSeconds);
                values.put("notes", exercise.notes);
                values.put("rest_seconds", exercise.restSeconds);
                values.put("bodyweight", bodyweight ? 1 : 0);
                values.put("assistance", assistance ? 1 : 0);
                values.put("load_mode", assistance ? WorkoutMath.LOAD_COUNTERBALANCED : bodyweight ? WorkoutMath.LOAD_BODYWEIGHT : WorkoutMath.LOAD_STANDARD);
                values.put("muscle_group", exercise.muscleGroup);
                db.insertOrThrow("exercises", null, values);
            }

            db.setTransactionSuccessful();
            return planId;
        } finally {
            db.endTransaction();
        }
    }
}
