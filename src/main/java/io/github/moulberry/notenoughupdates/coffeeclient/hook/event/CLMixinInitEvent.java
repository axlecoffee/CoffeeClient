package io.github.moulberry.notenoughupdates.coffeeclient.hook.event;

/**
 * Fired after all mod JARs have been injected and mixin classes collected.
 * This is the first lifecycle event a mod receives.
 */
public class CLMixinInitEvent {

    private final int loadedMods;
    private final int registeredMixins;

    public CLMixinInitEvent(int loadedMods, int registeredMixins) {
        this.loadedMods = loadedMods;
        this.registeredMixins = registeredMixins;
    }

    /** Number of mods discovered and instantiated. */
    public int getLoadedMods() {
        return loadedMods;
    }

    /** Number of external mixin classes registered. */
    public int getRegisteredMixins() {
        return registeredMixins;
    }
}
