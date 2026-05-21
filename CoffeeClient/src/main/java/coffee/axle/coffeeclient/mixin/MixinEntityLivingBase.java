package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.render.AntiDebuff;
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
        AntiDebuff feature = (AntiDebuff) CoffeeClient.featureManager.getFeature(AntiDebuff.class);
        if (feature != null && feature.isEnabled()) {
            int effectId = effect.getPotionID();
            if ((effectId == Potion.blindness.id && feature.blindness.getValue())
                    || (effectId == Potion.confusion.id && feature.nausea.getValue())) {
                ci.cancel();
            }
        }
    }
}
