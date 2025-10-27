/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.*;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ChatUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.SoundUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BedTrackerModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final ScheduledExecutorService executor;
    private final LinkedHashMap<String, Long> alertCooldowns;
    private final LinkedHashSet<EntityEnderPearl> trackedPearls;
    private final LinkedHashSet<String> whitelistedPlayers;
    private final Color wBed;
    private final Color rBed;
    private final Color yBed;
    private final Color gBed;
    private BlockPos bedPos;
    private long lastMarcoTime;
    private boolean waiting;

    public final BooleanProperty alerts;
    public final IntProperty alertRange;
    public final BooleanProperty alertOnPearl;
    public final ModeProperty alertSound;
    public final IntProperty alertFrequency;
    public final BooleanProperty marco;
    public final IntProperty marcoRange;
    public final BooleanProperty marcoOnPearl;
    public final TextProperty marcoText;
    public final IntProperty marcoDelay;
    public final BooleanProperty hud;
    public final ModeProperty hudPosX;
    public final ModeProperty hudPosY;
    public final IntProperty hudOffX;
    public final IntProperty hudOffY;
    public final FloatProperty hudScale;
    public final BooleanProperty hudShadow;

    public BedTrackerModule() {
        super("BedTracker", false, true);
        this.executor = Executors.newScheduledThreadPool(1);
        this.alertCooldowns = new LinkedHashMap<>();
        this.trackedPearls = new LinkedHashSet<>();
        this.whitelistedPlayers = new LinkedHashSet<>();
        this.wBed = new Color(0xFFFFFF);
        this.rBed = new Color(0xFF5555);
        this.yBed = new Color(0xFFFF55);
        this.gBed = new Color(0x55FF55);
        this.bedPos = null;
        this.lastMarcoTime = -1L;
        this.waiting = false;
        this.alerts = new BooleanProperty("alerts", true);
        this.alertRange = new IntProperty("alerts-range", 48, 8, 128, alerts::getValue);
        this.alertOnPearl = new BooleanProperty("alerts-on-pearl", true);
        this.alertSound = new ModeProperty("alerts-sound", 1, new String[] { "NONE", "MEOW", "ANVIL" },
                () -> alerts.getValue() || alertOnPearl.getValue());
        this.alertFrequency = new IntProperty("alerts-frequency", 5, 1, 30,
                () -> alerts.getValue() || alertOnPearl.getValue());
        this.marco = new BooleanProperty("macro", false);
        this.marcoRange = new IntProperty("macro-range", 24, 8, 128, marco::getValue);
        this.marcoOnPearl = new BooleanProperty("macro-on-pearl", false);
        this.marcoText = new TextProperty("macro-text", "/lobby",
                () -> marco.getValue() || marcoOnPearl.getValue());
        this.marcoDelay = new IntProperty("macro-delay", 1, 1, 10,
                () -> marco.getValue() || marcoOnPearl.getValue());
        this.hud = new BooleanProperty("hud", true);
        this.hudPosX = new ModeProperty("hud-position-x", 0, new String[] { "LEFT", "MIDDLE", "RIGHT" }, hud::getValue);
        this.hudPosY = new ModeProperty("hud-position-y", 0, new String[] { "TOP", "MIDDLE", "BOTTOM" }, hud::getValue);
        this.hudOffX = new IntProperty("hud-offset-x", 2, 0, 255, hud::getValue);
        this.hudOffY = new IntProperty("hud-offset-y", 2, 0, 255, hud::getValue);
        this.hudScale = new FloatProperty("hud-scale", 1.0F, 0.5F, 1.5F, hud::getValue);
        this.hudShadow = new BooleanProperty("hud-shadow", true, hud::getValue);
    }

    private void playAlertSound() {
        switch (alertSound.getValue()) {
            case 1:
                SoundUtil.playSound("mob.cat.meow");
                break;
            case 2:
                SoundUtil.playSound("random.anvil_land");
                break;
        }
    }

    private Color getHudColor(int distance) {
        if (distance < 0) {
            return wBed;
        } else if (distance <= 100) {
            return gBed;
        } else if (distance <= 114) {
            float factor = (float) (114 - distance) / 14.0F;
            return new Color(
                    (int) (yBed.getRed() * factor + gBed.getRed() * (1 - factor)),
                    (int) (yBed.getGreen() * factor + gBed.getGreen() * (1 - factor)),
                    (int) (yBed.getBlue() * factor + gBed.getBlue() * (1 - factor)));
        } else {
            if (distance > 128) {
                return rBed;
            }
            float factor = (float) (128 - distance) / 14.0F;
            return new Color(
                    (int) (rBed.getRed() * factor + yBed.getRed() * (1 - factor)),
                    (int) (rBed.getGreen() * factor + yBed.getGreen() * (1 - factor)),
                    (int) (rBed.getBlue() * factor + yBed.getBlue() * (1 - factor)));
        }
    }

    private boolean isBed(BlockPos blockPos) {
        return blockPos != null && mc.theWorld != null &&
                mc.theWorld.getBlockState(blockPos).getBlock() == Blocks.bed;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null || !isBed(bedPos)) {
            return;
        }

        long millis = System.currentTimeMillis();
        boolean pearl = false;
        boolean macro = false;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityEnderPearl) {
                EntityEnderPearl enderPearl = (EntityEnderPearl) entity;
                if (!trackedPearls.contains(enderPearl)) {
                    trackedPearls.add(enderPearl);
                    if (alertOnPearl.getValue()) {
                        pearl = true;
                        ChatUtil.sendRaw(
                                ChatUtil.formatColor(
                                        String.format(
                                                "&7[&bCoffeeClient&7] &fBedTracker: &cPearl detected&r")));
                    }
                    if (marcoOnPearl.getValue() && lastMarcoTime + (long) marcoDelay.getValue() * 1000L <= millis) {
                        lastMarcoTime = millis;
                        macro = true;
                    }
                }
            }
        }

        for (EntityPlayer player : mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer) entity)
                .filter(entityPlayer -> entityPlayer != mc.thePlayer
                        && !TeamUtil.isBot(entityPlayer)
                        && !whitelistedPlayers.contains(entityPlayer.getDisplayName().getUnformattedText()))
                .collect(Collectors.toList())) {

            if (TeamUtil.isSameTeam(player)) {
                whitelistedPlayers.add(player.getDisplayName().getUnformattedText());
            } else {
                double distance = player.getDistance(
                        (double) bedPos.getX() + 0.5,
                        (double) bedPos.getY() + 0.5,
                        (double) bedPos.getZ() + 0.5);
                String displayName = player.getDisplayName().getUnformattedText();
                String text = player.getDisplayName().getFormattedText();
                ItemStack item = player.getHeldItem();
                boolean isPearl = item != null && item.getItem() instanceof ItemEnderPearl;

                if (alerts.getValue() && distance < (double) alertRange.getValue()) {
                    Long cooldown = alertCooldowns.get(displayName);
                    if (cooldown == null || cooldown + (long) alertFrequency.getValue() * 1000L <= millis) {
                        alertCooldowns.put(displayName, millis);
                        pearl = true;
                        ChatUtil.sendRaw(
                                ChatUtil.formatColor(
                                        String.format(
                                                "&7[&bCoffeeClient&7] &fBedTracker: &c%s &7is &c%dm &7away&r",
                                                text,
                                                (int) distance)));
                    }
                }

                if (alertOnPearl.getValue() && isPearl) {
                    Long cooldown = alertCooldowns.get(displayName);
                    if (cooldown == null || cooldown + (long) alertFrequency.getValue() * 1000L <= millis) {
                        alertCooldowns.put(displayName, millis);
                        pearl = true;
                        ChatUtil.sendRaw(
                                ChatUtil.formatColor(
                                        String.format(
                                                "&7[&bCoffeeClient&7] &fBedTracker: &c%s &7has &6pearl&r",
                                                text)));
                    }
                }

                if ((marco.getValue() && distance < (double) marcoRange.getValue()
                        || marcoOnPearl.getValue() && isPearl) &&
                        lastMarcoTime + (long) marcoDelay.getValue() * 1000L <= millis) {
                    lastMarcoTime = millis;
                    macro = true;
                }
            }
        }

        if (pearl) {
            playAlertSound();
        }

        if (macro) {
            ChatUtil.sendRaw(
                    ChatUtil.formatColor(
                            String.format(
                                    "&7[&bCoffeeClient&7] &fBedTracker: &fRunning &6%s&r",
                                    marcoText.getValue())));
            ChatUtil.sendChatMessage(marcoText.getValue());
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || !hud.getValue()) {
            return;
        }

        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null || mc.gameSettings.showDebugInfo) {
            return;
        }

        GuiScreen currentScreen = mc.currentScreen;
        if (currentScreen != null && !(currentScreen instanceof GuiChat)) {
            return;
        }

        int distanceSq = 0;
        boolean hasBed = isBed(bedPos);
        if (hasBed) {
            double dx = bedPos.getX() + 0.5 - mc.thePlayer.posX;
            double dy = bedPos.getY() + 0.5 - mc.thePlayer.posY;
            double dz = bedPos.getZ() + 0.5 - mc.thePlayer.posZ;
            distanceSq = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        String text = ChatUtil.formatColor(
                String.format(
                        "&fBed: %s%s",
                        !hasBed ? "&cfalse&r" : "&atrue&r",
                        !hasBed ? ""
                                : String.format(" &7| &fDistance: &r%d%s", distanceSq,
                                        distanceSq >= 128 ? " &c&l⚠&r" : "")));

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float width = (float) mc.fontRendererObj.getStringWidth(text);
        float height = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
        float scaleX = (float) hudOffX.getValue() / hudScale.getValue();

        switch (hudPosX.getValue()) {
            case 0:
                break;
            case 1:
                scaleX += ((float) scaledResolution.getScaledWidth() / hudScale.getValue() - width) / 2.0F;
                break;
            case 2:
                scaleX += (float) scaledResolution.getScaledWidth() / hudScale.getValue() - width;
                break;
        }

        float offsetY = (float) hudOffY.getValue() / hudScale.getValue();

        switch (hudPosY.getValue()) {
            case 0:
                break;
            case 1:
                offsetY += ((float) scaledResolution.getScaledHeight() / hudScale.getValue() - height) / 2.0F;
                break;
            case 2:
                offsetY += (float) scaledResolution.getScaledHeight() / hudScale.getValue() - height;
                break;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(hudScale.getValue(), hudScale.getValue(), 1.0F);
        GlStateManager.translate(scaleX, offsetY, 0.0F);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.fontRendererObj.drawString(text, 0.0F, 0.0F, getHudColor(distanceSq).getRGB(), hudShadow.getValue());
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        waiting = false;
    }

    public void onPacketReceive(Object packet) {
        if (!isEnabled()) {
            return;
        }

        if (packet instanceof S02PacketChat) {
            String msg = ((S02PacketChat) packet).getChatComponent().getFormattedText();
            if (msg.contains("§e§lProtect your bed and destroy the enemy bed") ||
                    msg.contains("§e§lDestroy the enemy bed and then eliminate them")) {
                alertCooldowns.clear();
                trackedPearls.clear();
                whitelistedPlayers.clear();
                bedPos = null;
                waiting = true;
            }
        }

        if (packet instanceof S08PacketPlayerPosLook && waiting) {
            waiting = false;
            executor.schedule(() -> {
                if (mc.thePlayer == null) {
                    return;
                }

                int x = MathHelper.floor_double(mc.thePlayer.posX);
                int y = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
                int z = MathHelper.floor_double(mc.thePlayer.posZ);

                for (int i = x - 25; i <= x + 25; i++) {
                    for (int j = y - 10; j <= y + 10; j++) {
                        for (int k = z - 25; k <= z + 25; k++) {
                            BlockPos pos = new BlockPos(i, j, k);
                            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.bed) {
                                double distance = mc.thePlayer.getDistance((double) i + 0.5, (double) j + 0.5,
                                        (double) k + 0.5);
                                if (bedPos == null || distance < mc.thePlayer.getDistance(
                                        (double) bedPos.getX() + 0.5,
                                        (double) bedPos.getY() + 0.5,
                                        (double) bedPos.getZ() + 0.5)) {
                                    bedPos = pos;
                                }
                            }
                        }
                    }
                }
            }, 3000L, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onDisabled() {
        alertCooldowns.clear();
        trackedPearls.clear();
        whitelistedPlayers.clear();
        bedPos = null;
    }
}
