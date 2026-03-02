package io.github.moulberry.notenoughupdates.coffeeclient.feature;

import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.LinkedHashMap;

public class FeatureManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    public final LinkedHashMap<Class<?>, Feature> features = new LinkedHashMap<>();

    public Feature getFeature(String name) {
        for (Feature feature : this.features.values()) {
            if (feature.getName().equalsIgnoreCase(name)) {
                return feature;
            }
        }
        return null;
    }

    public Feature getFeature(Class<?> clazz) {
        return this.features.get(clazz);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (mc.currentScreen == null) {
            int keyCode = Keyboard.getEventKey();
            if (keyCode != 0 && Keyboard.getEventKeyState()) {
                for (Feature feature : this.features.values()) {
                    if (feature.getKey() == keyCode) {
                        feature.toggle();
                    }
                }
            }
        }
    }

    public void registerFeature(Class<?> clazz, Feature feature) {
        this.features.put(clazz, feature);
        MinecraftForge.EVENT_BUS.register(feature);
    }
}
