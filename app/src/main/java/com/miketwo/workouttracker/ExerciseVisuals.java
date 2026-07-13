package com.miketwo.workouttracker;

import java.util.Locale;

/** Maps the built-in exercise catalogue to its compact motion illustration. */
final class ExerciseVisuals {
    private ExerciseVisuals() {}

    static int drawableFor(String exerciseName) {
        String name = exerciseName == null ? "" : exerciseName.toLowerCase(Locale.ROOT);
        if (name.contains("lat pulldown") || name.contains("pull down")) return R.drawable.ex_lat_pulldown;
        if (name.contains("pull-up") || name.contains("pull up")) return R.drawable.ex_pull_up;
        if (name.contains("incline chest press") || name.equals("chest press")) return R.drawable.ex_chest_press;
        if (name.contains("shoulder press")) return R.drawable.ex_shoulder_press;
        if (name.contains("seated row") || name.contains("cable row")) return R.drawable.ex_seated_row;
        if (name.contains("pec fly")) return R.drawable.ex_pec_fly;
        if (name.contains("reverse fly")) return R.drawable.ex_reverse_fly;
        if (name.contains("biceps curl") || name.contains("bicep curl")) return R.drawable.ex_biceps_curl;
        if (name.contains("triceps press") || name.contains("tricep press")) return R.drawable.ex_triceps_press;
        if (name.contains("leg press")) return R.drawable.ex_leg_press;
        if (name.contains("leg extension")) return R.drawable.ex_leg_extension;
        if (name.contains("leg curl")) return R.drawable.ex_leg_curl;
        if (name.contains("hip abduction") || name.contains("hip adduction")) return R.drawable.ex_hip_machine;
        if (name.contains("calf raise")) return R.drawable.ex_calf_raise;
        if (name.contains("back extension")) return R.drawable.ex_back_extension;
        if (name.contains("abdominal crunch") || name.equals("crunch")) return R.drawable.ex_crunch;
        if (name.contains("dumbbell bench press")) return R.drawable.ex_dumbbell_bench;
        if (name.contains("goblet squat")) return R.drawable.ex_goblet_squat;
        if (name.contains("romanian deadlift") || name.contains("romanian dead lift")) return R.drawable.ex_romanian_deadlift;
        if (name.contains("plank")) return R.drawable.ex_plank;
        if (name.contains("push-up") || name.contains("push up")) return R.drawable.ex_push_up;
        if (name.contains("banded lateral walk") || name.contains("lateral walk")) return R.drawable.ex_lateral_walk;
        return 0;
    }
}
