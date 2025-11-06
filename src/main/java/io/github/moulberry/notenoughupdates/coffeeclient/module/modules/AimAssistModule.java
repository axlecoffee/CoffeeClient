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
import io.github.moulberry.notenoughupdates.coffeeclient.util.RandomUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RotationUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.TeamUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ItemUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AimAssistModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private float targetYaw = 0.0F;
    private float targetPitch = 0.0F;

    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 5.0F, 1.0F, 20.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 5.0F, 1.0F, 20.0F);
    public final FloatProperty range = new FloatProperty("range", 3.5F, 1.0F, 6.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 10, 360);
    public final BooleanProperty onClick = new BooleanProperty("on-click", false);
    public final BooleanProperty playersOnly = new BooleanProperty("players-only", true);
    public final BooleanProperty targetInvisible = new BooleanProperty("target-invisible", false);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("ignore-teammates", true);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false);
    public final BooleanProperty team = new BooleanProperty("teams", true);

    public AimAssistModule() {
        super("AimAssist", false);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // Check weapons-only restriction
        if (weaponsOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant()) {
            if (!allowTools.getValue() || !ItemUtil.isHoldingTool()) {
                return;
            }
        }

        if (event.phase == TickEvent.Phase.END) {
            if (!onClick.getValue() || mc.gameSettings.keyBindAttack.isKeyDown()) {
                EntityLivingBase target = findTarget();

                if (target != null) {

                    float[] rotations = RotationUtil.getRotationsToBox(
                            target.getEntityBoundingBox(),
                            mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
                            180.0F, 0.0F);

                    targetYaw = rotations[0];
                    targetPitch = rotations[1];

                    float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
                    float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;

                    float maxYawChange = hSpeed.getValue() + (float) (RandomUtil.nextDouble(-1.0, 1.0));
                    float maxPitchChange = vSpeed.getValue() + (float) (RandomUtil.nextDouble(-1.0, 1.0));

                    float yawChange = MathHelper.clamp_float(yawDiff, -maxYawChange, maxYawChange);
                    float pitchChange = MathHelper.clamp_float(pitchDiff, -maxPitchChange, maxPitchChange);

                    mc.thePlayer.rotationYaw += yawChange;
                    mc.thePlayer.rotationPitch += pitchChange;
                    mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0F, 90.0F);
                }
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

            if (playersOnly.getValue() && !(living instanceof EntityPlayer)) {
                continue;
            }

            if (!targetInvisible.getValue() && living.isInvisible()) {
                continue;
            }

            if (living instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) living;

                // Check teams setting
                if (team.getValue() && TeamUtil.isSameTeam(player)) {
                    continue;
                }

                // Legacy ignore teammates check
                if (ignoreTeammates.getValue() && mc.thePlayer.isOnSameTeam(player)) {
                    continue;
                }

                // Check for bots
                if (TeamUtil.isBot(player)) {
                    continue;
                }
            }

            double distance = mc.thePlayer.getDistanceToEntity(living);
            if (distance > range.getValue()) {
                continue;
            }

            float angleToEntity = RotationUtil.angleToEntity(living);
            if (angleToEntity > fov.getValue() / 2.0F) {
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
