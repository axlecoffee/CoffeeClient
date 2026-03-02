package io.github.moulberry.notenoughupdates.coffeeclient.hook.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.CoffeeMod;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.CoffeeLoader;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.util.Logger;

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

/**
 * Handles JAR injection and mixin class collection during
 * Ichor's mixin setup phase. Called from
 * {@link io.github.moulberry.notenoughupdates.envcheck.NEUMixinConfigPlugin}.
 */
public class CoffeeMixinBootstrap {

    private static final String COFFEE_MOD_DESC = "Lio/github/moulberry/notenoughupdates/coffeeclient/hook/CoffeeMod;";

    private static final List<String> externalMixins = new ArrayList<>();
    private static final List<ModInfo> discoveredMods = new ArrayList<>();
    private static String neuMixinPrefix;

    /**
     * Called from {@code NEUMixinConfigPlugin.onLoad()}.
     * Scans the mods directory, injects JARs, collects mixin classes.
     */
    public static void onLoad(String mixinPackage) {
        neuMixinPrefix = mixinPackage + ".";
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

    /** Returns external mixin class names relative to the NEU mixin package. */
    public static List<String> getMixins() {
        if (externalMixins.isEmpty())
            return null;
        return new ArrayList<>(externalMixins);
    }

    /** Returns mod metadata discovered during JAR scanning. */
    public static List<ModInfo> getDiscoveredMods() {
        return discoveredMods;
    }

    /** Returns the number of external mixin classes collected. */
    public static int getExternalMixinCount() {
        return externalMixins.size();
    }

    // =======================================================================

    private static void processJar(File jar, ClassLoader cl) throws Exception {
        URL jarUrl = jar.toURI().toURL();

        // Inject into Ichor classloader
        try {
            Method addUrl = cl.getClass().getMethod("addURL", URL.class);
            addUrl.invoke(cl, jarUrl);
        } catch (Exception e) {
            Logger.error("addURL failed for " + jar.getName() + ": " + e.getMessage());
            return;
        }

        // Inject into parent classloader
        ClassLoader parent = cl.getParent();
        if (parent != null) {
            try {
                Method addUrl = parent.getClass().getMethod("addURL", URL.class);
                addUrl.invoke(parent, jarUrl);
            } catch (Exception ignored) {
            }
        }

        try (JarFile jarFile = new JarFile(jar)) {
            // Discover @CoffeeMod annotated class via ASM
            ModInfo modInfo = scanForAnnotation(jarFile);

            // Fallback: manifest-based discovery
            if (modInfo == null) {
                modInfo = readManifest(jarFile);
            }

            if (modInfo != null) {
                discoveredMods.add(modInfo);
                Logger.info("Mod: " + modInfo.name + " v" + modInfo.version);
            }

            // Collect mixin configs
            for (String configName : findMixinConfigs(jarFile)) {
                collectMixinsFromConfig(jarFile, configName);
            }
        }
    }

    /**
     * Scans all class entries in the JAR for the {@code @CoffeeMod} annotation.
     * Uses a raw byte scan as a pre-filter, then loads matching classes via
     * reflection to read annotation values — avoids any dependency on
     * Mixin's repackaged ASM which varies between Ichor versions.
     */
    private static ModInfo scanForAnnotation(JarFile jarFile) {
        // Annotation descriptor bytes to search for in constant pool
        byte[] marker = COFFEE_MOD_DESC.getBytes(StandardCharsets.UTF_8);

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".class"))
                continue;
            // Skip relocated mixin classes
            if (entry.getName().startsWith("io/github/moulberry/notenoughupdates/mixins/"))
                continue;

            try (InputStream is = jarFile.getInputStream(entry)) {
                byte[] bytes = readBytes(is);

                // Quick pre-filter: check if class bytes contain the descriptor
                if (!containsBytes(bytes, marker))
                    continue;

                // Derive FQCN from entry path
                String path = entry.getName();
                String className = path.substring(0, path.length() - 6).replace('/', '.');

                // Load class and inspect via reflection
                Class<?> cls = Class.forName(className, false,
                        CoffeeMixinBootstrap.class.getClassLoader());
                CoffeeMod ann = cls.getAnnotation(CoffeeMod.class);
                if (ann == null)
                    continue;

                return new ModInfo(className, ann.name(), ann.version());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** Checks if {@code haystack} contains the byte sequence {@code needle}. */
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

    /** Reads mod metadata from the JAR manifest (backward compatibility). */
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

    /** Finds mixin config filenames from manifest or by scanning JAR entries. */
    private static List<String> findMixinConfigs(JarFile jarFile) throws IOException {
        List<String> configs = new ArrayList<>();

        // Check manifest first
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

        // Fallback: scan for mixins.*.json in JAR root
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
            if (fqcn.startsWith(neuMixinPrefix)) {
                String relative = fqcn.substring(neuMixinPrefix.length());
                externalMixins.add(relative);
            } else {
                Logger.warn("Mixin not in NEU namespace (missing shadow relocate?): " + fqcn);
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

    /** Metadata for a discovered mod. */
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
