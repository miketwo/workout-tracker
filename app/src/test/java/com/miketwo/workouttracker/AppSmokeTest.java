package com.miketwo.workouttracker;

import android.app.Application;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

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

    @Test public void splashShowsVersionQuoteAndRoutesToTheThreeActivities() {
        try (ActivityController<SplashActivity> splash = Robolectric.buildActivity(SplashActivity.class).setup()) {
            TextView version = findText(splash.get().getWindow().getDecorView(), "Version 0.1.4");
            assertNotNull(version);
            assertTrue(version.getText().toString().contains("build 5"));
            assertNotNull(findText(splash.get().getWindow().getDecorView(), "James Allen"));
            assertNull(findButton(splash.get().getWindow().getDecorView(), "Let's go!"));
            assertRoute(splash.get(), "Plan", PlansActivity.class);
        }
        try (ActivityController<SplashActivity> splash = Robolectric.buildActivity(SplashActivity.class).setup()) {
            assertRoute(splash.get(), "Workout", MainActivity.class);
        }
        try (ActivityController<SplashActivity> splash = Robolectric.buildActivity(SplashActivity.class).setup()) {
            assertRoute(splash.get(), "Review", HistoryActivity.class);
        }
    }

    private void assertRoute(SplashActivity splash, String label, Class<?> destination) {
            Button button = findButton(splash.getWindow().getDecorView(), label);
            assertNotNull(button);
            button.performClick();
            Intent next = shadowOf(splash).getNextStartedActivity();
            assertNotNull(next);
            assertEquals(destination.getName(), next.getComponent().getClassName());
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

    private TextView findText(View view, String fragment) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(fragment)) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i=0;i<group.getChildCount();i++) {
                TextView found = findText(group.getChildAt(i), fragment);
                if (found != null) return found;
            }
        }
        return null;
    }
}
