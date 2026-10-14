package coffee.axle.coffeeclient.feature.render;

import coffee.axle.coffeeclient.feature.Feature;

public class AntiObfuscate extends Feature {
    public AntiObfuscate() {
        super("AntiObfuscate", false);
    }

    public String stripObfuscated(String input) {
        return input.replaceAll("\u00a7k", "");
    }
}
