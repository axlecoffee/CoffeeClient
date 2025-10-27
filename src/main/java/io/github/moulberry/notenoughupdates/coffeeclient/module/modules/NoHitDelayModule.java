package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;

/**
 * NoHitDelay module - removes attack cooldown delay in 1.8.9.
 * This is implemented as an always-on module.
 * The actual hit delay removal would need to be implemented via mixin
 * to reset leftClickCounter in EntityPlayerSP.
 */
public class NoHitDelayModule extends Module {

    public NoHitDelayModule() {
        super("NoHitDelay", true, true);
    }
}
