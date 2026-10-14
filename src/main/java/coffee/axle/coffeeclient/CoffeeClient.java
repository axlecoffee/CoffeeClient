package coffee.axle.coffeeclient;

import coffee.axle.coffeeclient.command.CommandManager;
import coffee.axle.coffeeclient.config.Config;
import coffee.axle.coffeeclient.feature.FeatureManager;
import coffee.axle.coffeeclient.property.PropertyManager;
import coffee.axle.coffeeclient.util.SocialList;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;

public class CoffeeClient {

    public static final String CLIENT_NAME = "&7[&bCoffeeClient&7]&r ";
    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("CoffeeClient");

    public static FeatureManager featureManager;
    public static CommandManager commandManager;
    public static PropertyManager propertyManager;

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        LOGGER.info("Initializing CoffeeClient...");

        featureManager = new FeatureManager();
        commandManager = new CommandManager();
        propertyManager = new PropertyManager(featureManager.features.values());

        MinecraftForge.EVENT_BUS.register(featureManager);
        MinecraftForge.EVENT_BUS.register(commandManager);

        SocialList.friends.load();
        SocialList.enemies.load();
        new Config("default", false).load();

        initialized = true;
        LOGGER.info("CoffeeClient initialized successfully!");
    }
}
