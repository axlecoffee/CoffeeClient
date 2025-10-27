/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ChamsModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty bosses = new BooleanProperty("bosses", false);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty creepers = new BooleanProperty("creepers", false);
    public final BooleanProperty endermen = new BooleanProperty("endermen", false);
    public final BooleanProperty blazes = new BooleanProperty("blazes", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);
    public final BooleanProperty self = new BooleanProperty("self", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);

    public ChamsModule() {
        super("Chams", false);
    }

    private boolean shouldRenderChams(EntityLivingBase entity) {
        if (entity.deathTime > 0) {
            return false;
        }

        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > 512.0F) {
            return false;
        }

        if (entity instanceof EntityPlayer) {
            if (entity != mc.thePlayer && entity != mc.getRenderViewEntity()) {
                if (TeamUtil.isBot((EntityPlayer) entity)) {
                    return bots.getValue();
                }
                return players.getValue();
            } else {
                return self.getValue() && mc.gameSettings.thirdPersonView != 0;
            }
        }

        if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return !entity.isInvisible() && bosses.getValue();
        }

        if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntityCreeper) {
                return creepers.getValue();
            } else if (entity instanceof EntityEnderman) {
                return endermen.getValue();
            } else if (entity instanceof EntityBlaze) {
                return blazes.getValue();
            }
            return mobs.getValue();
        }

        if (entity instanceof EntityAnimal || entity instanceof EntityBat ||
                entity instanceof EntitySquid || entity instanceof EntityVillager) {
            return animals.getValue();
        }

        return false;
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!isEnabled()) {
            return;
        }

        if (shouldRenderChams(event.entity)) {
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0F, -2500000.0F);
        }
    }

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post<EntityLivingBase> event) {
        if (!isEnabled()) {
            return;
        }

        if (shouldRenderChams(event.entity)) {
            GL11.glPolygonOffset(1.0F, 2500000.0F);
            GL11.glDisable(32823);
        }
    }
}
