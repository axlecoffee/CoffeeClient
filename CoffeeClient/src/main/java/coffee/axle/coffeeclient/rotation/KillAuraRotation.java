/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.rotation;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

/**
 * Rotation controller for KillAura with VSPLIT aiming and proper silent rotation.
 * <p>
 * Silent rotation works at the packet level — the player's visual yaw/pitch are
 * NEVER modified. Instead, target angles are stored here and applied to outgoing
 * C03 packets by the KillAura packet handler.
 *
 * @author axle.coffee
 * @see Rotation base class with GCD + smoothing pipeline
 */
public class KillAuraRotation extends Rotation {

    /** Whether this rotation controller is actively spoofing server-side angles. */
    private boolean active;

    /** The target yaw/pitch to send to the server. */
    private float targetYaw;
    private float targetPitch;

    /** Hitbox bounds for VSPLIT aiming (0.0-1.5 range). */
    private float hitboxBoundsMin = 0.05f;
    private float hitboxBoundsMax = 0.95f;

    private final TrackingContext tracking = new TrackingContext();

    public KillAuraRotation(RotationState state) {
        super(state);
    }

    // ── State ─────────────────────────────────────────────────

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            tracking.reset();
        }
    }

    public float getTargetYaw() {
        return targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }

    public void setHitboxBounds(float min, float max) {
        this.hitboxBoundsMin = MathHelper.clamp_float(min, 0.0f, 1.5f);
        this.hitboxBoundsMax = MathHelper.clamp_float(max, 0.0f, 1.5f);
    }

    public TrackingContext getTracking() {
        return tracking;
    }

    // ── Aim computation ───────────────────────────────────────

    /**
     * Compute VSPLIT target angles for the given entity's bounding box.
     * Uses dynamic vertical multipoint and velocity-based extrapolation.
     *
     * @param entity the target entity
     * @return {@code [yaw, pitch]} raw target angles
     */
    public float[] computeVSplitTarget(EntityLivingBase entity) {
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        float border = entity.getCollisionBorderSize();
        AxisAlignedBB expandedBox = bb.expand(border, border, border);

        // Velocity tracking
        double velX = entity.posX - entity.prevPosX;
        double velY = entity.posY - entity.prevPosY;
        double velZ = entity.posZ - entity.prevPosZ;
        tracking.pushVelocity(velX, velZ);

        // Extrapolate bounding box forward by a few ticks based on velocity
        double variance = tracking.computeVelocityVariance();
        float confidence = 1.0f - MathHelper.clamp_float((float) (variance / 1.5), 0.0f, 1.0f);
        float ticksAhead = 2.0f + state.getEstimatedPingTicks();
        AxisAlignedBB workingBB = RotationMath.extrapolateBB(expandedBox, velX, velY, velZ, ticksAhead, confidence);

        // Dynamic contraction based on distance and angular velocity
        int entityId = entity.getEntityId();
        if (entityId != tracking.targetEntityId) {
            tracking.reset();
            tracking.targetEntityId = entityId;
        }

        double distance = RotationMath.distanceToBox(workingBB);
        float angularVelocity = tracking.computeAngularVelocity(
                state.getServerYaw(), state.getServerPitch());
        double targetSpeed = tracking.computeTargetSpeed(entity.posX, entity.posZ);
        float contraction = RotationMath.computeDynamicContraction(distance, angularVelocity, targetSpeed);
        contraction = MathHelper.clamp_float(contraction, 0.03f, 0.22f);
        workingBB = workingBB.contract(contraction, contraction, contraction);

        float verticalMultipoint = RotationMath.computeVerticalMultipointMotionAware(
                entity.posY, entity.motionY);

        tracking.storeTargetPosition(entity.posX, entity.posY, entity.posZ);

        return RotationMath.toRotationsBoxDynamic(
                workingBB, hitboxBoundsMin, hitboxBoundsMax, verticalMultipoint);
    }

    /**
     * Full aim pipeline: compute VSPLIT target, apply smoothing + GCD.
     * Updates the stored target angles that will be applied to outgoing packets.
     *
     * @param entity       the target entity
     * @param smoothFactor 0.0 = instant, 1.0 = maximum smoothing
     * @param hSpeed       horizontal speed multiplier (0-10)
     * @param vSpeed       vertical speed multiplier (0-10)
     * @return GCD-corrected {@code [yaw, pitch]}
     */
    public float[] aimAt(EntityLivingBase entity, float smoothFactor, float hSpeed, float vSpeed) {
        float[] raw = computeVSplitTarget(entity);
        float[] result = smoothAndGCD(raw[0], raw[1], smoothFactor, hSpeed, vSpeed);
        targetYaw = result[0];
        targetPitch = result[1];
        return result;
    }

    /**
     * Direct aim without smoothing — just compute target + GCD snap.
     * Used for instant-aim modes (LOCK_VIEW with no smoothing).
     *
     * @param entity the target entity
     * @return GCD-corrected {@code [yaw, pitch]}
     */
    public float[] aimAtDirect(EntityLivingBase entity) {
        float[] raw = computeVSplitTarget(entity);
        float sYaw = state.getServerYaw();
        float sPitch = state.getServerPitch();
        float newYaw = applyGCD(sYaw, raw[0]);
        float newPitch = applyGCD(sPitch, raw[1]);
        newPitch = MathHelper.clamp_float(newPitch, -90f, 90f);
        targetYaw = newYaw;
        targetPitch = newPitch;
        return new float[] { newYaw, newPitch };
    }

    /**
     * Smoothed aim using the old RotationUtil-style step + clamp approach.
     * Used for LEGIT mode where the player's actual view moves.
     *
     * @param entity       the target entity
     * @param currentYaw   the player's current visual yaw
     * @param currentPitch the player's current visual pitch
     * @param angleStep    max rotation per tick (degrees)
     * @param smoothFactor 0.0-1.0 smoothing
     * @return {@code [yaw, pitch]} to set on the player
     */
    public float[] aimAtStepped(EntityLivingBase entity, float currentYaw, float currentPitch,
                                float angleStep, float smoothFactor) {
        float[] raw = computeVSplitTarget(entity);

        float yawDelta = MathHelper.wrapAngleTo180_float(raw[0] - currentYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(raw[1] - currentPitch);

        yawDelta = clampAngle(yawDelta, angleStep);
        pitchDelta = clampAngle(pitchDelta, angleStep);

        yawDelta = smoothDelta(yawDelta, smoothFactor);
        pitchDelta = smoothDelta(pitchDelta, smoothFactor);

        float newYaw = applyGCD(currentYaw, currentYaw + yawDelta);
        float newPitch = applyGCD(currentPitch, currentPitch + pitchDelta);
        newPitch = MathHelper.clamp_float(newPitch, -90f, 90f);

        targetYaw = newYaw;
        targetPitch = newPitch;
        return new float[] { newYaw, newPitch };
    }
}
