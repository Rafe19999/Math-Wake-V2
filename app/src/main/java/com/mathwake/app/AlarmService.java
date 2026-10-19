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
   try{Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());player.setDataSource(this,uri);player.setLooping(true);player.prepare();player.start();}
   catch(Exception e){Log.e("MathWake","Cannot play alarm sound",e);}
   if(walking){
      steps=0;AlarmScheduler.prefs(this).edit().putInt("steps",0).apply();
      sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
      if(sensorManager!=null){stepDetector=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
          if(stepDetector!=null){try{sensorManager.registerListener(this,stepDetector,SensorManager.SENSOR_DELAY_NORMAL);}catch(SecurityException e){Log.e("MathWake","Activity recognition permission missing",e);}}
      }
   }
 }
 @Override public void onSensorChanged(SensorEvent event){
   if(walking && event.sensor.getType()==Sensor.TYPE_STEP_DETECTOR){
      // TYPE_STEP_DETECTOR events represent individual detected steps.
      steps++;
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
 @Override public void onDestroy(){if(sensorManager!=null)sensorManager.unregisterListener(this);if(player!=null){try{player.stop();}catch(Exception ignored){}player.release();player=null;}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
