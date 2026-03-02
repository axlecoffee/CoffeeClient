package io.github.moulberry.notenoughupdates.coffeeclient.feature.render.bedplates;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BlockBedTable {

    private static final Set<Block> INVALID;

    static {
        Set<Block> set = new HashSet<>();

        set.add(Blocks.air);
        set.add(Blocks.bed);

        set.add(Blocks.chest);
        set.add(Blocks.trapped_chest);
        set.add(Blocks.ender_chest);

        set.add(Blocks.torch);
        set.add(Blocks.redstone_torch);
        set.add(Blocks.unlit_redstone_torch);
        set.add(Blocks.crafting_table);
        set.add(Blocks.furnace);
        set.add(Blocks.lit_furnace);

        set.add(Blocks.ladder);
        set.add(Blocks.standing_sign);
        set.add(Blocks.wall_sign);
        set.add(Blocks.lever);
        set.add(Blocks.stone_button);
        set.add(Blocks.wooden_button);
        set.add(Blocks.stone_pressure_plate);
        set.add(Blocks.wooden_pressure_plate);
        set.add(Blocks.light_weighted_pressure_plate);
        set.add(Blocks.heavy_weighted_pressure_plate);
        set.add(Blocks.trapdoor);
        set.add(Blocks.iron_trapdoor);
        set.add(Blocks.oak_door);
        set.add(Blocks.spruce_door);
        set.add(Blocks.birch_door);
        set.add(Blocks.jungle_door);
        set.add(Blocks.acacia_door);
        set.add(Blocks.dark_oak_door);
        set.add(Blocks.redstone_wire);
        set.add(Blocks.unpowered_repeater);
        set.add(Blocks.powered_repeater);
        set.add(Blocks.unpowered_comparator);
        set.add(Blocks.powered_comparator);
        set.add(Blocks.dispenser);
        set.add(Blocks.dropper);
        set.add(Blocks.hopper);

        set.add(Blocks.rail);
        set.add(Blocks.activator_rail);
        set.add(Blocks.detector_rail);
        set.add(Blocks.golden_rail);

        set.add(Blocks.tallgrass);
        set.add(Blocks.deadbush);
        set.add(Blocks.water);
        set.add(Blocks.flowing_water);
        set.add(Blocks.lava);
        set.add(Blocks.flowing_lava);
        set.add(Blocks.fire);
        set.add(Blocks.snow_layer);
        set.add(Blocks.cactus);
        set.add(Blocks.reeds);
        set.add(Blocks.leaves);
        set.add(Blocks.leaves2);
        set.add(Blocks.vine);
        set.add(Blocks.red_flower);
        set.add(Blocks.yellow_flower);
        set.add(Blocks.double_plant);
        set.add(Blocks.brown_mushroom);
        set.add(Blocks.red_mushroom);

        set.add(Blocks.piston);
        set.add(Blocks.sticky_piston);
        set.add(Blocks.piston_head);
        set.add(Blocks.piston_extension);
        set.add(Blocks.flower_pot);
        set.add(Blocks.skull);
        set.add(Blocks.anvil);
        set.add(Blocks.cake);
        set.add(Blocks.jukebox);
        set.add(Blocks.daylight_detector);
        set.add(Blocks.daylight_detector_inverted);

        set.add(Blocks.farmland);
        set.add(Blocks.wheat);
        set.add(Blocks.carrots);
        set.add(Blocks.potatoes);

        INVALID = Collections.unmodifiableSet(set);
    }

    private BlockBedTable() {
    }

    public static boolean isInvalid(Block block) {
        return INVALID.contains(block);
    }
}
