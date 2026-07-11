package com.miketwo.workouttracker;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import java.time.LocalDate;

public class CardioActivity extends Activity {
    private Spinner activity,unit; private EditText date,duration,distance,intervals,notes,laps,poolLength; private LinearLayout body,fields;
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    private void render(){
        body=Ui.column(this);Ui.page(this,body);Button back=Ui.smallButton(this,"‹ Home");back.setOnClickListener(v->Ui.home(this));body.addView(back);body.addView(Ui.title(this,"Log cardio"));
        body.addView(Ui.text(this,"The phone can stay out of the way. Record the useful totals afterward.",16,Ui.MUTED));
        Button guided=Ui.button(this,"Start guided run/walk intervals",true);guided.setOnClickListener(v->startActivity(new Intent(this,IntervalActivity.class)));body.addView(guided);
        body.addView(Ui.label(this,"Activity"));activity=spinner(new String[]{"Run","Swim"});String requested=getIntent().getStringExtra("activity");if("Swim".equals(requested))activity.setSelection(1);activity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){renderFields();}public void onNothingSelected(android.widget.AdapterView<?> p){}});body.addView(activity);
        body.addView(Ui.label(this,"Date"));date=input(LocalDate.now().toString(),false);body.addView(date);fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);body.addView(fields);renderFields();
        Button save=Ui.button(this,"Save cardio session",true);save.setOnClickListener(v->save());body.addView(save);
    }
    private void renderFields(){if(fields==null)return;fields.removeAllViews();boolean swim=activity.getSelectedItemPosition()==1;
        fields.addView(Ui.label(this,"Duration (minutes)"));duration=input("",true);fields.addView(duration);
        if(swim){fields.addView(Ui.label(this,"Lengths completed"));laps=input("",false);fields.addView(laps);fields.addView(Ui.label(this,"Pool length"));poolLength=input("25",true);fields.addView(poolLength);fields.addView(Ui.label(this,"Pool unit"));unit=spinner(new String[]{"yd","m"});fields.addView(unit);distance=null;intervals=null;}
        else{fields.addView(Ui.label(this,"Distance"));distance=input("",true);fields.addView(distance);fields.addView(Ui.label(this,"Distance unit"));unit=spinner(new String[]{"mi","km"});fields.addView(unit);fields.addView(Ui.label(this,"Intervals used"));intervals=Ui.input(this,"For example: 6 min jog / 2 min walk");intervals.setText(getIntent().getStringExtra("intervals"));fields.addView(intervals);laps=null;poolLength=null;if(getIntent().hasExtra("duration"))duration.setText(WorkoutMath.formatWeight(getIntent().getDoubleExtra("duration",0)));}
        fields.addView(Ui.label(this,"Session notes"));notes=Ui.input(this,"How it went, treadmill details, or anything worth remembering");fields.addView(notes);
    }
    private void save(){boolean swim=activity.getSelectedItemPosition()==1;double mins=num(duration);if(mins<=0){duration.setError("Enter the session duration");return;}int lengthCount=swim?(int)num(laps):0;double pool=swim?num(poolLength):0;double dist=swim?lengthCount*pool:num(distance);String chosenUnit=(String)unit.getSelectedItem();String interval=swim?"":intervals.getText().toString().trim();Db.get(this).addCardio(swim?"Swim":"Run",date.getText().toString().trim(),mins,dist,chosenUnit,interval,notes.getText().toString().trim(),lengthCount,pool);Toast.makeText(this,"Cardio session saved",Toast.LENGTH_SHORT).show();finish();}
    private EditText input(String initial,boolean decimal){EditText e=Ui.input(this,"");e.setSingleLine();if(decimal)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setText(initial);return e;}
    private Spinner spinner(String[] data){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,data));return s;}
    private double num(EditText e){if(e==null)return 0;try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return 0;}}
}
