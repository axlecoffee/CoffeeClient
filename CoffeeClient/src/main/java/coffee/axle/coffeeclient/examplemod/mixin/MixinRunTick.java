package coffee.axle.coffeeclient.examplemod.mixin;

import coffee.axle.coffeeclient.examplemod.util.Logger;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinRunTick {

    @Unique
    private static boolean logged = false;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(CallbackInfo ci) {
        if (!logged) {
            logged = true;
            Logger.info("[ExampleMod] MIXIN FIRED! Minecraft.runTick() injected from external mod JAR!");
        }
    }
}
