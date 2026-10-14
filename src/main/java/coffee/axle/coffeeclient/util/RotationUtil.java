package coffee.axle.coffeeclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float wrapAngleDiff(float angle, float target) {
        return target + MathHelper.wrapAngleTo180_float(angle - target);
    }

    public static float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0f, Math.min(180.0f, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }
        return angle;
    }

    public static float smoothAngle(float angle, float smoothFactor) {
        return angle * (0.5f
                + 0.5f * (1.0f - Math.max(0.0f, Math.min(1.0f, smoothFactor + RandomUtil.nextFloat(-0.1f, 0.1f)))));
    }

    public static float quantizeAngle(float angle) {
        return (float) ((double) angle - (double) angle % (double) 0.0096f);
    }

    public static float[] getRotationsToBox(AxisAlignedBB boundingBox, float yaw, float pitch, float maxAngle,
            float smoothFactor) {
        return getRotationsToBoxDynamic(boundingBox, yaw, pitch, maxAngle, smoothFactor, 0.5f);
    }

    /**
     * Calculate rotations to a bounding box with dynamic vertical targeting.
     * 
     * @param verticalMultipoint 0.0 = top of hitbox (head), 1.0 = bottom of hitbox
     *                           (feet), 0.5 = center
     */
    public static float[] getRotationsToBoxDynamic(AxisAlignedBB boundingBox, float yaw, float pitch, float maxAngle,
            float smoothFactor, float verticalMultipoint) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        double boxHeight = boundingBox.maxY - boundingBox.minY;
        double targetY = boundingBox.maxY - (boxHeight * Math.max(0.05, Math.min(0.95, verticalMultipoint)));
        double deltaX = (boundingBox.minX + boundingBox.maxX) / 2.0 - eyePos.xCoord;
        double deltaY = targetY - eyePos.yCoord;
        double deltaZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - eyePos.zCoord;
        return getRotations(deltaX, deltaY, deltaZ, yaw, pitch, maxAngle, smoothFactor);
    }

    public static float[] getRotationsTo(double targetX, double targetY, double targetZ, float currentYaw,
            float currentPitch) {
        return getRotations(targetX, targetY, targetZ, currentYaw, currentPitch, 180.0f, 0.0f);
    }

    public static float[] getRotations(double targetX, double targetY, double targetZ, float currentYaw,
            float currentPitch, float maxAngle, float smoothFactor) {
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);
        float yawDelta = MathHelper
                .wrapAngleTo180_float((float) (Math.atan2(targetZ, targetX) * 180.0 / Math.PI) - 90.0f - currentYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(
                (float) (-Math.atan2(targetY, horizontalDistance) * 180.0 / Math.PI) - currentPitch);
        yawDelta = Math.abs(yawDelta) <= 1.0f ? 0.0f : smoothAngle(clampAngle(yawDelta, maxAngle), smoothFactor);
        pitchDelta = Math.abs(pitchDelta) <= 1.0f ? 0.0f : smoothAngle(clampAngle(pitchDelta, maxAngle), smoothFactor);
        return new float[] { quantizeAngle(currentYaw + yawDelta), quantizeAngle(currentPitch + pitchDelta) };
    }

    public static Vec3 clampVecToBox(Vec3 vector, AxisAlignedBB boundingBox) {
        double[] coords = new double[] { vector.xCoord, vector.yCoord, vector.zCoord };
        double[] minCoords = new double[] { boundingBox.minX, boundingBox.minY, boundingBox.minZ };
        double[] maxCoords = new double[] { boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ };
        for (int i = 0; i < 3; ++i) {
            if (coords[i] > maxCoords[i]) {
                coords[i] = maxCoords[i];
                continue;
            }
            if (!(coords[i] < minCoords[i]))
                continue;
            coords[i] = minCoords[i];
        }
        return new Vec3(coords[0], coords[1], coords[2]);
    }

    public static double distanceToEntity(Entity entity) {
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        return distanceToBox(boundingBox);
    }

    public static double distanceToBox(Entity entity, Vec3 point) {
        float borderSize = entity.getCollisionBorderSize();
        return clampVecToBox(entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize), point);
    }

    public static double distanceToBox(AxisAlignedBB boundingBox) {
        return clampVecToBox(boundingBox, mc.thePlayer.getPositionEyes(1.0f));
    }

    public static double clampVecToBox(AxisAlignedBB boundingBox, Vec3 point) {
        if (boundingBox.isVecInside(point)) {
            return 0.0;
        }
        Vec3 clampedPoint = clampVecToBox(point, boundingBox);
        double deltaX = clampedPoint.xCoord - point.xCoord;
        double deltaY = clampedPoint.yCoord - point.yCoord;
        double deltaZ = clampedPoint.zCoord - point.zCoord;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public static float angleToEntity(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        if (boundingBox.isVecInside(eyePos)) {
            return 0.0f;
        }
        double deltaX = entity.posX - eyePos.xCoord;
        double deltaZ = entity.posZ - eyePos.zCoord;
        return Math
                .abs(MathHelper.wrapAngleTo180_float(
                        (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f - mc.thePlayer.rotationYaw))
                * 2.0f;
    }

    public static float getYawBetween(double x1, double z1, double x2, double z2) {
        return MathHelper.wrapAngleTo180_float(
                (float) (Math.atan2(z2 - z1, x2 - x1) * 180.0 / Math.PI) - 90.0f - mc.thePlayer.rotationYaw);
    }

    private static Vec3 getVectorForRotation(float pitch, float yaw) {
        float pitchCos = MathHelper.cos(-pitch * 0.017453292f);
        float pitchSin = MathHelper.sin(-pitch * 0.017453292f);
        float yawCos = MathHelper.cos(-yaw * 0.017453292f - (float) Math.PI);
        float yawSin = MathHelper.sin(-yaw * 0.017453292f - (float) Math.PI);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    public static MovingObjectPosition rayTrace(float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 lookVec = getVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance,
                lookVec.zCoord * distance);
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        Vec3 targetPos = clampVecToBox(eyePos,
                entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize));
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(AxisAlignedBB boundingBox, float yaw, float pitch, double distance) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 lookVec = getVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance,
                lookVec.zCoord * distance);
        return boundingBox.calculateIntercept(eyePos, targetPos);
    }
}
