package com.miketwo.workouttracker;

import android.app.Application;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AppSmokeTest {
    private Application app;

    @Before public void freshDatabase() {
        app = ApplicationProvider.getApplicationContext();
        Db.resetForTests();
        app.deleteDatabase(Db.NAME);
        app.getSharedPreferences("active_workout", 0).edit().clear().commit();
    }

    @Test public void firstLaunchSeedsPlansAndHistory() {
        Db db = Db.get(app);
        assertEquals(4, db.plans().size());
        assertNotNull(db.plan(db.plans().get(0).id));
        assertEquals(6, db.completedSetCount(1));
    }

    @Test public void homeAndGuidedWorkoutLaunchAndRecordASet() {
        try (ActivityController<MainActivity> home = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertNotNull(home.get().getWindow().getDecorView());
        }
        Db db = Db.get(app);
        Models.Plan strength = null;
        for (Models.Plan p : db.plans()) if ("Strength".equals(p.type)) { strength = p; break; }
        assertNotNull(strength);
        Intent intent = new Intent(app, WorkoutActivity.class).putExtra("plan_id", strength.id);
        try (ActivityController<WorkoutActivity> workout = Robolectric.buildActivity(WorkoutActivity.class, intent).setup()) {
            Button complete = findButton(workout.get().getWindow().getDecorView(), "Complete set");
            assertNotNull(complete);
            complete.performClick();
            long session = app.getSharedPreferences("active_workout", 0).getLong("session_id", 0);
            assertEquals(1, db.completedSetCount(session));
        }
    }

    private Button findButton(View view, String text) {
        if (view instanceof Button && text.contentEquals(((Button) view).getText())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i=0;i<group.getChildCount();i++) {
                Button found = findButton(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
