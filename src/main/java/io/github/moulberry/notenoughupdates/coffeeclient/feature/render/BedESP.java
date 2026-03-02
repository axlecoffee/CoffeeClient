package io.github.moulberry.notenoughupdates.coffeeclient.feature.render;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.*;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RenderUtil;
import io.github.moulberry.notenoughupdates.mixins.AccessorRenderManager;

import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

public class BedESP extends Feature {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final CopyOnWriteArraySet<BlockPos> beds = new CopyOnWriteArraySet<>();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[] { "DEFAULT", "FULL" });
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[] { "CUSTOM", "HUD" });
    public final TextProperty customColor;
    public final IntProperty opacity;
    public final BooleanProperty outline;
    public final BooleanProperty obsidian;

    public BedESP() {
        super("BedESP", false);
        this.customColor = new TextProperty("custom-color", "#FF5555", () -> this.colorMode.getValue() == 0);
        this.opacity = new IntProperty("opacity", 25, 0, 100);
        this.outline = new BooleanProperty("outline", false);
        this.obsidian = new BooleanProperty("obsidian", true);
    }

    private Color parseHexColor(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.startsWith("0x")) {
                hex = hex.substring(2);
            }
            return new Color((int) Long.parseLong(hex, 16), true);
        } catch (Exception e) {
            return new Color(0xFFFF5555, true);
        }
    }

    private Color getColor() {
        switch (this.colorMode.getValue()) {
            case 0:
                return parseHexColor(this.customColor.getValue());
            case 1:
                HUD hud = (HUD) CoffeeClient.featureManager.getFeature(HUD.class);
                if (hud != null) {
                    return hud.getColor(System.currentTimeMillis());
                }
                return Color.WHITE;
            default:
                return Color.WHITE;
        }
    }

    public double getHeight() {
        return this.mode.getValue() == 1 ? 1.0 : 0.5625;
    }

    private void drawObsidian(BlockPos blockPos) {
        if (this.outline.getValue()) {
            RenderUtil.drawBlockBoundingBox(blockPos, 1.0, 170, 0, 170, 255, 1.5F);
        }
        RenderUtil.drawBlockBox(blockPos, 1.0, 170, 0, 170);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        AccessorRenderManager renderManager = (AccessorRenderManager) mc.getRenderManager();

        RenderUtil.enableRenderState();
        for (BlockPos blockPos : this.beds) {
            IBlockState state = mc.theWorld.getBlockState(blockPos);
            if (state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                BlockPos opposite = blockPos.offset(state.getValue(BlockBed.FACING).getOpposite());
                IBlockState oppositeState = mc.theWorld.getBlockState(opposite);

                if (oppositeState.getBlock() instanceof BlockBed
                        && oppositeState.getValue(BlockBed.PART) == EnumPartType.FOOT) {

                    if (this.obsidian.getValue()) {
                        for (EnumFacing facing : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST,
                                EnumFacing.SOUTH, EnumFacing.WEST)) {
                            BlockPos offsetX = blockPos.offset(facing);
                            BlockPos offsetZ = opposite.offset(facing);
                            boolean xObsidian = mc.theWorld.getBlockState(offsetX)
                                    .getBlock() instanceof BlockObsidian;
                            boolean zObsidian = mc.theWorld.getBlockState(offsetZ)
                                    .getBlock() instanceof BlockObsidian;

                            if (xObsidian && zObsidian) {
                                AxisAlignedBB obsidianBox = new AxisAlignedBB(
                                        Math.min(offsetX.getX(), offsetZ.getX()),
                                        offsetX.getY(),
                                        Math.min(offsetX.getZ(), offsetZ.getZ()),
                                        Math.max((double) offsetX.getX() + 1.0, (double) offsetZ.getX() + 1.0),
                                        (double) offsetX.getY() + 1.0,
                                        Math.max((double) offsetX.getZ() + 1.0, (double) offsetZ.getZ() + 1.0))
                                        .offset(
                                                -renderManager.getRenderPosX(),
                                                -renderManager.getRenderPosY(),
                                                -renderManager.getRenderPosZ());

                                if (this.outline.getValue()) {
                                    RenderUtil.drawBoundingBox(obsidianBox, 170, 0, 170, 255, 1.5F);
                                }
                                RenderUtil.drawFilledBox(obsidianBox, 170, 0, 170, 63);
                            } else if (xObsidian) {
                                this.drawObsidian(offsetX);
                            } else if (zObsidian) {
                                this.drawObsidian(offsetZ);
                            }
                        }
                    }

                    AxisAlignedBB aabb = new AxisAlignedBB(
                            Math.min(blockPos.getX(), opposite.getX()),
                            blockPos.getY(),
                            Math.min(blockPos.getZ(), opposite.getZ()),
                            Math.max((double) blockPos.getX() + 1.0, (double) opposite.getX() + 1.0),
                            (double) blockPos.getY() + this.getHeight(),
                            Math.max((double) blockPos.getZ() + 1.0, (double) opposite.getZ() + 1.0))
                            .offset(
                                    -renderManager.getRenderPosX(),
                                    -renderManager.getRenderPosY(),
                                    -renderManager.getRenderPosZ());

                    Color color = this.getColor();
                    int alpha = (int) (this.opacity.getValue() * 2.55f);

                    if (this.outline.getValue()) {
                        RenderUtil.drawBoundingBox(aabb, color.getRed(), color.getGreen(),
                                color.getBlue(), 255, 1.5F);
                    }

                    RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue(), alpha);
                }
            } else {
                this.beds.remove(blockPos);
            }
        }

        RenderUtil.disableRenderState();
    }

    @Override
    public void onEnabled() {
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}
