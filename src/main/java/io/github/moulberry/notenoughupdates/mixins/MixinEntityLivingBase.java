package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.coffeeclient.CoffeeClient;
import io.github.moulberry.notenoughupdates.coffeeclient.module.modules.AntiDebuffModule;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {
    @Inject(method = "addPotionEffect", at = @At("HEAD"), cancellable = true)
    private void onAddPotionEffect(PotionEffect effect, CallbackInfo ci) {
        AntiDebuffModule module = (AntiDebuffModule) CoffeeClient.moduleManager.getModule(AntiDebuffModule.class);
        if (module != null && module.isEnabled()) {
            int effectId = effect.getPotionID();
            if ((effectId == Potion.blindness.id && module.blindness.getValue())
                    || (effectId == Potion.confusion.id && module.nausea.getValue())) {
                ci.cancel();
            }
        }
    }
}
