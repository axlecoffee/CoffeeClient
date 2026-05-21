package com.replaymod.coffeeclient.hook;

import com.replaymod.coffeeclient.hook.event.CLInitEvent;
import com.replaymod.coffeeclient.hook.event.CLMixinInitEvent;
import com.replaymod.coffeeclient.hook.event.CLPostInitEvent;
import com.replaymod.coffeeclient.hook.event.CLPreInitEvent;
import com.replaymod.coffeeclient.hook.event.CLReplayModInitEvent;
import com.replaymod.coffeeclient.hook.mixin.CoffeeMixinBootstrap;
import com.replaymod.coffeeclient.hook.util.Logger;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoffeeLoader {

    private static final boolean DO_LOGGING = true;
    // my personal testing dirs pls dont use these for anything
    private static final String OVERRIDE_MODS_DIR = "F:\\CoffeeClient\\coffeemods";
    private static final String OVERRIDE_BASE_DIR = "F:\\CoffeeClient";

    private static File gameDir;
    private static File baseDir;
    private static File modsDir;

    private static final List<ModContainer> loadedMods = new ArrayList<>();
    private static boolean modsInstantiated = false;
    private static boolean initialized = false;

    public static void earlyInit() {
        if (initialized) return;
        initialized = true;
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
        Logger.info("CoffeeLoader earlyInit. Game dir: " + gameDir);
    }

    public static void onReplayModConstruct(Object replayModBackend) {
        instantiateMods();
        List<String> mixins = CoffeeMixinBootstrap.getMixins();
        fireEvent(new CLMixinInitEvent(
                getLoadedModObjects(),
                mixins != null ? mixins : new ArrayList<String>()));
        fireEvent(new CLReplayModInitEvent(replayModBackend));
    }

    public static void onPreInit() { fireEvent(new CLPreInitEvent()); }
    public static void onInit() { fireEvent(new CLInitEvent()); }
    public static void onPostInit() { fireEvent(new CLPostInitEvent()); }

    public static File getGameDir() { return gameDir; }
    public static File getBaseDir() { return baseDir; }

    public static File getModsDir() {
        if (modsDir == null) earlyInit();
        return modsDir;
    }

    public static int getLoadedModCount() { return loadedMods.size(); }

    private static List<Object> getLoadedModObjects() {
        List<Object> objs = new ArrayList<>();
        for (ModContainer c : loadedMods) objs.add(c.instance);
        return objs;
    }

    private static void instantiateMods() {
        if (modsInstantiated) return;
        modsInstantiated = true;

        List<CoffeeMixinBootstrap.ModInfo> discovered = CoffeeMixinBootstrap.getDiscoveredMods();
        if (discovered.isEmpty()) return;

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
            if (hasEventHandlerAnnotation(m)) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1) {
                    m.setAccessible(true);
                    mod.eventHandlers.put(params[0], m);
                }
            }
        }
    }

    private static boolean hasEventHandlerAnnotation(Method m) {
        for (Annotation a : m.getAnnotations()) {
            String typeName = a.annotationType().getName();
            if (typeName.endsWith("CoffeeMod$EventHandler")) return true;
        }
        return false;
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
            if (dir.exists()) return dir;
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
