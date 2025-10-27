package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;

public class AntiObfuscateModule extends Module {
    public AntiObfuscateModule() {
        super("AntiObfuscate", false);
    }

    public String stripObfuscated(String input) {
        return input.replaceAll("§k", "");
    }
}
