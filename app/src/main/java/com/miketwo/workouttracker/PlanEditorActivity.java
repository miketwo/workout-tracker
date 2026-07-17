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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

public class PlanEditorActivity extends Activity {
    private Db db; private long planId; private EditText name,notes; private Spinner type; private LinearLayout body,exerciseList; private ScrollView page; private final java.util.Map<LinearLayout,TextView> exerciseLabels=new java.util.HashMap<>();
    private final String[] types={"Strength","Run","Swim","Mixed"};
    private final String[] common={"Custom exercise…","Lat pulldown","Assisted pull-up","Chest press","Incline chest press","Shoulder press","Seated row","Pec fly","Reverse fly","Biceps curl","Triceps press","Leg press","Leg extension","Seated leg curl","Hip abduction","Hip adduction","Calf raise","Back extension","Abdominal crunch","Dumbbell bench press","Goblet squat","Romanian deadlift","Plank","Push-up","Banded lateral walk"};
    private final String[] groups={"Upper body","Lower body","Core","Full body","Cardio","Other"};
    @Override public void onCreate(Bundle state){super.onCreate(state);db=Db.get(this);planId=getIntent().getLongExtra("plan_id",0);render();}
    private void render(){
        Models.Plan p=planId>0?db.plan(planId):null;
        body=Ui.column(this);page=Ui.page(this,body);
        Button back=Ui.smallButton(this,"‹ Plans");back.setOnClickListener(v->finish());body.addView(back);
        body.addView(Ui.title(this,p==null?"Create plan":"Edit plan"));
        body.addView(Ui.label(this,"Plan name"));name=Ui.input(this,"Upper body");if(p!=null)name.setText(p.name);body.addView(name);
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
        planId=db.savePlan(planId,name.getText().toString().trim(),types[type.getSelectedItemPosition()],notes.getText().toString().trim());
        Toast.makeText(this,"Plan saved",Toast.LENGTH_SHORT).show();render();
    }
    private void renderExercises(Models.Plan p){
        exerciseList.removeAllViews();exerciseLabels.clear();int index=1;
        for(Models.Exercise e:p.exercises){
            LinearLayout card=Ui.card(this);LinearLayout header=Ui.row(this);LinearLayout title=Ui.exerciseHeader(this,e.name,index+". "+e.name,21);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView exerciseLabel=(TextView)title.getChildAt(title.getChildCount()-1);exerciseLabels.put(card,exerciseLabel);android.widget.ImageButton drag=Ui.dragHandle(this);header.addView(drag,new LinearLayout.LayoutParams(-2,-2));card.addView(header);String target=e.durationSeconds>0?e.sets+" sets × "+e.durationSeconds+" seconds":e.sets+" sets × "+e.reps+" reps @ "+WorkoutMath.formatWeight(e.weight)+" "+e.unit;card.addView(Ui.text(this,target+(e.muscleGroup.isBlank()?"":"  •  "+e.muscleGroup),16,Ui.MUTED));if(!e.notes.isBlank())card.addView(Ui.text(this,e.notes,14,Ui.MUTED));attachDragHandle(drag,card,p);Button delete=Ui.smallButton(this,"Remove");delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Remove "+e.name+" from this plan?").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{db.deleteExercise(e.id);render();}).show());card.addView(delete);exerciseList.addView(card);index++;
        }
        if(p.exercises.isEmpty())exerciseList.addView(Ui.text(this,"No exercises yet.",16,Ui.MUTED));
    }
    private void attachDragHandle(android.view.View handle,LinearLayout card,Models.Plan plan){
        Handler handler=new Handler();
        handle.setOnTouchListener(new android.view.View.OnTouchListener(){
            boolean dragging; float lastRawY,fingerOffsetY; int autoScrollDelta,dragStartIndex,dragTargetIndex;
            final Runnable autoScroll=new Runnable(){@Override public void run(){
                if(!dragging||autoScrollDelta==0)return;
                int before=page.getScrollY(); page.scrollBy(0,autoScrollDelta);dragTargetIndex=targetIndex(card,lastRawY);placeCardUnderFinger(card,lastRawY,fingerOffsetY);
                if(page.getScrollY()!=before)handler.postDelayed(this,16);
            }};
            public boolean onTouch(android.view.View v,android.view.MotionEvent event){
                switch(event.getActionMasked()){
                    case android.view.MotionEvent.ACTION_DOWN:
                        dragging=true; lastRawY=event.getRawY();dragStartIndex=exerciseList.indexOfChild(card);dragTargetIndex=dragStartIndex;fingerOffsetY=lastRawY-cardTopOnScreen(card);card.animate().cancel();card.setAlpha(.98f);card.setElevation(Ui.dp(PlanEditorActivity.this,12));card.setScaleX(1.02f);card.setScaleY(1.02f);
                        page.requestDisallowInterceptTouchEvent(true); return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        lastRawY=event.getRawY();dragTargetIndex=targetIndex(card,lastRawY);placeCardUnderFinger(card,lastRawY,fingerOffsetY);updateAutoScroll(lastRawY); return true;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(autoScroll); autoScrollDelta=0; page.requestDisallowInterceptTouchEvent(false);
                        if(dragging){dragging=false;card.setAlpha(1);card.setElevation(Ui.dp(PlanEditorActivity.this,1));if(dragTargetIndex!=dragStartIndex)moveCard(card,plan,dragTargetIndex);card.post(()->card.animate().translationY(0).scaleX(1).scaleY(1).setDuration(140).start());db.reorderExercises(planId,plan.exercises);updateExerciseNumbers(plan);}
                        return true;
                }
                return true;
            }
            private void updateAutoScroll(float rawY){
                int[] location=new int[2];page.getLocationOnScreen(location);int edge=Ui.dp(PlanEditorActivity.this,72);int next=0;
                if(rawY<location[1]+edge)next=-Math.max(4,Ui.dp(PlanEditorActivity.this,12));
                else if(rawY>location[1]+page.getHeight()-edge)next=Math.max(4,Ui.dp(PlanEditorActivity.this,12));
                if(next==autoScrollDelta)return;
                autoScrollDelta=next;handler.removeCallbacks(autoScroll);if(next!=0)handler.post(autoScroll);
            }
        });
    }
    private int targetIndex(LinearLayout card,float rawY){
        int to=0;
        for(int i=0;i<exerciseList.getChildCount();i++){android.view.View child=exerciseList.getChildAt(i);if(child==card)continue;int[] location=new int[2];child.getLocationOnScreen(location);if(rawY>location[1]+child.getHeight()/2)to++;}
        return to;
    }
    private void moveCard(LinearLayout card,Models.Plan plan,int to){
        int from=exerciseList.indexOfChild(card);if(from<0||to==from)return;exerciseList.removeView(card);exerciseList.addView(card,to);Models.Exercise exercise=plan.exercises.remove(from);plan.exercises.add(to,exercise);updateExerciseNumbers(plan);
    }
    private float cardTopOnScreen(LinearLayout card){int[] listLocation=new int[2];exerciseList.getLocationOnScreen(listLocation);return listLocation[1]+card.getTop();}
    private void placeCardUnderFinger(LinearLayout card,float rawY,float fingerOffsetY){card.setTranslationY(rawY-fingerOffsetY-cardTopOnScreen(card));}
    private void updateExerciseNumbers(Models.Plan plan){for(int i=0;i<exerciseList.getChildCount();i++){TextView label=exerciseLabels.get(exerciseList.getChildAt(i));if(label!=null)label.setText((i+1)+". "+plan.exercises.get(i).name);}}
    private void exerciseDialog(){
        LinearLayout form=Ui.column(this);form.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),0);
        form.addView(Ui.label(this,"Exercise"));
        Spinner catalog=spinner(common);form.addView(catalog);
        form.addView(Ui.label(this,"Custom exercise name"));
        EditText exName=Ui.input(this,"Exercise name");form.addView(exName);
        form.addView(Ui.label(this,"Muscle group"));
        Spinner muscle=spinner(groups);form.addView(muscle);catalog.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){if(pos>0){exName.setText(common[pos]);String g=groupFor(common[pos]);for(int i=0;i<groups.length;i++)if(groups[i].equals(g))muscle.setSelection(i);}}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        int[] sets={3},reps={8};form.addView(Ui.label(this,"Sets"));form.addView(countStepper(sets));form.addView(Ui.label(this,"Target reps"));form.addView(countStepper(reps));
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
            db.addExercise(planId,exName.getText().toString().trim(),sets[0],reps[0],decimal(weight,0),(String)unit.getSelectedItem(),exNotes.getText().toString().trim(),integer(rest,90),integer(timed,0),(String)muscle.getSelectedItem());render();
        }).show();
    }
    private EditText number(String hint,boolean decimal,String initial){EditText e=Ui.input(this,hint);e.setSingleLine();e.setInputType(InputType.TYPE_CLASS_NUMBER|(decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));e.setText(initial);e.setHint(hint);return e;}
    private LinearLayout countStepper(int[] value){LinearLayout row=Ui.row(this);Button minus=Ui.smallButton(this,"− 1"),plus=Ui.smallButton(this,"+ 1");TextView count=Ui.heading(this,String.valueOf(value[0]));count.setGravity(android.view.Gravity.CENTER);Ui.weighted(minus,1);row.addView(minus);Ui.weighted(count,1);row.addView(count);Ui.weighted(plus,1);row.addView(plus);minus.setOnClickListener(v->{value[0]=Math.max(1,value[0]-1);count.setText(String.valueOf(value[0]));});plus.setOnClickListener(v->{value[0]++;count.setText(String.valueOf(value[0]));});return row;}
    private int integer(EditText e,int fallback){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return fallback;}}
    private double decimal(EditText e,double fallback){try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return fallback;}}
    private String groupFor(String name){String n=name.toLowerCase(java.util.Locale.ROOT);if(n.contains("leg")||n.contains("calf")||n.contains("squat")||n.contains("hip")||n.contains("walk")||n.contains("deadlift"))return "Lower body";if(n.contains("ab")||n.contains("plank")||n.contains("back extension"))return "Core";return "Upper body";}
}
