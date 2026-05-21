package coffee.axle.coffeeclient;

import coffee.axle.coffeeclient.command.CommandManager;
import coffee.axle.coffeeclient.command.commands.BindCommand;
import coffee.axle.coffeeclient.command.commands.CoffeeCommand;
import coffee.axle.coffeeclient.command.commands.ConfigCommand;
import coffee.axle.coffeeclient.command.commands.FeatureCommand;
import coffee.axle.coffeeclient.command.commands.HideCommand;
import coffee.axle.coffeeclient.command.commands.ShowCommand;
import coffee.axle.coffeeclient.command.commands.ToggleCommand;
import coffee.axle.coffeeclient.config.Config;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.feature.FeatureManager;
import coffee.axle.coffeeclient.feature.combat.*;
import coffee.axle.coffeeclient.feature.misc.*;
import coffee.axle.coffeeclient.feature.player.*;
import coffee.axle.coffeeclient.feature.render.*;
import coffee.axle.coffeeclient.feature.world.*;
import coffee.axle.coffeeclient.property.Property;
import coffee.axle.coffeeclient.property.PropertyManager;
import coffee.axle.coffeeclient.util.Logger;
import com.replaymod.coffeeclient.hook.CoffeeMod;
import com.replaymod.coffeeclient.hook.event.CLInitEvent;
import com.replaymod.coffeeclient.hook.event.CLMixinInitEvent;
import com.replaymod.coffeeclient.hook.event.CLPostInitEvent;
import com.replaymod.coffeeclient.hook.event.CLPreInitEvent;
import com.replaymod.coffeeclient.hook.event.CLReplayModInitEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;
import java.util.ArrayList;

@CoffeeMod(name = "CoffeeClient", version = "1.0.0")
public class CoffeeClient {

    public static final String CLIENT_NAME = "&7[&bCoffeeClient&7]&r ";
    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("CoffeeClient");

    public static FeatureManager featureManager;
    public static CommandManager commandManager;
    public static PropertyManager propertyManager;

    private static boolean initialized = false;

    @CoffeeMod.EventHandler
    public void onMixinInit(CLMixinInitEvent event) {
        Logger.info("[CoffeeClient] Mixin init — "
                + event.getLoadedMods() + " mod(s), "
                + event.getRegisteredMixins() + " mixin(s)");
    }

    @CoffeeMod.EventHandler
    public void onPreInit(CLPreInitEvent event) {
        if (initialized) return;
        LOGGER.info("Initializing CoffeeClient...");

        featureManager = new FeatureManager();
        commandManager = new CommandManager();
        propertyManager = new PropertyManager();

        registerFeatures();
        registerCommands();
        discoverProperties();

        MinecraftForge.EVENT_BUS.register(featureManager);
        MinecraftForge.EVENT_BUS.register(commandManager);

        new Config("default", false).load();

        initialized = true;
        LOGGER.info("CoffeeClient initialized successfully!");
    }

    @CoffeeMod.EventHandler
    public void onReplayModInit(CLReplayModInitEvent event) {
        LOGGER.info("[CoffeeClient] ReplayMod init");
    }

    @CoffeeMod.EventHandler
    public void onInit(CLInitEvent event) {
        LOGGER.info("[CoffeeClient] Init");
    }

    @CoffeeMod.EventHandler
    public void onPostInit(CLPostInitEvent event) {
        LOGGER.info("[CoffeeClient] Post-init — fully loaded");
    }

    private static void registerFeatures() {
        featureManager.registerFeature(HUD.class, new HUD());
        featureManager.registerFeature(ESP.class, new ESP());
        featureManager.registerFeature(NameTags.class, new NameTags());
        featureManager.registerFeature(ItemESP.class, new ItemESP());
        featureManager.registerFeature(BedESP.class, new BedESP());
        featureManager.registerFeature(Bedplates.class, new Bedplates());
        featureManager.registerFeature(Chams.class, new Chams());
        featureManager.registerFeature(Indicators.class, new Indicators());
        featureManager.registerFeature(Trajectories.class, new Trajectories());
        featureManager.registerFeature(FullBright.class, new FullBright());
        featureManager.registerFeature(AntiObfuscate.class, new AntiObfuscate());
        featureManager.registerFeature(AntiDebuff.class, new AntiDebuff());
        featureManager.registerFeature(AimAssist.class, new AimAssist());
        featureManager.registerFeature(AutoClicker.class, new AutoClicker());
        featureManager.registerFeature(KillAura.class, new KillAura());
        featureManager.registerFeature(WTap.class, new WTap());
        featureManager.registerFeature(Velocity.class, new Velocity());
        featureManager.registerFeature(NoHitDelay.class, new NoHitDelay());
        featureManager.registerFeature(AntiFireball.class, new AntiFireball());
        featureManager.registerFeature(Eagle.class, new Eagle());
        featureManager.registerFeature(InvWalk.class, new InvWalk());
        featureManager.registerFeature(FastPlace.class, new FastPlace());
        featureManager.registerFeature(AutoTool.class, new AutoTool());
        featureManager.registerFeature(NoJumpDelay.class, new NoJumpDelay());
        featureManager.registerFeature(BedTracker.class, new BedTracker());
        featureManager.registerFeature(Test.class, new Test());
    }

    private static void registerCommands() {
        commandManager.commands.add(new CoffeeCommand());
        commandManager.commands.add(new BindCommand());
        commandManager.commands.add(new ToggleCommand());
        commandManager.commands.add(new ConfigCommand());
        commandManager.commands.add(new FeatureCommand());
        commandManager.commands.add(new HideCommand());
        commandManager.commands.add(new ShowCommand());
    }

    private static void discoverProperties() {
        for (Feature feature : featureManager.features.values()) {
            ArrayList<Property<?>> featureProperties = new ArrayList<>();
            for (Field field : feature.getClass().getDeclaredFields()) {
                if (Property.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Property<?> property = (Property<?>) field.get(feature);
                        if (property != null) {
                            featureProperties.add(property);
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Failed to access property field: " + field.getName(), e);
                    }
                }
            }
            if (!featureProperties.isEmpty()) {
                propertyManager.properties.put(feature.getClass(), featureProperties);
                LOGGER.info("Discovered " + featureProperties.size() + " properties for " + feature.getName());
            }
        }
    }
}
