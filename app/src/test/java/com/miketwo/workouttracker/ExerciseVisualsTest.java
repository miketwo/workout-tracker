package com.miketwo.workouttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ExerciseVisualsTest {
    @Test public void builtInExercisesHaveSpecificVisuals() {
        String[] names = {"Lat pulldown", "Assisted pull-up", "Chest press", "Incline chest press", "Shoulder press", "Seated row", "Pec fly", "Reverse fly", "Biceps curl", "Triceps press", "Leg press", "Leg extension", "Seated leg curl", "Hip abduction", "Hip adduction", "Calf raise", "Back extension", "Abdominal crunch", "Dumbbell bench press", "Goblet squat", "Romanian deadlift", "Plank", "Push-up", "Banded lateral walk"};
        for (String name : names) assertNotEquals(name, 0, ExerciseVisuals.drawableFor(name));
    }

    @Test public void customExercisesDoNotReceiveAnUnrelatedIllustration() {
        assertEquals(0, ExerciseVisuals.drawableFor("Custom kettlebell exercise"));
    }
}
