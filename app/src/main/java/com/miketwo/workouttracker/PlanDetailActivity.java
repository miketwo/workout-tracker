package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import java.time.DayOfWeek;
import java.util.Locale;

/** Read-only plan details, usable before either editing or starting a workout. */
public class PlanDetailActivity extends Activity {
    private Db db;
    private Models.Plan plan;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = Db.get(this);
        plan = db.plan(getIntent().getLongExtra("plan_id", 0));
        if (plan == null) { finish(); return; }
        render();
    }

    private void render() {
        LinearLayout body = Ui.column(this); Ui.page(this, body);
        Button back = Ui.smallButton(this, "‹ Back"); back.setOnClickListener(v -> finish()); body.addView(back);
        body.addView(Ui.title(this, plan.name));
        String day = DayOfWeek.of(plan.weekday).toString().toLowerCase(Locale.getDefault());
        day = Character.toUpperCase(day.charAt(0)) + day.substring(1);
        body.addView(Ui.text(this, day + "  •  " + plan.type, 17, Ui.MUTED));
        if (!plan.notes.isBlank()) body.addView(Ui.text(this, plan.notes, 16, Ui.MUTED));

        body.addView(Ui.heading(this, "Workout plan"));
        if (plan.exercises.isEmpty()) body.addView(Ui.text(this, "No exercises have been added yet.", 16, Ui.MUTED));
        int index = 1;
        for (Models.Exercise exercise : plan.exercises) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.heading(this, index++ + ". " + exercise.name));
            String target = exercise.durationSeconds > 0
                    ? exercise.sets + " sets × " + exercise.durationSeconds + " seconds"
                    : exercise.sets + " sets × " + exercise.reps + " reps @ " + targetWeight(exercise);
            card.addView(Ui.text(this, target + (exercise.muscleGroup.isBlank() ? "" : "  •  " + exercise.muscleGroup), 16, Ui.MUTED));
            if (exercise.restSeconds > 0) card.addView(Ui.text(this, "Rest " + exercise.restSeconds + " seconds between sets", 14, Ui.MUTED));
            if (!exercise.notes.isBlank()) card.addView(Ui.text(this, exercise.notes, 14, Ui.MUTED));
            body.addView(card);
        }

        Button packing = Ui.button(this, "Packing checklist", false);
        packing.setOnClickListener(v -> startActivity(new Intent(this, PackingChecklistActivity.class).putExtra("workout_type", plan.type))); body.addView(packing);
        Button start = Ui.button(this, plan.type.equals("Strength") ? "Start this workout" : "Log this " + plan.type.toLowerCase(Locale.getDefault()), true);
        start.setOnClickListener(v -> {
            if (plan.type.equals("Strength")) startActivity(new Intent(this, WorkoutActivity.class).putExtra("plan_id", plan.id));
            else startActivity(new Intent(this, CardioActivity.class).putExtra("activity", plan.type));
        });
        body.addView(start);
        Button edit = Ui.smallButton(this, "Edit plan"); edit.setOnClickListener(v -> startActivity(new Intent(this, PlanEditorActivity.class).putExtra("plan_id", plan.id))); body.addView(edit);
    }

    private String targetWeight(Models.Exercise exercise) {
        return exercise.bodyweight ? "bodyweight" : WorkoutMath.formatWeight(exercise.weight) + " " + exercise.unit;
    }
}
