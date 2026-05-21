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
 * Rotation controller for AimAssist with multiple aim modes.
 * <p>
 * Modes:
 * <ul>
 * <li>{@link AimMode#DEFAULT} — OpenMyau-style center-box aim</li>
 * <li>{@link AimMode#SIGHTLINE} — closest-point-on-box sightline</li>
 * <li>{@link AimMode#VSPLIT} — dynamic vertical split aim with hitbox
 * bounds</li>
 * </ul>
 *
 * @author axle.coffee
 */
public class AimAssistRotation extends Rotation {

    public enum AimMode {
        DEFAULT,
        SIGHTLINE,
        VSPLIT
    }

    public static final String[] MODE_NAMES = { "DEFAULT", "SIGHTLINE", "VSPLIT" };

    private AimMode mode = AimMode.DEFAULT;

    private float hitboxBoundsMin = 0.05f;
    private float hitboxBoundsMax = 0.95f;

    private boolean extrapolationEnabled = false;
    private boolean pingCompensation = false;
    private float contractionMin = 0.03f;
    private float contractionMax = 0.22f;
    private int lagDelayTicks = 0;

    private final TrackingContext tracking = new TrackingContext();

    public AimAssistRotation(RotationState state) {
        super(state);
    }

    // ── Mode ──────────────────────────────────────────────────

    public void setMode(AimMode mode) {
        this.mode = mode;
    }

    public void setMode(int ordinal) {
        AimMode[] modes = AimMode.values();
        this.mode = (ordinal >= 0 && ordinal < modes.length) ? modes[ordinal] : AimMode.DEFAULT;
    }

    public AimMode getMode() {
        return mode;
    }

    public boolean isDefaultMode() {
        return mode == AimMode.DEFAULT;
    }

    // ── Hitbox bounds ─────────────────────────────────────────

    public void setHitboxBounds(float min, float max) {
        this.hitboxBoundsMin = MathHelper.clamp_float(min, 0.0f, 1.5f);
        this.hitboxBoundsMax = MathHelper.clamp_float(max, 0.0f, 1.5f);
    }

    public float getHitboxBoundsMin() {
        return hitboxBoundsMin;
    }

    public float getHitboxBoundsMax() {
        return hitboxBoundsMax;
    }

    // ── Extrapolation / prediction ────────────────────────────

    public void setExtrapolationEnabled(boolean enabled) {
        this.extrapolationEnabled = enabled;
    }

    public boolean isExtrapolationEnabled() {
        return extrapolationEnabled;
    }

    public void setPingCompensation(boolean enabled) {
        this.pingCompensation = enabled;
    }

    public boolean isPingCompensation() {
        return pingCompensation;
    }

    public void setContractionRange(float min, float max) {
        this.contractionMin = MathHelper.clamp_float(min, 0.01f, 0.15f);
        this.contractionMax = MathHelper.clamp_float(max, 0.10f, 0.30f);
    }

    public float getContractionMin() {
        return contractionMin;
    }

    public float getContractionMax() {
        return contractionMax;
    }

    public void setLagDelayTicks(int ticks) {
        this.lagDelayTicks = Math.max(0, ticks);
    }

    public int getLagDelayTicks() {
        return lagDelayTicks;
    }

    public TrackingContext getTracking() {
        return tracking;
    }

    // ── Target computation ────────────────────────────────────

    /**
     * Compute raw target angles for the given entity based on the current aim mode.
     *
     * @param entity             the target entity
     * @param movementCorrection whether to apply movement correction adjustments
     * @return {@code [yaw, pitch]} or {@code null} for DEFAULT mode (handled
     *         externally)
     */
    public float[] computeTarget(EntityLivingBase entity, boolean movementCorrection) {
        switch (mode) {
            case SIGHTLINE:
                return calcSightline(entity, movementCorrection);
            case VSPLIT:
                return calcVSplit(entity);
            case DEFAULT:
            default:
                return null;
        }
    }

    /**
     * Full aim pipeline: compute target, apply smoothing + GCD correction.
     *
     * @param entity             the target entity
     * @param smoothFactor       0.0-1.0 dampening
     * @param hSpeed             horizontal speed multiplier (0-10)
     * @param vSpeed             vertical speed multiplier (0-10)
     * @param movementCorrection whether to apply movement correction
     * @return GCD-corrected {@code [yaw, pitch]} or {@code null} for DEFAULT mode
     */
    public float[] aimAtEntity(EntityLivingBase entity,
            float smoothFactor, float hSpeed, float vSpeed,
            boolean movementCorrection) {
        float[] raw = computeTarget(entity, movementCorrection);
        if (raw == null)
            return null;

        float effectiveSmooth = smoothFactor;
        if (movementCorrection && isPlayerMoving()) {
            effectiveSmooth *= 1.3f;
        }

        return smoothAndGCD(raw[0], raw[1], effectiveSmooth, hSpeed, vSpeed);
    }

    // ── Sightline (closest point on box) ──────────────────────

    private float[] calcSightline(EntityLivingBase entity, boolean movementCorrection) {
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        double shrinkValue = 0.1;
        if (movementCorrection && isPlayerMoving()) {
            shrinkValue = 0.2;
        }
        return RotationMath.toRotationsClosestPoint(bb, shrinkValue);
    }

    // ── VSPLIT (dynamic vertical split with hitbox bounds) ────

    private float[] calcVSplit(EntityLivingBase entity) {
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        float border = entity.getCollisionBorderSize();
        AxisAlignedBB expandedBox = bb.expand(border, border, border);

        AxisAlignedBB workingBB = expandedBox;

        // Apply extrapolation if enabled
        if (extrapolationEnabled) {
            double velX = entity.posX - entity.prevPosX;
            double velY = entity.posY - entity.prevPosY;
            double velZ = entity.posZ - entity.prevPosZ;
            tracking.pushVelocity(velX, velZ);

            double variance = tracking.computeVelocityVariance();
            float confidence = 1.0f - MathHelper.clamp_float((float) (variance / 1.5), 0.0f, 1.0f);
            float ticksAhead = 3.0f;
            if (pingCompensation) {
                ticksAhead += state.getEstimatedPingTicks();
            }

            workingBB = RotationMath.extrapolateBB(expandedBox, velX, velY, velZ, ticksAhead, confidence);
        }

        // Dynamic contraction
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
        contraction = MathHelper.clamp_float(contraction, contractionMin, contractionMax);
        workingBB = workingBB.contract(contraction, contraction, contraction);

        float verticalMultipoint = RotationMath.computeVerticalMultipointMotionAware(
                entity.posY, entity.motionY);

        tracking.storeTargetPosition(entity.posX, entity.posY, entity.posZ);

        return RotationMath.toRotationsBoxDynamic(
                workingBB, hitboxBoundsMin, hitboxBoundsMax, verticalMultipoint);
    }

    private boolean isPlayerMoving() {
        return mc.thePlayer != null
                && (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0);
    }
}
