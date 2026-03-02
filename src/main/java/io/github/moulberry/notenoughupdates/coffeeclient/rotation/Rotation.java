/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import java.util.Random;

/**
 * Abstract base for all rotation controllers.
 * Provides GCD correction, smoothing, and adaptive rotation pipelines.
 *
 * @author axle.coffee
 * @see AimAssistRotation combat aiming
 */
public abstract class Rotation {

    protected static final Minecraft mc = Minecraft.getMinecraft();
    protected final RotationState state;
    protected final Random random = new Random();

    protected Rotation(RotationState state) {
        this.state = state;
    }

    /**
     * Compute the GCD step based on the current mouse sensitivity.
     * This is the smallest angle increment Minecraft can produce from
     * a single mouse movement event.
     * <p>
     * Formula: {@code f = sens * 0.6 + 0.2; gcd = f^3 * 8}
     */
    protected float computeGCD() {
        float sens = mc.gameSettings.mouseSensitivity;
        float f = sens * 0.6f + 0.2f;
        return f * f * f * 8.0f;
    }

    /**
     * Snap a rotation delta to the GCD grid.
     *
     * @param currentAngle the angle before this tick's rotation
     * @param targetAngle  the desired angle after this tick
     * @return the target angle snapped to the nearest GCD step from current
     */
    protected float applyGCD(float currentAngle, float targetAngle) {
        float gcd = computeGCD();
        float delta = targetAngle - currentAngle;
        return currentAngle + delta - (delta % gcd);
    }

    /**
     * Clamp an angle delta to a maximum rotation per tick.
     */
    protected float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0f, Math.min(180.0f, maxAngle));
        return MathHelper.clamp_float(angle, -maxAngle, maxAngle);
    }

    /**
     * Apply smoothing dampening to an angle delta.
     * Includes slight randomization for human-like jitter.
     *
     * @param delta        rotation delta in degrees
     * @param smoothFactor 0.0 = no smoothing (instant), 1.0 = maximum smoothing
     */
    protected float smoothDelta(float delta, float smoothFactor) {
        float jitter = (float) (random.nextGaussian() * 0.015);
        return delta * (0.5f + 0.5f * (1.0f - MathHelper.clamp_float(
                smoothFactor + jitter, 0.0f, 1.0f)));
    }

    /**
     * Full rotation pipeline: compute delta, smooth, apply speed scaling, GCD snap.
     *
     * @param targetYaw    raw target yaw
     * @param targetPitch  raw target pitch
     * @param smoothFactor 0.0-1.0 dampening
     * @param hSpeed       horizontal speed multiplier (0-10)
     * @param vSpeed       vertical speed multiplier (0-10)
     * @return GCD-corrected {@code [yaw, pitch]} for this tick
     */
    protected float[] smoothAndGCD(float targetYaw, float targetPitch,
            float smoothFactor, float hSpeed, float vSpeed) {
        float sYaw = state.getServerYaw();
        float sPitch = state.getServerPitch();

        float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - sYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(targetPitch - sPitch);

        yawDelta = smoothDelta(yawDelta, smoothFactor);
        pitchDelta = smoothDelta(pitchDelta, smoothFactor);

        float newYaw = sYaw + yawDelta * Math.min(Math.abs(hSpeed), 10.0f) * 0.15f;
        float newPitch = sPitch + pitchDelta * Math.min(Math.abs(vSpeed), 10.0f) * 0.15f;

        newYaw = applyGCD(sYaw, newYaw);
        newPitch = applyGCD(sPitch, newPitch);
        newPitch = MathHelper.clamp_float(newPitch, -90f, 90f);

        return new float[] { newYaw, newPitch };
    }

    /**
     * Rotate WASD movement inputs to compensate for a yaw spoof.
     * Call after setting rotationYaw to the spoofed value.
     *
     * @param preSpoofYaw  the visual yaw before spoofing
     * @param postSpoofYaw the spoofed yaw that was applied
     */
    protected void applyMoveFix(float preSpoofYaw, float postSpoofYaw) {
        if (mc.thePlayer == null || mc.thePlayer.movementInput == null)
            return;

        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;
        if (Math.abs(forward) < 1.0E-4f && Math.abs(strafe) < 1.0E-4f)
            return;

        float delta = (float) Math.toRadians(preSpoofYaw - postSpoofYaw);
        float cos = MathHelper.cos(delta);
        float sin = MathHelper.sin(delta);

        mc.thePlayer.movementInput.moveForward = forward * cos + strafe * sin;
        mc.thePlayer.movementInput.moveStrafe = strafe * cos - forward * sin;
    }

    /**
     * Check if current aim is within tolerance of the target angles.
     */
    protected boolean withinTolerance(float currentYaw, float currentPitch,
            float targetYaw, float targetPitch,
            float tolerance) {
        float dy = Math.abs(MathHelper.wrapAngleTo180_float(currentYaw - targetYaw));
        float dp = Math.abs(MathHelper.wrapAngleTo180_float(currentPitch - targetPitch));
        return dy <= tolerance && dp <= tolerance;
    }

    /**
     * Adaptive rotation pipeline that adjusts smoothing based on tracking context.
     */
    protected float[] smoothAndGCDAdaptive(float targetYaw, float targetPitch,
            float baseSmoothFactor, float hSpeed, float vSpeed,
            float trackingConfidence, double targetLateralVel,
            double distance) {
        return smoothAndGCDAdaptive(targetYaw, targetPitch, baseSmoothFactor,
                hSpeed, vSpeed, trackingConfidence, targetLateralVel, distance, 0);
    }

    protected float[] smoothAndGCDAdaptive(float targetYaw, float targetPitch,
            float baseSmoothFactor, float hSpeed, float vSpeed,
            float trackingConfidence, double targetLateralVel,
            double distance, int lagDelayTicks) {
        float sYaw = state.getServerYaw();
        float sPitch = state.getServerPitch();

        float yawError = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - sYaw));
        float pitchError = Math.abs(MathHelper.wrapAngleTo180_float(targetPitch - sPitch));
        float totalError = (float) Math.sqrt(yawError * yawError + pitchError * pitchError);

        float errorFactor = 1.0f - MathHelper.clamp_float(totalError / 30.0f, 0.0f, 0.7f);
        float confFactor = 1.0f - trackingConfidence * 0.5f;
        float moveFactor = 1.0f - MathHelper.clamp_float((float) (targetLateralVel / 0.25), 0.0f, 0.3f);
        float distFactor = MathHelper.clamp_float((float) (distance / 4.0), 0.5f, 1.0f);

        float effectiveSmooth = baseSmoothFactor * errorFactor * confFactor * moveFactor * distFactor;

        if (lagDelayTicks > 0) {
            effectiveSmooth *= Math.max(0.3f, 1.0f - (lagDelayTicks / 20.0f));
        }

        effectiveSmooth = Math.max(effectiveSmooth, 0.05f);

        float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - sYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(targetPitch - sPitch);

        yawDelta = smoothDelta(yawDelta, effectiveSmooth);
        pitchDelta = smoothDelta(pitchDelta, effectiveSmooth);

        float newYaw = sYaw + yawDelta * Math.min(Math.abs(hSpeed), 10.0f) * 0.15f;
        float newPitch = sPitch + pitchDelta * Math.min(Math.abs(vSpeed), 10.0f) * 0.15f;

        newYaw = applyGCD(sYaw, newYaw);
        newPitch = applyGCD(sPitch, newPitch);
        newPitch = MathHelper.clamp_float(newPitch, -90f, 90f);

        return new float[] { newYaw, newPitch };
    }
}
