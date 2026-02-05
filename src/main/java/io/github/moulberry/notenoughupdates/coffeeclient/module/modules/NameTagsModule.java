package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.*;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ColorUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NameTagsModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormatter = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));

    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 2.0F);
    public final BooleanProperty autoScale = new BooleanProperty("auto-scale", true);
    public final IntProperty backgroundOpacity = new IntProperty("background", 25, 0, 100);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final ModeProperty distanceMode = new ModeProperty("distance", 0,
            new String[] { "NONE", "DEFAULT", "VAPE" });
    public final ModeProperty healthMode = new ModeProperty("health", 2,
            new String[] { "NONE", "HP", "HEARTS", "TAB" });
    public final BooleanProperty armor = new BooleanProperty("armor", true);
    public final BooleanProperty enchants = new BooleanProperty("enchants", true, armor::getValue);
    public final BooleanProperty effects = new BooleanProperty("effects", true);
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty self = new BooleanProperty("self", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);
    public final BooleanProperty bosses = new BooleanProperty("bosses", false);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty creepers = new BooleanProperty("creepers", false);
    public final BooleanProperty endermen = new BooleanProperty("endermen", false);
    public final BooleanProperty blazes = new BooleanProperty("blazes", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);

    public NameTagsModule() {
        super("NameTags", false);
    }

    public boolean shouldRenderTags(EntityLivingBase entity) {
        if (entity.deathTime > 0) {
            return false;
        }
        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > 512.0F) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            if (entity != mc.thePlayer && entity != mc.getRenderViewEntity()) {
                EntityPlayer player = (EntityPlayer) entity;
                if (TeamUtil.isBot(player)) {
                    return bots.getValue();
                }
                return players.getValue();
            } else {
                return self.getValue() && mc.gameSettings.thirdPersonView != 0;
            }
        } else if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return entity.isInvisible() ? false : bosses.getValue();
        } else if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntityCreeper) {
                return creepers.getValue();
            } else if (entity instanceof EntityEnderman) {
                return endermen.getValue();
            } else if (entity instanceof EntityBlaze) {
                return blazes.getValue();
            } else {
                return mobs.getValue();
            }
        } else if (entity instanceof EntityAnimal || entity instanceof EntityBat ||
                entity instanceof EntitySquid || entity instanceof EntityVillager) {
            return animals.getValue();
        }
        return false;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) {
            return;
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase livingEntity = (EntityLivingBase) entity;
            if (!shouldRenderTags(livingEntity)) {
                continue;
            }

            String teamName = TeamUtil.stripName(entity);
            if (StringUtils.isBlank(teamName.replaceAll("\u00a7[0-9a-fk-or]", ""))) {
                continue;
            }

            double renderPosX = mc.getRenderManager().viewerPosX;
            double renderPosY = mc.getRenderManager().viewerPosY;
            double renderPosZ = mc.getRenderManager().viewerPosZ;

            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - renderPosX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - renderPosY
                    + entity.getEyeHeight();
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - renderPosZ;

            double distance = mc.getRenderViewEntity().getDistanceToEntity(entity);

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y + (entity.isSneaking() ? 0.225 : 0.4), z);
            GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
            float view = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
            GlStateManager.rotate(mc.getRenderManager().playerViewX, view, 0.0F, 0.0F);

            double scaleValue = Math.pow(Math.min(Math.max(autoScale.getValue() ? distance : 0.0, 6.0), 128.0), 0.75)
                    * 0.0075;
            GlStateManager.scale(-scaleValue * scale.getValue(), -scaleValue * scale.getValue(), 1.0);

            String distanceText = "";
            switch (distanceMode.getValue()) {
                case 1:
                    distanceText = String.format("\u00a77%dm\u00a7r ", (int) distance);
                    break;
                case 2:
                    distanceText = String.format("\u00a7a[\u00a7f%d\u00a7a]\u00a7r ", (int) distance);
                    break;
            }

            float health = livingEntity.getHealth();
            float absorption = livingEntity.getAbsorptionAmount();
            float max = livingEntity.getMaxHealth();
            float percent = Math.min(Math.max((health + absorption) / max, 0.0F), 1.0F);

            String healText = "";
            switch (healthMode.getValue()) {
                case 1:
                    healText = String.format(" %d%s", (int) health,
                            absorption > 0.0F ? String.format(" \u00a76%d\u00a7r", (int) absorption) : "\u00a7r");
                    break;
                case 2:
                    healText = String.format(" %s%s", healthFormatter.format(health / 2.0),
                            absorption > 0.0F
                                    ? String.format(" \u00a76%s\u00a7r", healthFormatter.format(absorption / 2.0))
                                    : "\u00a7r");
                    break;
                case 3:
                    if (entity instanceof EntityPlayer) {
                        Scoreboard scoreboard = mc.theWorld.getScoreboard();
                        if (scoreboard != null) {
                            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);
                            if (objective != null) {
                                Score score = scoreboard.getValueFromObjective(entity.getName(), objective);
                                if (score != null) {
                                    healText = String.format(" \u00a7e%d\u00a7r", score.getScorePoints());
                                }
                            }
                        }
                    }
                    break;
            }

            String displayText = String.format("%s\u00a7f%s\u00a7r%s", distanceText, teamName, healText);
            int width = mc.fontRendererObj.getStringWidth(displayText);

            if (backgroundOpacity.getValue() > 0) {
                Color textColor = !entity.isSneaking() && !entity.isInvisible()
                        ? new Color(0.0F, 0.0F, 0.0F, backgroundOpacity.getValue() / 100.0F)
                        : new Color(0.33F, 0.0F, 0.33F, backgroundOpacity.getValue() / 100.0F);

                drawRect(-width / 2.0F - 1.0F, -mc.fontRendererObj.FONT_HEIGHT - 1.0F,
                        width / 2.0F + (shadow.getValue() ? 1.0F : 0.0F),
                        shadow.getValue() ? 0.0F : -1.0F, textColor.getRGB());
            }

            GlStateManager.disableDepth();
            mc.fontRendererObj.drawString(displayText, -width / 2.0F, -mc.fontRendererObj.FONT_HEIGHT,
                    ColorUtil.getHealthBlend(percent).getRGB(), shadow.getValue());
            GlStateManager.enableDepth();

            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                int yOffset = mc.fontRendererObj.FONT_HEIGHT + 2;

                if (armor.getValue()) {
                    ArrayList<ItemStack> renderingItems = new ArrayList<>();
                    for (int i = 4; i >= 0; i--) {
                        ItemStack itemStack;
                        if (i == 0) {
                            itemStack = player.getHeldItem();
                        } else {
                            itemStack = player.inventory.armorInventory[i - 1];
                        }
                        if (itemStack != null) {
                            renderingItems.add(itemStack);
                        }
                    }
                    if (!renderingItems.isEmpty()) {
                        int offset = renderingItems.size() * -8;
                        for (int i = 0; i < renderingItems.size(); i++) {
                            ItemStack itemStack = renderingItems.get(i);
                            int itemX = offset + i * 16;
                            renderItem(itemStack, itemX, -yOffset - 16);

                            if (enchants.getValue()) {
                                int enchantCount = getEnchantmentCount(itemStack);
                                if (enchantCount != 0) {
                                    // -1 means enchanted but count unknown (Hypixel strips data)
                                    String countText = enchantCount == -1 ? "E" : String.valueOf(enchantCount);
                                    int textWidth = mc.fontRendererObj.getStringWidth(countText);
                                    GlStateManager.pushMatrix();
                                    GlStateManager.scale(0.5F, 0.5F, 1.0F);
                                    GlStateManager.disableDepth();
                                    mc.fontRendererObj.drawString(
                                            countText,
                                            (itemX + 8) * 2 - textWidth / 2,
                                            (-yOffset - 16 - 5) * 2,
                                            0xFF55FF55,
                                            shadow.getValue());
                                    GlStateManager.enableDepth();
                                    GlStateManager.popMatrix();
                                }
                            }
                        }
                        yOffset += 16;
                    }
                }

                if (effects.getValue()) {
                    List<PotionEffect> activeEffects = new ArrayList<>();
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        if (Potion.potionTypes[effect.getPotionID()].hasStatusIcon()) {
                            activeEffects.add(effect);
                        }
                    }
                    if (!activeEffects.isEmpty()) {
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(0.5F, 0.5F, 1.0F);
                        int offset = activeEffects.size() * -9;
                        for (int i = 0; i < activeEffects.size(); i++) {
                            renderPotionEffect(activeEffects.get(i), offset + i * 18, -(yOffset * 2) - 18);
                        }
                        GlStateManager.popMatrix();
                    }
                }
            }

            GlStateManager.popMatrix();
        }
    }

    private void renderItem(ItemStack stack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0F, 1.0F, -0.01F);
        mc.getRenderItem().zLevel = -150.0F;
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x, y);
        mc.getRenderItem().zLevel = 0.0F;
        GlStateManager.popMatrix();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private int getEnchantmentCount(ItemStack stack) {
        if (stack == null) {
            return 0;
        }

        if (stack.hasTagCompound()) {
            NBTTagList enchants = stack.getEnchantmentTagList();
            if (enchants != null && enchants.tagCount() > 0) {
                return enchants.tagCount();
            }
        }

        // Fallback: if item has glint effect but no readable enchant data
        // (Hypixel strips enchant NBT for other players), show "?" indicator
        if (stack.hasEffect()) {
            return -1; // -1 means "enchanted but count unknown"
        }

        return 0;
    }

    private void renderPotionEffect(PotionEffect effect, int x, int y) {
        int iconIndex = Potion.potionTypes[effect.getPotionID()].getStatusIconIndex();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0F, 1.0F, -0.01F);
        mc.getTextureManager()
                .bindTexture(new net.minecraft.util.ResourceLocation("textures/gui/container/inventory.png"));
        net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x, y, iconIndex % 8 * 18,
                198 + iconIndex / 8 * 18, 18, 18, 256.0F, 256.0F);
        GlStateManager.popMatrix();

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private void drawRect(float left, float top, float right, float bottom, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(
                (color >> 16 & 255) / 255.0F,
                (color >> 8 & 255) / 255.0F,
                (color & 255) / 255.0F,
                (color >> 24 & 255) / 255.0F);

        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
