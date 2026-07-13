package com.miketwo.workouttracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.ArrayList;
import java.util.Locale;

/** Owns the monotonic phase deadline and schedules each cue exactly once. */
public class IntervalService extends Service {
    static final String ACTION_START="com.miketwo.workouttracker.START_INTERVALS",ACTION_TOGGLE="com.miketwo.workouttracker.TOGGLE_INTERVALS",ACTION_TOGGLE_AUDIO="com.miketwo.workouttracker.TOGGLE_INTERVAL_AUDIO",ACTION_STOP="com.miketwo.workouttracker.STOP_INTERVALS",ACTION_UPDATE="com.miketwo.workouttracker.INTERVAL_UPDATE";
    private static final String CHANNEL="run_intervals_silent";
    private final Handler handler=new Handler(Looper.getMainLooper());private final ArrayList<Runnable> cueJobs=new ArrayList<>();
    private int jog,walk,rounds,round=1,lastPublishedSeconds=-1,phaseToken;private boolean jogging=true,paused,focusHeld,soundEnabled=true;private long endAt,remainingPaused;
    private ToneGenerator tones;private PowerManager.WakeLock wakeLock;private AudioManager audio;private AudioFocusRequest focusRequest;
    private final Runnable releaseFocus=()->{if(focusHeld){audio.abandonAudioFocusRequest(focusRequest);focusHeld=false;}};
    private final Runnable clockTick=new Runnable(){@Override public void run(){if(paused)return;long left=Math.max(0,endAt-SystemClock.elapsedRealtime());int shown=seconds(left);if(shown!=lastPublishedSeconds){lastPublishedSeconds=shown;publish(left);}if(left<=0){transition();return;}long toNext=Math.max(20,left-(Math.max(0,shown-1)*1000L)+8);handler.postDelayed(this,toNext);}};
    @Override public void onCreate(){super.onCreate();audio=getSystemService(AudioManager.class);AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->{}).build();tones=new ToneGenerator(AudioManager.STREAM_ALARM,100);PowerManager pm=getSystemService(PowerManager.class);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"WorkoutTracker:Intervals");NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Run/walk intervals",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){String action=intent==null?null:intent.getAction();if(ACTION_START.equals(action)){jog=intent.getIntExtra("jog",360);walk=intent.getIntExtra("walk",120);rounds=intent.getIntExtra("rounds",3);round=1;jogging=true;paused=false;soundEnabled=true;if(!wakeLock.isHeld())wakeLock.acquire((long)(jog+walk)*rounds*1000+300000);startForeground(42,notification(jog*1000L));startPhase(jog);}else if(ACTION_TOGGLE.equals(action))toggle();else if(ACTION_TOGGLE_AUDIO.equals(action)){soundEnabled=!soundEnabled;if(!soundEnabled){handler.removeCallbacks(releaseFocus);releaseFocus.run();}publish(Math.max(0,endAt-SystemClock.elapsedRealtime()));}else if(ACTION_STOP.equals(action))stopTimer(true);return START_NOT_STICKY;}
    private void startPhase(int durationSeconds){startPhaseEndingAt(SystemClock.elapsedRealtime()+durationSeconds*1000L);}
    private void startPhaseEndingAt(long targetEndAt){clearSchedule();phaseToken++;endAt=targetEndAt;long phaseLeft=Math.max(0,endAt-SystemClock.elapsedRealtime());int wholeRemaining=(int)(phaseLeft/1000);lastPublishedSeconds=-1;publish(phaseLeft);for(int remaining=Math.min(3,wholeRemaining);remaining>=1;remaining--){final int token=phaseToken;long delay=Math.max(0,endAt-SystemClock.elapsedRealtime()-remaining*1000L);Runnable cue=()->{if(!paused&&token==phaseToken)warningCue();};cueJobs.add(cue);handler.postDelayed(cue,delay);}handler.post(clockTick);}
    private void warningCue(){if(!soundEnabled)return;requestFocus();tones.startTone(ToneGenerator.TONE_PROP_BEEP,100);}
    private void transition(){if(soundEnabled){requestFocus();tones.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT,400);}vibrate();if(!jogging){round++;if(round>rounds){broadcast(0,true);stopTimer(false);return;}}jogging=!jogging;handler.removeCallbacks(releaseFocus);if(soundEnabled)handler.postDelayed(releaseFocus,550);startPhase(jogging?jog:walk);}
    private void requestFocus(){if(!focusHeld&&audio.requestAudioFocus(focusRequest)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED)focusHeld=true;}
    private void toggle(){if(paused){paused=false;endAt=SystemClock.elapsedRealtime()+remainingPaused;if(!wakeLock.isHeld())wakeLock.acquire(remainingPaused+300000);resumePhase();}else{paused=true;remainingPaused=Math.max(0,endAt-SystemClock.elapsedRealtime());clearSchedule();handler.removeCallbacks(releaseFocus);releaseFocus.run();if(wakeLock.isHeld())wakeLock.release();}publish(paused?remainingPaused:Math.max(0,endAt-SystemClock.elapsedRealtime()));}
    private void resumePhase(){startPhaseEndingAt(SystemClock.elapsedRealtime()+remainingPaused);}
    private void clearSchedule(){handler.removeCallbacks(clockTick);for(Runnable job:cueJobs)handler.removeCallbacks(job);cueJobs.clear();}
    private void publish(long left){getSystemService(NotificationManager.class).notify(42,notification(left));broadcast(left,false);}
    private void broadcast(long left,boolean done){Intent i=new Intent(ACTION_UPDATE).setPackage(getPackageName());i.putExtra("remaining",left);i.putExtra("end_at",endAt);i.putExtra("phase",jogging?"Jog":"Walk");i.putExtra("round",round);i.putExtra("rounds",rounds);i.putExtra("paused",paused);i.putExtra("sound_enabled",soundEnabled);i.putExtra("done",done);sendBroadcast(i);}
    private Notification notification(long left){Intent open=new Intent(this,IntervalActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);PendingIntent content=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent toggle=PendingIntent.getService(this,2,new Intent(this,IntervalService.class).setAction(ACTION_TOGGLE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,3,new Intent(this,IntervalService.class).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle((paused?"Paused — ":"")+(jogging?"Jog":"Walk")+" · round "+round+" of "+rounds).setContentText(format(left)+" remaining").setContentIntent(content).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,paused?"Resume":"Pause",toggle).build()).addAction(new Notification.Action.Builder(null,"End",stop).build()).build();}
    private int seconds(long ms){return (int)((ms+999)/1000);}private String format(long ms){long s=seconds(ms);return String.format(Locale.US,"%d:%02d",s/60,s%60);}
    private void stopTimer(boolean cancelled){clearSchedule();handler.removeCallbacks(releaseFocus);releaseFocus.run();if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();if(cancelled)broadcast(0,true);}
    private void vibrate(){Vibrator v=getSystemService(Vibrator.class);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(180,VibrationEffect.DEFAULT_AMPLITUDE));}
    @Override public void onDestroy(){clearSchedule();handler.removeCallbacks(releaseFocus);releaseFocus.run();if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();if(tones!=null)tones.release();super.onDestroy();}@Override public IBinder onBind(Intent intent){return null;}
}
