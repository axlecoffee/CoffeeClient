/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.mixins.AccessorEntityLivingBase;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class NoJumpDelayModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty delay = new IntProperty("delay", 3, 0, 8);

    public NoJumpDelayModule() {
        super("NoJumpDelay", false);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START) {
            return;
        }

        if (mc.thePlayer == null) {
            return;
        }

        AccessorEntityLivingBase accessor = (AccessorEntityLivingBase) mc.thePlayer;
        accessor.setJumpTicks(Math.min(accessor.getJumpTicks(), delay.getValue() + 1));
    }

    @Override
    public String[] getSuffix() {
        return new String[] { delay.getValue().toString() };
    }
}
