package coffee.axle.coffeeclient.feature.render;

import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.mixin.AccessorRenderManager;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

public class Trajectories extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty opacity = new IntProperty("opacity", 100, 0, 100);
    public final BooleanProperty bow = new BooleanProperty("bow", true);
    public final BooleanProperty projectiles = new BooleanProperty("projectiles", false);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);

    public Trajectories() {
        super("Trajectories", false, true);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.thePlayer.getHeldItem() == null
                || mc.gameSettings.thirdPersonView != 0) {
            return;
        }

        Item item = mc.thePlayer.getHeldItem().getItem();
        RenderManager renderManager = mc.getRenderManager();
        boolean isBow = false;
        float velocityMultiplier = 1.5F;
        float drag = 0.99F;
        float gravity;
        float hitboxExpand;

        if (item instanceof ItemBow && bow.getValue()) {
            if (!mc.thePlayer.isUsingItem()) {
                return;
            }
            isBow = true;
            gravity = 0.05F;
            hitboxExpand = 0.3F;
            float charge = (float) mc.thePlayer.getItemInUseDuration() / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            if (charge < 0.1F) {
                return;
            }
            if (charge > 1.0F) {
                charge = 1.0F;
            }
            velocityMultiplier = charge * 3.0F;
        } else if (item instanceof ItemFishingRod && projectiles.getValue()) {
            gravity = 0.04F;
            hitboxExpand = 0.25F;
            drag = 0.92F;
        } else if ((item instanceof ItemSnowball || item instanceof ItemEgg) && projectiles.getValue()) {
            gravity = 0.03F;
            hitboxExpand = 0.25F;
        } else {
            if (!(item instanceof ItemEnderPearl) || !pearls.getValue()) {
                return;
            }
            gravity = 0.03F;
            hitboxExpand = 0.25F;
        }

        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        double x = ((AccessorRenderManager) renderManager).getRenderPosX() -
                (double) MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16;
        double y = ((AccessorRenderManager) renderManager).getRenderPosY() +
                (double) mc.thePlayer.getEyeHeight() - 0.1F;
        double z = ((AccessorRenderManager) renderManager).getRenderPosZ() -
                (double) MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16;

        double mx = (double) (MathHelper.sin(yaw / 180.0F * (float) Math.PI) *
                MathHelper.cos(pitch / 180.0F * (float) Math.PI)) * (isBow ? 1.0 : 0.4) * -1.0;
        double my = (double) MathHelper.sin(pitch / 180.0F * (float) Math.PI) *
                (isBow ? 1.0 : 0.4) * -1.0;
        double mz = (double) (MathHelper.cos(yaw / 180.0F * (float) Math.PI) *
                MathHelper.cos(pitch / 180.0F * (float) Math.PI)) * (isBow ? 1.0 : 0.4);

        float mag = MathHelper.sqrt_double(mx * mx + my * my + mz * mz);
        mx /= mag;
        my /= mag;
        mz /= mag;
        mx *= velocityMultiplier;
        my *= velocityMultiplier;
        mz *= velocityMultiplier;

        MovingObjectPosition mop = null;
        boolean hasHitBlock = false;
        boolean hasHitEntity = false;
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        ArrayList<Vec3> trajectoryPoints = new ArrayList<>();

        while (!hasHitBlock && y > 0.0) {
            Vec3 start = new Vec3(x, y, z);
            Vec3 end = new Vec3(x + mx, y + my, z + mz);
            mop = mc.theWorld.rayTraceBlocks(start, end, false, true, false);
            start = new Vec3(x, y, z);
            end = new Vec3(x + mx, y + my, z + mz);

            if (mop != null) {
                hasHitBlock = true;
                end = new Vec3(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
            }

            AxisAlignedBB aabb = new AxisAlignedBB(
                    x - (double) hitboxExpand,
                    y - (double) hitboxExpand,
                    z - (double) hitboxExpand,
                    x + (double) hitboxExpand,
                    y + (double) hitboxExpand,
                    z + (double) hitboxExpand).addCoord(mx, my, mz).expand(1.0, 1.0, 1.0);

            int minChunkX = MathHelper.floor_double((aabb.minX - 2.0) / 16.0);
            int maxChunkX = MathHelper.floor_double((aabb.maxX + 2.0) / 16.0);
            int minChunkZ = MathHelper.floor_double((aabb.minZ - 2.0) / 16.0);
            int maxChunkZ = MathHelper.floor_double((aabb.maxZ + 2.0) / 16.0);
            ArrayList<Entity> possibleEntities = new ArrayList<>();

            for (int x1 = minChunkX; x1 <= maxChunkX; ++x1) {
                for (int z1 = minChunkZ; z1 <= maxChunkZ; ++z1) {
                    mc.theWorld.getChunkFromChunkCoords(x1, z1).getEntitiesWithinAABBForEntity(
                            mc.thePlayer,
                            aabb,
                            possibleEntities,
                            null);
                }
            }

            for (Entity entity : possibleEntities) {
                if (entity.canBeCollidedWith() && entity != mc.thePlayer) {
                    AxisAlignedBB entityBox = entity.getEntityBoundingBox().expand(
                            hitboxExpand,
                            hitboxExpand,
                            hitboxExpand);
                    MovingObjectPosition intercept = entityBox.calculateIntercept(start, end);
                    if (intercept != null) {
                        hasHitEntity = true;
                        hasHitBlock = true;
                        mop = intercept;
                    }
                }
            }

            x += mx;
            y += my;
            z += mz;

            if (mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock().getMaterial() == Material.water) {
                mx *= 0.6;
                my *= 0.6;
                mz *= 0.6;
            } else {
                mx *= drag;
                my *= drag;
                mz *= drag;
            }
            my -= gravity;

            trajectoryPoints.add(
                    new Vec3(
                            x - ((AccessorRenderManager) renderManager).getRenderPosX(),
                            y - ((AccessorRenderManager) renderManager).getRenderPosY(),
                            z - ((AccessorRenderManager) renderManager).getRenderPosZ()));
        }

        if (trajectoryPoints.size() > 1) {
            enableRenderState();
            setColor(new Color(
                    hasHitEntity ? 85 : 255,
                    255,
                    hasHitEntity ? 85 : 255,
                    (int) (opacity.getValue().floatValue() / 100.0F * 255.0F)).getRGB());
            GL11.glLineWidth(1.5F);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

            worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
            trajectoryPoints.forEach(vec3 -> worldRenderer.pos(vec3.xCoord, vec3.yCoord, vec3.zCoord).endVertex());
            Tessellator.getInstance().draw();

            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    x - ((AccessorRenderManager) renderManager).getRenderPosX(),
                    y - ((AccessorRenderManager) renderManager).getRenderPosY(),
                    z - ((AccessorRenderManager) renderManager).getRenderPosZ());

            if (mop != null) {
                switch (mop.sideHit.getAxis().ordinal()) {
                    case 0:
                        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                        break;
                    case 1:
                        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                }
                drawLine(
                        -0.25F,
                        -0.25F,
                        0.25F,
                        0.25F,
                        1.5F,
                        new Color(
                                hasHitEntity ? 85 : 255,
                                255,
                                hasHitEntity ? 85 : 255,
                                (int) (opacity.getValue().floatValue() / 100.0F * 255.0F)).getRGB());
                drawLine(
                        -0.25F,
                        0.25F,
                        0.25F,
                        -0.25F,
                        1.5F,
                        new Color(
                                hasHitEntity ? 85 : 255,
                                255,
                                hasHitEntity ? 85 : 255,
                                (int) (opacity.getValue().floatValue() / 100.0F * 255.0F)).getRGB());
            }

            GlStateManager.popMatrix();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(2.0F);
            GlStateManager.resetColor();
            disableRenderState();
        }
    }

    private void enableRenderState() {
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    private void disableRenderState() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
    }

    private void setColor(int rgb) {
        float alpha = (float) (rgb >> 24 & 0xFF) / 255.0F;
        float red = (float) (rgb >> 16 & 0xFF) / 255.0F;
        float green = (float) (rgb >> 8 & 0xFF) / 255.0F;
        float blue = (float) (rgb & 0xFF) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);
    }

    private void drawLine(float x1, float y1, float x2, float y2, float width, int color) {
        GL11.glLineWidth(width);
        setColor(color);
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1, 0.0).endVertex();
        worldRenderer.pos(x2, y2, 0.0).endVertex();
        Tessellator.getInstance().draw();
    }
}
