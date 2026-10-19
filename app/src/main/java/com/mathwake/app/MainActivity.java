package com.mathwake.app;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import android.view.Gravity;
import android.provider.Settings;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private int hour=7, minute=0;
    private String selectedMode="math";
    private TextView time,status;
    private LinearLayout root;
    private boolean hasStepSensor() {
        SensorManager sm=(SensorManager)getSystemService(SENSOR_SERVICE);
        return sm!=null && (sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null || sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)!=null);
    }
    private boolean hasActivityPermission(){return Build.VERSION.SDK_INT<29 || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED;}
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        hour=AlarmScheduler.prefs(this).getInt("hour",7);
        minute=AlarmScheduler.prefs(this).getInt("minute",0);
        selectedMode=AlarmScheduler.prefs(this).getString("mode","math");
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);root.setPadding(40,56,40,24);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setBackgroundColor(0xFF0C1121);
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        label("MATHWAKE",32,0xFF76E0C4);label("Choose how to dismiss your alarm",16,Color.WHITE);
        time=label("",46,Color.WHITE);time.setPadding(0,35,0,25);displayTime();
        button("CHOOSE ALARM TIME").setOnClickListener(v->new TimePickerDialog(this,(view,h,m)->{hour=h;minute=m;displayTime();},hour,minute,false).show());
        label("ALARM DISMISS CHALLENGE",17,0xFF76E0C4).setPadding(0,28,0,12);
        RadioGroup choices=new RadioGroup(this);choices.setOrientation(RadioGroup.VERTICAL);root.addView(choices,new LinearLayout.LayoutParams(-1,-2));
        RadioButton math=new RadioButton(this);math.setText("Math Challenge  •  solve 3 problems");math.setTextColor(Color.WHITE);math.setId(10001);choices.addView(math);
        RadioButton walk=new RadioButton(this);walk.setText("Walking Challenge  •  walk 40 steps");walk.setTextColor(Color.WHITE);walk.setId(10002);choices.addView(walk);
        ("walk".equals(selectedMode)?walk:math).setChecked(true);
        choices.setOnCheckedChangeListener((group,checked)->selectedMode=checked==10002?"walk":"math");
        label(hasStepSensor()?"Walking step sensor available on this phone.":"No step sensor: Walking mode cannot be used on this phone.",13,0xFFABB6D0).setPadding(0,10,0,22);
        button("SET ALARM").setOnClickListener(v->setAlarm());
        button("CANCEL NEXT ALARM").setOnClickListener(v->{AlarmScheduler.cancel(this);refresh();});
        button("PRACTICE MATH").setOnClickListener(v->startActivity(new Intent(this,ChallengeActivity.class).putExtra("demo",true).putExtra("mode","math")));
        button("PRACTICE WALKING").setOnClickListener(v->{if(!hasStepSensor()){toast("Step sensor is not available");return;} if(!hasActivityPermission()){requestWalkPermission();return;}startActivity(new Intent(this,ChallengeActivity.class).putExtra("demo",true).putExtra("mode","walk"));});
        status=label("",16,Color.WHITE);status.setPadding(0,28,0,0);refresh();
        label("Walk mode counts physical steps detected by your phone. Keep phone in your pocket. One-time alarm; set a backup alarm for anything important.",13,0xFFABB6D0).setPadding(0,30,0,0);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},20);
    }
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);root.addView(t);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,7,0,7);root.addView(b,params);return b;}
    private void displayTime(){time.setText(String.format(java.util.Locale.US,"%02d:%02d",hour,minute));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void requestWalkPermission(){if(Build.VERSION.SDK_INT>=29)requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},31);}
    private void refresh(){if(status==null)return;boolean enabled=AlarmScheduler.prefs(this).getBoolean("enabled",false);long next=AlarmScheduler.prefs(this).getLong("next",0);String mode=AlarmScheduler.prefs(this).getString("mode","math");status.setText(enabled?"Alarm set: "+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(next))+"\nChallenge: "+("walk".equals(mode)?"40 steps":"3 maths"):"No alarm scheduled");}
    private void setAlarm(){
       if("walk".equals(selectedMode)){
           if(!hasStepSensor()){toast("Walking mode needs a built-in step sensor. Choose Math.");return;}
           if(!hasActivityPermission()){requestWalkPermission();toast("Grant Physical Activity permission, then press SET ALARM again.");return;}
       }
       try{AlarmScheduler.schedule(this,hour,minute,selectedMode);refresh();toast("Alarm set with "+("walk".equals(selectedMode)?"40 steps":"Math")+" challenge");}
       catch(SecurityException e){toast("Please allow exact alarms in Android settings");if(Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));}
    }
    @Override protected void onResume(){super.onResume();refresh();}
}
