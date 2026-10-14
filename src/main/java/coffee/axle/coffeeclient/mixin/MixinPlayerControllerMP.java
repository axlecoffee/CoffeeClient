package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.events.StopUsingItemEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method = "onStoppedUsingItem", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsingItem(CallbackInfo ci) {
        StopUsingItemEvent event = new StopUsingItemEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
