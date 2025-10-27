/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.FloatProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.ModeProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ColorUtil;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HUDModule extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private List<Module> activeModules = new ArrayList<>();

    public final ModeProperty colorMode = new ModeProperty("color-mode", 3,
            new String[] { "RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123" });
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F);
    public final IntProperty colorSaturation = new IntProperty("color-saturation", 50, 0, 100);
    public final IntProperty colorBrightness = new IntProperty("color-brightness", 100, 0, 100);
    public final IntProperty custom1 = new IntProperty("custom1", Color.WHITE.getRGB(), Integer.MIN_VALUE,
            Integer.MAX_VALUE);
    public final IntProperty custom2 = new IntProperty("custom2", Color.WHITE.getRGB(), Integer.MIN_VALUE,
            Integer.MAX_VALUE);
    public final IntProperty custom3 = new IntProperty("custom3", Color.WHITE.getRGB(), Integer.MIN_VALUE,
            Integer.MAX_VALUE);

    public final IntProperty posX = new IntProperty("pos-x", 0, 0, 1);
    public final IntProperty posY = new IntProperty("pos-y", 0, 0, 1);
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 1000);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 1000);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.1F, 5.0F);
    public final IntProperty background = new IntProperty("background", 25, 0, 100);
    public final BooleanProperty showBar = new BooleanProperty("show-bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);

    public HUDModule() {
        super("HUD", true);
    }

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    private String[] getModuleSuffix(Module module) {
        return new String[0];
    }

    private int getModuleWidth(Module module) {
        return calculateStringWidth(getModuleName(module), getModuleSuffix(module));
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width = mc.fontRendererObj.getStringWidth(string);
        if (suffixes.getValue()) {
            for (String str : arr) {
                width += 3 + mc.fontRendererObj.getStringWidth(str);
            }
        }
        return width;
    }

    private float getColorCycle(long time, long offset) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(time - offset * 300L) % speed) / (float) speed;
    }

    public Color getColor(long time) {
        return getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.white;
        switch (colorMode.getValue()) {
            case 0:
                color = ColorUtil.fromHSB(getColorCycle(time, offset), 1.0F, 1.0F);
                break;
            case 1:
                color = ColorUtil.fromHSB(getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                break;
            case 2:
                float cycle = getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) {
                    cycle = 1.0F - cycle % 1.0F;
                }
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3:
                color = new Color(custom1.getValue());
                break;
            case 4:
                double cycle1 = getColorCycle(time, offset);
                color = ColorUtil.interpolate(
                        (float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))),
                        new Color(custom1.getValue()),
                        new Color(custom2.getValue()));
                break;
            case 5:
                double cycle2 = getColorCycle(time, offset);
                float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                if (floor <= 0.5F) {
                    color = ColorUtil.interpolate(floor * 2.0F, new Color(custom1.getValue()),
                            new Color(custom2.getValue()));
                } else {
                    color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(custom2.getValue()),
                            new Color(custom3.getValue()));
                }
        }

        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * (colorSaturation.getValue() / 100.0F),
                hsb[2] * (colorBrightness.getValue() / 100.0F));
    }

    public void updateActiveModules() {
        if (isEnabled() && CoffeeClient.moduleManager != null) {
            activeModules = CoffeeClient.moduleManager.modules.values().stream()
                    .filter(module -> module.isEnabled() && !module.isHidden())
                    .sorted(Comparator.comparingInt(this::getModuleWidth).reversed())
                    .collect(Collectors.toList());
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (!isEnabled() || mc.gameSettings.showDebugInfo) {
            return;
        }

        updateActiveModules();

        float height = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
        float x = (float) offsetX.getValue()
                + (1.0F + (showBar.getValue() ? (shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * scale.getValue();
        float y = (float) offsetY.getValue() + 1.0F * scale.getValue();

        if (posX.getValue() == 1) {
            x = (float) new ScaledResolution(mc).getScaledWidth() - x;
        }
        if (posY.getValue() == 1) {
            y = (float) new ScaledResolution(mc).getScaledHeight() - y - height * scale.getValue();
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale.getValue(), scale.getValue(), 0.0F);

        long l = System.currentTimeMillis();
        long offset = 0L;

        for (Module module : activeModules) {
            String moduleName = getModuleName(module);
            String[] moduleSuffix = getModuleSuffix(module);
            float totalWidth = (float) (calculateStringWidth(moduleName, moduleSuffix) - (shadow.getValue() ? 0 : 1));
            int color = getColor(l, offset).getRGB();

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            if (background.getValue() > 0) {
                Utils.drawRect(
                        x / scale.getValue() - 1.0F - (posX.getValue() == 0 ? 0.0F : totalWidth),
                        y / scale.getValue() - (posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F)
                                : (shadow.getValue() ? 1.0F : 0.0F)),
                        x / scale.getValue() + 1.0F + (posX.getValue() == 0 ? totalWidth : 0.0F),
                        y / scale.getValue() + height
                                + (posY.getValue() == 0 ? (shadow.getValue() ? 1.0F : 0.0F)
                                        : (offset == 0L ? 1.0F : 0.0F)),
                        new Color(0.0F, 0.0F, 0.0F, background.getValue() / 100.0F).getRGB());
            }

            if (showBar.getValue()) {
                if (shadow.getValue()) {
                    Utils.drawRect(
                            x / scale.getValue() + (posX.getValue() == 0 ? -3.0F : 1.0F),
                            y / scale.getValue() - (posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                            x / scale.getValue() + (posX.getValue() == 0 ? -2.0F : 2.0F),
                            y / scale.getValue() + height
                                    + (posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                            color);
                    Utils.drawRect(
                            x / scale.getValue() + (posX.getValue() == 0 ? -2.0F : 2.0F),
                            y / scale.getValue() - (posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                            x / scale.getValue() + (posX.getValue() == 0 ? -1.0F : 3.0F),
                            y / scale.getValue() + height
                                    + (posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                            (color & 16579836) >> 2 | color & 0xFF000000);
                } else {
                    Utils.drawRect(
                            x / scale.getValue() + (posX.getValue() == 0 ? -2.0F : 1.0F),
                            y / scale.getValue() - (posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F),
                            x / scale.getValue() + (posX.getValue() == 0 ? -1.0F : 2.0F),
                            y / scale.getValue() + height
                                    + (posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F)),
                            color);
                }
            }

            GlStateManager.disableDepth();

            if (shadow.getValue()) {
                mc.fontRendererObj.drawStringWithShadow(
                        moduleName,
                        x / scale.getValue() - (posX.getValue() == 1 ? totalWidth : 0.0F),
                        y / scale.getValue(),
                        color);
            } else {
                mc.fontRendererObj.drawString(
                        moduleName,
                        x / scale.getValue() - (posX.getValue() == 1 ? totalWidth : 0.0F),
                        y / scale.getValue() + (posY.getValue() == 1 ? 1.0F : 0.0F),
                        color,
                        false);
            }

            y += (height + (shadow.getValue() ? 1.0F : 0.0F)) * scale.getValue()
                    * (posY.getValue() == 0 ? 1.0F : -1.0F);
            offset++;
        }

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }
}
