package com.miketwo.workouttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.Collections;

public class WorkoutActivity extends Activity {
    private Db db; private Models.Plan plan; private long sessionId; private int exerciseIndex,setIndex; private LinearLayout body; private boolean completionShown;
    private EditText repsInput,weightInput; private Spinner rirInput; private SharedPreferences state; private int timedCompleteSeconds;

    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);db=Db.get(this);state=getSharedPreferences("active_workout",MODE_PRIVATE);
        long planId=getIntent().getLongExtra("plan_id",0);plan=db.plan(planId);if(plan==null){finish();return;}
        if(state.getLong("plan_id",-1)==planId&&state.getLong("session_id",0)>0){sessionId=state.getLong("session_id",0);exerciseIndex=state.getInt("exercise",0);setIndex=state.getInt("set",0);for(Models.Exercise e:plan.exercises)e.sets+=state.getInt("extra_"+e.id,0);}else{state.edit().clear().apply();sessionId=db.beginSession(plan);exerciseIndex=0;setIndex=0;persist();}
        render();
    }

    private void persist(){state.edit().putLong("plan_id",plan.id).putLong("session_id",sessionId).putInt("exercise",exerciseIndex).putInt("set",setIndex).apply();}
    private int totalSets(){int n=0;for(Models.Exercise e:plan.exercises)n+=e.sets;return n;}
    private int position(){int n=0;for(int i=0;i<exerciseIndex&&i<plan.exercises.size();i++)n+=plan.exercises.get(i).sets;return n+setIndex;}

    private void render(){
        if(plan.exercises.isEmpty()){showEmpty();return;}if(exerciseIndex>=plan.exercises.size()){finishWorkout("complete");return;}
        Models.Exercise e=plan.exercises.get(exerciseIndex);
        body=Ui.column(this);Ui.page(this,body);
        LinearLayout top=Ui.row(this);Button exit=Ui.smallButton(this,"End workout");Ui.weighted(exit,1);exit.setOnClickListener(v->endDialog());top.addView(exit);Button undo=Ui.smallButton(this,"Undo");Ui.weighted(undo,1);undo.setEnabled(position()>0);undo.setOnClickListener(v->undo());top.addView(undo);body.addView(top);
        body.addView(Ui.label(this,plan.name));
        body.addView(Ui.title(this,"Exercise "+(exerciseIndex+1)+" of "+plan.exercises.size()));
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(totalSets());progress.setProgress(position());progress.setMinimumHeight(Ui.dp(this,12));body.addView(progress);
        body.addView(Ui.text(this,position()+" of "+totalSets()+" planned sets complete",14,Ui.MUTED));

        LinearLayout card=Ui.card(this);card.addView(Ui.label(this,"Set "+(setIndex+1)+" of "+e.sets));card.addView(Ui.title(this,e.name));
        String target=e.bodyweight?"Bodyweight":WorkoutMath.formatWeight(e.weight)+" "+e.unit;card.addView(Ui.text(this,e.durationSeconds>0?"Target  •  "+e.durationSeconds+" seconds":"Target  •  "+e.reps+" reps @ "+target,20,Ui.FOREST));
        if(!e.notes.isBlank())card.addView(Ui.text(this,e.notes,16,Ui.MUTED));
        Models.LastSet last=db.lastSet(e.name,setIndex+1,sessionId);if(last!=null){String effort=last.rir>=0?"  •  "+last.rir+" RIR":"";card.addView(Ui.text(this,"Last time  "+last.reps+" reps @ "+WorkoutMath.formatWeight(last.weight)+" "+e.unit+effort,15,Ui.MUTED));}
        body.addView(card);

        body.addView(Ui.heading(this,"Record what you did"));
        body.addView(Ui.label(this,"Repetitions"));repsInput=numeric(String.valueOf(e.reps),false);body.addView(stepper(repsInput,1));
        body.addView(Ui.label(this,e.assistance?"Assistance weight":"Weight ("+e.unit+")"));weightInput=numeric(WorkoutMath.formatWeight(e.weight),true);body.addView(stepper(weightInput,5));
        body.addView(Ui.label(this,"Reps in reserve (optional)"));rirInput=new Spinner(this);rirInput.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Not recorded","0 — no reps left","1","2","3","4","5+ — easy"}));body.addView(rirInput);
        String completeLabel=e.durationSeconds>0&&timedCompleteSeconds==0?"Start "+e.durationSeconds+"-second set":setIndex==e.sets-1?"Done — next exercise":"Complete set";Button complete=Ui.button(this,completeLabel,true);complete.setOnClickListener(v->{if(e.durationSeconds>0&&timedCompleteSeconds==0)startActivityForResult(new android.content.Intent(this,TimedExerciseActivity.class).putExtra("seconds",e.durationSeconds).putExtra("exercise",e.name),77);else record("complete");});body.addView(complete);
        Button skip=Ui.button(this,"Skip this set",false);skip.setOnClickListener(v->record("skipped"));body.addView(skip);
        Button extra=Ui.smallButton(this,"Add an extra set");extra.setOnClickListener(v->{e.sets++;state.edit().putInt("extra_"+e.id,state.getInt("extra_"+e.id,0)+1).apply();render();});body.addView(extra);
        Button skipExercise=Ui.smallButton(this,"Skip the rest of this exercise");skipExercise.setOnClickListener(v->skipExercise());body.addView(skipExercise);
        if(exerciseIndex<plan.exercises.size()-1){if(setIndex==0){Button unavailable=Ui.smallButton(this,"Machine busy — choose another exercise");unavailable.setOnClickListener(v->chooseAnother());body.addView(unavailable);}body.addView(Ui.text(this,"Next up  •  "+plan.exercises.get(exerciseIndex+1).name,14,Ui.MUTED));}
    }

    private LinearLayout stepper(EditText input,int amount){
        LinearLayout row=Ui.row(this);Button minus=Ui.button(this,"− "+amount,false);minus.setOnClickListener(v->adjust(input,-amount));Ui.weighted(minus,.8f);row.addView(minus);Ui.weighted(input,1.4f);row.addView(input);Button plus=Ui.button(this,"+ "+amount,false);plus.setOnClickListener(v->adjust(input,amount));Ui.weighted(plus,.8f);row.addView(plus);return row;
    }
    private EditText numeric(String value,boolean decimal){EditText e=Ui.input(this,"");e.setSingleLine();e.setGravity(android.view.Gravity.CENTER);e.setInputType(InputType.TYPE_CLASS_NUMBER|(decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));e.setText(value);e.setSelectAllOnFocus(true);return e;}
    private void adjust(EditText e,int amount){double n=decimal(e,0)+amount;n=Math.max(0,n);e.setText(e==repsInput?String.valueOf((int)n):WorkoutMath.formatWeight(n));}
    private int reps(){return (int)decimal(repsInput,0);} private double decimal(EditText e,double fallback){try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return fallback;}}
    private int rir(){int i=rirInput.getSelectedItemPosition();return i==0?-1:(i==6?5:i-1);}

    private void record(String status){
        Models.Exercise e=plan.exercises.get(exerciseIndex);int actualReps=status.equals("skipped")?0:reps();double actualWeight=status.equals("skipped")?0:decimal(weightInput,e.weight);
        String record=status.equals("complete")?db.personalRecord(e.name,actualReps,actualWeight):null;
        boolean hasMore=exerciseIndex<plan.exercises.size()-1||setIndex<e.sets-1;
        String next=setIndex<e.sets-1?e.name+" — set "+(setIndex+2):exerciseIndex<plan.exercises.size()-1?plan.exercises.get(exerciseIndex+1).name:"Workout complete";
        db.recordSet(sessionId,e,setIndex+1,actualReps,actualWeight,rir(),status,timedCompleteSeconds);
        if(!hasMore){exerciseIndex=plan.exercises.size();setIndex=0;persist();if(record!=null)Toast.makeText(this,"Personal best ✦ "+record,Toast.LENGTH_LONG).show();finishWorkout("complete");return;}
        advance();
        if(status.equals("complete")&&e.restSeconds>0&&hasMore)startActivity(new android.content.Intent(this,RestActivity.class).putExtra("seconds",e.restSeconds).putExtra("next",next).putExtra("record",record));else if(record!=null)celebrate(record);
    }
    private void celebrate(String message){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(90,VibrationEffect.DEFAULT_AMPLITUDE));new AlertDialog.Builder(this).setTitle("Personal best ✦").setMessage(message+" Nice work.").setPositiveButton("Keep going",null).show();}
    private void advance(){Models.Exercise e=plan.exercises.get(exerciseIndex);setIndex++;if(setIndex>=e.sets){setIndex=0;exerciseIndex++;}timedCompleteSeconds=0;persist();hideKeyboard();render();}
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){super.onActivityResult(request,result,data);if(request==77&&result==RESULT_OK){timedCompleteSeconds=data==null?0:data.getIntExtra("elapsed",0);render();}}
    private void undo(){if(position()==0)return;db.undoLastSet(sessionId);if(setIndex>0)setIndex--;else{exerciseIndex=Math.max(0,exerciseIndex-1);setIndex=plan.exercises.get(exerciseIndex).sets-1;}persist();render();Toast.makeText(this,"Last entry undone",Toast.LENGTH_SHORT).show();}
    private void chooseAnother(){if(exerciseIndex>=plan.exercises.size()-1)return;String[] names=new String[plan.exercises.size()-exerciseIndex-1];for(int i=0;i<names.length;i++)names[i]=plan.exercises.get(exerciseIndex+i+1).name;new AlertDialog.Builder(this).setTitle("Do which exercise next?").setItems(names,(d,which)->{Collections.swap(plan.exercises,exerciseIndex,exerciseIndex+which+1);render();Toast.makeText(this,"Order adjusted for this workout only",Toast.LENGTH_SHORT).show();}).setNegativeButton("Cancel",null).show();}
    private void skipExercise(){Models.Exercise e=plan.exercises.get(exerciseIndex);for(int s=setIndex;s<e.sets;s++)db.recordSet(sessionId,e,s+1,0,0,-1,"skipped");exerciseIndex++;setIndex=0;persist();if(exerciseIndex>=plan.exercises.size())finishWorkout("complete");else render();}
    private void hideKeyboard(){((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(body.getWindowToken(),0);}

    private void endDialog(){
        EditText note=Ui.input(this,"Optional workout note");
        new AlertDialog.Builder(this).setTitle("End this workout?").setMessage("Completed work will be saved. Remaining work will stay incomplete.").setView(note).setNegativeButton("Keep going",null).setPositiveButton("Save & end",(d,w)->{db.finishSession(sessionId,"incomplete",note.getText().toString().trim());clearAndExit();}).show();
    }
    private void finishWorkout(String status){if(completionShown)return;completionShown=true;EditText note=Ui.input(this,"Optional workout note");new AlertDialog.Builder(this).setCancelable(false).setTitle("Workout complete").setMessage(db.completedSetCount(sessionId)+" set entries recorded. Nice work showing up.").setView(note).setPositiveButton("View history",(d,w)->{db.finishSession(sessionId,status,note.getText().toString().trim());clearAndExit();startActivity(new android.content.Intent(this,HistoryActivity.class));}).setNegativeButton("Done",(d,w)->{db.finishSession(sessionId,status,note.getText().toString().trim());clearAndExit();}).show();}
    private void clearAndExit(){state.edit().clear().apply();finish();}
    private void showEmpty(){LinearLayout b=Ui.column(this);Ui.page(this,b);b.addView(Ui.title(this,"This plan is empty"));b.addView(Ui.text(this,"Add exercises to the plan before starting it.",17,Ui.MUTED));Button done=Ui.button(this,"Back",true);done.setOnClickListener(v->{db.finishSession(sessionId,"incomplete","");clearAndExit();});b.addView(done);}
    @Override public void onBackPressed(){endDialog();}
}
