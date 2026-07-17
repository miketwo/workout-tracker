package com.miketwo.workouttracker;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PlanDeletionTest {
    private Application app;

    @Before public void freshDatabase() {
        app = ApplicationProvider.getApplicationContext();
        Db.resetForTests();
        app.deleteDatabase(Db.NAME);
    }

    @Test public void deletingPlanRemovesExercisesButPreservesCompletedHistory() {
        Db db=Db.get(app);Models.Plan upper=findPlan(db,"Upper body");
        assertNotNull(upper);assertEquals(3,db.plan(upper.id).exercises.size());assertEquals(6,db.completedSetCount(1));

        db.deletePlan(upper.id);

        assertNull(db.plan(upper.id));assertEquals(6,db.completedSetCount(1));
        try(android.database.Cursor cursor=db.getReadableDatabase().rawQuery("SELECT plan_id FROM sessions WHERE id=1",null)){assertTrue(cursor.moveToFirst());assertTrue(cursor.isNull(0));}
    }

    @Test public void planTrashIconRequiresConfirmationBeforeDeletion() {
        Db db=Db.get(app);Models.Plan upper=findPlan(db,"Upper body");assertNotNull(upper);
        try(ActivityController<PlansActivity> controller=Robolectric.buildActivity(PlansActivity.class).setup()){
            ImageButton trash=findImageButton(controller.get().getWindow().getDecorView(),"Delete Upper body plan");assertNotNull(trash);trash.performClick();
            assertNotNull(db.plan(upper.id));
            AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertEquals("Delete Upper body?",shadowOf(dialog).getTitle());
            Button confirm=dialog.getButton(AlertDialog.BUTTON_POSITIVE);assertEquals("Delete plan",confirm.getText());confirm.performClick();shadowOf(Looper.getMainLooper()).idle();
            assertNull(db.plan(upper.id));
        }
    }

    private Models.Plan findPlan(Db db,String name){for(Models.Plan plan:db.plans())if(name.equals(plan.name))return plan;return null;}
    private ImageButton findImageButton(View view,String description){if(view instanceof ImageButton&&description.contentEquals(view.getContentDescription()))return(ImageButton)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){ImageButton found=findImageButton(group.getChildAt(i),description);if(found!=null)return found;}}return null;}
}
