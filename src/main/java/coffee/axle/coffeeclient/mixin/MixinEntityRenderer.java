package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.combat.KillAura;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// makes the first-person hand draw the sword block pose while auto-block fakes it server side
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Unique
    private boolean coffeeclient$spoofed;
    @Unique
    private ItemStack coffeeclient$prevItemInUse;
    @Unique
    private int coffeeclient$prevItemInUseCount;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"))
    private void beforeUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || CoffeeClient.featureManager == null) {
            return;
        }
        KillAura killAura = (KillAura) CoffeeClient.featureManager.getFeature(KillAura.class);
        if (killAura == null || !killAura.isEnabled() || !killAura.shouldRenderBlocking()) {
            return;
        }
        coffeeclient$prevItemInUse = mc.thePlayer.getItemInUse();
        coffeeclient$prevItemInUseCount = mc.thePlayer.getItemInUseCount();
        mc.thePlayer.setItemInUse(mc.thePlayer.inventory.getCurrentItem(), 69000);
        coffeeclient$spoofed = true;
    }

    @Inject(method = "updateCameraAndRender", at = @At("RETURN"))
    private void afterUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (!coffeeclient$spoofed) {
            return;
        }
        Minecraft.getMinecraft().thePlayer.setItemInUse(coffeeclient$prevItemInUse, coffeeclient$prevItemInUseCount);
        coffeeclient$prevItemInUse = null;
        coffeeclient$spoofed = false;
    }
}
