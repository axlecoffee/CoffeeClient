package io.github.moulberry.notenoughupdates.coffeeclient.feature.player;

import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.mixins.AccessorEntityLivingBase;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class NoJumpDelay extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty delay = new IntProperty("delay", 3, 0, 8);

    public NoJumpDelay() {
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
