package com.mathwake.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/** On-device indoor walking estimator. Not proof of walking; movement can imitate footsteps. */
public final class WalkingEngine {
    private float gravity = SensorManager.GRAVITY_EARTH;
    private float smooth = 0f;
    private boolean peakArmed = true;
    private long lastPeakNs = 0L;
    private long previousCandidateNs = 0L;
    private int cadenceRun = 0;
    private int motionSteps = 0;
    private int hardwareDetectorSteps = 0;
    private int hardwareCounterSteps = 0;
    private float counterBaseline = Float.NaN;
    private int displaySteps = 0;
    private String status = "Waiting for walking motion";

    public int steps() { return displaySteps; }
    public int motionSteps() { return motionSteps; }
    public String status() { return status; }

    public boolean onSensor(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            float magnitude = (float)Math.sqrt(x*x + y*y + z*z);
            gravity = 0.92f * gravity + 0.08f * magnitude;
            float linear = Math.abs(magnitude - gravity);
            smooth = smooth * 0.56f + linear * 0.44f;
            long now = event.timestamp;
            // Hysteresis keeps a single foot impact from producing multiple steps.
            if (smooth < 0.50f) peakArmed = true;
            if (peakArmed && smooth > 0.95f && (lastPeakNs == 0 || now-lastPeakNs > 320_000_000L)) {
                peakArmed = false;
                long gap = previousCandidateNs == 0 ? 0 : now-previousCandidateNs;
                previousCandidateNs = now;
                lastPeakNs = now;
                if (gap >= 350_000_000L && gap <= 1_250_000_000L) {
                    cadenceRun++;
                    if (cadenceRun == 2) {
                        motionSteps += 3; // recover the first two qualifying steps after rhythm locks
                    } else if (cadenceRun > 2) {
                        motionSteps++;
                    }
                    status = cadenceRun >= 2 ? "Walking rhythm detected" : "Checking walking rhythm";
                } else {
                    cadenceRun = 0;
                    status = "Waiting for regular walking rhythm";
                }
            }
        } else if (type == Sensor.TYPE_STEP_DETECTOR) {
            hardwareDetectorSteps += Math.max(1, (int)event.values[0]);
            status = "Hardware step signal received";
        } else if (type == Sensor.TYPE_STEP_COUNTER) {
            float current = event.values[0];
            if (Float.isNaN(counterBaseline) || current < counterBaseline) counterBaseline = current;
            else hardwareCounterSteps = Math.max(hardwareCounterSteps, (int)(current-counterBaseline));
        }
        // Accelerometer is the primary fast source. Hardware step counters can recover
        // updates only once a plausible walking rhythm has actually been observed.
        int candidate = motionSteps;
        if (cadenceRun >= 2 && motionSteps > 0) {
            candidate = Math.max(candidate, Math.min(Math.max(hardwareCounterSteps, hardwareDetectorSteps), motionSteps + 4));
        }
        int before = displaySteps;
        displaySteps = Math.min(40, Math.max(displaySteps, candidate));
        return before != displaySteps;
    }
}
