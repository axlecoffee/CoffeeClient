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

import io.github.moulberry.notenoughupdates.coffeeclient.events.LivingUpdateEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.MoveInputEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.UpdateEvent;
import io.github.moulberry.notenoughupdates.miscfeatures.SlotLocking;
import io.github.moulberry.notenoughupdates.util.SBInfo;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {
	@Inject(method = "dropOneItem", at = @At("HEAD"), cancellable = true)
	public void dropOneItem(CallbackInfoReturnable<EntityItem> ci) {
		if (SBInfo.getInstance().isInDungeon) {
			return;
		}

		int slot = Minecraft.getMinecraft().thePlayer.inventory.currentItem;
		if (SlotLocking.getInstance().isSlotIndexLocked(slot) || SlotLocking.getInstance().isSwapedSlotLocked()) {
			ci.cancel();
			Utils.addChatMessage(
					EnumChatFormatting.RED + "NotEnoughUpdates has prevented you from dropping that locked item!");
		}
	}

	@Inject(method = "onUpdate", at = @At("HEAD"))
	private void onUpdatePre(CallbackInfo callbackInfo) {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		if (player.worldObj.isBlockLoaded(new BlockPos(player.posX, 0.0, player.posZ))) {
			MinecraftForge.EVENT_BUS.post(new UpdateEvent(true));
		}
	}

	@Inject(method = "onUpdate", at = @At("RETURN"))
	private void onUpdatePost(CallbackInfo callbackInfo) {
		EntityPlayerSP player = (EntityPlayerSP) (Object) this;
		if (player.worldObj.isBlockLoaded(new BlockPos(player.posX, 0.0, player.posZ))) {
			MinecraftForge.EVENT_BUS.post(new UpdateEvent(false));
		}
	}

	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V"))
	private void onLivingUpdate(CallbackInfo callbackInfo) {
		MinecraftForge.EVENT_BUS.post(new LivingUpdateEvent());
	}

	@Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift = At.Shift.AFTER))
	private void onMoveInput(CallbackInfo callbackInfo) {
		MinecraftForge.EVENT_BUS.post(new MoveInputEvent());
	}
}
