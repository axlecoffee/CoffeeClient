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

import io.github.moulberry.notenoughupdates.coffeeclient.events.KnockbackEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
	@Shadow public double motionX;
	@Shadow public double motionY;
	@Shadow public double motionZ;

	// Fix NPE in vanilla code, that we need to work for VillagerTradeRecipe
	@Inject(method = "getBrightnessForRender", at = @At("HEAD"), cancellable = true)
	public void onGetBrightnessForRender(float p_getBrightnessForRender_1_, CallbackInfoReturnable<Integer> cir) {
		if (((Entity) (Object) this).worldObj == null)
			cir.setReturnValue(-1);
	}

	// Fix NPE in vanilla code, that we need to work for VillagerTradeRecipe
	@Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
	public void onGetBrightness(float p_getBrightness_1_, CallbackInfoReturnable<Float> cir) {
		if (((Entity) (Object) this).worldObj == null)
			cir.setReturnValue(1.0F);
	}

	/**
	 * Hook Entity.setVelocity to fire KnockbackEvent for the local player.
	 * When the event is cancelled (i.e. velocity was modified), we apply the
	 * modified values instead of the original ones.
	 */
	@Inject(method = "setVelocity", at = @At("HEAD"), cancellable = true)
	private void onSetVelocity(double x, double y, double z, CallbackInfo ci) {
		if ((Entity) (Object) this instanceof EntityPlayerSP) {
			KnockbackEvent event = new KnockbackEvent(x, y, z);
			MinecraftForge.EVENT_BUS.post(event);
			if (event.isCanceled()) {
				ci.cancel();
				this.motionX = event.getX();
				this.motionY = event.getY();
				this.motionZ = event.getZ();
			}
		}
	}
}
