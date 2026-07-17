package com.miketwo.workouttracker;

import android.app.Application;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

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

    @Test public void workoutDashboardAndGuidedWorkoutLaunchAndRecordASet() {
        try (ActivityController<WorkoutDashboardActivity> dashboard = Robolectric.buildActivity(WorkoutDashboardActivity.class).setup()) {
            assertNotNull(dashboard.get().getWindow().getDecorView());
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

    @Test public void homeShowsVersionQuoteAndRoutesToTheThreeActivities() {
        try (ActivityController<HomeActivity> home = Robolectric.buildActivity(HomeActivity.class).setup()) {
            TextView version = findText(home.get().getWindow().getDecorView(), "Version 0.1.15");
            assertNotNull(version);
            assertTrue(version.getText().toString().contains("build 16"));
            assertNotNull(findText(home.get().getWindow().getDecorView(), "James Allen"));
            assertNull(findButton(home.get().getWindow().getDecorView(), "Let's go!"));
            assertRoute(home.get(), "Plan", PlansActivity.class);
            assertFalse(home.get().isFinishing());
        }
        try (ActivityController<HomeActivity> home = Robolectric.buildActivity(HomeActivity.class).setup()) {
            assertRoute(home.get(), "Workout", WorkoutDashboardActivity.class);
        }
        try (ActivityController<HomeActivity> home = Robolectric.buildActivity(HomeActivity.class).setup()) {
            assertRoute(home.get(), "Review", ReviewActivity.class);
        }
    }

    @Test public void planHomeReturnsToTheOpeningScreen() {
        try (ActivityController<PlansActivity> plans = Robolectric.buildActivity(PlansActivity.class).setup()) {
            Button home = findButton(plans.get().getWindow().getDecorView(), "‹ Home");
            assertNotNull(home);
            home.performClick();
            Intent next = shadowOf(plans.get()).getNextStartedActivity();
            assertNotNull(next);
            assertEquals(HomeActivity.class.getName(), next.getComponent().getClassName());
            assertTrue((next.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
        }
    }

    @Test public void workoutCanQuitWithoutSaving() {
        Db db = Db.get(app);
        Models.Plan strength = null;
        for (Models.Plan p : db.plans()) if ("Strength".equals(p.type)) { strength = p; break; }
        assertNotNull(strength);
        Intent intent = new Intent(app, WorkoutActivity.class).putExtra("plan_id", strength.id);
        try (ActivityController<WorkoutActivity> workout = Robolectric.buildActivity(WorkoutActivity.class, intent).setup()) {
            long session = app.getSharedPreferences("active_workout", 0).getLong("session_id", 0);
            assertTrue(session > 0);
            Button end = findButton(workout.get().getWindow().getDecorView(), "End workout");
            assertNotNull(end);
            end.performClick();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(dialog);
            Button quit = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            assertEquals("Quit without saving", quit.getText());
            quit.performClick();
            shadowOf(Looper.getMainLooper()).idle();
            try (android.database.Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sessions WHERE id=?", new String[]{String.valueOf(session)})) {
                assertTrue(cursor.moveToFirst());
                assertEquals(0, cursor.getInt(0));
            }
            assertEquals(0, app.getSharedPreferences("active_workout", 0).getLong("session_id", 0));
        }
    }

    @Test public void guidedIntervalsAppearOnlyForRunLogging() {
        Intent swimIntent = new Intent(app, CardioActivity.class).putExtra("activity", "Swim");
        try (ActivityController<CardioActivity> swim = Robolectric.buildActivity(CardioActivity.class, swimIntent).setup()) {
            assertNull(findButton(swim.get().getWindow().getDecorView(), "Start guided run/walk intervals"));
        }
        try (ActivityController<CardioActivity> run = Robolectric.buildActivity(CardioActivity.class).setup()) {
            assertNotNull(findButton(run.get().getWindow().getDecorView(), "Start guided run/walk intervals"));
        }
    }

    @Test public void planEditorDragReordersCardsWithoutDuplicatingAnExercise() throws Exception {
        Db db=Db.get(app); Models.Plan plan=null;
        for(Models.Plan candidate:db.plans()) if("Upper body".equals(candidate.name)) {plan=candidate;break;}
        assertNotNull(plan);
        Intent intent=new Intent(app,PlanEditorActivity.class).putExtra("plan_id",plan.id);
        try(ActivityController<PlanEditorActivity> editor=Robolectric.buildActivity(PlanEditorActivity.class,intent).setup()){
            View decor=editor.get().getWindow().getDecorView();
            decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2400,View.MeasureSpec.EXACTLY));
            decor.layout(0,0,1080,2400);
            LinearLayout list=(LinearLayout)field(editor.get(),"exerciseList");
            assertEquals(3,list.getChildCount());
            ImageButton handle=findImageButton(editor.get().getWindow().getDecorView(),"Drag to reorder exercise");
            assertNotNull(handle);
            View destination=list.getChildAt(2);int[] destinationLocation=new int[2];destination.getLocationOnScreen(destinationLocation);
            int[] handleLocation=new int[2];handle.getLocationOnScreen(handleLocation);
            sendTouch(handle,android.view.MotionEvent.ACTION_DOWN,handleLocation[0]+handle.getWidth()/2,handleLocation[1]+handle.getHeight()/2);
            sendTouch(handle,android.view.MotionEvent.ACTION_MOVE,handleLocation[0]+handle.getWidth()/2,destinationLocation[1]+destination.getHeight()-1);
            View draggedCard=(View)handle.getParent().getParent();
            assertTrue(Math.abs(draggedCard.getTranslationY())>1);
            assertEquals(1.02f,draggedCard.getScaleX(),.001f);
            assertEquals(0,list.indexOfChild(draggedCard));
            sendTouch(handle,android.view.MotionEvent.ACTION_MOVE,handleLocation[0]+handle.getWidth()/2,destinationLocation[1]+destination.getHeight()-1);
            sendTouch(handle,android.view.MotionEvent.ACTION_MOVE,handleLocation[0]+handle.getWidth()/2,destinationLocation[1]+destination.getHeight()-1);
            sendTouch(handle,android.view.MotionEvent.ACTION_UP,handleLocation[0]+handle.getWidth()/2,destinationLocation[1]+destination.getHeight()-1);
            shadowOf(Looper.getMainLooper()).idle();
            Models.Plan reordered=db.plan(plan.id);
            assertEquals(3,reordered.exercises.size());
            assertEquals("Lat pulldown",reordered.exercises.get(2).name);
            java.util.HashSet<Long> ids=new java.util.HashSet<>();for(Models.Exercise exercise:reordered.exercises)ids.add(exercise.id);
            assertEquals(3,ids.size());
            LinearLayout reorderedCards=(LinearLayout)field(editor.get(),"exerciseList");
            assertEquals(3,reorderedCards.getChildCount());
            assertNotNull(findText(reorderedCards.getChildAt(2),"3. Lat pulldown"));
        }
    }

    @Test public void reviewLongPressOffersAndDeletesACompletedWorkout() {
        Db db = Db.get(app);
        try (ActivityController<ReviewActivity> review = Robolectric.buildActivity(ReviewActivity.class).setup()) {
            TextView workout = findText(review.get().getWindow().getDecorView(), "Upper body");
            assertNotNull(workout);
            View card = (View) workout.getParent();
            assertTrue(card.performLongClick());
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(dialog);
            try (android.database.Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sessions WHERE id=1", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals(1, cursor.getInt(0));
            }
            Button delete = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            assertEquals("Delete workout", delete.getText());
            delete.performClick();
            shadowOf(Looper.getMainLooper()).idle();
            try (android.database.Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sessions WHERE id=1", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals(0, cursor.getInt(0));
            }
            try (android.database.Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM set_results WHERE session_id=1", null)) {
                assertTrue(cursor.moveToFirst());
                assertEquals(0, cursor.getInt(0));
            }
        }
    }

    @Test public void reviewGroupsRecentActivityAndUsesDatesAsCardTitles() {
        try (ActivityController<ReviewActivity> review = Robolectric.buildActivity(ReviewActivity.class).setup()) {
            TextView date = findText(review.get().getWindow().getDecorView(), "Jul 9, 2026");
            assertNotNull(date);
            assertEquals(21f, date.getTextSize(), .01f);
            assertNotNull(findText(review.get().getWindow().getDecorView(), "Strength  •  Upper body  •  complete"));
            assertNotNull(findText(review.get().getWindow().getDecorView(), "Run  •  26 minutes"));
            View card = (View) date.getParent();
            assertEquals(Ui.MINT, ((android.graphics.drawable.GradientDrawable) ((View) card.getParent()).getBackground()).getColor().getDefaultColor());
        }
    }

    private void assertRoute(HomeActivity home, String label, Class<?> destination) {
            Button button = findButton(home.getWindow().getDecorView(), label);
            assertNotNull(button);
            button.performClick();
            Intent next = shadowOf(home).getNextStartedActivity();
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

    private ImageButton findImageButton(View view,String description) {
        if(view instanceof ImageButton&&description.contentEquals(view.getContentDescription()))return (ImageButton)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){ImageButton found=findImageButton(group.getChildAt(i),description);if(found!=null)return found;}}
        return null;
    }

    private Object field(Object target,String name) throws Exception {java.lang.reflect.Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
    private void sendTouch(View target,int action,float x,float y){android.view.MotionEvent event=android.view.MotionEvent.obtain(0,System.currentTimeMillis(),action,x,y,0);target.dispatchTouchEvent(event);event.recycle();}
}
