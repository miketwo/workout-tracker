package com.miketwo.workouttracker;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

/** The one local profile value needed to calculate assisted-bodyweight work. */
public class ProfileActivity extends Activity {
    private EditText weight; private Spinner unit;
    @Override public void onCreate(Bundle state){super.onCreate(state); Models.Profile p=Db.get(this).profile();LinearLayout body=Ui.column(this);Ui.page(this,body);Button back=Ui.smallButton(this,"‹ Home");back.setOnClickListener(v->finish());body.addView(back);body.addView(Ui.title(this,"Your profile"));body.addView(Ui.text(this,"Your body weight is used only to calculate the effective load for assisted bodyweight exercises.",16,Ui.MUTED));body.addView(Ui.label(this,"Body weight"));weight=Ui.input(this,"For example: 180");weight.setSingleLine();weight.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);if(p.bodyWeight>0)weight.setText(WorkoutMath.formatWeight(p.bodyWeight));body.addView(weight);body.addView(Ui.label(this,"Unit"));unit=new Spinner(this);unit.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"lb","kg"}));unit.setSelection("kg".equals(p.unit)?1:0);body.addView(unit);Button save=Ui.button(this,"Save profile",true);save.setOnClickListener(v->save());body.addView(save);}
    private void save(){double value;try{value=Double.parseDouble(weight.getText().toString());}catch(Exception e){value=0;}if(value<=0){weight.setError("Enter your current body weight");return;}Db.get(this).saveProfile(value,(String)unit.getSelectedItem());Toast.makeText(this,"Profile saved",Toast.LENGTH_SHORT).show();finish();}
}
