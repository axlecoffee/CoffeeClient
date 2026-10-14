package coffee.axle.coffeeclient.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface IAccessorMinecraft {
    @Accessor("rightClickDelayTimer")
    int getRightClickDelayTimer();

    @Accessor("rightClickDelayTimer")
    void setRightClickDelayTimer(int timer);
}
