/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.render.BedESP;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher {

    @Inject(method = "renderBlock", at = @At("HEAD"))
    private void onRenderBlock(
            IBlockState state,
            BlockPos pos,
            IBlockAccess blockAccess,
            WorldRenderer worldRenderer,
            CallbackInfoReturnable<Boolean> cir) {
        if (CoffeeClient.featureManager != null) {
            BedESP bedESP = (BedESP) CoffeeClient.featureManager.getFeature(BedESP.class);
            if (bedESP != null && bedESP.isEnabled() &&
                    state.getBlock() instanceof BlockBed &&
                    state.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                bedESP.beds.add(new BlockPos(pos));
            }
        }
    }
}
