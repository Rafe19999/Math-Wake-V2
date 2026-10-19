package com.mathwake.app;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

final class AlarmScheduler {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("alarm",Context.MODE_PRIVATE);}
 static PendingIntent alarmIntent(Context c){return PendingIntent.getBroadcast(c,1001,new Intent(c,AlarmReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static long nextTime(int hour,int minute){Calendar now=Calendar.getInstance(),next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,hour);next.set(Calendar.MINUTE,minute);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);if(!next.after(now))next.add(Calendar.DATE,1);return next.getTimeInMillis();}
 static void schedule(Context c,int hour,int minute,String mode){
  long when=nextTime(hour,minute);
  AlarmManager mgr=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  Intent show=new Intent(c,ChallengeActivity.class).putExtra("mode",mode);
  PendingIntent showPi=PendingIntent.getActivity(c,1002,show,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  mgr.setAlarmClock(new AlarmManager.AlarmClockInfo(when,showPi),alarmIntent(c));
  prefs(c).edit().putBoolean("enabled",true).putInt("hour",hour).putInt("minute",minute).putString("mode",mode).putLong("next",when).apply();
 }
 static void cancel(Context c){((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent(c));prefs(c).edit().putBoolean("enabled",false).remove("next").apply();}
}
