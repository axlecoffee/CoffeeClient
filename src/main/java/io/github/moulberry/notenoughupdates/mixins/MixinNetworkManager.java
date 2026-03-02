/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.coffeeclient.events.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into NetworkManager.channelRead0 to fire receive-side PacketEvents.
 * This allows features like Velocity to intercept incoming S12/S19/S27 packets
 * before they are processed by the client.
 */
@Mixin(value = NetworkManager.class, priority = 9999)
public class MixinNetworkManager {

	@Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
	private void onChannelRead0(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
		// Only intercept server->client packets (play.server)
		if (packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
			PacketEvent event = new PacketEvent(packet, false);
			MinecraftForge.EVENT_BUS.post(event);
			if (event.isCanceled()) {
				ci.cancel();
			}
		}
	}
}
