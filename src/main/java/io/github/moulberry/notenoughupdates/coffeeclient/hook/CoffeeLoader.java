package io.github.moulberry.notenoughupdates.coffeeclient.hook;

import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLMixinInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLNEUInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLPostInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLPreInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.mixin.CoffeeMixinBootstrap;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.util.Logger;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for the CoffeeClient mod loader.
 * Handles mod discovery, instantiation, and lifecycle event dispatch.
 *
 * <p>
 * Mods are loaded from {@code <gameDir>/coffeeloader/mods/} and must
 * use the {@code .coffeeclient.jar} file extension.
 * </p>
 */
public class CoffeeLoader {

    /** logging = true i am the best developer */
    private static final boolean DO_LOGGING = true;

    private static File gameDir;
    private static File baseDir;
    private static File modsDir;

    private static final List<ModContainer> loadedMods = new ArrayList<>();
    private static boolean modsInstantiated = false;

    private static final String OVERRIDE_MODS_DIR = "F:\\LC-TEST\\coffeemods";

    private static final String OVERRIDE_BASE_DIR = "F:\\LC-TEST";

    /** Called from {@link CoffeeMixinBootstrap} during the mixin phase. */
    public static void earlyInit() {
        Logger.setEnabled(DO_LOGGING);
        gameDir = findGameDir();

        File overrideMods = new File(OVERRIDE_MODS_DIR);
        if (overrideMods.isDirectory()) {
            baseDir = new File(OVERRIDE_BASE_DIR);
            modsDir = overrideMods;
            Logger.setLogFile(new File(baseDir, "coffee-loader.log"));
            Logger.info("Using override mods directory: " + OVERRIDE_MODS_DIR);
        } else {
            baseDir = new File(gameDir, "coffeeloader");
            modsDir = new File(baseDir, "mods");
            Logger.setLogFile(new File(baseDir, "coffee-loader.log"));
        }
    }

    /**
     * Called from the NEU constructor. Instantiates mods and fires early events.
     */
    public static void onNEUConstruct(Object neuInstance) {
        instantiateMods();
        fireEvent(new CLMixinInitEvent(
                loadedMods.size(),
                CoffeeMixinBootstrap.getExternalMixinCount()));
        fireEvent(new CLNEUInitEvent());
    }

    /** Called from NEU preinit. */
    public static void onPreInit() {
        fireEvent(new CLPreInitEvent());
    }

    /** Called from NEU init. */
    public static void onInit() {
        fireEvent(new CLInitEvent());
    }

    /** Called from NEU postinit. */
    public static void onPostInit() {
        fireEvent(new CLPostInitEvent());
    }

    public static File getGameDir() {
        return gameDir;
    }

    public static File getBaseDir() {
        return baseDir;
    }

    public static File getModsDir() {
        if (modsDir == null)
            earlyInit();
        return modsDir;
    }

    public static int getLoadedModCount() {
        return loadedMods.size();
    }

    private static void instantiateMods() {
        if (modsInstantiated)
            return;
        modsInstantiated = true;

        List<CoffeeMixinBootstrap.ModInfo> discovered = CoffeeMixinBootstrap.getDiscoveredMods();
        if (discovered.isEmpty())
            return;

        for (CoffeeMixinBootstrap.ModInfo info : discovered) {
            try {
                Class<?> modClass = Class.forName(info.className);
                Object instance = modClass.getDeclaredConstructor().newInstance();

                ModContainer container = new ModContainer();
                container.name = info.name;
                container.version = info.version;
                container.className = info.className;
                container.instance = instance;
                cacheEventHandlers(container);

                loadedMods.add(container);
                Logger.info("Loaded: " + info.name + " v" + info.version);
            } catch (Exception e) {
                Logger.error("Failed to instantiate: " + info.className, e);
            }
        }
    }

    private static void cacheEventHandlers(ModContainer mod) {
        for (Method m : mod.instance.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(CoffeeMod.EventHandler.class)) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1) {
                    m.setAccessible(true);
                    mod.eventHandlers.put(params[0], m);
                }
            }
        }
    }

    private static void fireEvent(Object event) {
        for (ModContainer mod : loadedMods) {
            Method handler = mod.eventHandlers.get(event.getClass());
            if (handler != null) {
                try {
                    handler.invoke(mod.instance, event);
                } catch (Exception e) {
                    Logger.error("Event handler failed in " + mod.name, e);
                }
            }
        }
    }

    private static File findGameDir() {
        String prop = System.getProperty("minecraft.gameDir");
        if (prop != null && !prop.isEmpty()) {
            File dir = new File(prop);
            if (dir.exists())
                return dir;
        }
        return new File(System.getProperty("user.dir", "."));
    }

    private static class ModContainer {
        String name;
        String version;
        String className;
        Object instance;
        final Map<Class<?>, Method> eventHandlers = new HashMap<>();
    }
}
