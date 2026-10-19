package com.mathwake.app;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.os.*;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.Locale;
import java.util.Random;

public class ChallengeActivity extends Activity implements SensorEventListener {
    private final Random random = new Random();
    private static final long STEP_MIN_INTERVAL_MS = 280;

    private boolean demo, walking;
    private int solved = 0, answer;
    private int demoDisplayedSteps = 0, demoDetectorSteps = 0, demoCounterSteps = 0;
    private float demoCounterBaseline = Float.NaN;
    private long demoLastAcceptedDetectorStepAt = 0L;

    private TextView progress, question, feedback, badge, subStatus;
    private EditText input;
    private SensorManager sensorManager;
    private Sensor demoCounterSensor, demoDetectorSensor;
    private BroadcastReceiver progressReceiver;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setShowWhenLocked(true);
        setTurnScreenOn(true);

        demo = getIntent().getBooleanExtra("demo", false);
        walking = "walk".equals(getIntent().getStringExtra("mode"));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0B1020);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout card = card();
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        badge = chip(card, demo ? "PRACTICE MODE" : "ALARM ACTIVE", demo ? 0xFF2A7F62 : 0xFF8B3D2F);
        TextView title = text(card, walking ? "Walking Challenge" : "Math Challenge", 28, Color.WHITE, true);
        title.setPadding(0, dp(10), 0, dp(6));
        text(card, walking ? "Walk to dismiss the alarm" : "Solve questions to dismiss the alarm", 15, 0xFFB9C2D0, false);

        progress = text(card, "", 30, 0xFF7BE0C5, true);
        progress.setPadding(0, dp(22), 0, dp(10));

        question = text(card, "", 22, Color.WHITE, true);
        question.setPadding(0, dp(10), 0, dp(18));

        subStatus = text(card, "", 14, 0xFF9AA8BC, false);
        subStatus.setPadding(0, 0, 0, dp(12));

        if (walking) {
            question.setText("Walk 40 verified steps.\nPocket mode is recommended.");
            feedback = text(card, "Waiting for steps...", 16, 0xFFFFC86A, false);
            feedback.setPadding(0, dp(6), 0, 0);

            if (demo) {
                sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                if (sensorManager != null) {
                    demoCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                    demoDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
                }
            }
            updateWalking(demo ? 0 : AlarmScheduler.prefs(this).getInt("steps", 0),
                    demo ? "demo ready" : AlarmScheduler.prefs(this).getString("step_status", "waiting for steps"));
        } else {
            input = new EditText(this);
            input.setSingleLine(true);
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(0xFF7E8B9F);
            input.setHint("Type your answer");
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            input.setPadding(dp(16), dp(14), dp(16), dp(14));
            input.setBackground(roundFill(0xFF121B33, 20, 0xFF24314F, 1));
            card.addView(input, new LinearLayout.LayoutParams(-1, -2));

            Button submit = actionButton("Check Answer", 0xFF64D2B0);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(14);
            card.addView(submit, p);

            feedback = text(card, "", 16, 0xFFFFC86A, false);
            feedback.setPadding(0, dp(14), 0, 0);
            submit.setOnClickListener(v -> check());
            newProblem();
        }
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackground(roundFill(0xFF11192E, 28, 0xFF1B2A49, 1));
        return box;
    }

    private TextView chip(LinearLayout root, String s, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        t.setBackground(roundFill(color, 999, color, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        root.addView(t, lp);
        return t;
    }

    private TextView text(LinearLayout root, String label, int size, int color, boolean center) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(center ? Gravity.CENTER : Gravity.START);
        root.addView(t);
        return t;
    }

    private Button actionButton(String s, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(0xFF07111F);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(roundFill(color, 20, color, 0));
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        return b;
    }

    private GradientDrawable roundFill(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int n(int lo, int hi) {
        return lo + random.nextInt(hi - lo + 1);
    }

    private void newProblem() {
        progress.setText(String.format(Locale.US, "%d / 3 solved", solved));
        subStatus.setText("Solve three Class 10 style questions to stop the alarm");
        input.setText("");
        feedback.setText("");
        String q;
        switch (random.nextInt(5)) {
            case 0: {
                int x = n(-9, 15), a = n(2, 8), b = n(-15, 15);
                answer = x;
                q = "Solve x\n" + a + "x " + (b >= 0 ? "+ " : "- ") + Math.abs(b) + " = " + (a * x + b);
                break;
            }
            case 1: {
                int x = n(2, 12), a = n(2, 7), b = n(2, 9);
                answer = x;
                q = "Solve x\n" + a + "(x + " + b + ") = " + (a * (x + b));
                break;
            }
            case 2: {
                int pct = n(1, 6) * 10, k = n(2, 10);
                answer = pct * k;
                q = "Find " + pct + "% of " + (k * 10);
                break;
            }
            case 3: {
                int[] triple = {3, 4, 5, 5, 12, 13, 8, 15, 17};
                int index = random.nextInt(3) * 3, m = n(1, 4);
                answer = triple[index + 2] * m;
                q = "Right triangle\nlegs = " + (triple[index] * m) + " and " + (triple[index + 1] * m) + "\nFind the hypotenuse";
                break;
            }
            default: {
                int a = n(2, 12), b = n(2, 12);
                answer = a * a - b * b;
                q = "Evaluate\n" + a + "² - " + b + "²";
                break;
            }
        }
        question.setText(q);
        input.requestFocus();
    }

    private void check() {
        try {
            int response = Integer.parseInt(input.getText().toString().trim());
            if (response == answer) {
                solved++;
                if (solved == 3) {
                    if (!demo) {
                        Intent stop = new Intent(this, AlarmService.class).setAction("STOP_AFTER_MATH");
                        startService(stop);
                    }
                    finish();
                } else {
                    newProblem();
                }
            } else {
                feedback.setText("Not quite. Try again.");
                input.selectAll();
            }
        } catch (NumberFormatException e) {
            feedback.setText("Enter a whole number.");
        }
    }

    private void updateWalking(int count, String status) {
        int shown = Math.min(count, 40);
        progress.setText(String.format(Locale.US, "%d / 40 steps", shown));
        subStatus.setText(status == null ? "" : "Sensor status: " + status);
        if (count >= 40) {
            feedback.setText("Challenge complete. Alarm dismissed.");
            finish();
        } else if (count == 0) {
            feedback.setText("Put the phone in your pocket and walk naturally.");
        } else {
            feedback.setText((40 - shown) + " steps left");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!walking) return;
        if (demo) {
            if (sensorManager != null) {
                try {
                    if (demoCounterSensor != null) sensorManager.registerListener(this, demoCounterSensor, SensorManager.SENSOR_DELAY_UI, 0);
                    if (demoDetectorSensor != null) sensorManager.registerListener(this, demoDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST, 0);
                } catch (SecurityException e) {
                    feedback.setText("Physical Activity permission needed.");
                }
            }
        } else {
            progressReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent event) {
                    updateWalking(event.getIntExtra("steps", 0), event.getStringExtra("status"));
                }
            };
            IntentFilter filter = new IntentFilter(AlarmService.ACTION_PROGRESS);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(progressReceiver, filter);
            updateWalking(AlarmScheduler.prefs(this).getInt("steps", 0),
                    AlarmScheduler.prefs(this).getString("step_status", "waiting for steps"));
        }
    }

    @Override
    protected void onStop() {
        if (demo && sensorManager != null) sensorManager.unregisterListener(this);
        if (progressReceiver != null) {
            unregisterReceiver(progressReceiver);
            progressReceiver = null;
        }
        super.onStop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!demo || !walking) return;
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_STEP_DETECTOR) {
            long now = SystemClock.elapsedRealtime();
            if (demoLastAcceptedDetectorStepAt == 0L || now - demoLastAcceptedDetectorStepAt >= STEP_MIN_INTERVAL_MS) {
                demoDetectorSteps++;
                demoLastAcceptedDetectorStepAt = now;
            }
        } else if (type == Sensor.TYPE_STEP_COUNTER) {
            float total = event.values[0];
            if (Float.isNaN(demoCounterBaseline)) demoCounterBaseline = total;
            else if (total < demoCounterBaseline) demoCounterBaseline = total;
            else demoCounterSteps = Math.max(demoCounterSteps, (int) (total - demoCounterBaseline));
        }
        demoDisplayedSteps = Math.max(demoDetectorSteps, demoCounterSteps);
        updateWalking(demoDisplayedSteps, "demo detector=" + demoDetectorSteps + " counter=" + demoCounterSteps);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onBackPressed() {
        if (demo) super.onBackPressed();
        else feedback.setText(walking ? "Walk 40 steps to dismiss the alarm." : "Solve 3 maths questions to dismiss the alarm.");
    }
}
