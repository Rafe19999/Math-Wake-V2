package com.mathwake.app;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.hardware.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;

public class AlarmService extends Service implements SensorEventListener {
 public static final String ACTION_PROGRESS="com.mathwake.app.STEP_PROGRESS";
 private static final String CHANNEL="mathwake_alarm";
 private MediaPlayer player;
 private SensorManager sensorManager;
 private Sensor stepDetector;
 private boolean useCumulativeCounter=false;
 private float counterBaseline=Float.NaN;
 private PowerManager.WakeLock wakeLock;
 private int steps=0;
 private boolean walking=false;
 @Override public void onCreate(){super.onCreate();
   String mode=AlarmScheduler.prefs(this).getString("active_mode",AlarmScheduler.prefs(this).getString("mode","math"));walking="walk".equals(mode);
   NotificationManager nm=getSystemService(NotificationManager.class);
   NotificationChannel ch=new NotificationChannel(CHANNEL,"MathWake alarms",NotificationManager.IMPORTANCE_HIGH);ch.setSound(null,null);nm.createNotificationChannel(ch);
   Intent screen=new Intent(this,ChallengeActivity.class).putExtra("mode",mode).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   PendingIntent pi=PendingIntent.getActivity(this,200,screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("MathWake alarm ringing").setContentText(walking?"Walk 40 steps to dismiss":"Solve 3 maths problems to dismiss").setCategory(Notification.CATEGORY_ALARM).setOngoing(true).setContentIntent(pi).setFullScreenIntent(pi,true).build();
   if(Build.VERSION.SDK_INT>=29)startForeground(200,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);else startForeground(200,n);
   String chosen = AlarmScheduler.prefs(this).getString("ringtone_uri", "");
   Uri custom = chosen.isEmpty() ? null : Uri.parse(chosen);
   boolean started = false;
   if (custom != null) {
      started = startAlarmAudio(custom);
      if (!started) Log.w("MathWake", "Selected audio unavailable, using default alarm");
   }
   if (!started) {
      Uri standard = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
      if (standard == null) standard = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      startAlarmAudio(standard);
   }
   if(walking){
      steps=0;AlarmScheduler.prefs(this).edit().putInt("steps",0).apply();
      PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
      if(pm!=null){wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MathWake:walking-alarm");wakeLock.acquire(30*60*1000L);}
      sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
      if(sensorManager!=null){
          Sensor counter=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
          Sensor detector=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
          stepDetector=counter!=null?counter:detector;
          useCumulativeCounter=counter!=null;
          if(stepDetector!=null){try{
             boolean ok=sensorManager.registerListener(this,stepDetector,SensorManager.SENSOR_DELAY_FASTEST,0);
             if(!ok) Log.e("MathWake","Sensor listener registration failed");
          }catch(SecurityException e){Log.e("MathWake","Physical Activity permission missing",e);}}
      }
   }
 }
 private boolean startAlarmAudio(Uri uri) {
    if (uri == null) return false;
    MediaPlayer attempt = new MediaPlayer();
    try {
       attempt.setAudioAttributes(new AudioAttributes.Builder()
           .setUsage(AudioAttributes.USAGE_ALARM)
           .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
       attempt.setDataSource(this, uri);
       attempt.setLooping(true);
       attempt.prepare();
       attempt.start();
       player = attempt;
       return true;
    } catch(Exception e) {
       Log.e("MathWake", "Could not play alarm audio: " + uri, e);
       attempt.release();
       return false;
    }
 }
 @Override public void onSensorChanged(SensorEvent event){
   if(walking && stepDetector!=null && event.sensor.getType()==stepDetector.getType()){
      if(useCumulativeCounter){
          float total=event.values[0];
          if(Float.isNaN(counterBaseline)){counterBaseline=total;return;}
          if(total<counterBaseline){counterBaseline=total;return;}
          steps=Math.max(steps,(int)(total-counterBaseline));
      }else steps++;
      AlarmScheduler.prefs(this).edit().putInt("steps",steps).apply();
      Intent update=new Intent(ACTION_PROGRESS).setPackage(getPackageName()).putExtra("steps",steps);sendBroadcast(update);
      if(steps>=40)stopSelf();
   }
 }
 @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
    if(intent!=null && "STOP_AFTER_MATH".equals(intent.getAction()) && !walking){stopSelf();return START_NOT_STICKY;}
    return START_NOT_STICKY;
 }
 @Override public void onDestroy(){if(sensorManager!=null)sensorManager.unregisterListener(this);if(wakeLock!=null && wakeLock.isHeld())wakeLock.release();if(player!=null){try{player.stop();}catch(Exception ignored){}player.release();player=null;}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
