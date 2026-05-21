package coffee.axle.coffeeclient.examplemod;

import coffee.axle.coffeeclient.examplemod.util.Logger;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.CoffeeMod;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLMixinInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLNEUInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLPostInitEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLPreInitEvent;
import net.minecraft.client.Minecraft;

/**
 * Example CoffeeClient mod.
 *
 * <p>
 * Since {@code remapJar} is disabled in the build, compiled bytecode
 * keeps MCP names — exactly what Ichor's runtime expects. Direct
 * Minecraft imports work without reflection.
 * </p>
 */
@CoffeeMod(name = "ExampleMod", version = "1.0.0")
public class ExampleMod {

    @CoffeeMod.EventHandler
    public void onMixinInit(CLMixinInitEvent event) {
        Logger.info("[ExampleMod] Mixin init — "
                + event.getLoadedMods() + " mod(s), "
                + event.getRegisteredMixins() + " mixin(s)");
    }

    @CoffeeMod.EventHandler
    public void onNEUInit(CLNEUInitEvent event) {
        Logger.info("[ExampleMod] NEU init");
    }

    @CoffeeMod.EventHandler
    public void onPreInit(CLPreInitEvent event) {
        Logger.info("[ExampleMod] Pre-init");
    }

    @CoffeeMod.EventHandler
    public void onInit(CLInitEvent event) {
        Logger.info("[ExampleMod] Init");
        Logger.info("[ExampleMod] MC version: " + Minecraft.getMinecraft().getVersion());
    }

    @CoffeeMod.EventHandler
    public void onPostInit(CLPostInitEvent event) {
        Logger.info("[ExampleMod] Post-init — fully loaded");
    }
}
