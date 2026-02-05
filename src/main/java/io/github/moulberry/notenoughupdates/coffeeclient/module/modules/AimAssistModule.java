/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.FloatProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ItemUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.KeyBindUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RotationUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.TeamUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AimAssistModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 3.0F, 0.0F, 10.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 0.0F, 0.0F, 10.0F);
    public final IntProperty smoothing = new IntProperty("smoothing", 50, 0, 100);
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", true);
    public final BooleanProperty playersOnly = new BooleanProperty("players-only", true);
    public final BooleanProperty targetInvisible = new BooleanProperty("target-invisible", false);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, weaponsOnly::getValue);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true);
    public final BooleanProperty team = new BooleanProperty("teams", true);
    public final BooleanProperty dynamic = new BooleanProperty("dynamic", true);

    public AimAssistModule() {
        super("AimAssist", false);
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    private boolean isAttackKeyDown() {
        return KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            return;
        }

        if (weaponsOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant()) {
            if (!allowTools.getValue() || !ItemUtil.isHoldingTool()) {
                return;
            }
        }

        if (event.phase == TickEvent.Phase.END) {
            if (requirePress.getValue() && !isAttackKeyDown()) {
                return;
            }

            if (isAttackKeyDown() && isLookingAtBlock()) {
                return;
            }

            EntityLivingBase target = findTarget();

            if (target != null && RotationUtil.distanceToEntity(target) > 0.0) {
                AxisAlignedBB boundingBox = target.getEntityBoundingBox();
                double collisionBorderSize = target.getCollisionBorderSize();
                AxisAlignedBB expandedBox = boundingBox.expand(collisionBorderSize, collisionBorderSize,
                        collisionBorderSize);

                float verticalMultipoint = 0.5F;
                if (dynamic.getValue()) {
                    float yDiff = (float) (target.posY - mc.thePlayer.posY);
                    if (yDiff > 0.5F) {
                        verticalMultipoint = 1.0F;
                    } else if (yDiff < -0.5F) {
                        verticalMultipoint = 0.0F;
                    }
                }

                float[] rotations = RotationUtil.getRotationsToBoxDynamic(
                        expandedBox,
                        mc.thePlayer.rotationYaw,
                        mc.thePlayer.rotationPitch,
                        180.0F,
                        smoothing.getValue() / 100.0F,
                        verticalMultipoint);

                float yaw = Math.min(Math.abs(hSpeed.getValue()), 10.0F);
                float pitch = Math.min(Math.abs(vSpeed.getValue()), 10.0F);

                mc.thePlayer.rotationYaw += (rotations[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw;
                mc.thePlayer.rotationPitch += (rotations[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch;
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0F, 90.0F);
            }
        }
    }

    private EntityLivingBase findTarget() {
        EntityLivingBase bestTarget = null;
        double closestDistance = range.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase living = (EntityLivingBase) entity;

            if (living == mc.thePlayer) {
                continue;
            }

            if (living == mc.thePlayer.ridingEntity) {
                continue;
            }

            if (living.deathTime > 0) {
                continue;
            }

            if (playersOnly.getValue() && !(living instanceof EntityPlayer)) {
                continue;
            }

            if (!targetInvisible.getValue() && living.isInvisible()) {
                continue;
            }

            if (living instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) living;

                if (team.getValue() && TeamUtil.isSameTeam(player)) {
                    continue;
                }

                if (botCheck.getValue() && TeamUtil.isBot(player)) {
                    continue;
                }
            }

            double distance = RotationUtil.distanceToEntity(living);
            if (distance > range.getValue()) {
                continue;
            }

            float angleToEntity = RotationUtil.angleToEntity(living);
            if (angleToEntity > (float) fov.getValue()) {
                continue;
            }

            if (distance < closestDistance) {
                closestDistance = distance;
                bestTarget = living;
            }
        }

        return bestTarget;
    }
}
