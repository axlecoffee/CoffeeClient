package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.events.LivingUpdateEvent;
import coffee.axle.coffeeclient.events.MoveInputEvent;
import coffee.axle.coffeeclient.events.UpdateEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void onUpdatePre(CallbackInfo callbackInfo) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        if (player.worldObj.isBlockLoaded(new BlockPos(player.posX, 0.0, player.posZ))) {
            MinecraftForge.EVENT_BUS.post(new UpdateEvent(true));
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void onUpdatePost(CallbackInfo callbackInfo) {
        EntityPlayerSP player = (EntityPlayerSP) (Object) this;
        if (player.worldObj.isBlockLoaded(new BlockPos(player.posX, 0.0, player.posZ))) {
            MinecraftForge.EVENT_BUS.post(new UpdateEvent(false));
        }
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V"))
    private void onLivingUpdate(CallbackInfo callbackInfo) {
        MinecraftForge.EVENT_BUS.post(new LivingUpdateEvent());
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift = At.Shift.AFTER))
    private void onMoveInput(CallbackInfo callbackInfo) {
        MinecraftForge.EVENT_BUS.post(new MoveInputEvent());
    }
}
