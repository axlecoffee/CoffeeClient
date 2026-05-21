package coffee.axle.coffeeclient;

import coffee.axle.coffeeclient.util.Logger;
import com.replaymod.coffeeclient.hook.CoffeeMod;
import com.replaymod.coffeeclient.hook.event.CLInitEvent;
import com.replaymod.coffeeclient.hook.event.CLMixinInitEvent;
import com.replaymod.coffeeclient.hook.event.CLReplayModInitEvent;
import com.replaymod.coffeeclient.hook.event.CLPostInitEvent;
import com.replaymod.coffeeclient.hook.event.CLPreInitEvent;
import net.minecraft.client.Minecraft;

/**
 * CoffeeClient mod.
 *
 * <p>
 * Since {@code remapJar} is disabled in the build, compiled bytecode
 * keeps MCP names — exactly what Ichor's runtime expects. Direct
 * Minecraft imports work without reflection.
 * </p>
 */
@CoffeeMod(name = "CoffeeClient", version = "1.0.0")
public class CoffeeClient {

    @CoffeeMod.EventHandler
    public void onMixinInit(CLMixinInitEvent event) {
        Logger.info("[CoffeeClient] Mixin init — "
                + event.getLoadedMods() + " mod(s), "
                + event.getRegisteredMixins() + " mixin(s)");
    }

    @CoffeeMod.EventHandler
    public void onNEUInit(CLReplayModInitEvent event) {
        Logger.info("[CoffeeClient] ReplayMod init");
    }

    @CoffeeMod.EventHandler
    public void onPreInit(CLPreInitEvent event) {
        Logger.info("[CoffeeClient] Pre-init");
    }

    @CoffeeMod.EventHandler
    public void onInit(CLInitEvent event) {
        Logger.info("[CoffeeClient] Init");
        Logger.info("[CoffeeClient] MC version: " + Minecraft.getMinecraft().getVersion());
    }

    @CoffeeMod.EventHandler
    public void onPostInit(CLPostInitEvent event) {
        Logger.info("[CoffeeClient] Post-init — fully loaded");
    }
}
