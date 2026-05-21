/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package coffee.axle.coffeeclient.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * Static rotation math utilities for aim calculations.
 *
 * @author axle.coffee
 */
public final class RotationMath {

    private RotationMath() {
    }

    public static float[] toRotations(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.xCoord;
        double dy = ty - eye.yCoord;
        double dz = tz - eye.zCoord;
        double hd = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        yaw = MathHelper.wrapAngleTo180_float(yaw);

        float pitch = (float) Math.toDegrees(-Math.atan2(dy, hd));
        return new float[] { yaw, pitch };
    }

    public static float[] toRotationsFromDelta(double dx, double dy, double dz) {
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yaw = MathHelper.wrapAngleTo180_float(
                (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f);
        float pitch = (float) (-Math.atan2(dy, hd) * 180.0 / Math.PI);
        return new float[] { yaw, pitch };
    }

    public static float yawDiff(float from, float to) {
        return MathHelper.wrapAngleTo180_float(to - from);
    }

    public static float yawDistance(float a, float b) {
        return Math.abs(MathHelper.wrapAngleTo180_float(a - b));
    }

    public static boolean isInFOV(Entity entity, float fovDegrees) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null)
            return false;
        float angle = angleToEntity(entity);
        return angle <= fovDegrees;
    }

    public static float angleToEntity(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null)
            return Float.MAX_VALUE;
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0f);
        AxisAlignedBB bb = getExpandedBB(entity);
        if (bb.isVecInside(eye))
            return 0.0f;
        double dx = entity.posX - eye.xCoord;
        double dz = entity.posZ - eye.zCoord;
        return Math.abs(MathHelper.wrapAngleTo180_float(
                (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f
                        - mc.thePlayer.rotationYaw))
                * 2.0f;
    }

    public static double distanceToEntity(Entity entity) {
        return distanceToBox(getExpandedBB(entity));
    }

    public static double distanceToBox(AxisAlignedBB bb) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0f);
        if (bb.isVecInside(eye))
            return 0.0;
        return eye.distanceTo(closestPointOnBox(eye, bb));
    }

    public static Vec3 closestPointOnBox(Vec3 point, AxisAlignedBB bb) {
        return new Vec3(
                MathHelper.clamp_double(point.xCoord, bb.minX, bb.maxX),
                MathHelper.clamp_double(point.yCoord, bb.minY, bb.maxY),
                MathHelper.clamp_double(point.zCoord, bb.minZ, bb.maxZ));
    }

    public static AxisAlignedBB getExpandedBB(Entity entity) {
        float border = entity.getCollisionBorderSize();
        return entity.getEntityBoundingBox().expand(border, border, border);
    }

    public static float computeVerticalMultipoint(double targetY) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null)
            return 0.5f;
        float yDiff = (float) (targetY - mc.thePlayer.posY);
        if (yDiff > 0.5f)
            return 1.0f;
        if (yDiff < -0.5f)
            return 0.0f;
        return 0.5f;
    }

    public static float computeVerticalMultipointMotionAware(double targetY, double targetMotionY) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null)
            return 0.5f;
        float yDiff = (float) (targetY - mc.thePlayer.posY);

        float base;
        if (yDiff > 0.5f)
            base = 1.0f;
        else if (yDiff < -0.5f)
            base = 0.0f;
        else
            base = 0.5f;

        if (targetMotionY > 0.1)
            base = Math.max(0.0f, base - 0.25f);
        else if (targetMotionY < -0.1)
            base = Math.min(1.0f, base + 0.25f);

        return MathHelper.clamp_float(base, 0.0f, 1.0f);
    }

    /**
     * Aim at a dynamic vertical point within the bounding box.
     *
     * @param boundingBox        expanded entity bounding box
     * @param hitboxBoundsMin    minimum vertical clamp (0.0-1.5)
     * @param hitboxBoundsMax    maximum vertical clamp (0.0-1.5)
     * @param verticalMultipoint 0.0 = top, 1.0 = bottom, 0.5 = center
     * @return [yaw, pitch] target angles
     */
    public static float[] toRotationsBoxDynamic(AxisAlignedBB boundingBox,
            float hitboxBoundsMin, float hitboxBoundsMax,
            float verticalMultipoint) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0f);
        double boxHeight = boundingBox.maxY - boundingBox.minY;
        double clampedVMP = MathHelper.clamp_double(verticalMultipoint,
                hitboxBoundsMin, hitboxBoundsMax);
        double targetY = boundingBox.maxY - (boxHeight * clampedVMP);
        double dx = (boundingBox.minX + boundingBox.maxX) / 2.0 - eye.xCoord;
        double dy = targetY - eye.yCoord;
        double dz = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - eye.zCoord;
        return toRotationsFromDelta(dx, dy, dz);
    }

    /**
     * Aim at the closest point on a bounding box (sightline mode).
     */
    public static float[] toRotationsClosestPoint(AxisAlignedBB boundingBox, double shrinkValue) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0f);
        AxisAlignedBB contracted = boundingBox.contract(shrinkValue, shrinkValue, shrinkValue);

        double cx = MathHelper.clamp_double(eye.xCoord, contracted.minX, contracted.maxX);
        double cy = MathHelper.clamp_double(eye.yCoord, contracted.minY, contracted.maxY);
        double cz = MathHelper.clamp_double(eye.zCoord, contracted.minZ, contracted.maxZ);

        return toRotationsFromDelta(cx - eye.xCoord, cy - eye.yCoord, cz - eye.zCoord);
    }

    /**
     * Aim at a fixed vertical fraction of the bounding box.
     */
    public static float[] toRotationsFixedPoint(AxisAlignedBB boundingBox, float verticalFraction) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0f);
        double boxHeight = boundingBox.maxY - boundingBox.minY;
        double targetY = boundingBox.maxY - (boxHeight * MathHelper.clamp_double(verticalFraction, 0.0, 1.0));
        double dx = (boundingBox.minX + boundingBox.maxX) / 2.0 - eye.xCoord;
        double dy = targetY - eye.yCoord;
        double dz = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - eye.zCoord;
        return toRotationsFromDelta(dx, dy, dz);
    }

    /**
     * Compute a dynamic contraction value based on distance, angular velocity, and
     * target speed.
     */
    public static float computeDynamicContraction(double distance, float angularVelocity, double targetSpeed) {
        float base = 0.1f;
        float distFactor = 0.15f * (1.0f - (float) MathHelper.clamp_double(distance / 4.0, 0.0, 1.0));
        float angVelFactor = 0.05f * MathHelper.clamp_float(angularVelocity / 30.0f, 0.0f, 1.0f);
        float speedFactor = 0.03f * (float) MathHelper.clamp_double(targetSpeed / 0.2, 0.0, 1.0);
        float result = base + distFactor - angVelFactor - speedFactor;
        return MathHelper.clamp_float(result, 0.02f, 0.25f);
    }

    /**
     * Extrapolate a bounding box forward in time based on velocity.
     */
    public static AxisAlignedBB extrapolateBB(AxisAlignedBB bb, double velX, double velY, double velZ,
            float ticksAhead, float confidence) {
        double ox = velX * ticksAhead * confidence;
        double oy = velY * ticksAhead * confidence;
        double oz = velZ * ticksAhead * confidence;
        return bb.offset(ox, oy, oz);
    }
}
