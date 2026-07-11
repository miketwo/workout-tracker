package com.miketwo.workouttracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.Locale;

public class IntervalService extends Service {
    static final String ACTION_START="com.miketwo.workouttracker.START_INTERVALS",ACTION_TOGGLE="com.miketwo.workouttracker.TOGGLE_INTERVALS",ACTION_STOP="com.miketwo.workouttracker.STOP_INTERVALS",ACTION_UPDATE="com.miketwo.workouttracker.INTERVAL_UPDATE";
    private static final String CHANNEL="run_intervals";private final Handler handler=new Handler(Looper.getMainLooper());private int jog,walk,rounds,round=1;private boolean jogging=true,paused=false,warned=false;private long endAt,remainingPaused;private ToneGenerator tones;private PowerManager.WakeLock wakeLock;
    private final Runnable tick=new Runnable(){@Override public void run(){if(paused)return;long left=Math.max(0,endAt-SystemClock.elapsedRealtime());if(left<=5000&&!warned&&left>0){warned=true;tones.startTone(ToneGenerator.TONE_PROP_BEEP,140);}if(left<=0){transition();return;}publish(left);handler.postDelayed(this,250);}};
    @Override public void onCreate(){super.onCreate();tones=new ToneGenerator(AudioManager.STREAM_MUSIC,85);PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"WorkoutTracker:Intervals");NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Run/walk intervals",NotificationManager.IMPORTANCE_HIGH));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){String action=intent==null?null:intent.getAction();if(ACTION_START.equals(action)){jog=intent.getIntExtra("jog",360);walk=intent.getIntExtra("walk",120);rounds=intent.getIntExtra("rounds",3);round=1;jogging=true;paused=false;startPhase(jog);if(!wakeLock.isHeld())wakeLock.acquire((long)(jog+walk)*rounds*1000+300000);startForeground(42,notification(jog*1000L));}else if(ACTION_TOGGLE.equals(action))toggle();else if(ACTION_STOP.equals(action))stopTimer(true);return START_NOT_STICKY;}
    private void startPhase(int seconds){warned=false;endAt=SystemClock.elapsedRealtime()+seconds*1000L;handler.removeCallbacks(tick);handler.post(tick);}
    private void transition(){tones.startTone(jogging?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP2,450);vibrate();if(!jogging){round++;if(round>rounds){broadcast(0,true);stopTimer(false);return;}}jogging=!jogging;startPhase(jogging?jog:walk);}
    private void toggle(){if(paused){paused=false;endAt=SystemClock.elapsedRealtime()+remainingPaused;if(!wakeLock.isHeld())wakeLock.acquire(remainingPaused+300000);handler.post(tick);}else{paused=true;remainingPaused=Math.max(0,endAt-SystemClock.elapsedRealtime());handler.removeCallbacks(tick);if(wakeLock.isHeld())wakeLock.release();}publish(paused?remainingPaused:Math.max(0,endAt-SystemClock.elapsedRealtime()));}
    private void publish(long left){getSystemService(NotificationManager.class).notify(42,notification(left));broadcast(left,false);}
    private void broadcast(long left,boolean done){Intent i=new Intent(ACTION_UPDATE).setPackage(getPackageName());i.putExtra("remaining",left);i.putExtra("phase",jogging?"Jog":"Walk");i.putExtra("round",round);i.putExtra("rounds",rounds);i.putExtra("paused",paused);i.putExtra("done",done);sendBroadcast(i);}
    private Notification notification(long left){Intent open=new Intent(this,IntervalActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);PendingIntent content=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent toggle=PendingIntent.getService(this,2,new Intent(this,IntervalService.class).setAction(ACTION_TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,3,new Intent(this,IntervalService.class).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);String time=String.format(Locale.US,"%d:%02d",(left/1000)/60,(left/1000)%60);return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle((paused?"Paused — ":"")+(jogging?"Jog":"Walk")+" · round "+round+" of "+rounds).setContentText(time+" remaining").setContentIntent(content).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,paused?"Resume":"Pause",toggle).build()).addAction(new Notification.Action.Builder(null,"End",stop).build()).build();}
    private void stopTimer(boolean cancelled){handler.removeCallbacks(tick);if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();if(cancelled)broadcast(0,true);}
    private void vibrate(){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(180,VibrationEffect.DEFAULT_AMPLITUDE));}
    @Override public void onDestroy(){handler.removeCallbacks(tick);if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();if(tones!=null)tones.release();super.onDestroy();}@Override public IBinder onBind(Intent intent){return null;}
}
