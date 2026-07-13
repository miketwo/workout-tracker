package com.miketwo.workouttracker;

import java.util.ArrayList;
import java.util.List;

final class Models {
    private Models() {}

    static final class Plan {
        long id;
        String name;
        int weekday;
        String type;
        String notes;
        final List<Exercise> exercises = new ArrayList<>();
    }

    static final class Exercise {
        long id;
        long planId;
        String name;
        int position;
        int sets;
        int reps;
        double weight;
        String unit;
        int durationSeconds;
        double distance;
        String notes;
        int restSeconds;
        boolean bodyweight;
        boolean assistance;
        String loadMode = "standard";
        String muscleGroup;
    }

    static final class Profile {
        double bodyWeight;
        String unit = "lb";
    }

    static final class PackingItem {
        long id; String workoutType; String name; int position;
    }

    static final class LastSet {
        int reps;
        double weight;
        int rir;
    }
}
