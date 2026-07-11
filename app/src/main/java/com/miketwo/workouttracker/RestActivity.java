package com.miketwo.workouttracker;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class RestActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());private long endAt;private TextView clock;private boolean ended;
    private final Runnable tick=new Runnable(){@Override public void run(){long left=Math.max(0,endAt-System.currentTimeMillis());clock.setText(format(left));if(left<=0)complete();else handler.postDelayed(this,200);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);int seconds=getIntent().getIntExtra("seconds",90);endAt=state==null?System.currentTimeMillis()+seconds*1000L:state.getLong("end_at");
        LinearLayout body=Ui.column(this);body.setGravity(Gravity.CENTER);setContentView(body);body.addView(Ui.label(this,"Rest"));clock=Ui.title(this,format(seconds*1000L));clock.setTextSize(72);clock.setGravity(Gravity.CENTER);body.addView(clock);
        String record=getIntent().getStringExtra("record");if(record!=null){TextView pr=Ui.heading(this,"Personal best ✦\n"+record);pr.setTextColor(Ui.FOREST);pr.setGravity(Gravity.CENTER);body.addView(pr);}
        body.addView(Ui.text(this,"Next up",14,Ui.MUTED));TextView next=Ui.heading(this,getIntent().getStringExtra("next"));next.setGravity(Gravity.CENTER);body.addView(next);Button skip=Ui.button(this,"Skip rest",false);skip.setOnClickListener(v->finish());body.addView(skip);
    }
    @Override protected void onResume(){super.onResume();handler.post(tick);}@Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}@Override protected void onSaveInstanceState(Bundle out){out.putLong("end_at",endAt);super.onSaveInstanceState(out);}
    private String format(long ms){long total=(ms+999)/1000;return String.format(Locale.US,"%d:%02d",total/60,total%60);}
    private void complete(){if(ended)return;ended=true;Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(120,VibrationEffect.DEFAULT_AMPLITUDE));finish();}
}
