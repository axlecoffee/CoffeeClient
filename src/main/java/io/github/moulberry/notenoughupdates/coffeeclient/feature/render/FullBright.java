package io.github.moulberry.notenoughupdates.coffeeclient.feature.render;

import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.ModeProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FullBright extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private float prevGamma = Float.NaN;
    private boolean appliedNightVision = false;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[] { "GAMMA", "EFFECT" });

    public FullBright() {
        super("FullBright", false);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.phase == TickEvent.Phase.END) {
            switch (mode.getValue()) {
                case 0:
                    mc.gameSettings.gammaSetting = 1000.0F;
                    break;
                case 1:
                    mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 25940, 0));
                    break;
            }
        }
    }

    @Override
    public void onEnabled() {
        switch (mode.getValue()) {
            case 0:
                prevGamma = mc.gameSettings.gammaSetting;
                break;
            case 1:
                appliedNightVision = true;
                break;
        }
    }

    @Override
    public void onDisabled() {
        if (!Float.isNaN(prevGamma)) {
            mc.gameSettings.gammaSetting = prevGamma;
            prevGamma = Float.NaN;
        }
        if (appliedNightVision) {
            if (mc.thePlayer != null) {
                mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
            }
            appliedNightVision = false;
        }
    }
}
