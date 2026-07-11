package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class ExportActivity extends Activity {
    private static final int CSV=10, JSON=11;
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout body=Ui.column(this);Ui.page(this,body);Button back=Ui.smallButton(this,"‹ History");back.setOnClickListener(v->finish());body.addView(back);body.addView(Ui.title(this,"Protect your data"));body.addView(Ui.text(this,"Android removes local app data when an app is uninstalled. Export a full JSON backup before uninstalling or moving phones.",17,Ui.RED));
        Button json=Ui.button(this,"Export full JSON backup",true);json.setOnClickListener(v->choose(JSON,"application/json","workout-tracker-backup-"+LocalDate.now()+".json"));body.addView(json);
        Button csv=Ui.button(this,"Export readable CSV history",false);csv.setOnClickListener(v->choose(CSV,"text/csv","workout-history-"+LocalDate.now()+".csv"));body.addView(csv);
        body.addView(Ui.text(this,"JSON preserves plans, exercise settings, set targets, actual results, effort, notes, and cardio details. CSV is convenient for a spreadsheet.",14,Ui.MUTED));}
    private void choose(int request,String mime,String name){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,request);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;Uri uri=data.getData();try(OutputStream out=getContentResolver().openOutputStream(uri)){String text=request==JSON?json():csv();out.write(text.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"Export saved",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Could not export: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    private String json() throws Exception {JSONObject root=new JSONObject();root.put("format_version",1);root.put("exported_at",java.time.LocalDateTime.now().toString());SQLiteDatabase db=Db.get(this).getReadableDatabase();root.put("plans",table(db,"SELECT * FROM plans"));root.put("exercises",table(db,"SELECT * FROM exercises"));root.put("sessions",table(db,"SELECT * FROM sessions"));root.put("set_results",table(db,"SELECT * FROM set_results"));root.put("cardio",table(db,"SELECT * FROM cardio"));return root.toString(2);}
    private JSONArray table(SQLiteDatabase db,String query) throws Exception {JSONArray rows=new JSONArray();try(Cursor c=db.rawQuery(query,null)){while(c.moveToNext()){JSONObject row=new JSONObject();for(int i=0;i<c.getColumnCount();i++){switch(c.getType(i)){case Cursor.FIELD_TYPE_NULL:row.put(c.getColumnName(i),JSONObject.NULL);break;case Cursor.FIELD_TYPE_INTEGER:row.put(c.getColumnName(i),c.getLong(i));break;case Cursor.FIELD_TYPE_FLOAT:row.put(c.getColumnName(i),c.getDouble(i));break;default:row.put(c.getColumnName(i),c.getString(i));}}rows.put(row);}}return rows;}
    private String csv(){StringBuilder s=new StringBuilder("kind,date,workout,exercise,set,reps,weight,unit,duration_min,distance,intervals,notes\n");SQLiteDatabase db=Db.get(this).getReadableDatabase();try(Cursor c=db.rawQuery("SELECT s.started_at,s.plan_name,r.exercise_name,r.set_number,r.actual_reps,r.actual_weight,s.notes FROM set_results r JOIN sessions s ON s.id=r.session_id WHERE r.status='complete' ORDER BY s.started_at,r.exercise_position,r.set_number",null)){while(c.moveToNext())s.append("strength,").append(q(c.getString(0).substring(0,10))).append(',').append(q(c.getString(1))).append(',').append(q(c.getString(2))).append(',').append(c.getInt(3)).append(',').append(c.getInt(4)).append(',').append(c.getDouble(5)).append(",lb,,,,").append(q(c.getString(6))).append('\n');}try(Cursor c=db.rawQuery("SELECT activity,date,duration_min,distance,unit,intervals,notes FROM cardio ORDER BY date",null)){while(c.moveToNext())s.append("cardio,").append(q(c.getString(1))).append(',').append(q(c.getString(0))).append(",,,,,,").append(c.getDouble(2)).append(',').append(c.getDouble(3)).append(',').append(q(c.getString(5))).append(',').append(q(c.getString(6))).append('\n');}return s.toString();}
    private String q(String value){if(value==null)return "";return '"'+value.replace("\"","\"\"")+'"';}
}
