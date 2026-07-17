package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;
import java.util.Locale;

public class WorkoutDashboardActivity extends Activity {
    private Db db;
    private LinearLayout body;

    @Override public void onCreate(Bundle state){super.onCreate(state);db=Db.get(this);render();}
    @Override protected void onResume(){super.onResume();if(body!=null)render();}

    private void render(){
        body=Ui.column(this);Ui.page(this,body);
        body.addView(Ui.title(this,"Choose a workout"));
        body.addView(Ui.text(this,"Pick the plan that fits today. Plans are not tied to a schedule.",17,Ui.MUTED));
        Ui.spacer(body,10);
        List<Models.Plan> plans=db.plans();
        for(Models.Plan p:plans){
            Models.Plan full=db.plan(p.id);
            LinearLayout optionCard=Ui.card(this);
            optionCard.addView(Ui.heading(this,p.name));
            optionCard.addView(Ui.text(this,p.type+(full.exercises.isEmpty()?"":"  •  "+full.exercises.size()+" exercises"),15,Ui.MUTED));
            if(!p.notes.isBlank())optionCard.addView(Ui.text(this,p.notes,14,Ui.MUTED));
            Button start=Ui.button(this,p.type.equals("Strength")?"Start workout":"Log "+p.type.toLowerCase(Locale.getDefault()),true);
            start.setOnClickListener(v->{if(p.type.equals("Strength"))startWorkout(p.id);else startActivity(new Intent(this,CardioActivity.class).putExtra("activity",p.type));});
            optionCard.addView(start);
            LinearLayout actions=Ui.row(this);
            Button view=Ui.smallButton(this,"View plan");Ui.weighted(view,1);view.setOnClickListener(v->viewPlan(p.id));actions.addView(view);
            View gap=new View(this);actions.addView(gap,new LinearLayout.LayoutParams(Ui.dp(this,10),1));
            Button packing=Ui.smallButton(this,"Packing list");Ui.weighted(packing,1);packing.setOnClickListener(v->viewPackingChecklist(p.type));actions.addView(packing);
            optionCard.addView(actions);
            body.addView(optionCard);
        }
        if(plans.isEmpty())body.addView(Ui.text(this,"No workout plans yet. Create or import one from Plans.",16,Ui.MUTED));
        Ui.spacer(body,12);
        LinearLayout nav=Ui.row(this);
        Button plansButton=Ui.smallButton(this,"Plans");Ui.weighted(plansButton,1);plansButton.setOnClickListener(v->startActivity(new Intent(this,PlansActivity.class)));nav.addView(plansButton);
        View gap=new View(this);nav.addView(gap,new LinearLayout.LayoutParams(Ui.dp(this,10),1));
        Button logButton=Ui.smallButton(this,"Log cardio");Ui.weighted(logButton,1);logButton.setOnClickListener(v->startActivity(new Intent(this,CardioActivity.class)));nav.addView(logButton);
        body.addView(nav);
        Button history=Ui.button(this,"History & progress",false);history.setOnClickListener(v->startActivity(new Intent(this,ReviewActivity.class)));body.addView(history);
        body.addView(Ui.text(this,"Your data stays on this phone in this release. Back up before uninstalling.",13,Ui.MUTED));
    }

    private void startWorkout(long planId){startActivity(new Intent(this,WorkoutActivity.class).putExtra("plan_id",planId));}
    private void viewPlan(long planId){startActivity(new Intent(this,PlanDetailActivity.class).putExtra("plan_id",planId));}
    private void viewPackingChecklist(String workoutType){startActivity(new Intent(this,PackingChecklistActivity.class).putExtra("workout_type",workoutType));}
}
