package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.render.NameTags;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> {

    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    public void canRenderName(T entityLivingBase, CallbackInfoReturnable<Boolean> cir) {
        if (CoffeeClient.featureManager != null) {
            NameTags nameTags = (NameTags) CoffeeClient.featureManager.getFeature(NameTags.class);
            if (nameTags != null && nameTags.isEnabled() && nameTags.shouldRenderTags(entityLivingBase)) {
                cir.setReturnValue(false);
            }
        }
    }
}
