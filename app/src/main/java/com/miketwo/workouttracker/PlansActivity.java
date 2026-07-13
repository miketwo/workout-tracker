package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import java.time.DayOfWeek;
import java.util.Locale;

public class PlansActivity extends Activity {
    private LinearLayout body;
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    @Override protected void onResume(){super.onResume();if(body!=null)render();}
    private void render(){
        body=Ui.column(this);Ui.page(this,body);
        Button back=Ui.smallButton(this,"‹ Home");back.setOnClickListener(v->Ui.openHome(this));body.addView(back);
        body.addView(Ui.title(this,"Workout plans"));
        body.addView(Ui.text(this,"Plan here. At the gym, the app will simply guide you through it.",16,Ui.MUTED));
        Button packing=Ui.smallButton(this,"Manage packing lists");packing.setOnClickListener(v->startActivity(new Intent(this,PackingListsActivity.class)));body.addView(packing);
        Ui.spacer(body,6);
        for(Models.Plan p:Db.get(this).plans()){
            LinearLayout card=Ui.card(this);
            card.addView(Ui.heading(this,p.name));
            Models.Plan full=Db.get(this).plan(p.id);
            String day=DayOfWeek.of(p.weekday).toString().toLowerCase(Locale.getDefault());day=Character.toUpperCase(day.charAt(0))+day.substring(1);
            card.addView(Ui.text(this,day+"  •  "+p.type+"  •  "+full.exercises.size()+" exercises",15,Ui.MUTED));
            Button view=Ui.button(this,"View plan",true);view.setOnClickListener(v->startActivity(new Intent(this,PlanDetailActivity.class).putExtra("plan_id",p.id)));card.addView(view);
            Button edit=Ui.smallButton(this,"Edit plan");edit.setOnClickListener(v->startActivity(new Intent(this,PlanEditorActivity.class).putExtra("plan_id",p.id)));card.addView(edit);
            body.addView(card);
        }
        Button add=Ui.button(this,"Create a plan",true);add.setOnClickListener(v->startActivity(new Intent(this,PlanEditorActivity.class)));body.addView(add);
    }
}
