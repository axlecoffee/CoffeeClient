package coffee.axle.coffeeclient.feature.render;

import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.FloatProperty;
import coffee.axle.coffeeclient.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class Indicators extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty scale = new FloatProperty("scale", 1.0f, 0.5f, 1.5f);
    public final FloatProperty offset = new FloatProperty("offset", 50.0f, 0.0f, 255.0f);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty fireballs = new BooleanProperty("fireballs", true);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);
    public final BooleanProperty arrows = new BooleanProperty("arrows", true);

    public Indicators() {
        super("Indicators", false, true);
    }

    private boolean shouldRender(Entity entity, float partialTicks) {
        double velocityDotProduct = (entity.posX - entity.lastTickPosX) * (mc.thePlayer.posX - entity.posX) +
                (entity.posY - entity.lastTickPosY)
                        * (mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - entity.posY - entity.height / 2.0)
                +
                (entity.posZ - entity.lastTickPosZ) * (mc.thePlayer.posZ - entity.posZ);

        if (velocityDotProduct == 0.0) {
            return false;
        }

        if (velocityDotProduct < 0.0 && directionCheck.getValue()) {
            return false;
        }

        if (fireballs.getValue() && entity instanceof EntityFireball) {
            return true;
        }

        if (pearls.getValue() && entity instanceof EntityEnderPearl) {
            return true;
        }

        if (!arrows.getValue()) {
            return false;
        }

        return entity instanceof EntityArrow;
    }

    private Item getIndicatorItem(Entity entity) {
        if (entity instanceof EntityFireball) {
            return Items.fire_charge;
        }
        if (entity instanceof EntityEnderPearl) {
            return Items.ender_pearl;
        }
        if (entity instanceof EntityArrow) {
            return Items.arrow;
        }
        return null;
    }

    private Color getIndicatorColor(Entity entity) {
        if (entity instanceof EntityFireball) {
            return new Color(12676363);
        }
        if (entity instanceof EntityEnderPearl) {
            return new Color(2458740);
        }
        if (entity instanceof EntityArrow) {
            return new Color(0x969696);
        }
        return new Color(-1);
    }

    private float getYawBetween(double x1, double z1, double x2, double z2) {
        return MathHelper.wrapAngleTo180_float(
                (float) (Math.atan2(z2 - z1, x2 - x1) * 180.0 / Math.PI) - 90.0f - mc.thePlayer.rotationYaw);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        float partialTicks = event.partialTicks;

        List<Entity> entities = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> shouldRender(entity, partialTicks))
                .sorted((e1, e2) -> Double.compare(
                        mc.thePlayer.getDistanceToEntity(e1),
                        mc.thePlayer.getDistanceToEntity(e2)))
                .collect(Collectors.toList());

        for (Entity entity : entities) {
            float baseOffset = 10.0f + offset.getValue();

            double playerX = RenderUtil.lerpDouble(mc.thePlayer.posX, mc.thePlayer.prevPosX, partialTicks);
            double playerZ = RenderUtil.lerpDouble(mc.thePlayer.posZ, mc.thePlayer.prevPosZ, partialTicks);
            double entityX = RenderUtil.lerpDouble(entity.posX, entity.prevPosX, partialTicks);
            double entityZ = RenderUtil.lerpDouble(entity.posZ, entity.prevPosZ, partialTicks);

            float yawBetween = getYawBetween(playerX, playerZ, entityX, entityZ);

            if (mc.gameSettings.thirdPersonView == 2) {
                yawBetween += 180.0f;
            }

            float x = (float) Math.sin(Math.toRadians(yawBetween));
            float z = (float) Math.cos(Math.toRadians(yawBetween)) * -1.0f;

            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.scale(scale.getValue(), scale.getValue(), 0.0f);

            ScaledResolution sr = new ScaledResolution(mc);
            GlStateManager.translate(
                    (float) sr.getScaledWidth() / 2.0f / scale.getValue(),
                    (float) sr.getScaledHeight() / 2.0f / scale.getValue(),
                    0.0f);

            GlStateManager.pushMatrix();
            GlStateManager.translate((baseOffset + 0.0f) * x - 8.0f, (baseOffset + 0.0f) * z - 8.0f, -300.0f);
            Item item = getIndicatorItem(entity);
            if (item != null) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(item), 0, 0);
            }
            GlStateManager.popMatrix();

            String distanceText = String.format("%dm", (int) mc.thePlayer.getDistanceToEntity(entity));

            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    (baseOffset + 0.0f) * x - (float) mc.fontRendererObj.getStringWidth(distanceText) / 2.0f + 1.0f,
                    (baseOffset + 0.0f) * z + 1.0f,
                    -100.0f);
            mc.fontRendererObj.drawStringWithShadow(distanceText, 0.0f, 0.0f, 0x808080 & 0xFFFFFF | 0xBF000000);
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            GlStateManager.translate((baseOffset + 15.0f) * x + 1.0f, (baseOffset + 15.0f) * z + 1.0f, -100.0f);
            RenderUtil.enableRenderState();
            RenderUtil.drawArrow(
                    0.0f,
                    0.0f,
                    (float) (Math.atan2(z, x) + Math.PI),
                    7.5f,
                    1.5f,
                    getIndicatorColor(entity).getRGB());
            RenderUtil.disableRenderState();
            GlStateManager.popMatrix();

            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }
}
