/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.rotation;

import io.github.moulberry.notenoughupdates.coffeeclient.events.PacketEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Singleton server-side rotation tracker.
 * <p>
 * Intercepts outgoing C03/C05/C06 packets to maintain an accurate
 * picture of what yaw/pitch the server believes the player has.
 * Also snapshots client-side angles before any feature spoofs them.
 * <p>
 * Register once via {@link #register()} during init; shared
 * by all rotation consumers (AimAssist, etc.).
 *
 * @author axle.coffee
 */
public final class RotationState {

    private static final RotationState INSTANCE = new RotationState();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private float serverYaw;
    private float serverPitch;

    /** Client-side angles captured before any feature modifies them. */
    private float clientYaw;
    private float clientPitch;

    private boolean initialized;

    private RotationState() {
    }

    public static RotationState getInstance() {
        return INSTANCE;
    }

    /**
     * Register on the Forge event bus. Safe to call multiple times —
     * only registers once.
     */
    public void register() {
        if (!initialized) {
            MinecraftForge.EVENT_BUS.register(this);
            initialized = true;
        }
    }

    /**
     * Track server-side rotation from outgoing packets.
     * Uses HIGHEST priority so we see the packet before any feature
     * can cancel or modify it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!event.isSend())
            return;
        if (!(event.getPacket() instanceof C03PacketPlayer))
            return;
        C03PacketPlayer packet = (C03PacketPlayer) event.getPacket();

        if (packet instanceof C03PacketPlayer.C05PacketPlayerLook
                || packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            serverYaw = packet.getYaw();
            serverPitch = packet.getPitch();
        }
    }

    /**
     * Snapshot the player's current (visual) rotation before any feature
     * spoofs it. Call once per tick at the start of rotation processing.
     */
    public void captureClientAngles() {
        if (mc.thePlayer != null) {
            clientYaw = mc.thePlayer.rotationYaw;
            clientPitch = mc.thePlayer.rotationPitch;
        }
    }

    /**
     * Force-set the server angles (e.g. on module enable when no packet
     * has been sent yet).
     */
    public void setServerAngles(float yaw, float pitch) {
        this.serverYaw = yaw;
        this.serverPitch = pitch;
    }

    public float getServerYaw() {
        return serverYaw;
    }

    public float getServerPitch() {
        return serverPitch;
    }

    public float getClientYaw() {
        return clientYaw;
    }

    public float getClientPitch() {
        return clientPitch;
    }

    private float estimatedPingTicks;

    public void updatePing() {
        if (mc.thePlayer == null || mc.getNetHandler() == null)
            return;
        try {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) {
                estimatedPingTicks = info.getResponseTime() / 50.0f;
            }
        } catch (Exception ignored) {
        }
    }

    public float getEstimatedPingTicks() {
        return estimatedPingTicks;
    }
}
