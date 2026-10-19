package com.mathwake.app;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private int hour = 7, minute = 0;
    private String selectedMode = "math";
    private TextView time, status, info;
    private LinearLayout root;

    private boolean hasStepSensor() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        return sm != null && (sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null || sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null);
    }

    private boolean hasActivityPermission() {
        return Build.VERSION.SDK_INT < 29 || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        hour = AlarmScheduler.prefs(this).getInt("hour", 7);
        minute = AlarmScheduler.prefs(this).getInt("minute", 0);
        selectedMode = AlarmScheduler.prefs(this).getString("mode", "math");

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0B1020);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = sectionCard();
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_mathwake_logo);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(82), dp(82));
        header.addView(icon, iconLp);

        TextView title = label(header, "MathWake", 30, Color.WHITE, true, true);
        title.setPadding(0, dp(10), 0, dp(4));
        label(header, "Wake up by solving maths or walking verified steps", 15, 0xFFB9C2D0, true, false);

        LinearLayout timeCard = sectionCard();
        timeCard.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(-1, -2);
        timeLp.topMargin = dp(16);
        root.addView(timeCard, timeLp);

        label(timeCard, "ALARM TIME", 13, 0xFF7BE0C5, true, true);
        time = label(timeCard, "", 46, Color.WHITE, true, true);
        time.setPadding(0, dp(10), 0, dp(12));
        displayTime();
        timeCard.addView(actionButton("Choose alarm time", 0xFF69D7B3), fullWidth());
        ((Button) timeCard.getChildAt(timeCard.getChildCount()-1)).setOnClickListener(v ->
                new TimePickerDialog(this, (view, h, m) -> {
                    hour = h;
                    minute = m;
                    displayTime();
                }, hour, minute, false).show());

        LinearLayout modeCard = sectionCard();
        LinearLayout.LayoutParams modeLp = new LinearLayout.LayoutParams(-1, -2);
        modeLp.topMargin = dp(16);
        root.addView(modeCard, modeLp);
        label(modeCard, "DISMISS CHALLENGE", 13, 0xFF7BE0C5, false, true).setPadding(0, 0, 0, dp(10));

        RadioGroup choices = new RadioGroup(this);
        choices.setOrientation(RadioGroup.VERTICAL);
        modeCard.addView(choices, fullWidth());
        RadioButton math = radio("Math Challenge  •  solve 3 questions");
        math.setId(10001);
        choices.addView(math);
        RadioButton walk = radio("Walking Challenge  •  walk 40 verified steps");
        walk.setId(10002);
        choices.addView(walk);
        ("walk".equals(selectedMode) ? walk : math).setChecked(true);
        choices.setOnCheckedChangeListener((group, checked) -> {
            selectedMode = checked == 10002 ? "walk" : "math";
            updateInfoText();
        });

        info = label(modeCard, "", 13, 0xFF9AA8BC, false, false);
        info.setPadding(0, dp(10), 0, 0);
        updateInfoText();

        LinearLayout buttonCard = sectionCard();
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(-1, -2);
        buttonLp.topMargin = dp(16);
        root.addView(buttonCard, buttonLp);

        Button set = actionButton("Set alarm", 0xFF69D7B3);
        buttonCard.addView(set, fullWidth());
        set.setOnClickListener(v -> setAlarm());

        Button cancel = actionButton("Cancel next alarm", 0xFF2A3658);
        LinearLayout.LayoutParams lp2 = fullWidth();
        lp2.topMargin = dp(10);
        buttonCard.addView(cancel, lp2);
        cancel.setOnClickListener(v -> {
            AlarmScheduler.cancel(this);
            refresh();
        });

        Button pm = actionButton("Practice math", 0xFF1C2744);
        LinearLayout.LayoutParams lp3 = fullWidth();
        lp3.topMargin = dp(10);
        buttonCard.addView(pm, lp3);
        pm.setOnClickListener(v -> startActivity(new Intent(this, ChallengeActivity.class).putExtra("demo", true).putExtra("mode", "math")));

        Button pw = actionButton("Practice walking", 0xFF1C2744);
        LinearLayout.LayoutParams lp4 = fullWidth();
        lp4.topMargin = dp(10);
        buttonCard.addView(pw, lp4);
        pw.setOnClickListener(v -> {
            if (!hasStepSensor()) {
                toast("Step sensor is not available");
                return;
            }
            if (!hasActivityPermission()) {
                requestWalkPermission();
                return;
            }
            startActivity(new Intent(this, ChallengeActivity.class).putExtra("demo", true).putExtra("mode", "walk"));
        });

        LinearLayout statusCard = sectionCard();
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(16);
        root.addView(statusCard, statusLp);
        label(statusCard, "STATUS", 13, 0xFF7BE0C5, false, true).setPadding(0, 0, 0, dp(10));
        status = label(statusCard, "", 15, Color.WHITE, false, false);
        refresh();
        label(statusCard,
                "Tip: for walking mode, keep MathWake unrestricted in battery settings and walk naturally with the phone in your pocket.",
                13, 0xFF9AA8BC, false, false).setPadding(0, dp(14), 0, 0);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        }
    }

    private RadioButton radio(String s) {
        RadioButton r = new RadioButton(this);
        r.setText(s);
        r.setTextColor(Color.WHITE);
        r.setPadding(0, dp(6), 0, dp(6));
        return r;
    }

    private void updateInfoText() {
        if (info == null) return;
        String sensorLine = hasStepSensor() ?
                "Step sensor found on this phone." :
                "No step sensor found, so walking mode will not be reliable.";
        String modeLine = "walk".equals(selectedMode)
                ? "Indoor walking mode uses adaptive motion rhythm detection, with hardware sensor cross-checks."
                : "Math mode stops the alarm after 3 correct answers.";
        info.setText(sensorLine + "\n" + modeLine);
    }

    private LinearLayout sectionCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackground(roundFill(0xFF11192E, 28, 0xFF1B2A49, 1));
        return box;
    }

    private TextView label(LinearLayout parent, String s, int size, int color, boolean center, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(center ? Gravity.CENTER : Gravity.START);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        parent.addView(t);
        return t;
    }

    private Button actionButton(String s, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(color == 0xFF69D7B3 ? 0xFF07111F : Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        b.setBackground(roundFill(color, 20, color, 0));
        return b;
    }

    private GradientDrawable roundFill(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void displayTime() {
        time.setText(String.format(Locale.US, "%02d:%02d", hour, minute));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void requestWalkPermission() {
        if (Build.VERSION.SDK_INT >= 29) requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 31);
    }

    private void refresh() {
        if (status == null) return;
        boolean enabled = AlarmScheduler.prefs(this).getBoolean("enabled", false);
        long next = AlarmScheduler.prefs(this).getLong("next", 0);
        String mode = AlarmScheduler.prefs(this).getString("mode", "math");
        if (enabled) {
            status.setText("Next alarm: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(next)) +
                    "\nChallenge: " + ("walk".equals(mode) ? "40 verified steps" : "3 maths questions"));
        } else {
            status.setText("No alarm scheduled yet.");
        }
    }

    private void setAlarm() {
        if ("walk".equals(selectedMode)) {
            if (!hasStepSensor()) {
                toast("Walking mode needs a built-in step sensor. Choose Math.");
                return;
            }
            if (!hasActivityPermission()) {
                requestWalkPermission();
                toast("Grant Physical Activity permission, then press Set alarm again.");
                return;
            }
        }
        try {
            AlarmScheduler.schedule(this, hour, minute, selectedMode);
            refresh();
            toast("Alarm set with " + ("walk".equals(selectedMode) ? "Walking" : "Math") + " challenge");
        } catch (SecurityException e) {
            toast("Please allow exact alarms in Android settings");
            if (Build.VERSION.SDK_INT >= 31) startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        updateInfoText();
    }
}
