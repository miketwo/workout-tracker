package com.miketwo.workouttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReviewActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    private void render(){
        LinearLayout body=Ui.column(this);Ui.page(this,body);Button back=Ui.smallButton(this,"‹ Home");back.setOnClickListener(v->Ui.openHome(this));body.addView(back);body.addView(Ui.title(this,"History & progress"));
        android.database.sqlite.SQLiteDatabase db=Db.get(this).getReadableDatabase();
        int workouts=scalarInt(db,"SELECT COUNT(*) FROM sessions WHERE status<>'active'");int sets=scalarInt(db,"SELECT COUNT(*) FROM set_results WHERE status='complete'");double volume=scalarDouble(db,"SELECT COALESCE(SUM(actual_reps*actual_weight),0) FROM set_results WHERE status='complete'");double cardioMin=scalarDouble(db,"SELECT COALESCE(SUM(duration_min),0) FROM cardio");
        LinearLayout stats=Ui.card(this);stats.addView(Ui.label(this,"All-time totals"));stats.addView(Ui.heading(this,workouts+" strength workouts  •  "+sets+" sets"));stats.addView(Ui.text(this,WorkoutMath.formatWeight(volume)+" lb total lifting volume",17,Ui.FOREST));stats.addView(Ui.text(this,WorkoutMath.formatWeight(cardioMin)+" cardio minutes",16,Ui.MUTED));body.addView(stats);
        int week=scalarInt(db,"SELECT COUNT(DISTINCT day) FROM (SELECT substr(started_at,1,10) day FROM sessions WHERE status<>'active' UNION SELECT date day FROM cardio) WHERE day>=date('now','-6 days')");int month=scalarInt(db,"SELECT COUNT(DISTINCT day) FROM (SELECT substr(started_at,1,10) day FROM sessions WHERE status<>'active' UNION SELECT date day FROM cardio) WHERE day>=date('now','start of month')");LinearLayout consistency=Ui.card(this);consistency.addView(Ui.label(this,"Consistency"));consistency.addView(Ui.heading(this,week+" active day"+(week==1?"":"s")+" this week"));consistency.addView(Ui.text(this,month+" active days this month. Every completed session counts.",16,Ui.MUTED));body.addView(consistency);
        body.addView(Ui.heading(this,"Strength by exercise"));
        try(Cursor c=db.rawQuery("SELECT exercise_name,COUNT(DISTINCT session_id),MAX(actual_weight),SUM(actual_reps*actual_weight) FROM set_results WHERE status='complete' GROUP BY exercise_name ORDER BY SUM(actual_reps*actual_weight) DESC",null)){while(c.moveToNext()){LinearLayout card=Ui.card(this);card.addView(Ui.heading(this,c.getString(0)));card.addView(Ui.text(this,c.getInt(1)+" session"+(c.getInt(1)==1?"":"s")+"  •  heaviest "+WorkoutMath.formatWeight(c.getDouble(2))+" lb",15,Ui.MUTED));card.addView(Ui.text(this,WorkoutMath.formatWeight(c.getDouble(3))+" lb total volume",17,Ui.FOREST));body.addView(card);}}
        LinearLayout recent=Ui.column(this);recent.setPadding(Ui.dp(this,16),Ui.dp(this,10),Ui.dp(this,16),Ui.dp(this,10));recent.setBackground(Ui.bg(Ui.MINT,Ui.dp(this,18)));LinearLayout.LayoutParams recentParams=new LinearLayout.LayoutParams(-1,-2);recentParams.setMargins(0,Ui.dp(this,14),0,Ui.dp(this,8));recent.setLayoutParams(recentParams);
        recent.addView(Ui.heading(this,"Recent activity"));
        recent.addView(Ui.text(this,"Long press a workout to delete it.",14,Ui.MUTED));
        boolean any=false;
        try(Cursor c=db.rawQuery("SELECT s.id,plan_name,started_at,status,(SELECT COUNT(*) FROM set_results r WHERE r.session_id=s.id),(SELECT COALESCE(SUM(actual_reps*actual_weight),0) FROM set_results r WHERE r.session_id=s.id) FROM sessions s WHERE status<>'active' ORDER BY id DESC LIMIT 30",null)){
            while(c.moveToNext()){any=true;long sessionId=c.getLong(0);String name=c.getString(1);LinearLayout card=Ui.card(this);card.addView(Ui.heading(this,formatDate(c.getString(2))));card.addView(Ui.text(this,"Strength  •  "+name+"  •  "+c.getString(3),14,Ui.MUTED));card.addView(Ui.text(this,c.getInt(4)+" set entries  •  "+WorkoutMath.formatWeight(c.getDouble(5))+" lb volume",16,Ui.INK));card.setOnLongClickListener(v->{confirmDeleteSession(sessionId,name);return true;});recent.addView(card);}
        }
        try(Cursor c=db.rawQuery("SELECT id,activity,date,duration_min,distance,unit,intervals,laps FROM cardio ORDER BY id DESC LIMIT 30",null)){
            while(c.moveToNext()){any=true;long cardioId=c.getLong(0);String activity=c.getString(1);LinearLayout card=Ui.card(this);card.addView(Ui.heading(this,formatDay(c.getString(2))));card.addView(Ui.text(this,activity+"  •  "+WorkoutMath.formatWeight(c.getDouble(3))+" minutes",14,Ui.MUTED));String detail=c.getInt(7)>0?c.getInt(7)+" lengths  •  "+WorkoutMath.formatWeight(c.getDouble(4))+" "+c.getString(5):WorkoutMath.formatWeight(c.getDouble(4))+" "+c.getString(5)+(c.getString(6).isBlank()?"":"  •  "+c.getString(6));card.addView(Ui.text(this,detail,16,Ui.INK));card.setOnLongClickListener(v->{confirmDeleteCardio(cardioId,activity);return true;});recent.addView(card);}
        }
        if(!any)recent.addView(Ui.text(this,"Completed workouts will appear here.",17,Ui.MUTED));
        body.addView(recent);
        Button export=Ui.button(this,"Back up or export data",false);export.setOnClickListener(v->startActivity(new android.content.Intent(this,ExportActivity.class)));body.addView(export);
    }
    private void confirmDeleteSession(long id,String name){new AlertDialog.Builder(this).setTitle("Delete workout?").setMessage("Delete "+name+" and all of its recorded sets?").setNegativeButton("Cancel",null).setPositiveButton("Delete workout",(d,w)->{Db.get(this).deleteSession(id);render();}).show();}
    private void confirmDeleteCardio(long id,String activity){new AlertDialog.Builder(this).setTitle("Delete workout?").setMessage("Delete this "+activity.toLowerCase(java.util.Locale.ROOT)+" entry?").setNegativeButton("Cancel",null).setPositiveButton("Delete workout",(d,w)->{Db.get(this).deleteCardio(id);render();}).show();}
    private int scalarInt(android.database.sqlite.SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getInt(0):0;}}
    private double scalarDouble(android.database.sqlite.SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getDouble(0):0;}}
    private String formatDate(String iso){try{return LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("MMM d, yyyy"));}catch(Exception e){return iso;}}
    private String formatDay(String iso){try{return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM d, yyyy"));}catch(Exception e){return iso;}}
}
