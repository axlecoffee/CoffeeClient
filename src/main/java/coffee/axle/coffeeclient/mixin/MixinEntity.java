package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.events.KnockbackEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
    @Shadow public double motionX;
    @Shadow public double motionY;
    @Shadow public double motionZ;

    @Inject(method = "getBrightnessForRender", at = @At("HEAD"), cancellable = true)
    public void onGetBrightnessForRender(float p_getBrightnessForRender_1_, CallbackInfoReturnable<Integer> cir) {
        if (((Entity) (Object) this).worldObj == null)
            cir.setReturnValue(-1);
    }

    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    public void onGetBrightness(float p_getBrightness_1_, CallbackInfoReturnable<Float> cir) {
        if (((Entity) (Object) this).worldObj == null)
            cir.setReturnValue(1.0F);
    }

    @Inject(method = "setVelocity", at = @At("HEAD"), cancellable = true)
    private void onSetVelocity(double x, double y, double z, CallbackInfo ci) {
        if ((Entity) (Object) this instanceof EntityPlayerSP) {
            KnockbackEvent event = new KnockbackEvent(x, y, z);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                ci.cancel();
                this.motionX = event.getX();
                this.motionY = event.getY();
                this.motionZ = event.getZ();
            }
        }
    }
}
