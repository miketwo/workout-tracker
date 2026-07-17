package com.miketwo.workouttracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Versioned, deliberately small interchange format for AI-generated strength plans. */
final class PlanImport {
    static final int MAX_JSON_CHARS = 256 * 1024;
    private static final Set<String> UNITS = new HashSet<>(Arrays.asList("lb", "kg", "bodyweight", "assisted lb", "assisted kg"));
    private static final Set<String> MUSCLE_GROUPS = new HashSet<>(Arrays.asList("Upper body", "Lower body", "Core", "Full body", "Cardio", "Other"));
    private static final String CANONICAL_EXERCISES = "Lat pulldown, Assisted pull-up, Chest press, Incline chest press, Shoulder press, Seated row, Pec fly, Reverse fly, Biceps curl, Triceps press, Leg press, Leg extension, Seated leg curl, Hip abduction, Hip adduction, Calf raise, Back extension, Abdominal crunch, Dumbbell bench press, Goblet squat, Romanian deadlift, Plank, Push-up, Banded lateral walk";

    private PlanImport() {}

    static final class PlanDraft {
        final String name;
        final String type;
        final String notes;
        final List<ExerciseDraft> exercises;

        PlanDraft(String name, String type, String notes, List<ExerciseDraft> exercises) {
            this.name = name;
            this.type = type;
            this.notes = notes;
            this.exercises = exercises;
        }
    }

    static final class ExerciseDraft {
        final String name;
        final String muscleGroup;
        final int sets;
        final int reps;
        final double weight;
        final String unit;
        final int restSeconds;
        final int durationSeconds;
        final String notes;

        ExerciseDraft(String name, String muscleGroup, int sets, int reps, double weight, String unit, int restSeconds, int durationSeconds, String notes) {
            this.name = name;
            this.muscleGroup = muscleGroup;
            this.sets = sets;
            this.reps = reps;
            this.weight = weight;
            this.unit = unit;
            this.restSeconds = restSeconds;
            this.durationSeconds = durationSeconds;
            this.notes = notes;
        }
    }

    static final class FormatException extends Exception {
        FormatException(String message) { super(message); }
    }

