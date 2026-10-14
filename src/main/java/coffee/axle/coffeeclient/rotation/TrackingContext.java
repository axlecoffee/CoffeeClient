/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.rotation;

import net.minecraft.util.MathHelper;

/**
 * Tracks target velocity history and aim confidence for adaptive rotation.
 *
 * @author axle.coffee
 */
public class TrackingContext {

    private static final int HISTORY_SIZE = 10;

    int targetEntityId = -1;
    float trackingConfidence = 0.0f;
    int ticksTracking = 0;
    boolean firstHitLanded = false;

    double[] velXHistory = new double[HISTORY_SIZE];
    double[] velZHistory = new double[HISTORY_SIZE];
    int historyIndex = 0;
    int historyCount = 0;

    float prevOutputYaw;
    float prevOutputPitch;
    double lastTargetX;
    double lastTargetY;
    double lastTargetZ;

    public void reset() {
        targetEntityId = -1;
        trackingConfidence = 0.0f;
        ticksTracking = 0;
        firstHitLanded = false;
        historyIndex = 0;
        historyCount = 0;
        prevOutputYaw = 0;
        prevOutputPitch = 0;
        lastTargetX = 0;
        lastTargetY = 0;
        lastTargetZ = 0;
        for (int i = 0; i < HISTORY_SIZE; i++) {
            velXHistory[i] = 0;
            velZHistory[i] = 0;
        }
    }

    public void pushVelocity(double vx, double vz) {
        velXHistory[historyIndex] = vx;
        velZHistory[historyIndex] = vz;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        if (historyCount < HISTORY_SIZE)
            historyCount++;
    }

    public double computeVelocityVariance() {
        if (historyCount < 2)
            return 0.0;

        double sumAngle = 0;
        double sumAngleSq = 0;
        int count = 0;

        for (int i = 0; i < historyCount; i++) {
            double vx = velXHistory[i];
            double vz = velZHistory[i];
            double speed = Math.sqrt(vx * vx + vz * vz);
            if (speed < 0.001)
                continue;
            double angle = Math.atan2(vz, vx);
            sumAngle += angle;
            sumAngleSq += angle * angle;
            count++;
        }

        if (count < 2)
            return 0.0;
        double mean = sumAngle / count;
        return (sumAngleSq / count) - (mean * mean);
    }

    public float computeAngularVelocity(float currentYaw, float currentPitch) {
        float dy = MathHelper.wrapAngleTo180_float(currentYaw - prevOutputYaw);
        float dp = MathHelper.wrapAngleTo180_float(currentPitch - prevOutputPitch);
        return (float) Math.sqrt(dy * dy + dp * dp);
    }

    public double computeTargetSpeed(double currentX, double currentZ) {
        double dx = currentX - lastTargetX;
        double dz = currentZ - lastTargetZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public void updateOnTarget(boolean onTarget) {
        if (onTarget) {
            trackingConfidence = Math.min(1.0f, trackingConfidence + 0.15f);
            ticksTracking++;
            firstHitLanded = true;
        } else {
            trackingConfidence = Math.max(0.0f, trackingConfidence - 0.05f);
            ticksTracking = 0;
        }
    }

    public void storeOutputAngles(float yaw, float pitch) {
        prevOutputYaw = yaw;
        prevOutputPitch = pitch;
    }

    public void storeTargetPosition(double x, double y, double z) {
        lastTargetX = x;
        lastTargetY = y;
        lastTargetZ = z;
    }
}
