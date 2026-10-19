# MathWake v1.1 — Math or Walk to Dismiss

Native Android alarm app. When setting an alarm choose **Math (3 answers)** or **Walking (40 detected steps)**.

## Build without Android Studio
Upload files AND the hidden `.github/workflows/build-apk.yml` to the root of your existing GitHub repository, replacing matching files. On GitHub Actions open the latest successful run > Artifacts > MathWake-APK.

## Walking mode
Uses `Sensor.TYPE_STEP_DETECTOR` from a real phone and Physical Activity (ACTIVITY_RECOGNITION) permission on Android 10+. AlarmService holds the step listener during the ringing foreground service. Challenge UI updates live. 40 events stop the alarm. It does not use GPS or count shaking deliberately. No hardware detector => only Math Mode is supported. Some manufacturers restrict sensor events while the screen is off; test before relying on it. Phone must be carried on your body.

## Limitations
One-time alarm only. Alarm behavior on some devices depends on notifications, full-screen intent and background/battery permissions. Android can force stop apps; disabling app/force stop or phone powered off prevents alarm. Always use a backup alarm for important events. Full-screen/alarm sound behavior and physical steps must be verified on real hardware before relying on it.
