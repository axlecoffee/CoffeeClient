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
import io.github.moulberry.notenoughupdates.coffeeclient.events.LeftClickMouseEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.combat.NoHitDelay;
import io.github.moulberry.notenoughupdates.miscfeatures.SlotLocking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    private int leftClickCounter;

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onClickMouse(CallbackInfo ci) {
        if (CoffeeClient.featureManager != null) {
            NoHitDelay noHitDelay = (NoHitDelay) CoffeeClient.featureManager
                    .getFeature(NoHitDelay.class);
            if (noHitDelay != null && noHitDelay.isEnabled()) {
                this.leftClickCounter = 0;
            }
        }
        LeftClickMouseEvent event = new LeftClickMouseEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }
    // Commented as they weren't being loaded before
    /*
     * @Shadow
     * public WorldClient theWorld;
     * 
     * @Shadow
     * public EntityRenderer entityRenderer;
     * 
     * @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
     * public void shutdownMinecraftApplet(CallbackInfo ci) {
     * NotEnoughUpdates.INSTANCE.saveConfig();
     * }
     * 
     * @Inject(method =
     * "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
     * at = @At("HEAD"))
     * public void onLoadWorld(WorldClient worldClientIn, String loadingMessage,
     * CallbackInfo ci) {
     * if (worldClientIn != theWorld) {
     * entityRenderer.getMapItemRenderer().clearLoadedMaps();
     * }
     * }
     * 
     * @Redirect(method =
     * "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
     * at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
     * public void loadWorld_gc() {}
     */

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I", opcode = Opcodes.PUTFIELD))
    public void currentItemMixin(CallbackInfo ci) {
        SlotLocking.getInstance().changedSlot(Minecraft.getMinecraft().thePlayer.inventory.currentItem);
    }
}
