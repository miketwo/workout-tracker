package com.miketwo.workouttracker;

import java.util.Locale;

public final class WorkoutMath {
    private WorkoutMath() {}

    public static double volume(int reps, double weight) {
        return reps * weight;
    }

    public static String formatWeight(double value) {
        if (value == Math.rint(value)) return String.format(Locale.US, "%.0f", value);
        return String.format(Locale.US, "%.1f", value);
    }

    public static String pace(double minutes, double distance) {
        if (minutes <= 0 || distance <= 0) return "—";
        double raw = minutes / distance;
        int whole = (int) raw;
        int seconds = (int) Math.round((raw - whole) * 60);
        if (seconds == 60) { whole++; seconds = 0; }
        return String.format(Locale.US, "%d:%02d", whole, seconds);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
