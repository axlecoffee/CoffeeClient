package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.player.ClickOutOfContainers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// vanilla GuiContainer swallows clicks outside the panel, clicking outside closes it like a normal screen
@Mixin(GuiContainer.class)
public class MixinGuiContainer {

    @Shadow
    protected int guiLeft;
    @Shadow
    protected int guiTop;
    @Shadow
    protected int xSize;
    @Shadow
    protected int ySize;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (CoffeeClient.featureManager == null) return;

        ClickOutOfContainers feature = (ClickOutOfContainers) CoffeeClient.featureManager
                .getFeature(ClickOutOfContainers.class);
        if (feature == null || !feature.isEnabled()) return;
        if (mouseButton != 0 && mouseButton != 1) return;

        boolean outside = mouseX < guiLeft || mouseY < guiTop
                || mouseX >= guiLeft + xSize || mouseY >= guiTop + ySize;

        if (outside && Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null) {
            Minecraft.getMinecraft().thePlayer.closeScreen();
            ci.cancel();
        }
    }
}
