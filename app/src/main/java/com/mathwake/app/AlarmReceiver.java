package com.mathwake.app;
import android.content.*;
import android.os.Build;
public class AlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
   String mode=AlarmScheduler.prefs(context).getString("mode","math");
   AlarmScheduler.prefs(context).edit().putBoolean("enabled",false).remove("next").putString("active_mode",mode).putInt("steps",0).apply();
   Intent svc=new Intent(context,AlarmService.class);
   if(Build.VERSION.SDK_INT>=26)context.startForegroundService(svc);else context.startService(svc);
 }
}
