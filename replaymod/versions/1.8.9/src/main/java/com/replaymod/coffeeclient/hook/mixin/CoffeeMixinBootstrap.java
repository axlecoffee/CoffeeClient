package com.replaymod.coffeeclient.hook.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.replaymod.coffeeclient.hook.CoffeeMod;
import com.replaymod.coffeeclient.hook.CoffeeLoader;
import com.replaymod.coffeeclient.hook.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class CoffeeMixinBootstrap {

    private static final String COFFEE_MOD_DESC = "Lcom/replaymod/coffeeclient/hook/CoffeeMod;";

    private static final List<String> externalMixins = new ArrayList<>();
    private static final List<ModInfo> discoveredMods = new ArrayList<>();
    private static String mixinPrefix;
    private static boolean initialized = false;

    public static void onLoad(String mixinPackage) {
        if (initialized) return;
        initialized = true;
        mixinPrefix = mixinPackage + ".";
        Logger.info("onLoad: mixinPackage=" + mixinPackage + " -> mixinPrefix=" + mixinPrefix);
        CoffeeLoader.earlyInit();

        File modsDir = CoffeeLoader.getModsDir();
        if (!modsDir.exists()) {
            modsDir.mkdirs();
            return;
        }

        File[] jars = modsDir.listFiles((d, n) -> n.endsWith(".coffeeclient.jar"));
        if (jars == null || jars.length == 0)
            return;

        Logger.info("Found " + jars.length + " mod(s)");
        ClassLoader cl = CoffeeMixinBootstrap.class.getClassLoader();

        for (File jar : jars) {
            try {
                processJar(jar, cl);
            } catch (Exception e) {
                Logger.error("Failed to load: " + jar.getName(), e);
            }
        }

        Logger.info("Loaded " + discoveredMods.size() + " mod(s), "
                + externalMixins.size() + " mixin(s)");
    }

    public static List<String> getMixins() {
        if (externalMixins.isEmpty())
            return null;
        return new ArrayList<>(externalMixins);
    }

    public static List<ModInfo> getDiscoveredMods() {
        return discoveredMods;
    }

    public static int getExternalMixinCount() {
        return externalMixins.size();
    }

    private static void processJar(File jar, ClassLoader cl) throws Exception {
        Logger.info("Processing: " + jar.getName() + " (" + jar.length() + " bytes)");
        URL jarUrl = jar.toURI().toURL();

        try {
            Method addUrl = cl.getClass().getMethod("addURL", URL.class);
            addUrl.invoke(cl, jarUrl);
            Logger.info("addURL OK: " + cl.getClass().getName());
        } catch (Exception e) {
            Logger.error("addURL failed for " + jar.getName() + ": " + e.getMessage());
            return;
        }

        ClassLoader parent = cl.getParent();
        if (parent != null) {
            try {
                Method addUrl = parent.getClass().getMethod("addURL", URL.class);
                addUrl.invoke(parent, jarUrl);
                Logger.info("addURL OK (parent): " + parent.getClass().getName());
            } catch (Exception e) {
                Logger.warn("addURL failed on parent: " + e.getMessage());
            }
        }

        try (JarFile jarFile = new JarFile(jar)) {
            Logger.info("Scanning for @CoffeeMod annotation in: " + jar.getName());
            ModInfo modInfo = scanForAnnotation(jarFile);

            if (modInfo == null) {
                Logger.info("No @CoffeeMod annotation found, falling back to manifest: " + jar.getName());
                modInfo = readManifest(jarFile);
            }

            if (modInfo != null) {
                discoveredMods.add(modInfo);
                Logger.info("Mod: " + modInfo.name + " v" + modInfo.version + " [" + modInfo.className + "]");
            } else {
                Logger.warn("No mod metadata found in: " + jar.getName() + " (skipping)");
            }

            List<String> configs = findMixinConfigs(jarFile);
            Logger.info("Mixin config(s) found: " + configs + " in " + jar.getName());
            for (String configName : configs) {
                collectMixinsFromConfig(jarFile, configName);
            }
        }
    }

    private static ModInfo scanForAnnotation(JarFile jarFile) {
        byte[] marker = COFFEE_MOD_DESC.getBytes(StandardCharsets.UTF_8);

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".class"))
                continue;
            if (entry.getName().startsWith("com/replaymod/replay/mixin/"))
                continue;

            try (InputStream is = jarFile.getInputStream(entry)) {
                byte[] bytes = readBytes(is);

                if (!containsBytes(bytes, marker))
                    continue;

                String path = entry.getName();
                String className = path.substring(0, path.length() - 6).replace('/', '.');

                Class<?> cls = Class.forName(className, false,
                        CoffeeMixinBootstrap.class.getClassLoader());
                CoffeeMod ann = cls.getAnnotation(CoffeeMod.class);
                if (ann == null)
                    continue;

                return new ModInfo(className, ann.name(), ann.version());
            } catch (Exception e) {
                Logger.warn("Error scanning class " + entry.getName() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j])
                    continue outer;
            }
            return true;
        }
        return false;
    }

    private static ModInfo readManifest(JarFile jarFile) throws IOException {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null)
            return null;

        String entry = manifest.getMainAttributes().getValue("CoffeeMod-Entry");
        if (entry == null)
            return null;

        String name = manifest.getMainAttributes().getValue("CoffeeMod-Name");
        String version = manifest.getMainAttributes().getValue("CoffeeMod-Version");
        return new ModInfo(
                entry.trim(),
                name != null ? name : "Unknown",
                version != null ? version : "1.0.0");
    }

    private static List<String> findMixinConfigs(JarFile jarFile) throws IOException {
        List<String> configs = new ArrayList<>();

        Manifest manifest = jarFile.getManifest();
        if (manifest != null) {
            String value = manifest.getMainAttributes().getValue("MixinConfigs");
            if (value != null) {
                for (String cfg : value.split(",")) {
                    cfg = cfg.trim();
                    if (!cfg.isEmpty())
                        configs.add(cfg);
                }
                return configs;
            }
        }

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("mixins.") && name.endsWith(".json") && !name.contains("/")) {
                configs.add(name);
            }
        }
        return configs;
    }

    private static void collectMixinsFromConfig(JarFile jarFile, String configName) {
        JarEntry entry = jarFile.getJarEntry(configName);
        if (entry == null)
            return;

        try (InputStream is = jarFile.getInputStream(entry);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

            JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
            String pkg = "";
            if (config.has("package") && !config.get("package").getAsString().isEmpty()) {
                pkg = config.get("package").getAsString().replace("/", ".") + ".";
            }
            Logger.info("Mixin config: " + configName + " | package=" + (pkg.isEmpty() ? "<empty>" : pkg.substring(0, pkg.length() - 1)));

            collectClassNames(config, "mixins", pkg);
            collectClassNames(config, "client", pkg);
            collectClassNames(config, "server", pkg);
        } catch (Exception e) {
            Logger.error("Failed to parse mixin config: " + configName, e);
        }
    }

    private static void collectClassNames(JsonObject config, String field, String pkg) {
        if (!config.has(field))
            return;
        JsonArray arr = config.getAsJsonArray(field);
        if (arr == null)
            return;

        for (JsonElement elem : arr) {
            String className = elem.getAsString().trim();
            if (className.isEmpty())
                continue;

            String fqcn = pkg + className;
            if (fqcn.startsWith(mixinPrefix)) {
                String relative = fqcn.substring(mixinPrefix.length());
                externalMixins.add(relative);
                Logger.info("  [+] mixin accepted: " + relative + " (fqcn=" + fqcn + ")");
            } else {
                Logger.warn("Mixin not in ReplayMod namespace (missing shadow relocate?): " + fqcn + " (expected prefix: " + mixinPrefix + ")");
            }
        }
    }

    private static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    public static class ModInfo {
        public final String className;
        public final String name;
        public final String version;

        public ModInfo(String className, String name, String version) {
            this.className = className;
            this.name = name;
            this.version = version;
        }
    }
}
