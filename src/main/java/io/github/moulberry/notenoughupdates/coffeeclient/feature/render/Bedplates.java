package io.github.moulberry.notenoughupdates.coffeeclient.feature.render;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.render.bedplates.BedBillboardRenderer;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.render.bedplates.BlockBedScanner;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.*;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class Bedplates extends Feature {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty yOffset = new FloatProperty("y-offset", 2f, -5f, 10f);
    public final FloatProperty updateRate = new FloatProperty("update-rate", 1000f, 250f, 5000f);
    public final FloatProperty range = new FloatProperty("range", 0f, 0f, 1000f);
    public final FloatProperty layers = new FloatProperty("layers", 5f, 1f, 10f);
    public final BooleanProperty firstBedOnly = new BooleanProperty("first-bed-only", false);
    public final ModeProperty borderMode = new ModeProperty("border", 0,
            new String[] { "NONE", "HUD" });
    public final FloatProperty borderThickness = new FloatProperty("border-thickness", 1.5f, 0.5f, 5f);
    public final ModeProperty autoScale = new ModeProperty("auto-scale", 1,
            new String[] { "LINEAR", "SUBLINEAR" });
    public final FloatProperty billboardScale = new FloatProperty("scale", 1f, 0.1f, 5f);
    public final FloatProperty itemSize = new FloatProperty("item-size", 16f, 8f, 32f);
    public final ModeProperty backgroundMode = new ModeProperty("background", 1,
            new String[] { "NONE", "DEFAULT", "HUD", "CUSTOM" });
    public final TextProperty backgroundColor = new TextProperty("background-color", "000000");

    private final List<BlockPos> bedFeet = new ArrayList<>();
    private final List<BlockPos> bedHeads = new ArrayList<>();
    private final List<Map<Block, Integer>> bedLayers = new ArrayList<>();
    private long lastUpdateTime = 0;

    public Bedplates() {
        super("BedPlates", false);
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            clearBedData();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (!isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();
        long rate = updateRate.getValue().longValue();
        if (now - lastUpdateTime < rate) return;
        lastUpdateTime = now;

        refreshBedPositions();
        rebuildAllLayers();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (bedFeet.isEmpty()) return;

        boolean firstOnly = firstBedOnly.getValue();
        int limit = firstOnly
                ? Math.min(1, bedFeet.size())
                : bedFeet.size();

        float rangeVal = range.getValue();

        for (int i = 0; i < limit; i++) {
            if (i < bedLayers.size()
                    && !bedLayers.get(i).isEmpty()) {
                BlockPos pos = bedFeet.get(i);
                if (rangeVal > 0) {
                    double dist = mc.thePlayer.getDistance(
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5);
                    if (dist > rangeVal) continue;
                }
                drawPlate(pos, i);
            }
        }
    }

    private void refreshBedPositions() {
        float rangeVal = range.getValue();
        boolean firstOnly = firstBedOnly.getValue();

        CopyOnWriteArraySet<BlockPos> espBeds = getEspBeds();

        pruneDestroyedBeds();
        pruneOutOfRangeBeds(rangeVal);

        if (espBeds != null && !espBeds.isEmpty()) {
            readBedsFromESP(espBeds, rangeVal, firstOnly);
        }
    }

    private CopyOnWriteArraySet<BlockPos> getEspBeds() {
        BedESP bedESP = (BedESP) CoffeeClient.featureManager.getFeature(BedESP.class);
        if (bedESP != null) {
            return bedESP.beds;
        }
        return null;
    }

    private void pruneOutOfRangeBeds(float rangeVal) {
        if (rangeVal <= 0) return;
        Iterator<BlockPos> it = bedFeet.iterator();
        int idx = 0;
        while (it.hasNext()) {
            BlockPos pos = it.next();
            double dist = mc.thePlayer.getDistance(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5);
            if (dist > rangeVal) {
                it.remove();
                if (idx < bedHeads.size()) bedHeads.remove(idx);
                if (idx < bedLayers.size()) bedLayers.remove(idx);
            } else {
                idx++;
            }
        }
    }

    private void pruneDestroyedBeds() {
        Iterator<BlockPos> it = bedFeet.iterator();
        int idx = 0;
        while (it.hasNext()) {
            BlockPos pos = it.next();
            IBlockState state = mc.theWorld.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockBed)) {
                it.remove();
                if (idx < bedHeads.size()) bedHeads.remove(idx);
                if (idx < bedLayers.size()) bedLayers.remove(idx);
            } else {
                idx++;
            }
        }
    }

    private void readBedsFromESP(
            Set<BlockPos> espBeds,
            float rangeVal, boolean firstOnly) {
        Set<Long> knownFeet = new HashSet<>();
        for (BlockPos p : bedFeet) {
            knownFeet.add(p.toLong());
        }

        for (BlockPos pos : espBeds) {
            double dist = mc.thePlayer.getDistance(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5);
            if (rangeVal > 0 && dist > rangeVal) continue;

            IBlockState state = mc.theWorld.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockBed)) continue;

            BlockPos footPos;
            BlockPos headPos;

            if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                footPos = pos;
                headPos = findBedHead(pos, state);
            } else {
                headPos = pos;
                footPos = findBedFoot(pos, state);
                if (footPos == null) continue;
            }

            if (knownFeet.contains(footPos.toLong())) continue;

            if (firstOnly) {
                clearBedData();
                bedFeet.add(footPos);
                bedHeads.add(headPos);
                bedLayers.add(new LinkedHashMap<>());
                return;
            }

            bedFeet.add(footPos);
            bedHeads.add(headPos);
            bedLayers.add(new LinkedHashMap<>());
            knownFeet.add(footPos.toLong());
        }
    }

    private void rebuildAllLayers() {
        int maxLayers = layers.getValue().intValue();

        for (int i = 0; i < bedFeet.size()
                && i < bedLayers.size(); i++) {
            BlockPos footPos = bedFeet.get(i);
            IBlockState footState = mc.theWorld.getBlockState(footPos);
            if (!(footState.getBlock() instanceof BlockBed)) continue;

            BlockPos headPos = i < bedHeads.size()
                    ? bedHeads.get(i) : null;
            if (headPos == null) continue;

            BlockBedScanner.scan(
                    footPos, headPos,
                    maxLayers, bedLayers.get(i));
        }
    }

    private BlockPos findBedHead(BlockPos footPos, IBlockState footState) {
        try {
            EnumFacing facing = footState.getValue(BlockBed.FACING);
            BlockPos headPos = footPos.offset(facing);
            IBlockState headState = mc.theWorld.getBlockState(headPos);
            if (headState.getBlock() instanceof BlockBed
                    && headState.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                return headPos;
            }
        } catch (Exception ignored) {
        }

        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            BlockPos check = footPos.offset(face);
            IBlockState state = mc.theWorld.getBlockState(check);
            if (state.getBlock() instanceof BlockBed
                    && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                return check;
            }
        }
        return null;
    }

    private BlockPos findBedFoot(BlockPos headPos, IBlockState headState) {
        try {
            EnumFacing facing = headState.getValue(BlockBed.FACING);
            BlockPos footPos = headPos.offset(facing.getOpposite());
            IBlockState footState = mc.theWorld.getBlockState(footPos);
            if (footState.getBlock() instanceof BlockBed
                    && footState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                return footPos;
            }
        } catch (Exception ignored) {
        }

        for (EnumFacing face : EnumFacing.HORIZONTALS) {
            BlockPos check = headPos.offset(face);
            IBlockState state = mc.theWorld.getBlockState(check);
            if (state.getBlock() instanceof BlockBed
                    && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                return check;
            }
        }
        return null;
    }

    private void drawPlate(BlockPos footPos, int index) {
        Map<Block, Integer> layerMap = bedLayers.get(index);
        float yShift = yOffset.getValue();

        BlockPos headPos = index < bedHeads.size()
                ? bedHeads.get(index) : null;

        double centerX, centerZ;
        if (headPos != null) {
            centerX = (footPos.getX() + headPos.getX()) / 2.0 + 0.5;
            centerZ = (footPos.getZ() + headPos.getZ()) / 2.0 + 0.5;
        } else {
            centerX = footPos.getX() + 0.5;
            centerZ = footPos.getZ() + 0.5;
        }
        double centerY = footPos.getY() + yShift + 1;

        double distance = mc.thePlayer.getDistance(
                centerX, footPos.getY(), centerZ);

        float maxDist = range.getValue();
        int scaleMode = autoScale.getValue();
        float scaleMultiplier = billboardScale.getValue();
        double baseBillboardScale = BedBillboardRenderer.DEFAULT_BILLBOARD_SCALE
                * scaleMultiplier;
        double scaleFactor = BedBillboardRenderer.computeScale(
                distance, maxDist, scaleMode, baseBillboardScale);

        if (scaleFactor <= 0) return;

        int borderModeVal = borderMode.getValue();
        float borderThicknessVal = borderThickness.getValue();
        Color borderColor = null;
        if (borderModeVal == 1) {
            HUD hud = (HUD) CoffeeClient.featureManager.getFeature(HUD.class);
            if (hud != null) {
                borderColor = hud.getColor(System.currentTimeMillis());
            } else {
                borderColor = new Color(80, 200, 220);
            }
        }

        int itemSizeVal = itemSize.getValue().intValue();

        int bgMode = backgroundMode.getValue();
        Color bgColor = null;
        if (bgMode == BedBillboardRenderer.BG_HUD) {
            HUD hud = (HUD) CoffeeClient.featureManager.getFeature(HUD.class);
            if (hud != null) {
                bgColor = hud.getColor(System.currentTimeMillis());
            } else {
                bgColor = new Color(80, 200, 220);
            }
        } else if (bgMode == BedBillboardRenderer.BG_CUSTOM) {
            String hex = backgroundColor.getValue();
            bgColor = BedBillboardRenderer.parseHexColor(hex);
        }

        BedBillboardRenderer.draw(
                layerMap, centerX, centerY, centerZ,
                scaleFactor, borderColor, borderThicknessVal,
                itemSizeVal, bgMode, bgColor);
    }

    private void clearBedData() {
        bedFeet.clear();
        bedHeads.clear();
        bedLayers.clear();
    }

    @Override
    public void onDisabled() {
        clearBedData();
    }
}
