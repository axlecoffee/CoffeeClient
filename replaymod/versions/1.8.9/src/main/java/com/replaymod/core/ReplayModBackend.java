package com.replaymod.core;

import com.replaymod.coffeeclient.hook.CoffeeLoader;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.forge.EventsAdapter;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.List;

import static com.replaymod.core.ReplayMod.MOD_ID;
import static com.replaymod.core.ReplayMod.jGuiResourcePack;
import static com.replaymod.core.versions.MCVer.getMinecraft;

@Mod(modid = ReplayMod.MOD_ID,
        useMetadata = true,
        acceptableRemoteVersions = "*",
        clientSideOnly = true,
        updateJSON = "https://raw.githubusercontent.com/ReplayMod/ReplayMod/master/versions.json",
        guiFactory = "com.replaymod.core.gui.GuiFactory")
public class ReplayModBackend {
    private final ReplayMod mod = new ReplayMod(this);
    private final EventsAdapter eventsAdapter = new EventsAdapter();

    @Deprecated
    public static Configuration config;

    public ReplayModBackend() {
        CoffeeLoader.onReplayModConstruct(this);
    }

    @EventHandler
    public void init(FMLPreInitializationEvent event) {
        System.out.println("[CoffeeClient] I miss NEU bruh...");
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
        settingsRegistry.backend.setConfiguration(config);
        settingsRegistry.save();
        try {
            CoffeeLoader.onPreInit();
            System.out.println("[CoffeeClient] Pre-initialization complete.");
        } catch (Exception e) {
            System.err.println("[CoffeeLoader] Failed to initialize: " + e.getMessage());
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        mod.initModules();
        eventsAdapter.register();
        try {
            CoffeeLoader.onInit();
            System.out.println("[CoffeeClient] Initialization complete.");
        } catch (Exception e) {
            System.err.println("[CoffeeLoader] Failed to initialize: " + e.getMessage());
        }
    }

    public String getVersion() {
        return Loader.instance().getIndexedModList().get(MOD_ID).getVersion();
    }

    public String getMinecraftVersion() {
        return Loader.MC_VERSION;
    }

    public boolean isModLoaded(String id) {
        return Loader.isModLoaded(id);
    }

    static {
        CoffeeLoader.earlyInit();
        List<IResourcePack> defaultResourcePacks = ((MinecraftAccessor) getMinecraft()).getDefaultResourcePacks();
        if (jGuiResourcePack != null) {
            defaultResourcePacks.add(jGuiResourcePack);
        }
    }
}
