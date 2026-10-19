package com.mathwake.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
   if(AlarmScheduler.prefs(context).getBoolean("enabled",false)){
     int h=AlarmScheduler.prefs(context).getInt("hour",7),m=AlarmScheduler.prefs(context).getInt("minute",0);
     String mode=AlarmScheduler.prefs(context).getString("mode","math");
     try{AlarmScheduler.schedule(context,h,m,mode);}catch(SecurityException ignored){}
   }
 }
}
