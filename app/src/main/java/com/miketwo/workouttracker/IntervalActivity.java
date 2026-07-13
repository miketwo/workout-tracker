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
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** A stable interval screen: service events update state, not the entire layout. */
public class IntervalActivity extends Activity {
    private LinearLayout body;private TextView phase,clock,progress,jogValue,walkValue,roundsValue;private Button toggle,audio;private IntervalMotionView motion;private boolean running,paused;private int jogSeconds=360,walkSeconds=120,roundCount=3;private long startedAt,totalSeconds,endAt;private final Handler clockHandler=new Handler(Looper.getMainLooper());
    private final Runnable clockTick=new Runnable(){public void run(){if(!running||paused)return;long left=Math.max(0,endAt-SystemClock.elapsedRealtime());clock.setText(format(left));if(left>0){int displayed=(int)((left+999)/1000);clockHandler.postDelayed(this,Math.max(20,left-Math.max(0,displayed-1)*1000L+8));}}};
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(!IntervalService.ACTION_UPDATE.equals(i.getAction()))return;if(i.getBooleanExtra("done",false)){running=false;clockHandler.removeCallbacks(clockTick);showComplete();return;}applyState(i.getStringExtra("phase"),i.getLongExtra("remaining",0),i.getLongExtra("end_at",0),i.getIntExtra("round",1),i.getIntExtra("rounds",1),i.getBooleanExtra("paused",false),i.getBooleanExtra("sound_enabled",true));}};
    @Override public void onCreate(Bundle state){super.onCreate(state);showSetup();}
    @SuppressLint("InlinedApi") @Override protected void onStart(){super.onStart();registerReceiver(updates,new IntentFilter(IntervalService.ACTION_UPDATE),RECEIVER_NOT_EXPORTED);}
    @Override protected void onStop(){unregisterReceiver(updates);clockHandler.removeCallbacks(clockTick);super.onStop();}
    private void base(){body=Ui.column(this);Ui.page(this,body);}
    private void showSetup(){running=false;base();Button back=Ui.smallButton(this,"‹ Cardio");back.setOnClickListener(v->finish());body.addView(back);body.addView(Ui.title(this,"Run/walk intervals"));body.addView(Ui.text(this,"Three evenly spaced countdown cues lead into a distinct transition alarm.",16,Ui.MUTED));durationStepper("Jog time",true);durationStepper("Walk time",false);roundStepper();Button start=Ui.button(this,"Start intervals",true);start.setOnClickListener(v->start());body.addView(start);}
    private void durationStepper(String label,boolean jog){body.addView(Ui.label(this,label));LinearLayout row=Ui.row(this);Button minus=Ui.smallButton(this,"− 0:30"),plus=Ui.smallButton(this,"+ 0:30");TextView value=Ui.heading(this,formatSeconds(jog?jogSeconds:walkSeconds));value.setGravity(Gravity.CENTER);Ui.weighted(minus,1);row.addView(minus);Ui.weighted(value,1);row.addView(value);Ui.weighted(plus,1);row.addView(plus);body.addView(row);if(jog)jogValue=value;else walkValue=value;minus.setOnClickListener(v->adjustDuration(jog,-30));plus.setOnClickListener(v->adjustDuration(jog,30));}
    private void roundStepper(){body.addView(Ui.label(this,"Rounds"));LinearLayout row=Ui.row(this);Button minus=Ui.smallButton(this,"− 1"),plus=Ui.smallButton(this,"+ 1");roundsValue=Ui.heading(this,String.valueOf(roundCount));roundsValue.setGravity(Gravity.CENTER);Ui.weighted(minus,1);row.addView(minus);Ui.weighted(roundsValue,1);row.addView(roundsValue);Ui.weighted(plus,1);row.addView(plus);body.addView(row);minus.setOnClickListener(v->{roundCount=Math.max(1,roundCount-1);roundsValue.setText(String.valueOf(roundCount));});plus.setOnClickListener(v->{roundCount++;roundsValue.setText(String.valueOf(roundCount));});}
    private void adjustDuration(boolean jog,int change){if(jog){jogSeconds=Math.max(30,jogSeconds+change);jogValue.setText(formatSeconds(jogSeconds));}else{walkSeconds=Math.max(30,walkSeconds+change);walkValue.setText(formatSeconds(walkSeconds));}}
    private void start(){totalSeconds=(long)(jogSeconds+walkSeconds)*roundCount;startedAt=System.currentTimeMillis();running=true;showRunning();Intent i=new Intent(this,IntervalService.class).setAction(IntervalService.ACTION_START).putExtra("jog",jogSeconds).putExtra("walk",walkSeconds).putExtra("rounds",roundCount);startForegroundService(i);applyState("Jog",jogSeconds*1000L,SystemClock.elapsedRealtime()+jogSeconds*1000L,1,roundCount,false,true);}
    private void showRunning(){base();phase=Ui.title(this,"Jog");phase.setGravity(Gravity.CENTER);body.addView(phase);motion=new IntervalMotionView(this);body.addView(motion,new LinearLayout.LayoutParams(-1,Ui.dp(this,220)));clock=Ui.title(this,"0:00");clock.setTextSize(72);clock.setGravity(Gravity.CENTER);body.addView(clock);progress=Ui.heading(this,"");progress.setGravity(Gravity.CENTER);body.addView(progress);audio=Ui.smallButton(this,"🔊 Audio on");audio.setOnClickListener(v->startService(new Intent(this,IntervalService.class).setAction(IntervalService.ACTION_TOGGLE_AUDIO)));body.addView(audio);body.addView(Ui.text(this,"The timer continues with the screen locked or while another app is open.",15,Ui.MUTED));toggle=Ui.button(this,"Pause",true);toggle.setOnClickListener(v->startService(new Intent(this,IntervalService.class).setAction(IntervalService.ACTION_TOGGLE)));body.addView(toggle);Button end=Ui.button(this,"End intervals",false);end.setOnClickListener(v->startService(new Intent(this,IntervalService.class).setAction(IntervalService.ACTION_STOP)));body.addView(end);}
    private void applyState(String current,long remaining,long publishedEndAt,int round,int roundCount,boolean isPaused,boolean soundEnabled){if(!running)return;paused=isPaused;endAt=publishedEndAt>0?publishedEndAt:SystemClock.elapsedRealtime()+remaining;phase.setText(paused?"Paused":current);progress.setText("Round "+round+" of "+roundCount);motion.setJogging("Jog".equals(current));motion.setPaused(paused);audio.setText(soundEnabled?"🔊 Audio on":"🔇 Audio off");toggle.setText(paused?"Resume":"Pause");clockHandler.removeCallbacks(clockTick);clock.setText(format(remaining));if(!paused)clockHandler.post(clockTick);}
    private void showComplete(){base();long elapsed=startedAt>0?(System.currentTimeMillis()-startedAt)/1000:totalSeconds;body.addView(Ui.title(this,"Intervals complete"));body.addView(Ui.text(this,"Nice work. Add the treadmill or Strava distance to finish the log.",17,Ui.MUTED));Button log=Ui.button(this,"Log run totals",true);log.setOnClickListener(v->{Intent i=new Intent(this,CardioActivity.class).putExtra("activity","Run").putExtra("duration",elapsed/60.0).putExtra("intervals","Guided run/walk intervals");startActivity(i);finish();});body.addView(log);Button done=Ui.button(this,"Done without logging",false);done.setOnClickListener(v->finish());body.addView(done);}
    private String format(long ms){long s=(ms+999)/1000;return String.format(Locale.US,"%d:%02d",s/60,s%60);}private String formatSeconds(int seconds){return String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);}
    @Override protected void onDestroy(){clockHandler.removeCallbacks(clockTick);super.onDestroy();}
}
