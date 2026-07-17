package com.miketwo.workouttracker;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PlanImportTest {
    private Application app;

    @Before public void freshDatabase() {
        app = ApplicationProvider.getApplicationContext();
        Db.resetForTests();
        app.deleteDatabase(Db.NAME);
    }

    @Test public void parsesOrdinaryAndTimedExercisesFromFencedJson() throws Exception {
        PlanImport.PlanDraft plan = PlanImport.parse("```json\n" + validJson() + "\n```");

        assertEquals("Full Body", plan.name);
        assertEquals("Strength", plan.type);
        assertEquals(2, plan.exercises.size());
        assertEquals("Goblet squat", plan.exercises.get(0).name);
        assertEquals(8, plan.exercises.get(0).reps);
        assertEquals(30, plan.exercises.get(1).durationSeconds);
        assertEquals(0, plan.exercises.get(1).reps);
    }

    @Test public void rejectsUnsupportedVersionAndContradictoryTimedTarget() throws Exception {
        expectError(validJson().replace("\"version\":1", "\"version\":2"), "version 1");
        expectError(validJson().replace("\"reps\":0", "\"reps\":8"), "reps 0");
    }

    @Test public void importedPlanPreservesExerciseOrderAndTargets() throws Exception {
        Db db = Db.get(app);
        int before = db.plans().size();
        long id = PlanImporter.save(db, PlanImport.parse(validJson()));

        assertEquals(before + 1, db.plans().size());
        Models.Plan saved = db.plan(id);
        assertEquals("Full Body", saved.name);
        assertEquals(2, saved.exercises.size());
        assertEquals("Goblet squat", saved.exercises.get(0).name);
        assertEquals(40, saved.exercises.get(0).weight, .001);
        assertEquals("Plank", saved.exercises.get(1).name);
        assertEquals(30, saved.exercises.get(1).durationSeconds);
    }

    @Test public void assistedKilogramImportUsesKilogramsForLoadMath() throws Exception {
        Db db = Db.get(app);
        String json = validJson().replace("\"weight\":40,\"unit\":\"lb\"", "\"weight\":25,\"unit\":\"assisted kg\"");
        long id = PlanImporter.save(db, PlanImport.parse(json));

        Models.Exercise assisted = db.plan(id).exercises.get(0);
        assertTrue(assisted.assistance);
        assertEquals("kg", assisted.unit);
        assertEquals(WorkoutMath.LOAD_COUNTERBALANCED, assisted.loadMode);
    }

    @Test public void databaseFailureRollsBackPlanAndEarlierExercises() {
        Db db = Db.get(app);
        int before = db.plans().size();
        PlanImport.ExerciseDraft valid = new PlanImport.ExerciseDraft("Goblet squat", "Lower body", 3, 8, 40, "lb", 90, 0, "");
        PlanImport.ExerciseDraft invalid = new PlanImport.ExerciseDraft(null, "Other", 3, 8, 0, "lb", 90, 0, "");
        PlanImport.PlanDraft plan = new PlanImport.PlanDraft("Should roll back", "Strength", "", Arrays.asList(valid, invalid));

        try {
            PlanImporter.save(db, plan);
            fail("Expected the invalid exercise to fail");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage() == null || !expected.getMessage().isEmpty());
        }
        assertEquals(before, db.plans().size());
    }

    @Test public void aiInstructionsContainTheImportContractAndCanonicalNames() {
        String instructions = PlanImport.aiInstructions();
        assertTrue(instructions.contains("workout-tracker-plan"));
        assertTrue(instructions.contains("Goblet squat"));
        assertTrue(instructions.contains("Return only one JSON object"));
        assertTrue(instructions.endsWith("THE USER'S WORKOUT REQUEST FOLLOWS:\n\n"));
    }

    private void expectError(String json, String fragment) throws Exception {
        try {
            PlanImport.parse(json);
            fail("Expected invalid JSON plan");
        } catch (PlanImport.FormatException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(fragment));
        }
    }

    private String validJson() {
        return "{\"format\":\"workout-tracker-plan\",\"version\":1,\"plan\":{"
                + "\"name\":\"Full Body\",\"type\":\"Strength\",\"notes\":\"Balanced session\",\"exercises\":["
                + "{\"name\":\"Goblet squat\",\"muscle_group\":\"Lower body\",\"sets\":3,\"reps\":8,\"weight\":40,\"unit\":\"lb\",\"rest_seconds\":90,\"duration_seconds\":0,\"notes\":\"\"},"
                + "{\"name\":\"Plank\",\"muscle_group\":\"Core\",\"sets\":3,\"reps\":0,\"weight\":0,\"unit\":\"bodyweight\",\"rest_seconds\":60,\"duration_seconds\":30,\"notes\":\"\"}"
                + "]}}";
    }
}
