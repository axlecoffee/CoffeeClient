package com.replaymod.coffeeclient.hook.event;

import java.util.List;

public class CLMixinInitEvent {
    private final List<Object> loadedMods;
    private final List<String> registeredMixins;

    public CLMixinInitEvent(List<Object> loadedMods, List<String> registeredMixins) {
        this.loadedMods = loadedMods;
        this.registeredMixins = registeredMixins;
    }

    public List<Object> getLoadedMods() { return loadedMods; }
    public List<String> getRegisteredMixins() { return registeredMixins; }
}