    static PlanDraft parse(String input) throws FormatException {
        String json = unwrapCodeFence(input == null ? "" : input.trim());
        if (json.isEmpty()) throw new FormatException("Paste a workout plan or choose a JSON file.");
        if (json.length() > MAX_JSON_CHARS) throw new FormatException("The plan is too large. The maximum size is 256 KB.");

        try {
            JSONObject root = new JSONObject(json);
            if (!"workout-tracker-plan".equals(root.optString("format"))) {
                throw new FormatException("format must be \"workout-tracker-plan\".");
            }
            if (root.optInt("version", -1) != 1) throw new FormatException("Only plan format version 1 is supported.");
            JSONObject plan = root.optJSONObject("plan");
            if (plan == null) throw new FormatException("plan must be a JSON object.");

            String name = requiredText(plan, "name", "plan.name", 100);
            String type = requiredText(plan, "type", "plan.type", 30);
            if (!"Strength".equals(type)) throw new FormatException("plan.type must be \"Strength\" in format version 1.");
            String notes = optionalText(plan, "notes", "plan.notes", 2000);
            JSONArray items = plan.optJSONArray("exercises");
            if (items == null) throw new FormatException("plan.exercises must be a JSON array.");
            if (items.length() < 1) throw new FormatException("The plan must contain at least one exercise.");
            if (items.length() > 50) throw new FormatException("A plan can contain at most 50 exercises.");

            List<ExerciseDraft> exercises = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject exercise = items.optJSONObject(i);
                String path = "plan.exercises[" + i + "]";
                if (exercise == null) throw new FormatException(path + " must be a JSON object.");
                String exerciseName = requiredText(exercise, "name", path + ".name", 100);
                String muscleGroup = optionalText(exercise, "muscle_group", path + ".muscle_group", 30);
                if (muscleGroup.isEmpty()) muscleGroup = "Other";
                if (!MUSCLE_GROUPS.contains(muscleGroup)) throw new FormatException(path + ".muscle_group has an unsupported value.");
                int sets = requiredInt(exercise, "sets", path + ".sets", 1, 20);
                int reps = requiredInt(exercise, "reps", path + ".reps", 0, 1000);
                double weight = requiredDouble(exercise, "weight", path + ".weight", 0, 5000);
                String unit = requiredText(exercise, "unit", path + ".unit", 20);
                if (!UNITS.contains(unit)) throw new FormatException(path + ".unit must be lb, kg, bodyweight, assisted lb, or assisted kg.");
                int rest = optionalInt(exercise, "rest_seconds", path + ".rest_seconds", 90, 0, 3600);
                int duration = optionalInt(exercise, "duration_seconds", path + ".duration_seconds", 0, 0, 3600);
                String exerciseNotes = optionalText(exercise, "notes", path + ".notes", 1000);
                if (duration == 0 && reps == 0) throw new FormatException(path + ".reps must be at least 1 for an ordinary set.");
                if (duration > 0 && reps != 0) throw new FormatException(path + " must use reps 0 when duration_seconds is set.");
                if ("bodyweight".equals(unit) && weight != 0) throw new FormatException(path + ".weight must be 0 for a bodyweight exercise.");
                exercises.add(new ExerciseDraft(exerciseName, muscleGroup, sets, reps, weight, unit, rest, duration, exerciseNotes));
            }
            return new PlanDraft(name, type, notes, exercises);
        } catch (JSONException e) {
            throw new FormatException("This is not valid JSON: " + e.getMessage());
        }
    }

    static String aiInstructions() {
        return "Create a strength workout plan for the Workout Tracker Android app. Return only one JSON object, with no explanation. You may wrap it in a single ```json code fence.\n\n"
                + "Use this exact format:\n"
                + "{\n"
                + "  \"format\": \"workout-tracker-plan\",\n"
                + "  \"version\": 1,\n"
                + "  \"plan\": {\n"
                + "    \"name\": \"Full Body\",\n"
                + "    \"type\": \"Strength\",\n"
                + "    \"notes\": \"Optional plan notes\",\n"
                + "    \"exercises\": [\n"
                + "      {\n"
                + "        \"name\": \"Goblet squat\",\n"
                + "        \"muscle_group\": \"Lower body\",\n"
                + "        \"sets\": 3,\n"
                + "        \"reps\": 8,\n"
                + "        \"weight\": 40,\n"
                + "        \"unit\": \"lb\",\n"
                + "        \"rest_seconds\": 90,\n"
                + "        \"duration_seconds\": 0,\n"
                + "        \"notes\": \"Optional exercise notes\"\n"
                + "      }\n"
                + "    ]\n"
                + "  }\n"
                + "}\n\n"
                + "Rules:\n"
                + "- type must be Strength. Include 1 to 50 exercises.\n"
                + "- muscle_group must be Upper body, Lower body, Core, Full body, Cardio, or Other.\n"
                + "- unit must be lb, kg, bodyweight, assisted lb, or assisted kg. Use weight 0 for bodyweight.\n"
                + "- sets must be 1 to 20. For ordinary sets, reps must be 1 to 1000 and duration_seconds must be 0. For timed sets, reps must be 0 and duration_seconds must be 1 to 3600.\n"
                + "- rest_seconds must be 0 to 3600. Do not include database IDs, weekdays, markdown prose, comments, or trailing commas.\n"
                + "- Reuse these exact exercise names when appropriate so workout history stays connected: " + CANONICAL_EXERCISES + ". Custom exercise names are allowed.\n\n"
                + "---\n"
                + "THE USER'S WORKOUT REQUEST FOLLOWS:\n\n";
    }

    private static String unwrapCodeFence(String value) throws FormatException {
        if (!value.startsWith("```")) return value;
        int firstLine = value.indexOf('\n');
        int closing = value.lastIndexOf("```");
        if (firstLine < 0 || closing <= firstLine || !value.substring(closing + 3).trim().isEmpty()) {
            throw new FormatException("The JSON code fence is incomplete.");
        }
        return value.substring(firstLine + 1, closing).trim();
    }

    private static String requiredText(JSONObject object, String key, String path, int max) throws FormatException, JSONException {
        if (!object.has(key) || object.isNull(key) || !(object.get(key) instanceof String)) throw new FormatException(path + " must be text.");
        String value = object.getString(key).trim();
        if (value.isEmpty()) throw new FormatException(path + " cannot be empty.");
        if (value.length() > max) throw new FormatException(path + " is too long (maximum " + max + " characters).");
        return value;
    }

    private static String optionalText(JSONObject object, String key, String path, int max) throws FormatException, JSONException {
        if (!object.has(key) || object.isNull(key)) return "";
        if (!(object.get(key) instanceof String)) throw new FormatException(path + " must be text.");
        String value = object.getString(key).trim();
        if (value.length() > max) throw new FormatException(path + " is too long (maximum " + max + " characters).");
        return value;
    }

    private static int requiredInt(JSONObject object, String key, String path, int min, int max) throws FormatException, JSONException {
        if (!object.has(key) || !(object.get(key) instanceof Number)) throw new FormatException(path + " must be a whole number.");
        double raw = object.getDouble(key);
        if (!Double.isFinite(raw) || raw != Math.rint(raw) || raw < min || raw > max) throw new FormatException(path + " must be a whole number from " + min + " to " + max + ".");
        return (int) raw;
    }

    private static int optionalInt(JSONObject object, String key, String path, int fallback, int min, int max) throws FormatException, JSONException {
        if (!object.has(key) || object.isNull(key)) return fallback;
        return requiredInt(object, key, path, min, max);
    }

    private static double requiredDouble(JSONObject object, String key, String path, double min, double max) throws FormatException, JSONException {
        if (!object.has(key) || !(object.get(key) instanceof Number)) throw new FormatException(path + " must be a number.");
        double value = object.getDouble(key);
        if (!Double.isFinite(value) || value < min || value > max) throw new FormatException(path + " must be from " + min + " to " + max + ".");
        return value;
    }
}
