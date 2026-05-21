package coffee.axle.coffeeclient.feature.render.bedplates;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class BlockBedScanner {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private BlockBedScanner() {
    }

    public static void scan(
            BlockPos footPos, BlockPos headPos,
            int maxLayers, Map<Block, Integer> layerResult) {

        layerResult.clear();

        boolean facingZ = Math.abs(
                headPos.getZ() - footPos.getZ()) > Math.abs(
                        headPos.getX() - footPos.getX());

        BlockPos[] bedParts = { footPos, headPos };
        int airLayersCount = 0;

        for (int layer = 1; layer <= maxLayers; layer++) {
            Map<Block, Integer> layerCounts = new HashMap<>();
            int totalBlocks = 0;
            int airBlocks = 0;

            for (int part = 0; part < bedParts.length; part++) {
                BlockPos bed = bedParts[part];
                int offset = (part == 0) ? layer : -layer;

                int sX = facingZ
                        ? bed.getX()
                        : bed.getX() + offset;
                int sY = bed.getY();
                int sZ = facingZ
                        ? bed.getZ() + offset
                        : bed.getZ();

                for (int s1 = 0; s1 <= layer; s1++) {
                    int yOff = 0;
                    for (int s2 = s1; s2 >= 0; s2--) {
                        BlockPos p1, p2;
                        if (facingZ) {
                            p1 = new BlockPos(
                                    sX - s2,
                                    sY + yOff,
                                    sZ - (part == 0
                                            ? s1
                                            : -s1));
                            p2 = new BlockPos(
                                    sX + s2,
                                    sY + yOff,
                                    sZ - (part == 0
                                            ? s1
                                            : -s1));
                        } else {
                            p1 = new BlockPos(
                                    sX - (part == 0
                                            ? s1
                                            : -s1),
                                    sY + yOff,
                                    sZ - s2);
                            p2 = new BlockPos(
                                    sX - (part == 0
                                            ? s1
                                            : -s1),
                                    sY + yOff,
                                    sZ + s2);
                        }

                        Block b1 = mc.theWorld
                                .getBlockState(p1)
                                .getBlock();
                        totalBlocks++;
                        if (b1 == Blocks.air) {
                            airBlocks++;
                        } else if (!BlockBedTable.isInvalid(b1)) {
                            layerCounts.merge(
                                    b1, 1,
                                    Integer::sum);
                        }

                        if (!p1.equals(p2)) {
                            Block b2 = mc.theWorld
                                    .getBlockState(p2)
                                    .getBlock();
                            totalBlocks++;
                            if (b2 == Blocks.air) {
                                airBlocks++;
                            } else if (!BlockBedTable.isInvalid(b2)) {
                                layerCounts.merge(
                                        b2, 1,
                                        Integer::sum);
                            }
                        }

                        if (s2 > 0) yOff++;
                    }
                }
            }

            if (totalBlocks == 0) continue;

            float airRatio = (float) airBlocks / totalBlocks;

            if (airRatio > 0.8f) {
                if (++airLayersCount >= 2) break;
                continue;
            }

            airLayersCount = 0;

            for (Map.Entry<Block, Integer> entry : layerCounts.entrySet()) {
                float blockRatio = (float) entry.getValue() / totalBlocks;
                if (blockRatio >= 0.2f) {
                    layerResult.merge(
                            entry.getKey(),
                            entry.getValue(),
                            Integer::sum);
                }
            }
        }
    }
}
