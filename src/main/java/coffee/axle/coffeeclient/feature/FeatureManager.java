package coffee.axle.coffeeclient.feature;

import coffee.axle.coffeeclient.feature.combat.*;
import coffee.axle.coffeeclient.feature.exploit.*;
import coffee.axle.coffeeclient.feature.misc.*;
import coffee.axle.coffeeclient.feature.player.*;
import coffee.axle.coffeeclient.feature.render.*;
import coffee.axle.coffeeclient.feature.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.LinkedHashMap;

public class FeatureManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    public final LinkedHashMap<Class<?>, Feature> features = new LinkedHashMap<>();

    public FeatureManager() {
        registerFeature(new HUD());
        registerFeature(new ESP());
        registerFeature(new NameTags());
        registerFeature(new ItemESP());
        registerFeature(new BedESP());
        registerFeature(new Bedplates());
        registerFeature(new Chams());
        registerFeature(new Indicators());
        registerFeature(new Trajectories());
        registerFeature(new FullBright());
        registerFeature(new AntiObfuscate());
        registerFeature(new AntiDebuff());
        registerFeature(new AimAssist());
        registerFeature(new AutoClicker());
        registerFeature(new KillAura());
        registerFeature(new WTap());
        registerFeature(new Velocity());
        registerFeature(new NoHitDelay());
        registerFeature(new AntiFireball());
        registerFeature(new Eagle());
        registerFeature(new InvWalk());
        registerFeature(new FastPlace());
        registerFeature(new AutoTool());
        registerFeature(new NoJumpDelay());
        registerFeature(new BedTracker());
        registerFeature(new Chester());
        registerFeature(new ClickOutOfContainers());
        registerFeature(new Test());
    }

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

    public void registerFeature(Feature feature) {
        this.features.put(feature.getClass(), feature);
        MinecraftForge.EVENT_BUS.register(feature);
    }
}
