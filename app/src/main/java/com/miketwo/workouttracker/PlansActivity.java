package com.miketwo.workouttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


public class PlansActivity extends Activity {
    private LinearLayout body;
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    @Override protected void onResume(){super.onResume();if(body!=null)render();}
    private void render(){
        body=Ui.column(this);Ui.page(this,body);
        Button back=Ui.smallButton(this,"‹ Home");back.setOnClickListener(v->Ui.openHome(this));body.addView(back);
        body.addView(Ui.title(this,"Workout plans"));
        body.addView(Ui.text(this,"Build a reusable workout or import one generated for you.",16,Ui.MUTED));
        Button importPlan=Ui.button(this,"Import a plan",true);importPlan.setOnClickListener(v->startActivity(new Intent(this,PlanImportActivity.class)));body.addView(importPlan);
        Button packing=Ui.smallButton(this,"Manage packing lists");packing.setOnClickListener(v->startActivity(new Intent(this,PackingListsActivity.class)));body.addView(packing);
        Ui.spacer(body,6);
        for(Models.Plan p:Db.get(this).plans()){
            LinearLayout card=Ui.card(this);
            LinearLayout header=Ui.row(this);
            android.widget.TextView title=Ui.heading(this,p.name);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            ImageButton delete=new ImageButton(this);delete.setImageResource(R.drawable.ic_delete_plan);delete.setContentDescription("Delete "+p.name+" plan");delete.setBackgroundColor(android.graphics.Color.TRANSPARENT);delete.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12));delete.setOnClickListener(v->confirmDelete(p));header.addView(delete,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));card.addView(header);
            Models.Plan full=Db.get(this).plan(p.id);
            card.addView(Ui.text(this,p.type+"  •  "+full.exercises.size()+" exercises",15,Ui.MUTED));
            Button view=Ui.button(this,"View plan",true);view.setOnClickListener(v->startActivity(new Intent(this,PlanDetailActivity.class).putExtra("plan_id",p.id)));card.addView(view);
            Button edit=Ui.smallButton(this,"Edit plan");edit.setOnClickListener(v->startActivity(new Intent(this,PlanEditorActivity.class).putExtra("plan_id",p.id)));card.addView(edit);
            body.addView(card);
        }
        Button add=Ui.button(this,"Create a plan",true);add.setOnClickListener(v->startActivity(new Intent(this,PlanEditorActivity.class)));body.addView(add);
    }

    private void confirmDelete(Models.Plan plan){
        new AlertDialog.Builder(this).setTitle("Delete "+plan.name+"?").setMessage("This removes the plan and its exercises. Completed workout history will be kept.").setNegativeButton("Cancel",null).setPositiveButton("Delete plan",(dialog,which)->{Db.get(this).deletePlan(plan.id);Toast.makeText(this,"Plan deleted",Toast.LENGTH_SHORT).show();render();}).show();
    }
}
