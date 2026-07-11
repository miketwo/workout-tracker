package com.miketwo.workouttracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class IntervalActivity extends Activity {
    private LinearLayout body;private TextView phase,clock,progress;private EditText jog,walk,rounds;private boolean running;private long startedAt;private int totalSeconds;
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(!IntervalService.ACTION_UPDATE.equals(i.getAction()))return;if(i.getBooleanExtra("done",false)){running=false;showComplete();return;}showRunning(i.getStringExtra("phase"),i.getLongExtra("remaining",0),i.getIntExtra("round",1),i.getIntExtra("rounds",1),i.getBooleanExtra("paused",false));}};
    @Override public void onCreate(Bundle state){super.onCreate(state);showSetup();}
    @SuppressLint("InlinedApi") @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter(IntervalService.ACTION_UPDATE);registerReceiver(updates,f,RECEIVER_NOT_EXPORTED);}
    @Override protected void onStop(){unregisterReceiver(updates);super.onStop();}
    private void base(){body=Ui.column(this);Ui.page(this,body);}
    private void showSetup(){base();Button back=Ui.smallButton(this,"‹ Cardio");back.setOnClickListener(v->finish());body.addView(back);body.addView(Ui.title(this,"Run/walk intervals"));body.addView(Ui.text(this,"Cues will continue over music and with the screen locked. You’ll hear a warning at five seconds and a distinct transition tone.",16,Ui.MUTED));body.addView(Ui.label(this,"Jog minutes"));jog=number("6");body.addView(jog);body.addView(Ui.label(this,"Walk minutes"));walk=number("2");body.addView(walk);body.addView(Ui.label(this,"Rounds"));rounds=number("3");body.addView(rounds);Button start=Ui.button(this,"Start intervals",true);start.setOnClickListener(v->start());body.addView(start);}
    private EditText number(String value){EditText e=Ui.input(this,"");e.setSingleLine();e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setText(value);return e;}
    private void start(){int j=(int)(num(jog)*60),w=(int)(num(walk)*60),r=(int)num(rounds);if(j<1||w<1||r<1)return;if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);totalSeconds=(j+w)*r;startedAt=System.currentTimeMillis();running=true;Intent i=new Intent(this,IntervalService.class).setAction(IntervalService.ACTION_START).putExtra("jog",j).putExtra("walk",w).putExtra("rounds",r);startForegroundService(i);showRunning("Jog",j*1000L,1,r,false);}
    private void showRunning(String current,long remaining,int round,int roundCount,boolean paused){base();phase=Ui.title(this,paused?"Paused":current);phase.setGravity(Gravity.CENTER);body.addView(phase);clock=Ui.title(this,format(remaining));clock.setTextSize(72);clock.setGravity(Gravity.CENTER);body.addView(clock);progress=Ui.heading(this,"Round "+round+" of "+roundCount);progress.setGravity(Gravity.CENTER);body.addView(progress);body.addView(Ui.text(this,"You can lock the screen or switch to Spotify. The timer will keep running.",15,Ui.MUTED));Button toggle=Ui.button(this,paused?"Resume":"Pause",true);toggle.setOnClickListener(v->startService(new Intent(this,IntervalService.class).setAction(IntervalService.ACTION_TOGGLE)));body.addView(toggle);Button end=Ui.button(this,"End intervals",false);end.setOnClickListener(v->startService(new Intent(this,IntervalService.class).setAction(IntervalService.ACTION_STOP)));body.addView(end);}
    private void showComplete(){base();long elapsed=startedAt>0?(System.currentTimeMillis()-startedAt)/1000:totalSeconds;body.addView(Ui.title(this,"Intervals complete"));body.addView(Ui.text(this,"Nice work. Add the treadmill or Strava distance to finish the log.",17,Ui.MUTED));Button log=Ui.button(this,"Log run totals",true);log.setOnClickListener(v->{Intent i=new Intent(this,CardioActivity.class).putExtra("activity","Run").putExtra("duration",elapsed/60.0).putExtra("intervals","Guided run/walk intervals");startActivity(i);finish();});body.addView(log);Button done=Ui.button(this,"Done without logging",false);done.setOnClickListener(v->finish());body.addView(done);}
    private double num(EditText e){try{return Double.parseDouble(e.getText().toString());}catch(Exception x){return 0;}}private String format(long ms){long s=(ms+999)/1000;return String.format(Locale.US,"%d:%02d",s/60,s%60);}
}
