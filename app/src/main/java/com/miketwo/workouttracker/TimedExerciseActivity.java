package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class TimedExerciseActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());private int target;private long started,endAt,pausedLeft;private boolean paused;private TextView clock;private Button toggle;
    private final Runnable tick=new Runnable(){@Override public void run(){long left=Math.max(0,endAt-SystemClock.elapsedRealtime());clock.setText(format(left));if(left<=0)complete(target);else handler.postDelayed(this,100);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);target=getIntent().getIntExtra("seconds",30);started=SystemClock.elapsedRealtime();endAt=started+target*1000L;LinearLayout body=Ui.column(this);body.setGravity(Gravity.CENTER);Ui.screen(this,body);String exercise=getIntent().getStringExtra("exercise");body.addView(Ui.exerciseHeader(this,exercise,exercise,22));clock=Ui.title(this,format(target*1000L));clock.setTextSize(72);clock.setGravity(Gravity.CENTER);body.addView(clock);toggle=Ui.button(this,"Pause",true);toggle.setOnClickListener(v->toggle());body.addView(toggle);Button early=Ui.button(this,"Finish early",false);early.setOnClickListener(v->{long left=paused?pausedLeft:Math.max(0,endAt-SystemClock.elapsedRealtime());complete((int)((target*1000L-left)/1000));});body.addView(early);}
    @Override protected void onResume(){super.onResume();if(!paused)handler.post(tick);}@Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
    private void toggle(){if(paused){paused=false;endAt=SystemClock.elapsedRealtime()+pausedLeft;toggle.setText(R.string.pause);handler.post(tick);}else{paused=true;pausedLeft=Math.max(0,endAt-SystemClock.elapsedRealtime());toggle.setText(R.string.resume);handler.removeCallbacks(tick);}}
    private void complete(int elapsed){handler.removeCallbacks(tick);Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(120,VibrationEffect.DEFAULT_AMPLITUDE));setResult(RESULT_OK,new Intent().putExtra("elapsed",elapsed));finish();}
    private String format(long ms){long s=(ms+999)/1000;return String.format(Locale.US,"%d:%02d",s/60,s%60);}
}
