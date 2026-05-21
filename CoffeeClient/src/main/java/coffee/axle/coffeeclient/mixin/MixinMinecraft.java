package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.events.LeftClickMouseEvent;
import coffee.axle.coffeeclient.feature.combat.NoHitDelay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    private int leftClickCounter;

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onClickMouse(CallbackInfo ci) {
        if (CoffeeClient.featureManager != null) {
            NoHitDelay noHitDelay = (NoHitDelay) CoffeeClient.featureManager.getFeature(NoHitDelay.class);
            if (noHitDelay != null && noHitDelay.isEnabled()) {
                this.leftClickCounter = 0;
            }
        }
        LeftClickMouseEvent event = new LeftClickMouseEvent();
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
