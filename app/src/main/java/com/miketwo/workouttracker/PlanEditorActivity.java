package com.miketwo.workouttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class PlanEditorActivity extends Activity {
    private Db db; private long planId; private EditText name,notes; private Spinner day,type; private LinearLayout body,exerciseList;
    private final String[] days={"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
    private final String[] types={"Strength","Run","Swim","Mixed"};
    private final String[] common={"Custom exercise…","Lat pulldown","Assisted pull-up","Chest press","Incline chest press","Shoulder press","Seated row","Pec fly","Reverse fly","Biceps curl","Triceps press","Leg press","Leg extension","Seated leg curl","Hip abduction","Hip adduction","Calf raise","Back extension","Abdominal crunch","Dumbbell bench press","Goblet squat","Romanian deadlift","Plank","Push-up","Banded lateral walk"};
    private final String[] groups={"Upper body","Lower body","Core","Full body","Cardio","Other"};
    @Override public void onCreate(Bundle state){super.onCreate(state);db=Db.get(this);planId=getIntent().getLongExtra("plan_id",0);render();}
    private void render(){
        Models.Plan p=planId>0?db.plan(planId):null;
        body=Ui.column(this);Ui.page(this,body);
        Button back=Ui.smallButton(this,"‹ Plans");back.setOnClickListener(v->finish());body.addView(back);
        body.addView(Ui.title(this,p==null?"Create plan":"Edit plan"));
        body.addView(Ui.label(this,"Plan name"));name=Ui.input(this,"Upper body");if(p!=null)name.setText(p.name);body.addView(name);
        body.addView(Ui.label(this,"Scheduled day"));day=spinner(days);if(p!=null)day.setSelection(p.weekday-1);body.addView(day);
        body.addView(Ui.label(this,"Workout type"));type=spinner(types);if(p!=null){for(int i=0;i<types.length;i++)if(types[i].equals(p.type))type.setSelection(i);}body.addView(type);
        body.addView(Ui.label(this,"Plan notes"));notes=Ui.input(this,"What to bring or remember");if(p!=null)notes.setText(p.notes);body.addView(notes);
        Button save=Ui.button(this,p==null?"Save plan":"Save changes",true);save.setOnClickListener(v->save());body.addView(save);
        if(p!=null){
            body.addView(Ui.heading(this,"Exercises in order"));exerciseList=new LinearLayout(this);exerciseList.setOrientation(LinearLayout.VERTICAL);body.addView(exerciseList);renderExercises(p);
            Button add=Ui.button(this,"Add exercise",false);add.setOnClickListener(v->exerciseDialog());body.addView(add);
        } else body.addView(Ui.text(this,"Save the plan, then reopen it to add exercises.",14,Ui.MUTED));
    }
    private Spinner spinner(String[] values){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));s.setPadding(Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8));return s;}
    private void save(){
        if(name.getText().toString().trim().isEmpty()){name.setError("Give the plan a name");return;}
        planId=db.savePlan(planId,name.getText().toString().trim(),day.getSelectedItemPosition()+1,types[type.getSelectedItemPosition()],notes.getText().toString().trim());
        Toast.makeText(this,"Plan saved",Toast.LENGTH_SHORT).show();render();
    }
    private void renderExercises(Models.Plan p){
        exerciseList.removeAllViews();int index=1;
        for(Models.Exercise e:p.exercises){
            LinearLayout card=Ui.card(this);card.addView(Ui.heading(this,index+". "+e.name));String target=e.durationSeconds>0?e.sets+" sets × "+e.durationSeconds+" seconds":e.sets+" sets × "+e.reps+" reps @ "+WorkoutMath.formatWeight(e.weight)+" "+e.unit;card.addView(Ui.text(this,target+(e.muscleGroup.isBlank()?"":"  •  "+e.muscleGroup),16,Ui.MUTED));if(!e.notes.isBlank())card.addView(Ui.text(this,e.notes,14,Ui.MUTED));
            Button delete=Ui.smallButton(this,"Remove");delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Remove "+e.name+" from this plan?").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{db.deleteExercise(e.id);render();}).show());card.addView(delete);exerciseList.addView(card);index++;
        }
        if(p.exercises.isEmpty())exerciseList.addView(Ui.text(this,"No exercises yet.",16,Ui.MUTED));
    }
    private void exerciseDialog(){
        LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),0);
        form.addView(Ui.label(this,"Exercise"));
        Spinner catalog=spinner(common);form.addView(catalog);
        form.addView(Ui.label(this,"Custom exercise name"));
        EditText exName=Ui.input(this,"Exercise name");form.addView(exName);
        form.addView(Ui.label(this,"Muscle group"));
        Spinner muscle=spinner(groups);form.addView(muscle);catalog.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){if(pos>0){exName.setText(common[pos]);String g=groupFor(common[pos]);for(int i=0;i<groups.length;i++)if(groups[i].equals(g))muscle.setSelection(i);}}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        form.addView(Ui.label(this,"Sets"));
        EditText sets=number("Sets",false,"3");form.addView(sets);
        form.addView(Ui.label(this,"Target reps"));
        EditText reps=number("Target reps",false,"8");form.addView(reps);
        form.addView(Ui.label(this,"Target weight"));
        EditText weight=number("Target weight",true,"0");form.addView(weight);
        form.addView(Ui.label(this,"Weight unit"));
        Spinner unit=spinner(new String[]{"lb","kg","bodyweight","assisted lb","assisted kg"});form.addView(unit);
        form.addView(Ui.label(this,"Timed set seconds"));
        EditText timed=number("0 for ordinary sets",false,"0");form.addView(timed);
        form.addView(Ui.label(this,"Rest between sets (seconds)"));
        EditText rest=number("Rest seconds",false,"90");form.addView(rest);
        form.addView(Ui.label(this,"Notes / seat position"));
        EditText exNotes=Ui.input(this,"Optional notes");form.addView(exNotes);
        new AlertDialog.Builder(this).setTitle("Add exercise").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->{
            if(exName.getText().toString().trim().isEmpty())return;
            db.addExercise(planId,exName.getText().toString().trim(),integer(sets,3),integer(reps,8),decimal(weight,0),(String)unit.getSelectedItem(),exNotes.getText().toString().trim(),integer(rest,90),integer(timed,0),(String)muscle.getSelectedItem());render();
        }).show();
    }
    private EditText number(String hint,boolean decimal,String initial){EditText e=Ui.input(this,hint);e.setSingleLine();e.setInputType(InputType.TYPE_CLASS_NUMBER|(decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));e.setText(initial);e.setHint(hint);return e;}
    private int integer(EditText e,int fallback){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return fallback;}}
    private double decimal(EditText e,double fallback){try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return fallback;}}
    private String groupFor(String name){String n=name.toLowerCase(java.util.Locale.ROOT);if(n.contains("leg")||n.contains("calf")||n.contains("squat")||n.contains("hip")||n.contains("walk")||n.contains("deadlift"))return "Lower body";if(n.contains("ab")||n.contains("plank")||n.contains("back extension"))return "Core";return "Upper body";}
}
