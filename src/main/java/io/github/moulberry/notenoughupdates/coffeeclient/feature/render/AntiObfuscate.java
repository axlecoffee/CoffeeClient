package io.github.moulberry.notenoughupdates.coffeeclient.feature.render;

import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;

public class AntiObfuscate extends Feature {
    public AntiObfuscate() {
        super("AntiObfuscate", false);
    }

    public String stripObfuscated(String input) {
        return input.replaceAll("\u00a7k", "");
    }
}
