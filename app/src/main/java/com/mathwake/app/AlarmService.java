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
    public static final String ACTION_PROGRESS = "com.mathwake.app.STEP_PROGRESS";
    private static final String CHANNEL = "mathwake_alarm";
    private static final long STEP_MIN_INTERVAL_MS = 280;   // reject impossible rapid shake steps
    private static final long STEP_MAX_INTERVAL_MS = 2200;  // walking pause threshold

    private MediaPlayer player;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private WalkingEngine walkingEngine = new WalkingEngine();
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;
    private PowerManager.WakeLock wakeLock;

    private int displayedSteps = 0;
    private int detectorSteps = 0;
    private int counterSteps = 0;
    private float counterBaseline = Float.NaN;
    private boolean walking = false;
    private long lastAcceptedDetectorStepAt = 0L;
    private String lastSource = "waiting";

    @Override
    public void onCreate() {
        super.onCreate();
        String mode = AlarmScheduler.prefs(this).getString("active_mode",
                AlarmScheduler.prefs(this).getString("mode", "math"));
        walking = "walk".equals(mode);

        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "MathWake alarms", NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);
        nm.createNotificationChannel(ch);

        Intent screen = new Intent(this, ChallengeActivity.class)
                .putExtra("mode", mode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 200, screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("MathWake alarm ringing")
                .setContentText(walking ? "Walk 40 verified steps to dismiss" : "Solve 3 maths problems to dismiss")
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(200, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(200, n);
        }

        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            Log.e("MathWake", "Cannot play alarm sound", e);
        }

        if (walking) {
            displayedSteps = 0;
            detectorSteps = 0;
            counterSteps = 0;
            counterBaseline = Float.NaN;
            lastSource = "waiting for steps";
            saveProgressAndBroadcast();

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MathWake:walking-alarm");
                wakeLock.acquire(30 * 60 * 1000L);
            }

            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
                registerSensors();
            }
        }
    }

    private void registerSensors() {
        try {
            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME, 0);
                lastSource = "Adaptive motion calibration ready";
            }
            if (stepCounterSensor != null) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI, 0);
                lastSource = "step counter ready";
            }
            if (stepDetectorSensor != null) {
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST, 0);
                lastSource = stepCounterSensor != null ? "detector + counter ready" : "step detector ready";
            }
            if (stepCounterSensor == null && stepDetectorSensor == null && accelerometerSensor == null) {
                lastSource = "no step sensor available";
            }
            saveProgressAndBroadcast();
        } catch (SecurityException e) {
            lastSource = "activity permission missing";
            saveProgressAndBroadcast();
            Log.e("MathWake", "Physical Activity permission missing", e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!walking) return;
        boolean changed = walkingEngine.onSensor(event);
        lastSource = walkingEngine.status();
        if (changed) {
            displayedSteps = walkingEngine.steps();
            saveProgressAndBroadcast();
            if (displayedSteps >= 40) stopSelf();
        }
    }

    private void saveProgressAndBroadcast() {
        AlarmScheduler.prefs(this).edit()
                .putInt("steps", displayedSteps)
                .putString("step_status", lastSource)
                .apply();
        Intent update = new Intent(ACTION_PROGRESS)
                .setPackage(getPackageName())
                .putExtra("steps", displayedSteps)
                .putExtra("status", lastSource)
                .putExtra("detectorSteps", detectorSteps)
                .putExtra("counterSteps", counterSteps);
        sendBroadcast(update);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_AFTER_MATH".equals(intent.getAction()) && !walking) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
