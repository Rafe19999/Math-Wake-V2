package com.mathwake.app;
import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.hardware.*;
import android.os.*;
import android.view.Gravity;
import android.text.InputType;
import android.widget.*;
import java.util.Random;

public class ChallengeActivity extends Activity implements SensorEventListener {
 private final Random random=new Random();
 private boolean demo,walking;
 private int solved=0,answer,demoSteps=0;
 private TextView progress,question,feedback;
 private EditText input;
 private SensorManager sensorManager;
 private Sensor sensor;
 private BroadcastReceiver progressReceiver;
 @Override public void onCreate(Bundle state){super.onCreate(state);setShowWhenLocked(true);setTurnScreenOn(true);
    demo=getIntent().getBooleanExtra("demo",false);
    walking="walk".equals(getIntent().getStringExtra("mode"));
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(36,65,36,35);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setBackgroundColor(0xFF0C1121);setContentView(root);
    text(root,demo?"PRACTICE MODE":"WAKE UP!",33,0xFF76E0C4);
    progress=text(root,"",24,Color.WHITE);progress.setPadding(0,45,0,30);
    question=text(root,"",26,Color.WHITE);question.setPadding(0,15,0,35);
    if(walking){
      question.setText("Walk 40 real steps with your phone in your pocket.\nThe alarm stops automatically!");
      feedback=text(root,"Keep moving...",17,0xFFFFC86A);
      if(demo){sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);if(sensorManager!=null)sensor=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);}
      updateWalking(demo?0:AlarmScheduler.prefs(this).getInt("steps",0));
    } else {
      input=new EditText(this);input.setSingleLine(true);input.setTextColor(Color.WHITE);input.setHintTextColor(0xFF9CA3AF);input.setHint("Your answer");input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);root.addView(input,new LinearLayout.LayoutParams(-1,-2));
      Button submit=new Button(this);submit.setText("CHECK ANSWER");root.addView(submit,new LinearLayout.LayoutParams(-1,-2));
      feedback=text(root,"",17,0xFFFFC86A);feedback.setPadding(0,25,0,0);submit.setOnClickListener(v->check());newProblem();
    }
 }
 private TextView text(LinearLayout root,String label,int size,int color){TextView t=new TextView(this);t.setText(label);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.CENTER);root.addView(t);return t;}
 private int n(int lo,int hi){return lo+random.nextInt(hi-lo+1);}
 private void newProblem(){progress.setText("Solved "+solved+" / 3");input.setText("");feedback.setText("");String q;switch(random.nextInt(5)){
 case 0:{int x=n(-9,15),a=n(2,8),b=n(-15,15);answer=x;q="Solve x:\n"+a+"x "+(b>=0?"+ ":"- ")+Math.abs(b)+" = "+(a*x+b);break;}
 case 1:{int x=n(2,12),a=n(2,7),b=n(2,9);answer=x;q="Solve x:\n"+a+"(x + "+b+") = "+(a*(x+b));break;}
 case 2:{int pct=n(1,6)*10,k=n(2,10);answer=pct*k;q="Find "+pct+"% of "+(k*10)+":";break;}
 case 3:{int[] triple={3,4,5,5,12,13,8,15,17};int index=random.nextInt(3)*3,m=n(1,4);answer=triple[index+2]*m;q="Right triangle: legs = "+(triple[index]*m)+" and "+(triple[index+1]*m)+". Find hypotenuse.";break;}
 default:{int a=n(2,12),b=n(2,12);answer=a*a-b*b;q="Evaluate:\n"+a+"² - "+b+"²";break;}}
 question.setText(q);input.requestFocus();}
 private void check(){try{int response=Integer.parseInt(input.getText().toString().trim());if(response==answer){solved++;if(solved==3){if(!demo){Intent stop=new Intent(this,AlarmService.class).setAction("STOP_AFTER_MATH");startService(stop);}finish();}else newProblem();}else{feedback.setText("Not quite. Try again!");input.selectAll();}}catch(NumberFormatException e){feedback.setText("Enter a whole number.");}}
 private void updateWalking(int count){progress.setText("STEPS: "+Math.min(count,40)+" / 40");if(count>=40){feedback.setText("Challenge complete!");finish();}}
 @Override protected void onStart(){super.onStart();if(!walking)return;
    if(demo){if(sensor!=null)try{sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL);}catch(SecurityException e){feedback.setText("Physical Activity permission needed.");}}
    else {progressReceiver=new BroadcastReceiver(){@Override public void onReceive(Context ctx,Intent event){updateWalking(event.getIntExtra("steps",0));}};
      IntentFilter filter=new IntentFilter(AlarmService.ACTION_PROGRESS);
      if(Build.VERSION.SDK_INT>=33)registerReceiver(progressReceiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(progressReceiver,filter);
      updateWalking(AlarmScheduler.prefs(this).getInt("steps",0));}
 }
 @Override protected void onStop(){if(demo && sensorManager!=null)sensorManager.unregisterListener(this);if(progressReceiver!=null){unregisterReceiver(progressReceiver);progressReceiver=null;}super.onStop();}
 @Override public void onSensorChanged(SensorEvent event){if(demo && walking && event.sensor.getType()==Sensor.TYPE_STEP_DETECTOR){demoSteps++;updateWalking(demoSteps);}}
 @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
 @Override public void onBackPressed(){if(demo)super.onBackPressed();else feedback.setText(walking?"Walk 40 steps to dismiss the alarm.":"Solve 3 maths questions to dismiss the alarm.");}
}
