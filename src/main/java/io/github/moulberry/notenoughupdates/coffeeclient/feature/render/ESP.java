package io.github.moulberry.notenoughupdates.coffeeclient.feature.render;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.*;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ColorUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RenderUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.TeamUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.shader.GlowShader;
import io.github.moulberry.notenoughupdates.coffeeclient.util.shader.OutlineShader;
import io.github.moulberry.notenoughupdates.mixins.AccessorEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ESP extends Feature {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final OutlineShader outlineShader = new OutlineShader();
    private final GlowShader glowShader = new GlowShader();

    private Framebuffer framebuffer = null;

    private boolean outline = true;
    private boolean glow = true;

    private final List<EntityPlayer> renderedEntities = new ArrayList<>();

    public final ModeProperty mode = new ModeProperty("mode", 2,
            new String[] { "NONE", "2D", "3D", "OUTLINE", "BOX" });

    public final ModeProperty colorMode = new ModeProperty("color", 0,
            new String[] { "DEFAULT", "TEAMS", "RAINBOW" });

    public final ModeProperty healthBar = new ModeProperty("health-bar", 0, new String[] { "NONE", "2D", "RAVEN" });

    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty bots = new BooleanProperty("bots", false);
    public final BooleanProperty self = new BooleanProperty("self", false);

    public final FloatProperty lineWidth = new FloatProperty("line-width", 1.5f, 0.5f, 5.0f);

    public ESP() {
        super("ESP", false);
    }

    public boolean isOutlineEnabled() {
        return this.outline;
    }

    public boolean isGlowEnabled() {
        return this.glow;
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        int modeValue = mode.getValue();

        if (modeValue == 3) {
            if (!renderedEntities.isEmpty()) {
                if (this.framebuffer == null) {
                    this.framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
                }

                GlStateManager.pushMatrix();
                GlStateManager.pushAttrib();

                this.framebuffer.bindFramebuffer(false);
                boolean shadow = mc.gameSettings.entityShadows;
                mc.gameSettings.entityShadows = false;

                this.outline = false;
                this.glow = false;
                this.glowShader.use();

                for (EntityPlayer player : renderedEntities) {
                    Color color = getEntityColor(player);
                    this.glowShader.setColor(color);

                    boolean invisible = player.isInvisible();
                    player.setInvisible(false);
                    mc.getRenderManager().renderEntityStatic(player, event.partialTicks, true);
                    player.setInvisible(invisible);
                }

                this.glowShader.stop();
                this.glow = true;
                this.outline = true;
                mc.gameSettings.entityShadows = shadow;

                mc.getFramebuffer().bindFramebuffer(false);
                GlStateManager.popAttrib();
                GlStateManager.popMatrix();
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        int modeValue = mode.getValue();

        if (modeValue == 3) {
            if (!renderedEntities.isEmpty() && this.framebuffer != null) {
                
                this.outlineShader.use();
                RenderUtil.drawFramebuffer(this.framebuffer);
                this.outlineShader.stop();
                this.framebuffer.framebufferClear();
            }
        }

        if (modeValue == 1 || healthBar.getValue() == 1) {
            if (!renderedEntities.isEmpty()) {
                RenderUtil.enableRenderState();

                ScaledResolution sr = new ScaledResolution(mc);
                double scaleFactor = sr.getScaleFactor();
                double scale = scaleFactor / Math.pow(scaleFactor, 2.0);

                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, scale);

                for (EntityPlayer player : renderedEntities) {
                    
                    ((AccessorEntityRenderer) mc.entityRenderer)
                            .invokeSetupCameraTransform(event.partialTicks, 0);

                    float[] screenPos = RenderUtil.projectToScreen(player, scaleFactor, event.partialTicks);

                    mc.entityRenderer.setupOverlayRendering();

                    if (screenPos != null) {
                        float x = screenPos[0];
                        float y = screenPos[1];
                        float z = screenPos[2];
                        float w = screenPos[3];

                        if (modeValue == 1) {
                            int color = getEntityColor(player).getRGB();
                            
                            RenderUtil.drawOutlineRect(x, y, z, w, 3.0f, 0,
                                    (color & 0x00FCFCFC) >> 2 | color & 0xFF000000);
                            
                            RenderUtil.drawOutlineRect(x, y, z, w, 1.5f, 0, color);
                        }

                        if (healthBar.getValue() == 1) {
                            float heal = player.getHealth() + player.getAbsorptionAmount();
                            float percent = Math.min(Math.max(heal / player.getMaxHealth(), 0.0f), 1.0f);
                            float box = (z - x) * 0.08f;
                            Color healthColor = ColorUtil.getHealthBlend(percent);
                            RenderUtil.drawLine(x - box, y, x - box, w,
                                    3.0f, ColorUtil.darker(healthColor, 0.2f).getRGB());
                            RenderUtil.drawLine(x - box, w, x - box, w + (y - w) * percent,
                                    1.5f, healthColor.getRGB());
                        }
                    }
                }

                GlStateManager.popMatrix();
                RenderUtil.disableRenderState();
            }
        }
    }

    private boolean shouldRenderPlayer(EntityPlayer player) {
        if (player.isDead || player.getHealth() <= 0) {
            return false;
        }

        if (mc.getRenderViewEntity().getDistanceToEntity(player) > 512.0F) {
            return false;
        }

        if (player == mc.thePlayer && !self.getValue()) {
            return false;
        }

        if (player == mc.thePlayer && self.getValue() && mc.gameSettings.thirdPersonView == 0) {
            return false;
        }

        if (TeamUtil.isBot(player) && !bots.getValue()) {
            return false;
        }

        return players.getValue();
    }

    private Color getEntityColor(EntityPlayer player) {
        switch (colorMode.getValue()) {
            case 0:
            case 1:
                return TeamUtil.getTeamColor(player, 1.0f);
            case 2:
                HUD hud = (HUD) CoffeeClient.featureManager.getFeature(HUD.class);
                if (hud != null) {
                    return hud.getColor(System.currentTimeMillis());
                }
                return Color.WHITE;
            default:
                return Color.WHITE;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        int modeValue = mode.getValue();

        if (modeValue == 0) {
            return;
        }

        renderedEntities.clear();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!shouldRenderPlayer(player)) {
                continue;
            }

            renderedEntities.add(player);
            Color color = getEntityColor(player);

            if (modeValue == 1 || modeValue == 3) {
                continue;
            }

            double renderPosX = mc.getRenderManager().viewerPosX;
            double renderPosY = mc.getRenderManager().viewerPosY;
            double renderPosZ = mc.getRenderManager().viewerPosZ;

            double x = RenderUtil.interpolate(player.posX, player.lastTickPosX, event.partialTicks) - renderPosX;
            double y = RenderUtil.interpolate(player.posY, player.lastTickPosY, event.partialTicks) - renderPosY;
            double z = RenderUtil.interpolate(player.posZ, player.lastTickPosZ, event.partialTicks) - renderPosZ;

            AxisAlignedBB box = player.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
            AxisAlignedBB renderBox = new AxisAlignedBB(
                    box.minX - player.posX + x,
                    box.minY - player.posY + y,
                    box.minZ - player.posZ + z,
                    box.maxX - player.posX + x,
                    box.maxY - player.posY + y,
                    box.maxZ - player.posZ + z);

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, 1.0f);

            if (modeValue >= 2 && modeValue != 3) {
                RenderUtil.drawBoundingBox(renderBox, color, lineWidth.getValue());
            }

            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();

            int healthBarMode = healthBar.getValue();
            if (healthBarMode == 2) {
                float health = player.getHealth() + player.getAbsorptionAmount();
                float maxHealth = player.getMaxHealth();
                float percent = Math.min(Math.max(health / maxHealth, 0.0F), 1.0F);
                Color healthColor = ColorUtil.getHealthBlend(percent);
                float height = player.height + 0.2F;

                GlStateManager.pushMatrix();
                GlStateManager.translate(x, y, z);
                GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.disableDepth();
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldRenderer = tessellator.getWorldRenderer();

                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                worldRenderer.pos(0.5725, -0.0275, 0.7275).color(0, 0, 0, 255).endVertex();
                worldRenderer.pos(0.5725, height + 0.0275, 0.7275).color(0, 0, 0, 255).endVertex();
                worldRenderer.pos(0.7275, height + 0.0275, 0.7275).color(0, 0, 0, 255).endVertex();
                worldRenderer.pos(0.7275, -0.0275, 0.7275).color(0, 0, 0, 255).endVertex();
                tessellator.draw();

                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                worldRenderer.pos(0.6, 0.0, 0.7).color(64, 64, 64, 255).endVertex();
                worldRenderer.pos(0.6, height, 0.7).color(64, 64, 64, 255).endVertex();
                worldRenderer.pos(0.7, height, 0.7).color(64, 64, 64, 255).endVertex();
                worldRenderer.pos(0.7, 0.0, 0.7).color(64, 64, 64, 255).endVertex();
                tessellator.draw();

                float healthHeight = height * percent;
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                worldRenderer.pos(0.6, 0.0, 0.7).color(healthColor.getRed(), healthColor.getGreen(),
                        healthColor.getBlue(), healthColor.getAlpha()).endVertex();
                worldRenderer.pos(0.6, healthHeight, 0.7).color(healthColor.getRed(), healthColor.getGreen(),
                        healthColor.getBlue(), healthColor.getAlpha()).endVertex();
                worldRenderer.pos(0.7, healthHeight, 0.7).color(healthColor.getRed(), healthColor.getGreen(),
                        healthColor.getBlue(), healthColor.getAlpha()).endVertex();
                worldRenderer.pos(0.7, 0.0, 0.7).color(healthColor.getRed(), healthColor.getGreen(),
                        healthColor.getBlue(), healthColor.getAlpha()).endVertex();
                tessellator.draw();

                GlStateManager.enableDepth();
                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }
    }
}
