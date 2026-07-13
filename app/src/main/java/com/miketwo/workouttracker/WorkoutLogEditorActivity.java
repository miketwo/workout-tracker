package com.miketwo.workouttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Edit saved strength results without ever changing the original workout plan. */
public class WorkoutLogEditorActivity extends Activity {
    private long session; private SQLiteDatabase db; private LinearLayout body;
    @Override public void onCreate(Bundle state){super.onCreate(state);session=getIntent().getLongExtra("session_id",0);db=Db.get(this).getWritableDatabase();render();}
    private void render(){body=Ui.column(this);Ui.page(this,body);Button back=Ui.smallButton(this,"‹ Summary");back.setOnClickListener(v->finish());body.addView(back);body.addView(Ui.title(this,"Edit workout log"));body.addView(Ui.text(this,"Changes affect this logged workout only.",16,Ui.MUTED));try(Cursor c=db.rawQuery("SELECT id,exercise_name,set_number,actual_reps,raw_weight,raw_unit,rir,status FROM set_results WHERE session_id=? ORDER BY exercise_position,set_number",new String[]{String.valueOf(session)})){while(c.moveToNext()){long id=c.getLong(0);int reps=c.getInt(3);double weight=c.getDouble(4);LinearLayout card=Ui.card(this);card.addView(Ui.exerciseHeader(this,c.getString(1),c.getString(1)+" • set "+c.getInt(2),18));card.addView(Ui.text(this,reps+" reps @ "+WorkoutMath.formatWeight(weight)+" "+c.getString(5),16,Ui.MUTED));Button edit=Ui.smallButton(this,"Edit set");edit.setOnClickListener(v->editSet(id,reps,weight));card.addView(edit);Button remove=Ui.smallButton(this,"Remove set");remove.setOnClickListener(v->{db.delete("set_results","id=?",new String[]{String.valueOf(id)});render();});card.addView(remove);body.addView(card);}}
        Button add=Ui.button(this,"Add exercise to this log",false);add.setOnClickListener(v->addExercise());body.addView(add);}
    private void editSet(long id,int reps,double weight){LinearLayout form=Ui.column(this);EditText r=input(String.valueOf(reps),false),w=input(WorkoutMath.formatWeight(weight),true);form.addView(Ui.label(this,"Reps"));form.addView(r);form.addView(Ui.label(this,"Weight"));form.addView(w);new AlertDialog.Builder(this).setTitle("Edit set").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,x)->{android.content.ContentValues v=new android.content.ContentValues();v.put("actual_reps",number(r));v.put("actual_weight",decimal(w));v.put("raw_weight",decimal(w));v.put("effective_weight_lb",decimal(w));db.update("set_results",v,"id=?",new String[]{String.valueOf(id)});render();}).show();}
    private void addExercise(){LinearLayout form=Ui.column(this);EditText n=Ui.input(this,"Exercise name"),r=input("8",false),w=input("0",true);form.addView(n);form.addView(Ui.label(this,"Reps"));form.addView(r);form.addView(Ui.label(this,"Weight (lb)"));form.addView(w);new AlertDialog.Builder(this).setTitle("Add exercise").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,x)->{if(n.getText().toString().trim().isEmpty())return;android.content.ContentValues v=new android.content.ContentValues();v.put("session_id",session);v.put("exercise_name",n.getText().toString().trim());v.put("exercise_position",999);v.put("set_number",1);v.put("target_reps",number(r));v.put("target_weight",decimal(w));v.put("actual_reps",number(r));v.put("actual_weight",decimal(w));v.put("raw_weight",decimal(w));v.put("raw_unit","lb");v.put("effective_weight_lb",decimal(w));v.put("load_mode",WorkoutMath.LOAD_STANDARD);v.put("created_at",java.time.LocalDateTime.now().toString());db.insertOrThrow("set_results",null,v);render();}).show();}
    private EditText input(String value,boolean decimal){EditText e=Ui.input(this,"");e.setSingleLine();e.setInputType(InputType.TYPE_CLASS_NUMBER|(decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));e.setText(value);return e;}private int number(EditText e){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return 0;}}private double decimal(EditText e){try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return 0;}}
}
